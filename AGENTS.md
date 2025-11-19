# AGENTS.md

This file provides guidance to AI coding assistants (Claude Code, GitHub Copilot, etc.) when working with code in this repository.

## Project Overview

This is an Eclipse Plugin Development (PDE) repository containing educational examples and working plugins for Eclipse IDE extensions. The project demonstrates Eclipse RCP development, custom editors with TextMate grammars, OSGi services, Language Server Protocol integration, and Eclipse 4 application model concepts.

**Build System**: Maven with Eclipse Tycho 5.0.0
**Target Platform**: Eclipse 2025-09 release
**Java Version**: Java SE-21 (required, not optional)
**Framework**: Eclipse RCP, OSGi, E4 Application Model

## Build Commands

### Critical: Java Version Requirement

**You MUST verify Java 21 before any build operation:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Must show Java 21
```

Using Java 17 will cause build failures with errors like "Type TychoFeatureMapping not present" or "UnsupportedClassVersionError". Tycho 5.0.0 requires Java 21.

### Standard Build Commands

```bash
# Clean build entire project
mvn clean compile

# Build specific module
mvn clean compile -pl <module-name>

# Run tests
mvn test

# Full verification (includes packaging)
mvn clean verify

# Package update site
mvn clean verify -pl updatesite
```

### Working Module Subset

Some modules have dependency conflicts. To build reliably, use this subset:
```bash
mvn clean compile -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page,com.vogella.ide.feature,com.vogella.tasks.events,com.vogella.tasks.services,com.vogella.swt.widgets,com.vogella.tasks.ui
```

### Running Tests

Tests require headless display server. Note that Tycho executes tests during the `compile` phase, not just `test` or `verify`.

```bash
# Setup Xvfb for headless testing
Xvfb :99 &
export DISPLAY=:99

# Run specific test module
mvn test -pl com.vogella.tasks.services.tests
mvn test -pl com.vogella.ide.editor.asciidoc.tests
```

## Architecture Overview

### Core Application Pattern

This project demonstrates the **Eclipse 4 Application Model** approach:

1. **Model Layer** (`com.vogella.tasks.model`): Plain Java domain objects with JavaBeans property change support
   - `Task.java`: Core domain entity with PropertyChangeSupport
   - `TaskService.java`: Service interface for CRUD operations

2. **Service Layer** (`com.vogella.tasks.services`): OSGi services with Eclipse DI integration
   - Services are registered using E4 context functions (see `TaskServiceContextFunction.java`)
   - Event notification via `IEventBroker` for task creation/update/deletion
   - Pattern: `TransientTaskServiceImpl` uses `@Inject` for IEventBroker dependency

3. **UI Layer** (`com.vogella.tasks.ui`): Eclipse 4 parts and handlers
   - Uses E4 model fragments (`fragment.e4xmi`, `commands.e4xmi`)
   - UI components declared via model fragments, not extension points
   - Dependency injection throughout UI components

### Custom Editor Architecture

Editors use Eclipse's **Generic Editor** framework with TextMate grammar integration:

**Key components** (example: `com.vogella.ide.editor.asciidoc`):
1. Content type definition in `plugin.xml`
2. TextMate grammar JSON in `syntaxes/` directory
3. Language configuration JSON in `language-configurations/`
4. Extension points:
   - `org.eclipse.tm4e.registry.grammars` - syntax highlighting
   - `org.eclipse.ui.genericeditor.contentAssistProcessors` - code completion
   - `org.eclipse.ui.workbench.texteditor.hyperlinkDetectors` - hyperlink navigation
   - `org.eclipse.ui.genericeditor.hoverProviders` - hover information

**Compare/Merge Support** (recent addition):
- Custom merge viewers extend `TextMergeViewer` (see `AsciidocMergeViewer.java`)
- Registered via `org.eclipse.compare.contentMergeViewers` extension point
- Links merge viewer to content type using `contentTypeBinding`

### Language Server Integration

Pattern demonstrated in `com.vogella.lsp.asciidoc.server`:
- Uses LSP4J library for Language Server Protocol implementation
- Eclipse LSP4E for client-side integration
- Separate bundles for server and client components

### OSGi Service Registration Pattern

Services are registered using **E4 context functions** instead of traditional DS:

```java
// See TaskServiceContextFunction.java
@Override
public Object compute(IEclipseContext context, String contextKey) {
    // Create service instance
    TransientTaskServiceImpl service = ContextInjectionFactory.make(TransientTaskServiceImpl.class, context);
    // Register in context
    context.set(TaskService.class, service);
    return service;
}
```

Registered in `OSGI-INF/component.xml` with `service.context.key` property.

### Event Communication Pattern

Task operations publish events via `IEventBroker`:
```java
broker.post(TaskEventConstants.TOPIC_TASKS_NEW,
    Map.of(TaskEventConstants.TOPIC_TASKS_NEW, task.getId()));
