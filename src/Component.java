import java.util.List;
import java.util.Set;

public class Component {
    private String name;
    private Set<String> composedClasses;
    private Set<String> providedInterfaces;
    private Set<String> requiredInterfaces;

    public Component() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getComposedClasses() {
        return composedClasses;
    }

    public void setComposedClasses(Set<String> composedClasses) {
        this.composedClasses = composedClasses;
    }

    public Set<String> getProvidedInterfaces() {
        return providedInterfaces;
    }

    public void setProvidedInterfaces(Set<String> providedInterfaces) {
        this.providedInterfaces = providedInterfaces;
    }

    public Set<String> getRequiredInterfaces() {
        return requiredInterfaces;
    }

    public void setRequiredInterfaces(Set<String> requiredInterfaces) {
        this.requiredInterfaces = requiredInterfaces;
    }


}
