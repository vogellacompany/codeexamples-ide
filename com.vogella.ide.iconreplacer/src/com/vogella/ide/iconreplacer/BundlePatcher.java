package com.vogella.ide.iconreplacer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.FrameworkWiring;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BundlePatcher {

	public static void apply(URL mappingUrl, URL iconFolderUrl) throws Exception {
		Map<String, List<String>> mapping;
		try (InputStreamReader reader = new InputStreamReader(mappingUrl.openStream(), StandardCharsets.UTF_8)) {
			Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
			mapping = new Gson().fromJson(reader, type);
		}

		BundleContext ctx = FrameworkUtil.getBundle(BundlePatcher.class).getBundleContext();

		for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
			String iconFileName = entry.getKey();
			URL replacement = iconFolderUrl.toURI().resolve(iconFileName).toURL();

			for (String targetPath : entry.getValue()) {
				int slash = targetPath.indexOf('/');
				if (slash < 0) {
					continue;
				}
				String bsn = targetPath.substring(0, slash);
				String iconPath = targetPath.substring(slash + 1);

				Bundle bundle = findBundle(ctx, bsn);
				if (bundle == null) {
					continue;
				}

				try (InputStream patched = rebuildJar(bundle, iconPath, replacement)) {
					bundle.update(patched);
				}
			}
		}

		FrameworkWiring wiring = ctx.getBundle(0).adapt(FrameworkWiring.class);
		wiring.refreshBundles(null);
	}

	private static Bundle findBundle(BundleContext ctx, String symbolicName) {
		for (Bundle b : ctx.getBundles()) {
			if (symbolicName.equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	private static InputStream rebuildJar(Bundle bundle, String replacedPath, URL replacement)
			throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (ZipOutputStream zout = new ZipOutputStream(buffer)) {
			try (ZipInputStream zin = new ZipInputStream(new URL(bundle.getLocation()).openStream())) {
				ZipEntry entry;
				while ((entry = zin.getNextEntry()) != null) {
					zout.putNextEntry(new ZipEntry(entry.getName()));
					if (entry.getName().equals(replacedPath)) {
						try (InputStream in = replacement.openStream()) {
							in.transferTo(zout);
						}
					} else {
						zin.transferTo(zout);
					}
					zout.closeEntry();
				}
			}
		}
		return new ByteArrayInputStream(buffer.toByteArray());
	}
}
