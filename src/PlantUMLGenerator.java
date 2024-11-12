import java.util.Set;
import java.util.HashSet;

public class PlantUMLGenerator {

    private Set<Component> components;

    public PlantUMLGenerator(Set<Component> components) {
        this.components = components;
    }

    public String generatePlantUML() {
        StringBuilder umlBuilder = new StringBuilder();

        umlBuilder.append("@startuml\n");

        // Step 1: Define each component (package) with its classes and interfaces
        for (Component component : components) {
            // Assign a default name for default components
            String packageName = component.getName().isEmpty() ? "default" : component.getName();

            umlBuilder.append("package ").append(packageName).append(" {\n");

            // Add each class inside the component
            for (String className : component.getComposedParts()) {
                umlBuilder.append("  class ").append(className).append("\n");
            }

            // Define provided interfaces
            for (String providedInterface : component.getProvidedInterfaces()) {
                umlBuilder.append("  interface ").append(providedInterface).append("\n");
            }

            umlBuilder.append("}\n\n");
        }

        // Step 2: Add relationships based on required and provided interfaces
        for (Component component : components) {
            String fromPackageName = component.getName().isEmpty() ? "default" : component.getName();

            for (String requiredInterface : component.getRequiredInterfaces()) {
                // Find the component that provides this interface
                for (Component targetComponent : components) {
                    if (targetComponent.getProvidedInterfaces().contains(requiredInterface)) {
                        String toPackageName = targetComponent.getName().isEmpty() ? "default" : targetComponent.getName();
                        // Create a dependency arrow for the required interface
                        umlBuilder.append(fromPackageName)
                                .append(" --> ")
                                .append(toPackageName)
                                .append(" : uses ")
                                .append(requiredInterface)
                                .append("\n");
                        break;
                    }
                }
            }
        }

        umlBuilder.append("@enduml\n");
        return umlBuilder.toString();
    }

}