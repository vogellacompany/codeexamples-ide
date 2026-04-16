package com.vogella.ide.parallelstart;

import java.util.Set;

import org.eclipse.osgi.container.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Configures Eclipse platform bundles for parallel activation during startup.
 *
 * <p>
 * By default, Equinox activates bundles sequentially within each start level.
 * When the framework properties {@code equinox.start.level.thread.count} and
 * {@code equinox.start.level.restrict.parallel=true} are set, only bundles
 * explicitly marked via {@link Module#setParallelActivation(boolean)} are
 * activated in parallel.
 * </p>
 *
 * <p>
 * This component marks well-known Eclipse platform bundles as safe for parallel
 * activation. The setting is persisted by the framework, so it only needs to
 * take effect once. On subsequent launches the flags are already stored in the
 * module database.
 * </p>
 *
 * <p>
 * Required framework configuration (e.g. in config.ini or launch args):
 * </p>
 *
 * <pre>
 * equinox.start.level.thread.count=0
 * equinox.start.level.restrict.parallel=true
 * </pre>
 */
@Component(immediate = true)
public class ParallelStartConfigurator {

	/**
	 * Eclipse platform bundles considered safe for parallel activation. These
	 * bundles have thread-safe activators or use lazy/DS activation and do not
	 * depend on a specific activation order beyond what OSGi services guarantee.
	 */
	private static final Set<String> PARALLEL_SAFE_BUNDLES = Set.of(
			// Eclipse core runtime
			"org.eclipse.core.runtime",
			"org.eclipse.core.commands",
			"org.eclipse.core.contenttype",
			"org.eclipse.core.databinding",
			"org.eclipse.core.databinding.beans",
			"org.eclipse.core.databinding.observable",
			"org.eclipse.core.databinding.property",
			"org.eclipse.core.expressions",
			"org.eclipse.core.filesystem",
			"org.eclipse.core.jobs",
			"org.eclipse.core.resources",
			"org.eclipse.core.net",

			// Equinox
			"org.eclipse.equinox.common",
			"org.eclipse.equinox.preferences",
			"org.eclipse.equinox.registry",
			"org.eclipse.equinox.app",
			"org.eclipse.equinox.event",

			// E4 platform
			"org.eclipse.e4.core.commands",
			"org.eclipse.e4.core.contexts",
			"org.eclipse.e4.core.di",
			"org.eclipse.e4.core.di.annotations",
			"org.eclipse.e4.core.di.extensions",
			"org.eclipse.e4.core.di.extensions.supplier",
			"org.eclipse.e4.core.services",
			"org.eclipse.e4.ui.bindings",
			"org.eclipse.e4.ui.css.core",
			"org.eclipse.e4.ui.css.swt",
			"org.eclipse.e4.ui.css.swt.theme",
			"org.eclipse.e4.ui.di",
			"org.eclipse.e4.ui.model.workbench",
			"org.eclipse.e4.ui.services",
			"org.eclipse.e4.ui.workbench",
			"org.eclipse.e4.ui.workbench.addons.swt",
			"org.eclipse.e4.ui.workbench.renderers.swt",
			"org.eclipse.e4.ui.workbench.swt",
			"org.eclipse.e4.ui.workbench3",
			"org.eclipse.e4.emf.xpath",

			// UI and JFace
			"org.eclipse.jface",
			"org.eclipse.jface.databinding",
			"org.eclipse.jface.text",
			"org.eclipse.ui",
			"org.eclipse.ui.forms",
			"org.eclipse.ui.navigator",
			"org.eclipse.ui.views",
			"org.eclipse.ui.workbench",
			"org.eclipse.ui.ide",

			// Text and editors
			"org.eclipse.text",
			"org.eclipse.ui.editors",
			"org.eclipse.ui.genericeditor",
			"org.eclipse.ui.workbench.texteditor",

			// Help
			"org.eclipse.help",
			"org.eclipse.help.base",
			"org.eclipse.help.ui",

			// EMF (commonly used, thread-safe activators)
			"org.eclipse.emf.common",
			"org.eclipse.emf.ecore",
			"org.eclipse.emf.ecore.xmi",

			// Third-party libraries (typically stateless)
			"org.eclipse.equinox.bidi",
			"com.ibm.icu"
	);

	@Activate
	void activate(BundleContext context) {
		int count = 0;
		for (Bundle bundle : context.getBundles()) {
			String bsn = bundle.getSymbolicName();
			if (bsn != null && PARALLEL_SAFE_BUNDLES.contains(bsn)) {
				Module module = bundle.adapt(Module.class);
				if (module != null && !module.isParallelActivated()) {
					module.setParallelActivation(true);
					count++;
				}
			}
		}
		System.out.println("ParallelStartConfigurator: marked " + count
				+ " bundles for parallel activation");
	}
}
