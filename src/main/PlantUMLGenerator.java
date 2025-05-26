package main;

import java.util.*;

public class PlantUMLGenerator implements UMLGenerator {

    private Set<String> allUsedInterfaces = new HashSet<>();

    public PlantUMLGenerator() {
    }

    // Collect all interfaces that are used by implementations and required interfaces.
    private void computeAllUsedInterfaces(Set<Component> components) {
        allUsedInterfaces.clear();
        for (Component component : components) {
            for (Set<String> impls : component.getClassImplementations().values()) {
                allUsedInterfaces.addAll(impls);
            }
            allUsedInterfaces.addAll(component.getRequiredInterfaces());
        }
    }

    @Override
    public String generateUML(Set<Component> components, VisualizationMode mode, int grayBoxLevel, int globalMaxDepth) {
        computeAllUsedInterfaces(components);
        StringBuilder umlBuilder = new StringBuilder();
        umlBuilder.append("@startuml\n");

        // Generate package structure
        Set<String> processedPackages = new HashSet<>();
        // process root packages
        for (Component component : components) {
            if (isRootPackage(component, components)) {
                generateComponentUML(component, umlBuilder, processedPackages, components, mode, grayBoxLevel, globalMaxDepth, true);
            }
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
    private void generateComponentUML(Component component, StringBuilder umlBuilder, Set<String> processedPackages,Set<Component> components, VisualizationMode mode, int grayBoxLevel, int globalMaxDepth, boolean isRootCall) {
        String packageName = component.getName().isEmpty() ? "default" : component.getName();

        // Skip already processed packages
        if (processedPackages.contains(packageName)) return;
        processedPackages.add(packageName);

        // Only root calls should check for parent existence
        if (isRootCall && !isRootPackage(component, components)) return;

        boolean hideDetails = (mode == VisualizationMode.GRAY_BOX && component.getDepth() > (globalMaxDepth - grayBoxLevel));

        umlBuilder.append("component ").append(packageName).append(" {\n");

        if (!hideDetails && mode != VisualizationMode.BLACK_BOX) {
            for (String className : component.getComposedParts()) {
                if (!component.getProvidedInterfaces().contains(className)) {
                    umlBuilder.append("  class ").append(className).append("\n");
                }
            }
            for (String iface : component.getProvidedInterfaces()) {
                if (!allUsedInterfaces.contains(iface)) {
                    umlBuilder.append("  interface ").append(iface).append("\n");
                }
            }
        }

        // Recursively process sub-packages
        for (Component subPackage : component.getSubPackages().values()) {
            generateComponentUML(subPackage, umlBuilder, processedPackages, components, mode, grayBoxLevel, globalMaxDepth, false);
        }

        umlBuilder.append("}\n");
    }


    private boolean isRootPackage(Component component, Set<Component> components) {
        String parentName = getParentPackage(component.getName());
        return parentName == null || !componentExists(parentName, components);
    }

    private boolean componentExists(String packageName, Set<Component> components) {
        return components.stream().anyMatch(c -> c.getName().equals(packageName));
    }

    private String getParentPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        int lastDotIndex = packageName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null;
        }
        return packageName.substring(0, lastDotIndex);
    }
}
