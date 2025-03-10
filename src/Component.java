import java.util.*;

public class Component {
    private String name;
    private Set<String> composedParts = new HashSet<>();
    private Set<String> providedInterfaces = new HashSet<>();
    private Set<String> requiredInterfaces = new HashSet<>();
    private Set<String> explicitImplementation = new HashSet<>();

    // New: Track sub-packages (sub-components)
    private Map<String, Component> subPackages = new HashMap<>();

    // Track class-to-interface implementations
    private Map<String, Set<String>> classImplementations = new HashMap<>();

    public Component(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addSubPackage(String subPackageName, Component subComponent) {
        subPackages.put(subPackageName, subComponent);
    }

    public Map<String, Component> getSubPackages() {
        return subPackages;
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

    public void addClassImplementation(String className, String interfaceName) {
        classImplementations.computeIfAbsent(className, k -> new HashSet<>()).add(interfaceName);
    }

    public Map<String, Set<String>> getClassImplementations() {
        return classImplementations;
    }
}
