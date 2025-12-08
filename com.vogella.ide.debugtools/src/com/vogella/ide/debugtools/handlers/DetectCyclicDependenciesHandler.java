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
import org.osgi.framework.Constants;

/**
 * Eclipse e4 Handler to detect cyclic dependencies between plug-ins in the workspace.
 * Detects cycles from both Require-Bundle and Import-Package dependencies.
 */
public class DetectCyclicDependenciesHandler {

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
        try {
            CyclicDependencyDetector detector = new CyclicDependencyDetector();
            List<CycleInfo> cycles = detector.detectCycles();
            
            if (cycles.isEmpty()) {
                MessageDialog.openInformation(shell, "Cyclic Dependencies", 
                    "No cyclic dependencies found in workspace plug-ins.");
            } else {
                StringBuilder message = new StringBuilder();
                message.append("Found ").append(cycles.size()).append(" cycle(s):\n\n");
                
                for (int i = 0; i < cycles.size(); i++) {
                    CycleInfo cycleInfo = cycles.get(i);
                    message.append("Cycle ").append(i + 1).append(":\n");
                    List<String> cycle = cycleInfo.cycle;
                    for (int j = 0; j < cycle.size() - 1; j++) {
                        message.append("  ").append(cycle.get(j));
                        String depType = cycleInfo.getEdgeType(cycle.get(j), cycle.get(j + 1));
                        message.append(" -[").append(depType).append("]-> \n");
                    }
                    message.append("\n");
                }
                
                MessageDialog.openWarning(shell, "Cyclic Dependencies Detected", 
                    message.toString());
            }
        } catch (Exception e) {
            MessageDialog.openError(shell, "Error", 
                "Error detecting cycles: " + e.getMessage());
            Platform.getLog(getClass()).log(new Status(Status.ERROR, 
                "com.vogella.ide.debugtools", "Error detecting cycles", e));
        }
    }
    
    /**
     * Holds information about a cycle including the edge types
     */
    private static class CycleInfo {
        List<String> cycle;
        Map<String, String> edgeTypes; // Key: "from->to", Value: "Require-Bundle" or "Import-Package: pkg.name"
        
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
    
    /**
     * Core logic for detecting cyclic dependencies
     */
    private static class CyclicDependencyDetector {
        private Map<String, Set<DependencyEdge>> dependencyGraph;
        private Set<String> visited;
        private Set<String> recursionStack;
        private List<CycleInfo> cycles;
        private Map<String, String> parent;
        private Map<String, DependencyEdge> parentEdge;
        
        private static class DependencyEdge {
            String target;
            String type; // "Require-Bundle" or "Import-Package: package.name"
            
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
            
            // First pass: collect all workspace bundles and their exported packages
            Map<String, String> packageToBundle = new HashMap<>();
            Map<String, IPluginModelBase> workspaceModels = new HashMap<>();
            
            for (IProject project : projects) {
                if (project.isOpen() && project.hasNature("org.eclipse.pde.PluginNature")) {
                    IPluginModelBase model = PluginRegistry.findModel(project);
                    if (model != null && model.getBundleDescription() != null) {
                        String pluginId = model.getPluginBase().getId();
                        workspaceModels.put(pluginId, model);
                        
                        // Collect exported packages
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
            
            // Second pass: build dependency graph
            for (Map.Entry<String, IPluginModelBase> entry : workspaceModels.entrySet()) {
                String pluginId = entry.getKey();
                IPluginModelBase model = entry.getValue();
                Set<DependencyEdge> dependencies = new HashSet<>();
                
                BundleDescription bundleDesc = model.getBundleDescription();
                if (bundleDesc != null) {
                    // Get Require-Bundle dependencies
                    BundleSpecification[] requiredBundles = bundleDesc.getRequiredBundles();
                    if (requiredBundles != null) {
                        for (BundleSpecification spec : requiredBundles) {
                            String depId = spec.getName();
                            if (workspaceModels.containsKey(depId)) {
                                dependencies.add(new DependencyEdge(depId, "Require-Bundle"));
                            }
                        }
                    }
                    
                    // Get Import-Package dependencies
                    ImportPackageSpecification[] importedPackages = bundleDesc.getImportPackages();
                    if (importedPackages != null) {
                        for (ImportPackageSpecification importSpec : importedPackages) {
                            String packageName = importSpec.getName();
                            String providingBundle = packageToBundle.get(packageName);
                            
                            // Only add if it's a workspace bundle and not self-import
                            if (providingBundle != null && !providingBundle.equals(pluginId)) {
                                dependencies.add(new DependencyEdge(
                                    providingBundle, 
                                    "Import-Package: " + packageName
                                ));
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
                        // Cycle detected
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
            path.addFirst(current); // Start with the node where recursion found cycle

            // Reconstruct path from 'current' back to 'cycleStart'
            String node = current;
            while (!node.equals(cycleStart)) {
                node = parent.get(node);
                path.addFirst(node);
            }
            // Now 'path' is [cycleStart, ..., current]

            // Create cycle list for CycleInfo: [cycleStart, ..., current, cycleStart]
            List<String> cycleList = new ArrayList<>(path);
            cycleList.add(cycleStart); // Close the cycle

            CycleInfo cycleInfo = new CycleInfo(cycleList);

            // Populate edge types for the cycle
            // Edges from cycleStart to current
            for (int i = 0; i < path.size() - 1; i++) {
                String from = path.get(i);
                String to = path.get(i + 1);
                // The edge that leads to 'to' from 'from'
                DependencyEdge edge = parentEdge.get(to); 
                cycleInfo.addEdge(from, to, edge.type);
            }

            // The final edge from 'current' back to 'cycleStart'
            cycleInfo.addEdge(current, cycleStart, finalEdge.type);

            return cycleInfo;
        }
        
        private boolean isDuplicateCycle(CycleInfo newCycleInfo) {
            List<String> normalized = normalizeCycle(newCycleInfo.cycle);
            
            for (CycleInfo existingCycleInfo : cycles) {
                List<String> normalizedExisting = normalizeCycle(existingCycleInfo.cycle);
                if (normalized.equals(normalizedExisting)) {
                    return true;
                }
            }
            return false;
        }
        
        private List<String> normalizeCycle(List<String> cycle) {
            if (cycle.size() <= 1) return new ArrayList<>(cycle);
            
            // Remove the duplicate last element for comparison
            List<String> temp = new ArrayList<>(cycle.subList(0, cycle.size() - 1));
            
            // Find the minimum element
            int minIndex = 0;
            for (int i = 1; i < temp.size(); i++) {
                if (temp.get(i).compareTo(temp.get(minIndex)) < 0) {
                    minIndex = i;
                }
            }
            
            // Rotate to start with minimum element
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < temp.size(); i++) {
                normalized.add(temp.get((minIndex + i) % temp.size()));
            }
            normalized.add(normalized.get(0)); // Add back the duplicate to complete the cycle
            
            return normalized;
        }
    }
}