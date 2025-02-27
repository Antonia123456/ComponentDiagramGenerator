import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Component {
    private String name;
    private Set<String> composedParts;
    private Set<String> providedInterfaces;
    private Set<String> requiredInterfaces;
    private Set<String> explicitImplementation;

    public Component() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getComposedParts() {
        return composedParts;
    }

    public void setComposedParts(Set<String> composedParts) {
        this.composedParts = composedParts;
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


    public Set<String> getExplicitImplementation() {
        return explicitImplementation;
    }

    public void setExplicitImplementation(Set<String> explicitImplementation) {
        this.explicitImplementation = explicitImplementation;
    }

    //Track class-to-interface implementations
    private Map<String, Set<String>> classImplementations = new HashMap<>();

    public void addClassImplementation(String className, String interfaceName) {
        classImplementations.computeIfAbsent(className, k -> new HashSet<>()).add(interfaceName);
    }

    public Map<String, Set<String>> getClassImplementations() {
        return classImplementations;
    }
}
