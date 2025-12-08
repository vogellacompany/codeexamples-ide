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

            sb.append("      |\n");
            sb.append("      |  [").append(type).append("]\n");
            sb.append("      v\n");
        }

        String lastNode = cycle.get(cycle.size() - 1);
        sb.append(horizontalBorder).append("\n");
        sb.append(String.format("  | %-" + (boxWidth - 4) + "s |\n", lastNode));
        sb.append(horizontalBorder).append("\n");
        
        sb.append("      ^ (Loops back to start)\n");
        sb.append("      |______________________|\n");

        return sb.toString();
    }
    
    /**
     * Finds or creates the console with the given name.
     */
    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        
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
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            String id = IConsoleConstants.ID_CONSOLE_VIEW;
            IConsoleView view = (IConsoleView) page.showView(id);
            view.display(myConsole);
        } catch (PartInitException e) {
            // Log error if view cannot be opened, but don't fail the whole operation
            Platform.getLog(getClass()).log(new Status(Status.WARNING, 
                "com.vogella.ide.debugtools", "Could not open console view", e));
        }
    }
    
    // --- Nested Helper Classes (CycleInfo, CyclicDependencyDetector) remain unchanged ---
    
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
        private Map<String, Set<DependencyEdge>> dependencyGraph;
        private Set<String> visited;
        private Set<String> recursionStack;
        private List<CycleInfo> cycles;
        private Map<String, String> parent;
        private Map<String, DependencyEdge> parentEdge;
        
        private static class DependencyEdge {
            String target;
            String type;
            
            DependencyEdge(String target, String type) {
                this.target = target;
                this.type = type;
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                DependencyEdge that = (DependencyEdge) o;
                return Objects.equals(target, that.target) && Objects.equals(type, that.type);
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(target, type);
            }
        }
        
        public List<CycleInfo> detectCycles() throws CoreException {
            dependencyGraph = new HashMap<>();
            cycles = new ArrayList<>();
            buildDependencyGraph();
            findAllCycles();
            return cycles;
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
                Set<DependencyEdge> dependencies = new HashSet<>();
                BundleDescription bundleDesc = model.getBundleDescription();
                if (bundleDesc != null) {
                    BundleSpecification[] requiredBundles = bundleDesc.getRequiredBundles();
                    if (requiredBundles != null) {
                        for (BundleSpecification spec : requiredBundles) {
                            String depId = spec.getName();
                            if (workspaceModels.containsKey(depId)) {
                                dependencies.add(new DependencyEdge(depId, "Require-Bundle"));
                            }
                        }
                    }
                    ImportPackageSpecification[] importedPackages = bundleDesc.getImportPackages();
                    if (importedPackages != null) {
                        for (ImportPackageSpecification importSpec : importedPackages) {
                            String packageName = importSpec.getName();
                            String providingBundle = packageToBundle.get(packageName);
                            if (providingBundle != null && !providingBundle.equals(pluginId)) {
                                dependencies.add(new DependencyEdge(providingBundle, "Import-Package: " + packageName));
                            }
                        }
                    }
                }
                dependencyGraph.put(pluginId, dependencies);
            }
        }
        
        private void findAllCycles() {
            visited = new HashSet<>();
            for (String plugin : dependencyGraph.keySet()) {
                if (!visited.contains(plugin)) {
                    recursionStack = new HashSet<>();
                    parent = new HashMap<>();
                    parentEdge = new HashMap<>();
                    detectCycleFromNode(plugin);
                }
            }
        }
        
        private void detectCycleFromNode(String node) {
            visited.add(node);
            recursionStack.add(node);
            Set<DependencyEdge> dependencies = dependencyGraph.get(node);
            if (dependencies != null) {
                for (DependencyEdge edge : dependencies) {
                    String dep = edge.target;
                    if (!visited.contains(dep)) {
                        parent.put(dep, node);
                        parentEdge.put(dep, edge);
                        detectCycleFromNode(dep);
                    } else if (recursionStack.contains(dep)) {
                        CycleInfo cycle = extractCycle(node, dep, edge);
                        if (!isDuplicateCycle(cycle)) {
                            cycles.add(cycle);
                        }
                    }
                }
            }
            recursionStack.remove(node);
        }
        
        private CycleInfo extractCycle(String current, String cycleStart, DependencyEdge finalEdge) {
            LinkedList<String> path = new LinkedList<>();
            path.addFirst(current); 
            String node = current;
            while (!node.equals(cycleStart)) {
                node = parent.get(node);
                path.addFirst(node);
            }
            List<String> cycleList = new ArrayList<>(path);
            cycleList.add(cycleStart); 
            CycleInfo cycleInfo = new CycleInfo(cycleList);
            for (int i = 0; i < path.size() - 1; i++) {
                String from = path.get(i);
                String to = path.get(i + 1);
                DependencyEdge edge = parentEdge.get(to); 
                cycleInfo.addEdge(from, to, edge.type);
            }
            cycleInfo.addEdge(current, cycleStart, finalEdge.type);
            return cycleInfo;
        }
        
        private boolean isDuplicateCycle(CycleInfo newCycleInfo) {
            List<String> normalized = normalizeCycle(newCycleInfo.cycle);
            for (CycleInfo existingCycleInfo : cycles) {
                List<String> normalizedExisting = normalizeCycle(existingCycleInfo.cycle);
                if (normalized.equals(normalizedExisting)) return true;
            }
            return false;
        }
        
        private List<String> normalizeCycle(List<String> cycle) {
            if (cycle.size() <= 1) return new ArrayList<>(cycle);
            List<String> temp = new ArrayList<>(cycle.subList(0, cycle.size() - 1));
            int minIndex = 0;
            for (int i = 1; i < temp.size(); i++) {
                if (temp.get(i).compareTo(temp.get(minIndex)) < 0) minIndex = i;
            }
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < temp.size(); i++) {
                normalized.add(temp.get((minIndex + i) % temp.size()));
            }
            normalized.add(normalized.get(0));
            return normalized;
        }
    }
}