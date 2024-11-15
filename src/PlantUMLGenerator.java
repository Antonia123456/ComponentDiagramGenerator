import java.util.Set;

public class PlantUMLGenerator {

    private Set<Component> components;

    public PlantUMLGenerator(Set<Component> components) {
        this.components = components;
    }

    public String generatePlantUML() {
        StringBuilder umlBuilder = new StringBuilder();

        umlBuilder.append("@startuml\n");

        //define each component with its classes and interfaces
        for (Component component : components) {
            //default name in case of default package
            String packageName = component.getName().isEmpty() ? "default" : component.getName();

            umlBuilder.append("component ").append(packageName).append(" {\n");

            for (String className : component.getComposedParts()) {
                if (!component.getProvidedInterfaces().contains(className)) { //avoid double-adding interfaces
                    umlBuilder.append("  class ").append(className).append("\n");
                }
            }

            for (String providedInterface : component.getProvidedInterfaces()) {
                umlBuilder.append("  interface ").append(providedInterface).append("\n");
            }

            umlBuilder.append("}\n\n");
        }

        //add relationships between components
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