import java.util.*;

public class PlantUMLGenerator {

    public enum VisualizationMode { WHITE_BOX, GRAY_BOX, BLACK_BOX }

    private Set<Component> components;
    private VisualizationMode mode;
    private int grayBoxLevel;
    private int globalMaxDepth;
    private Set<String> allUsedInterfaces = new HashSet<>();

    public PlantUMLGenerator(Set<Component> components, VisualizationMode mode, int grayBoxLevel) {
        this.components = components;
        this.mode = mode;
        this.grayBoxLevel = grayBoxLevel;
        this.globalMaxDepth = computeGlobalMaxDepth(components);
        computeAllUsedInterfaces();
    }

    // Collect all interfaces that are used by implementations and required interfaces.
    private void computeAllUsedInterfaces() {
        for (Component component : components) {
            for (Set<String> impls : component.getClassImplementations().values()) {
                allUsedInterfaces.addAll(impls);
            }
            allUsedInterfaces.addAll(component.getRequiredInterfaces());
        }
    }

    public String generatePlantUML() {
        StringBuilder umlBuilder = new StringBuilder();
        umlBuilder.append("@startuml\n");

        // Generate package structure
        for (Component component : components) {
            generateComponentUML(component, umlBuilder);
        }

        // Add implementation relationships ( -0)- )
        boolean isWhiteBoxMode = (mode == VisualizationMode.WHITE_BOX);
        for (Component component : components) {
            for (Map.Entry<String, Set<String>> entry : component.getClassImplementations().entrySet()) {
                String className = entry.getKey();
                for (String interfaceName : entry.getValue()) {
                    String simpleInterfaceName = interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
                    String packageName = interfaceName.contains(".")
                            ? interfaceName.substring(0, interfaceName.lastIndexOf('.'))
                            : "";

                    if (isWhiteBoxMode) {
                        umlBuilder.append(className)
                                .append(" -0)- ")
                                .append(packageName)
                                .append(" : \"")
                                .append(simpleInterfaceName)
                                .append("\"\n");
                    } else {
                        umlBuilder.append(component.getName())
                                .append(" -() ")
                                .append(simpleInterfaceName)
                                .append("\n");
                    }
                    // Remove this interface from requiredInterfaces to avoid duplicates.
                    component.getRequiredInterfaces().remove(interfaceName);
                }
            }
        }

        // Add relationships between components ( -(0- )
        for (Component component : components) {
            String fromPackageName = component.getName().isEmpty() ? "default" : component.getName();
            for (String requiredInterface : component.getRequiredInterfaces()) {
                for (Component target : components) {
                    if (target.getProvidedInterfaces().contains(requiredInterface)) {
                        String toPackageName = target.getName().isEmpty() ? "default" : target.getName();
                        String simpleInterfaceName = requiredInterface.substring(requiredInterface.lastIndexOf('.') + 1);
                        umlBuilder.append(fromPackageName)
                                .append(" -(0- ")
                                .append(toPackageName)
                                .append(" : \"")
                                .append(simpleInterfaceName)
                                .append("\"\n");
                        break;
                    }
                }
            }
        }

        umlBuilder.append("@enduml\n");
        return umlBuilder.toString();
    }

    // Recursive method to generate package contents.
    // In WHITE_BOX mode, it shows classes (if not provided interfaces) and interfaces (if not used).
    // In GRAY_BOX mode, if details are not hidden, we mimic the same behavior.
    private void generateComponentUML(Component component, StringBuilder umlBuilder) {
        String packageName = component.getName().isEmpty() ? "default" : component.getName();

        if (mode == VisualizationMode.BLACK_BOX) {
            // Black-box: only show package boundaries.
            umlBuilder.append("package ").append(packageName).append(" {\n}\n");
            return;
        }

        // For GRAY_BOX mode, hide details if component's depth is greater than (globalMaxDepth - grayBoxLevel)
        boolean hideDetails = (mode == VisualizationMode.GRAY_BOX && component.getDepth() > (globalMaxDepth - grayBoxLevel));

        umlBuilder.append("package ").append(packageName).append(" {\n");

        if (mode == VisualizationMode.WHITE_BOX || (mode == VisualizationMode.GRAY_BOX && !hideDetails)) {
            // Use white-box logic to display classes and interfaces.
            for (String className : component.getComposedParts()) {
                if (!component.getProvidedInterfaces().contains(className)) {
                    umlBuilder.append("  class ").append(className).append("\n");
                }
            }
            for (String providedInterface : component.getProvidedInterfaces()) {
                if (!allUsedInterfaces.contains(providedInterface)) {
                    umlBuilder.append("  interface ").append(providedInterface).append("\n");
                }
            }
        }
        // Recurse for sub-packages.
        for (Map.Entry<String, Component> entry : component.getSubPackages().entrySet()) {
            generateComponentUML(entry.getValue(), umlBuilder);
        }
        umlBuilder.append("}\n");
    }

    // Compute the maximum depth among all components (including sub-packages).
    private int computeGlobalMaxDepth(Set<Component> comps) {
        int max = 1;
        for (Component comp : comps) {
            max = Math.max(max, getComponentMaxDepth(comp));
        }
        return max;
    }

    private int getComponentMaxDepth(Component comp) {
        int localMax = comp.getDepth();
        for (Component sub : comp.getSubPackages().values()) {
            localMax = Math.max(localMax, getComponentMaxDepth(sub));
        }
        return localMax;
    }
}
