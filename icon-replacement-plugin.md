# Icon Replacement Plugin Implementation

## Overview

This plugin replaces icons in the running Eclipse installation by patching existing
bundles using `Bundle.update(InputStream)` — no file locking issues since content
is streamed directly into Equinox's internal bundle cache. After patching, the IDE
restarts with `-clean -clearPersistedState` to apply changes.

Icon packs are contributed via an extension point, pointing to an
[`icon-mapping.json`](https://github.com/eclipse-platform/ui-best-practices/blob/main/iconpacks/eclipse-dual-tone/icon-mapping.json)
file and a folder of replacement SVG files.

The mapping format is:

```json
{
  "terminal.svg": [
    "org.eclipse.ui.console/icons/full/eview/console_view.svg"
  ]
}
```

Each key is a filename in the icon pack folder; each value is an array of
`bundleSymbolicName/path` pairs to replace.

A similar (workspace-based) approach exists in `com.vogella.ide.icons.dualtone`.

---

## 1. Extension Point (`com.vogella.ide.iconreplacer`)

Define `schema/iconpack.exsd`:

```xml
<element name="iconpack">
  <attribute name="mappingFile" type="string" use="required">
    <!-- Bundle-relative path or absolute URL to icon-mapping.json -->
  </attribute>
  <attribute name="iconFolder" type="string" use="required">
    <!-- Bundle-relative path to the folder containing replacement SVG files -->
  </attribute>
</element>
```

`plugin.xml` registration:

```xml
<extension-point
    id="iconpack"
    name="Icon Pack"
    schema="schema/iconpack.exsd"/>
```

Consumers contribute like this:

```xml
<extension point="com.vogella.ide.iconreplacer.iconpack">
  <iconpack
      mappingFile="iconpacks/eclipse-dual-tone/icon-mapping.json"
      iconFolder="iconpacks/eclipse-dual-tone/icons/"/>
</extension>
```

---

## 2. Plugin Structure

```
com.vogella.ide.iconreplacer/
  META-INF/MANIFEST.MF
  plugin.xml
  schema/iconpack.exsd
  src/com/vogella/ide/iconreplacer/
    IconReplacerHandler.java   ← e4 handler, triggered by a command/menu entry
    BundlePatcher.java         ← patches a single bundle JAR
    RestartHelper.java         ← restarts with -clean -clearPersistedState
```

**`MANIFEST.MF`** — key dependencies:

```
Require-Bundle:
 org.eclipse.core.runtime,
 org.eclipse.ui,
 org.eclipse.equinox.app,
 com.google.gson
```

---

## 3. Reading Extension Point Contributions

```java
IExtensionRegistry registry = Platform.getExtensionRegistry();
IConfigurationElement[] elements =
    registry.getConfigurationElementsFor("com.vogella.ide.iconreplacer.iconpack");

for (IConfigurationElement element : elements) {
    Bundle contributor = Platform.getBundle(element.getContributor().getName());
    String mappingFile = element.getAttribute("mappingFile");
    String iconFolder  = element.getAttribute("iconFolder");

    URL mappingUrl = contributor.getEntry(mappingFile);
    URL iconFolderUrl = contributor.getEntry(iconFolder);
    // ... parse and apply
}
```

---

## 4. Parsing the Mapping and Patching Bundles

**`BundlePatcher.java`**

```java
// Parse icon-mapping.json with Gson
Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
Map<String, List<String>> mapping = new Gson().fromJson(reader, type);

BundleContext ctx = FrameworkUtil.getBundle(BundlePatcher.class).getBundleContext();

for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
    String iconFileName = entry.getKey();          // e.g. "terminal.svg"
    URL replacement = iconFolderUrl.toURI().resolve(iconFileName).toURL();

    for (String targetPath : entry.getValue()) {   // e.g. "org.eclipse.ui.console/icons/..."
        int slash = targetPath.indexOf('/');
        String bsn      = targetPath.substring(0, slash);          // bundle symbolic name
        String iconPath = targetPath.substring(slash + 1);         // path inside bundle

        Bundle bundle = findBundle(ctx, bsn);
        if (bundle == null) continue;

        try (InputStream patched = rebuildJar(bundle, iconPath, replacement)) {
            bundle.update(patched);   // streams into Equinox cache — no file lock issue
        }
    }
}

// Refresh all updated bundles
FrameworkWiring wiring = ctx.getBundle(0).adapt(FrameworkWiring.class);
wiring.refreshBundles(null);
```

**`rebuildJar`** — copies the existing bundle JAR into a new in-memory JAR, replacing
the target entry:

```java
private InputStream rebuildJar(Bundle bundle, String replacedPath, URL replacement)
        throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (ZipOutputStream zout = new ZipOutputStream(buffer)) {
        // Copy all existing entries except the one being replaced
        URL location = bundle.getEntry("/");
        try (ZipInputStream zin = new ZipInputStream(
                new URL(bundle.getLocation()).openStream())) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals(replacedPath)) {
                    replacement.openStream().transferTo(zout);
                } else {
                    zin.transferTo(zout);
                }
                zout.closeEntry();
            }
        }
    }
    return new ByteArrayInputStream(buffer.toByteArray());
}
```

> **Note:** `bundle.getLocation()` returns a `reference:file:` URL pointing to Equinox's
> internal cache copy of the JAR, not the original installation path. This is safe on all
> platforms — no file locking concerns even on Windows.

---

## 5. Restart with `-clean -clearPersistedState`

**`RestartHelper.java`**

```java
public static void restartWithClean() {
    List<String> commands = new ArrayList<>();
    String existingCmds = System.getProperty("eclipse.commands", "");
    for (String line : existingCmds.split("[\r\n]+")) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty()
                && !trimmed.equals("-clean")
                && !trimmed.equals("-clearPersistedState")) {
            commands.add(trimmed);
        }
    }
    commands.add("-clean");
    commands.add("-clearPersistedState");

    System.setProperty("eclipse.exitcode", "24"); // 24 = RESTART
    System.setProperty("eclipse.exitdata", String.join("\n", commands));

    PlatformUI.getWorkbench().restart();
}
```

Call this after `wiring.refreshBundles(null)` completes.

---

## 6. Handler (`IconReplacerHandler.java`)

Wire everything together as an e4 handler:

```java
@Execute
public void execute(Shell shell) {
    boolean confirmed = MessageDialog.openConfirm(shell,
        "Apply Icon Pack",
        "This will patch bundle JARs and restart Eclipse. Continue?");
    if (!confirmed) return;

    try {
        for (/* each extension point contribution */) {
            BundlePatcher.apply(mappingUrl, iconFolderUrl);
        }
        RestartHelper.restartWithClean();
    } catch (Exception e) {
        MessageDialog.openError(shell, "Icon Replacement Failed", e.getMessage());
    }
}
```

Expose via a `plugin.xml` command and menu contribution in the usual e4 way.
