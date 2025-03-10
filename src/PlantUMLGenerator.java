import java.util.*;

public class PlantUMLGenerator {

    private Set<Component> components;
    // Track all interfaces that are used (either required or implemented)
    Set<String> allUsedInterfaces = new HashSet<>();

    public PlantUMLGenerator(Set<Component> components) {
        this.components = components;
    }

    public String generatePlantUML(boolean isWhiteBoxMode) {
        StringBuilder umlBuilder = new StringBuilder();
        umlBuilder.append("@startuml\n");


        // Collect implemented and required interfaces
        for (Component component : components) {
            for (Map.Entry<String, Set<String>> entry : component.getClassImplementations().entrySet()) {
                allUsedInterfaces.addAll(entry.getValue());
            }
            allUsedInterfaces.addAll(component.getRequiredInterfaces());
        }

        // Define each component with its classes and interfaces
        for (Component component : components) {
            generateComponentUML(component, umlBuilder, isWhiteBoxMode);
        }

        // Add implementation relationships
        for (Component component : components) {
            for (Map.Entry<String, Set<String>> entry : component.getClassImplementations().entrySet()) {
                String className = entry.getKey();
                for (String interfaceName : entry.getValue()) {
                    String simpleInterfaceName = interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
                    String packageName = interfaceName.contains(".") ? interfaceName.substring(0, interfaceName.lastIndexOf('.')) : "";

                    if (isWhiteBoxMode) {
                        umlBuilder.append(className)
                                .append(" -0)- ")
                                .append(packageName)
                                .append(" : \"")
                                .append(simpleInterfaceName).append("\"\n");
                    } else {
                        umlBuilder.append(component.getName())
                                .append(" -() ")
                                .append(simpleInterfaceName).append("\n");
                    }

                    component.getRequiredInterfaces().remove(interfaceName);
                }
            }
        }

        // Add relationships between components
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
                                .append(simpleInterfaceName).append("\"\n");
                        break;
                    }
                }
            }
        }

        umlBuilder.append("@enduml\n");
        return umlBuilder.toString();
    }

    private void generateComponentUML(Component component, StringBuilder umlBuilder, boolean isWhiteBoxMode) {
        String packageName = component.getName().isEmpty() ? "default" : component.getName();

        if (isWhiteBoxMode) {
            umlBuilder.append("package ").append(packageName).append(" {\n");
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
            umlBuilder.append("}\n\n");
        } else {
            umlBuilder.append("package ").append(packageName).append(" {\n");
            for (Map.Entry<String, Component> entry : component.getSubPackages().entrySet()) {
                generateComponentUML(entry.getValue(), umlBuilder, isWhiteBoxMode);
            }
            umlBuilder.append("}\n");
        }
    }
}
