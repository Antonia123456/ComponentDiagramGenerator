import java.util.List;

public class Component {
    private String name;
    private List<String> composedClasses;
    private List<String> providedInterfaces;
    private List<String> requiredInterfaces;

    public Component() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getComposedClasses() {
        return composedClasses;
    }

    public void setComposedClasses(List<String> composedClasses) {
        this.composedClasses = composedClasses;
    }

    public List<String> getProvidedInterfaces() {
        return providedInterfaces;
    }

    public void setProvidedInterfaces(List<String> providedInterfaces) {
        this.providedInterfaces = providedInterfaces;
    }

    public List<String> getRequiredInterfaces() {
        return requiredInterfaces;
    }

    public void setRequiredInterfaces(List<String> requiredInterfaces) {
        this.requiredInterfaces = requiredInterfaces;
    }


}
