import java.util.*;

public class Component {
    private String name;
    private int depth;
    private Set<String> composedParts = new HashSet<>();
    private Set<String> providedInterfaces = new HashSet<>();
    private Set<String> requiredInterfaces = new HashSet<>();
    private Set<String> explicitImplementation = new HashSet<>();

    // Track sub-packages
    private Map<String, Component> subPackages = new HashMap<>();

    // Track class-to-interface implementations
    private Map<String, Set<String>> classImplementations = new HashMap<>();

    public Component(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
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

    public Set<String> getProvidedInterfaces() {
        return providedInterfaces;
    }

    public Set<String> getRequiredInterfaces() {
        return requiredInterfaces;
    }

    public Set<String> getExplicitImplementation() {
        return explicitImplementation;
    }

    public void addClassImplementation(String className, String interfaceName) {
        classImplementations.computeIfAbsent(className, k -> new HashSet<>()).add(interfaceName);
    }

    public Map<String, Set<String>> getClassImplementations() {
        return classImplementations;
    }
}