```

UI components subscribe to these events via `@EventTopic` injection.

## Module Structure

**Task Management Sample Application:**
- `com.vogella.tasks.model` - Domain model (Task, TaskService interface)
- `com.vogella.tasks.services` - Service implementations with OSGi/E4 integration
- `com.vogella.tasks.ui` - Eclipse 4 UI parts (viewers, editors, handlers)
- `com.vogella.tasks.events` - Event topic constants
- `com.vogella.tasks.services.tests` - JUnit 5 tests using fragment host pattern

**Custom Editors:**
- `com.vogella.ide.editor.asciidoc` - AsciiDoc editor with tm4e, content assist, hyperlinks, compare mode
- `com.vogella.ide.editor.gradle` - Gradle file editor
- `com.vogella.ide.editor.tasks` - Task file format editor
- `com.vogella.ide.editor.shell` - Shell script editor

**Language Server Examples:**
- `com.vogella.lsp.asciidoc.server` - LSP4J-based AsciiDoc language server
- `com.vogella.lsp.asciidoc.client` - Eclipse LSP4E client integration
- `com.vogella.languageserver.dart` - Dart language server integration

**Utility/Example Modules:**
- `com.vogella.ide.first` - Basic Eclipse plugin example
- `com.vogella.swt.widgets` - SWT widget demonstrations
- `com.vogella.preferences.page` - Preference page contribution example
- `com.vogella.resources` - Resource API examples
- `com.vogella.adapters` - Adapter pattern demonstrations
- `com.vogella.eclipse.css` - CSS styling examples

**Build Modules:**
- `com.vogella.ide.feature` - Feature definition aggregating plugins
- `updatesite` - P2 update site generation
- `target-platform` - Target platform definition

## Key Configuration Files

**Bundle Configuration:**
- `META-INF/MANIFEST.MF` - OSGi bundle manifest (dependencies, exports, bundle metadata)
- `plugin.xml` - Eclipse extension point contributions
- `build.properties` - Tycho build configuration (source folders, binary includes)

**E4 Model Fragments:**
- `fragment.e4xmi` - UI model contributions (parts, menus, toolbars)
- `commands.e4xmi` - Command and handler definitions
- Files in Application.e4xmi format but contributed as fragments

**Build Configuration:**
- `pom.xml` - Maven/Tycho build configuration
- `target-platform/target-platform.target` - Eclipse platform dependencies

## Testing

**Test Framework**: JUnit 5 (Jupiter API)
**Pattern**: Fragment host testing (tests in separate bundle that extends host)

Test bundles:
- `com.vogella.tasks.services.tests` - Tests for task service implementations
- `com.vogella.ide.editor.asciidoc.tests` - Tests for AsciiDoc editor features

Fragment host pattern in `MANIFEST.MF`:
```
Fragment-Host: com.vogella.tasks.services
```

This gives tests access to host bundle's internal packages while maintaining separation.

## Known Build Issues

1. **TextMate dependency conflicts** - `com.vogella.ide.editor.asciidoc` may fail if target platform doesn't include compatible tm4e bundles
2. **Workbench version mismatches** - `com.vogella.contribute.parts` requires specific Eclipse version alignment
3. **Java version errors** - Always verify Java 21 is active; Java 17 causes Tycho class version errors

When encountering build failures, use the working module subset listed in Build Commands section.

## Target Platform

Defined in `target-platform/target-platform.target`:
- Eclipse Platform 2025-09 (primary)
- Eclipse JDT 2024-12
- TextMate support (tm4e) - latest release
- LSP4E 0.27.8
- Google Gson 2.11.0 (via Maven coordinates)

## Development Notes

**When adding new editor support:**
1. Create content type in `plugin.xml`
2. Add TextMate grammar JSON to `syntaxes/` directory
3. Add language configuration JSON to `language-configurations/`
4. Bind grammar to content type via `org.eclipse.tm4e.registry.grammars`
5. Optionally add content assist, hover providers, hyperlink detectors

**When adding compare/merge support:**
1. Extend `TextMergeViewer` class
2. Create viewer creator implementing `IContentMergeViewerCreator`
3. Register via `org.eclipse.compare.contentMergeViewers` extension
4. Bind to content type using `contentTypeBinding`

**When adding new OSGi services:**
1. Define service interface in model bundle
2. Implement service with `@Inject` for dependencies
3. Create context function extending `ContextFunction`
4. Register via OSGI-INF/component.xml with `service.context.key` property

**CI/CD**: GitHub Actions workflow runs `mvn clean verify` on Java 21 with Xvfb for UI tests
