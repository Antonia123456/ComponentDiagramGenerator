import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlantUMLGenerator {

    private Set<Component> components;

    public PlantUMLGenerator(Set<Component> components) {
        this.components = components;
    }

    public String generatePlantUML(boolean isWhiteBoxMode) {
        StringBuilder umlBuilder = new StringBuilder();

        umlBuilder.append("@startuml\n");

        //Track ALL interfaces that are USED (either required or implemented)
        Set<String> allUsedInterfaces = new HashSet<>();

        //Collect implemented interfaces
        for (Component component : components) {
            for (Map.Entry<String, Set<String>> entry : component.getClassImplementations().entrySet()) {
                allUsedInterfaces.addAll(entry.getValue());
            }
        }

        //Collect required interfaces
        for (Component component : components) {
            allUsedInterfaces.addAll(component.getRequiredInterfaces());
        }


        //define each component with its classes and interfaces
        for (Component component : components) {
            //default name in case of default package
            String packageName = component.getName().isEmpty() ? "default" : component.getName();

            if (isWhiteBoxMode) {
                umlBuilder.append("component ").append(packageName).append(" {\n");

                for (String className : component.getComposedParts()) {
                    if (!component.getProvidedInterfaces().contains(className)) { //avoid double-adding interfaces
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
                //Black-box
                umlBuilder.append("component ").append(packageName).append("\n");
            }
        }

        // Add implementation relationships -0)-
        for (Component component : components) {
            for (Map.Entry<String, Set<String>> entry : component.getClassImplementations().entrySet()) {
                String className = entry.getKey();
                for (String interfaceName : entry.getValue()) {

                    String simpleInterfaceName = interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
                    String packageName = interfaceName.substring(0, interfaceName.lastIndexOf('.'));

                    if(isWhiteBoxMode) {
                        umlBuilder.append(className)
                                .append(" -0)- ")
                                .append(packageName)
                                .append(" : \"\"").append(simpleInterfaceName).append("\"\"\n");
                    }
                    else {
                        umlBuilder.append(component.getName())
                                .append(" -() ")
                                .append(simpleInterfaceName).append("\n");
                    }

                    // Remove these from required interfaces to avoid duplicates
                    component.getRequiredInterfaces().remove(interfaceName);
                }
            }
        }


        //add relationships between components -(0-
        for (Component component : components) {
            String fromPackageName = component.getName().isEmpty() ? "default" : component.getName();

            for (String requiredInterface : component.getRequiredInterfaces()) {
                //Searching the component that provides this interface
                for (Component target : components) {
                    if (target.getProvidedInterfaces().contains(requiredInterface)) {
                        String toPackageName = target.getName().isEmpty() ? "default" : target.getName();

                        //remove package prefix
                        String simpleInterfaceName = requiredInterface.substring(requiredInterface.lastIndexOf('.') + 1);

                        umlBuilder.append(fromPackageName)
                                .append(" -(0- ")
                                .append(toPackageName)
                                .append(" : \"\"").append(simpleInterfaceName).append("\"\"\n");
                        break;
                    }
                }
            }
        }

        umlBuilder.append("@enduml\n");
        return umlBuilder.toString();
    }

}