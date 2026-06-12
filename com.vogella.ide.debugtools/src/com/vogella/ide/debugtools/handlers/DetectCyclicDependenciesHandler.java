package com.vogella.ide.debugtools.handlers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import java.util.*;
import jakarta.inject.Named;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.ui.console.*; // Requires 'org.eclipse.ui.console' dependency
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * Eclipse e4 Handler to detect cyclic dependencies between plug-ins in the workspace.
 * Detects cycles from both Require-Bundle and Import-Package dependencies.
 * Uses Johnson's algorithm to enumerate all elementary cycles, each reported exactly once.
 */
public class DetectCyclicDependenciesHandler {

    private static final String CONSOLE_NAME = "Cyclic Dependency Analysis";

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
        try {
            CyclicDependencyDetector detector = new CyclicDependencyDetector();
            List<CycleInfo> cycles = detector.detectCycles();
            
            // Clear and prepare the console
            MessageConsole console = findConsole(CONSOLE_NAME);
            console.clearConsole();
            MessageConsoleStream out = console.newMessageStream();
            
            // Bring Console View to front
            showConsoleView(console);

            if (cycles.isEmpty()) {
                out.println("No cyclic dependencies found in workspace plug-ins.");
                MessageDialog.openInformation(shell, "Cyclic Dependencies", 
                    "No cyclic dependencies found in workspace plug-ins.");
            } else {
                StringBuilder dialogMessage = new StringBuilder();
                dialogMessage.append("Found ").append(cycles.size()).append(" cycle(s). See Console for details.\n\n");
                
                // Console Header
                out.println("=================================================");
                out.println("         CYCLIC DEPENDENCIES DETECTED            ");
                out.println("=================================================");

                for (int i = 0; i < cycles.size(); i++) {
                    CycleInfo cycleInfo = cycles.get(i);
                    
                    // 1. Build string for Dialog (Simplified)
                    dialogMessage.append("Cycle ").append(i + 1).append(": ");
                    dialogMessage.append(cycleInfo.cycle.get(0)).append(" ...\n");

                    // 2. Generate and Print ASCII Art to Eclipse Console
                    out.println("\nCycle " + (i + 1) + ":");
                    out.println(generateAsciiArt(cycleInfo));
                }

                if (detector.isLimitReached()) {
                    out.println("\nNote: more cycles exist; output was truncated at " + cycles.size() + " cycles.");
                }

                out.println("=================================================");

                // Show a dialog, but refer them to the console for the big ASCII art
                MessageDialog.openWarning(shell, "Cyclic Dependencies Detected", 
                    dialogMessage.toString());
            }
        } catch (Exception e) {
            MessageDialog.openError(shell, "Error", 
                "Error detecting cycles: " + e.getMessage());
            Platform.getLog(getClass()).log(new Status(Status.ERROR, 
                "com.vogella.ide.debugtools", "Error detecting cycles", e));
        }
    }

    /**
     * Generates a vertical ASCII art flow for the cycle.
     */
    private String generateAsciiArt(CycleInfo cycleInfo) {
        StringBuilder sb = new StringBuilder();
        List<String> cycle = cycleInfo.cycle;
        
        int maxLen = 0;
        for (String node : cycle) maxLen = Math.max(maxLen, node.length());
        int boxWidth = maxLen + 4; 

        String horizontalBorder = "  +" + "-".repeat(boxWidth - 2) + "+";

        for (int i = 0; i < cycle.size() - 1; i++) {
            String current = cycle.get(i);
            String next = cycle.get(i + 1);
            String type = cycleInfo.getEdgeType(current, next);
            sb.append(horizontalBorder).append("\n");
            sb.append(String.format("  | %-" + (boxWidth - 4) + "s |\n", current));
            sb.append(horizontalBorder).append("\n");
            if (i < cycle.size() - 2) {
                sb.append("      |\n");
                sb.append("      |  [").append(type).append("]\n");
                sb.append("      v\n");
            } else {
                sb.append("      |\n");
                sb.append("      |  [").append(type).append("]\n");
                sb.append("      ^ (Loops back to start)\n");
                sb.append("      |______________________|\n");
            }
        }

        return sb.toString();
    }
    
    /**
     * Finds or creates the console with the given name.
     */
    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (IConsole console : existing) {
            if (name.equals(console.getName())) {
                return (MessageConsole) console;
            }
        }
        
        // No console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[]{myConsole});
        return myConsole;
    }

    /**
     * Forces the Console view to open and display our specific console.
     */
    private void showConsoleView(IConsole myConsole) {
        try {
            org.eclipse.ui.IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                Platform.getLog(getClass()).log(new Status(Status.WARNING, "com.vogella.ide.debugtools", "Could not open console view: no active window."));
                return;
            }
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                Platform.getLog(getClass()).log(new Status(Status.WARNING, "com.vogella.ide.debugtools", "Could not open console view: no active page."));
                return;
            }
            String id = IConsoleConstants.ID_CONSOLE_VIEW;
            IConsoleView view = (IConsoleView) page.showView(id);
            view.display(myConsole);
        } catch (PartInitException e) {
            // Log error if view cannot be opened, but don't fail the whole operation
            Platform.getLog(getClass()).log(new Status(Status.WARNING, 
                "com.vogella.ide.debugtools", "Could not open console view", e));
        }
    }
    
    // --- Nested Helper Classes ---

    private static class CycleInfo {
        List<String> cycle;
        Map<String, String> edgeTypes; 
        
        CycleInfo(List<String> cycle) {
            this.cycle = cycle;
            this.edgeTypes = new HashMap<>();
        }
        
        void addEdge(String from, String to, String type) {
            edgeTypes.put(from + "->" + to, type);
        }
        
        String getEdgeType(String from, String to) {
            return edgeTypes.getOrDefault(from + "->" + to, "unknown");
        }
    }
    
    private static class CyclicDependencyDetector {

        private static final int MAX_CYCLES = 1000;

        // from -> (to -> dependency type of that edge)
        private Map<String, Map<String, String>> dependencyGraph;
        private Map<String, Set<String>> reverseGraph;
        private List<CycleInfo> cycles;
        private boolean limitReached;

        // state of Johnson's circuit search
        private Deque<String> path;
        private Set<String> blocked;
        private Map<String, Set<String>> blockedBy;

        public List<CycleInfo> detectCycles() throws CoreException {
            dependencyGraph = new HashMap<>();
            cycles = new ArrayList<>();
            buildDependencyGraph();
            buildReverseGraph();
            findAllCycles();
            return cycles;
        }

        public boolean isLimitReached() {
            return limitReached;
        }
        
        private void buildDependencyGraph() throws CoreException {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = root.getProjects();
            Map<String, String> packageToBundle = new HashMap<>();
            Map<String, IPluginModelBase> workspaceModels = new HashMap<>();
            
            for (IProject project : projects) {
                if (project.isOpen() && project.hasNature("org.eclipse.pde.PluginNature")) {
                    IPluginModelBase model = PluginRegistry.findModel(project);
                    if (model != null && model.getBundleDescription() != null) {
                        String pluginId = model.getPluginBase().getId();
                        workspaceModels.put(pluginId, model);
                        BundleDescription bundleDesc = model.getBundleDescription();
                        ExportPackageDescription[] exports = bundleDesc.getExportPackages();
                        if (exports != null) {
                            for (ExportPackageDescription export : exports) {
                                packageToBundle.put(export.getName(), pluginId);
                            }
                        }
                    }
                }
            }
            
            for (Map.Entry<String, IPluginModelBase> entry : workspaceModels.entrySet()) {
                String pluginId = entry.getKey();
                IPluginModelBase model = entry.getValue();
                Map<String, String> dependencies = new HashMap<>();
                BundleDescription bundleDesc = model.getBundleDescription();
                if (bundleDesc != null) {
                    BundleSpecification[] requiredBundles = bundleDesc.getRequiredBundles();
                    if (requiredBundles != null) {
                        for (BundleSpecification spec : requiredBundles) {
                            String depId = spec.getName();
                            if (workspaceModels.containsKey(depId)) {
                                dependencies.putIfAbsent(depId, "Require-Bundle");
                            }
                        }
                    }
                    ImportPackageSpecification[] importedPackages = bundleDesc.getImportPackages();
                    if (importedPackages != null) {
                        for (ImportPackageSpecification importSpec : importedPackages) {
                            String packageName = importSpec.getName();
                            String providingBundle = packageToBundle.get(packageName);
                            if (providingBundle != null && !providingBundle.equals(pluginId)) {
                                dependencies.putIfAbsent(providingBundle, "Import-Package: " + packageName);
                            }
                        }
                    }
                }
                dependencyGraph.put(pluginId, dependencies);
            }
        }

        private void buildReverseGraph() {
            reverseGraph = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : dependencyGraph.entrySet()) {
                for (String target : entry.getValue().keySet()) {
                    reverseGraph.computeIfAbsent(target, k -> new HashSet<>()).add(entry.getKey());
                }
            }
        }
        
        /**
         * Johnson's algorithm: for each vertex (in sorted order) enumerate all
         * cycles through it within its strongly connected component, then
         * remove it from the graph. Every elementary cycle is found exactly
         * once, rooted at its lexicographically smallest plug-in.
         */
        private void findAllCycles() {
            List<String> vertices = new ArrayList<>(dependencyGraph.keySet());
            Collections.sort(vertices);
            Set<String> removed = new HashSet<>();
            for (String start : vertices) {
                if (limitReached) {
                    return;
                }
                Map<String, String> edges = dependencyGraph.get(start);
                if (edges.containsKey(start)) {
                    CycleInfo selfCycle = new CycleInfo(List.of(start, start));
                    selfCycle.addEdge(start, start, edges.get(start));
                    addCycle(selfCycle);
                }
                Set<String> component = strongComponentOf(start, removed);
                if (component.size() > 1) {
                    path = new ArrayDeque<>();
                    blocked = new HashSet<>();
                    blockedBy = new HashMap<>();
                    circuit(start, start, component);
                }
                removed.add(start);
            }
        }

        /**
         * Strongly connected component containing start, ignoring removed
         * vertices: the intersection of the vertices reachable from start and
         * the vertices from which start is reachable.
         */
        private Set<String> strongComponentOf(String start, Set<String> removed) {
            Set<String> forward = new HashSet<>();
            collectReachable(start, removed, forward, false);
            Set<String> backward = new HashSet<>();
            collectReachable(start, removed, backward, true);
            forward.retainAll(backward);
            return forward;
        }

        private void collectReachable(String start, Set<String> removed, Set<String> seen, boolean reverse) {
            Deque<String> work = new ArrayDeque<>();
            seen.add(start);
            work.push(start);
            while (!work.isEmpty()) {
                String node = work.pop();
                Collection<String> targets = reverse
                        ? reverseGraph.getOrDefault(node, Collections.emptySet())
                        : dependencyGraph.getOrDefault(node, Collections.emptyMap()).keySet();
                for (String next : targets) {
                    if (!removed.contains(next) && seen.add(next)) {
                        work.push(next);
                    }
                }
            }
        }

        private boolean circuit(String node, String start, Set<String> component) {
            boolean foundCycle = false;
            path.addLast(node);
            blocked.add(node);
            for (String next : dependencyGraph.get(node).keySet()) {
                if (!component.contains(next) || limitReached) {
                    continue;
                }
                if (next.equals(start)) {
                    // path.size() == 1 is the self-loop, already recorded in findAllCycles()
                    if (path.size() > 1) {
                        recordCycle();
                        foundCycle = true;
                    }
                } else if (!blocked.contains(next) && circuit(next, start, component)) {
                    foundCycle = true;
                }
            }
            if (foundCycle) {
                unblock(node);
            } else {
                for (String next : dependencyGraph.get(node).keySet()) {
                    if (component.contains(next)) {
                        blockedBy.computeIfAbsent(next, k -> new HashSet<>()).add(node);
                    }
                }
            }
            path.removeLast();
            return foundCycle;
        }

        private void unblock(String node) {
            blocked.remove(node);
            Set<String> dependents = blockedBy.remove(node);
            if (dependents != null) {
                for (String dependent : dependents) {
                    if (blocked.contains(dependent)) {
                        unblock(dependent);
                    }
                }
            }
        }

        private void recordCycle() {
            List<String> cycleList = new ArrayList<>(path);
            cycleList.add(cycleList.get(0));
            CycleInfo cycleInfo = new CycleInfo(cycleList);
            for (int i = 0; i < cycleList.size() - 1; i++) {
                String from = cycleList.get(i);
                String to = cycleList.get(i + 1);
                cycleInfo.addEdge(from, to, dependencyGraph.get(from).get(to));
            }
            addCycle(cycleInfo);
        }

        private void addCycle(CycleInfo cycleInfo) {
            if (cycles.size() >= MAX_CYCLES) {
                limitReached = true;
                return;
            }
            cycles.add(cycleInfo);
        }
    }
}