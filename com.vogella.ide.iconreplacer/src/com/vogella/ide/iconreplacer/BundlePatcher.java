package com.vogella.ide.iconreplacer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.FileLocator;
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

		// Group all replacements by bundle symbolic name so we rebuild each JAR once
		Map<String, Map<String, URL>> replacementsByBundle = new HashMap<>();

		for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
			String iconFileName = entry.getKey();
			URL replacement = new URL(iconFolderUrl, iconFileName);

			for (String targetPath : entry.getValue()) {
				int slash = targetPath.indexOf('/');
				if (slash < 0) {
					continue;
				}
				String bsn = targetPath.substring(0, slash);
				String iconPath = targetPath.substring(slash + 1);

				replacementsByBundle
						.computeIfAbsent(bsn, k -> new HashMap<>())
						.put(iconPath, replacement);
			}
		}

		for (Map.Entry<String, Map<String, URL>> bundleEntry : replacementsByBundle.entrySet()) {
			String bsn = bundleEntry.getKey();
			Map<String, URL> replacements = bundleEntry.getValue();

			Bundle bundle = findBundle(ctx, bsn);
			if (bundle == null) {
				continue;
			}

			try (InputStream patched = rebuildJar(bundle, replacements)) {
				bundle.update(patched);
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

	private static InputStream rebuildJar(Bundle bundle, Map<String, URL> replacements)
			throws Exception {
		File bundleFile = FileLocator.getBundleFileLocation(bundle)
				.orElseThrow(() -> new IOException("Cannot locate bundle file for " + bundle.getSymbolicName()));

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (ZipOutputStream zout = new ZipOutputStream(buffer);
				ZipInputStream zin = new ZipInputStream(new FileInputStream(bundleFile))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				zout.putNextEntry(new ZipEntry(entry.getName()));
				URL replacement = replacements.get(entry.getName());
				if (replacement != null) {
					try (InputStream in = replacement.openStream()) {
						in.transferTo(zout);
					}
				} else {
					zin.transferTo(zout);
				}
				zout.closeEntry();
			}
		}
		return new ByteArrayInputStream(buffer.toByteArray());
	}
}
