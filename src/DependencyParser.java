import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.lang.reflect.*;
import java.util.jar.JarFile;

public class DependencyParser {

    private Map<String, Component> componentMap = new HashMap<>();
    private List<String> ignoreList = Arrays.asList("java.lang", "java.io", "java.util");

    public void parseXML(File xmlFile, String jarFileName) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        NodeList packageList = document.getElementsByTagName("package");

        // Load classes from JAR
        JarFile jarFile = new JarFile(new File(jarFileName));
        URLClassLoader loader = new URLClassLoader(new URL[]{new File(jarFileName).toURI().toURL()});

        jarFile.stream().forEach(entry -> {
            if (entry.getName().endsWith(".class")) {
                try {
                    // Convert to fully qualified class name format used in Java + remove the ".class" extension to get actual class name
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    loader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        // Process each package
        for (int i = 0; i < packageList.getLength(); i++) {
            Node packageNode = packageList.item(i);
            if (packageNode.getNodeType() == Node.ELEMENT_NODE) {
                Element packageElement = (Element) packageNode;
                String confirmedAttribute = packageElement.getAttribute("confirmed");
                if (!"yes".equals(confirmedAttribute)) continue;

                String packageName = packageElement.getElementsByTagName("name").item(0).getTextContent();
                Component component = componentMap.computeIfAbsent(packageName, Component::new);

                // Handle package hierarchy
                String parentPackage = getParentPackage(packageName);
                if (parentPackage != null) {
                    Component parentComponent = componentMap.computeIfAbsent(parentPackage, Component::new);
                    parentComponent.addSubPackage(packageName, component);
                }

                // Process classes in package
                NodeList classList = packageElement.getElementsByTagName("class");
                for (int j = 0; j < classList.getLength(); j++) {
                    Node classNode = classList.item(j);
                    if (classNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element classElement = (Element) classNode;
                        String className = classElement.getElementsByTagName("name").item(0).getTextContent();

                        try {
                            Class<?> clazz = loader.loadClass(className);
                            component.getComposedParts().add(className);

                            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                                component.getProvidedInterfaces().add(clazz.getName());
                            }

                            // Check if a class explicitly extends a concrete class
                            if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                                Class<?> superclass = clazz.getSuperclass();

                                if (superclass != null && !superclass.getName().equals("java.lang.Object")) {
                                    if (!superclass.isInterface() && !Modifier.isAbstract(superclass.getModifiers())) {
                                        component.getExplicitImplementation().add(className);
                                    } else if (Modifier.isAbstract(superclass.getModifiers())) {
                                        // If it extends an abstract class, register it
                                        component.addClassImplementation(clazz.getName(), superclass.getName());
                                    }
                                }//???

                                // Track implemented interfaces
                                for (Class<?> interfaceClass : clazz.getInterfaces()) {
                                    component.addClassImplementation(clazz.getName(), interfaceClass.getName());
                                }
                            }

                        } catch (ClassNotFoundException e) {
                            System.err.println("Class not found: " + className);
                        }

                        // Process dependencies
                        NodeList outboundNodes = classElement.getElementsByTagName("outbound");
                        for (int k = 0; k < outboundNodes.getLength(); k++) {
                            Node outboundNode = outboundNodes.item(k);
                            if (outboundNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element outboundElement = (Element) outboundNode;
                                String outboundName = outboundNode.getTextContent();
                                String outboundType = outboundElement.getAttribute("type");

                                if (outboundType.equals("class")) {
                                    try {
                                        Class<?> outboundClass = loader.loadClass(outboundName);
                                        boolean shouldIgnore = ignoreList.stream().anyMatch(ignore -> outboundClass.getName().startsWith(ignore));

                                        if (!shouldIgnore) {
                                            if (outboundClass.isInterface() || Modifier.isAbstract(outboundClass.getModifiers())) {
                                                component.getRequiredInterfaces().add(outboundName);
                                            } else {
                                                component.getExplicitImplementation().add(outboundName);
                                            }
                                        }

                                    } catch (ClassNotFoundException e) {
                                        System.out.println("Class not found: " + outboundName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getParentPackage(String packageName) {
        int lastDotIndex = packageName.lastIndexOf('.');
        return (lastDotIndex == -1) ? null : packageName.substring(0, lastDotIndex);
    }

    public Set<Component> getComponents() {
        return new HashSet<>(componentMap.values());
    }

    public void printComponents() {
        for (Component component : componentMap.values()) {
            System.out.println("Component: " + component.getName());
            System.out.println("Composed Parts: " + component.getComposedParts());
            System.out.println("Provided Interfaces: " + component.getProvidedInterfaces());
            System.out.println("Required Interfaces: " + component.getRequiredInterfaces());
            System.out.println("Explicit Implementations: " + component.getExplicitImplementation());
            System.out.println("Sub-Packages: " + component.getSubPackages().keySet());
            System.out.println();
        }
    }

    public boolean hasExplicitImplementations() {
        return componentMap.values().stream().anyMatch(c -> !c.getExplicitImplementation().isEmpty());
    }

    public void generateBadDesignReport() {
        System.out.println("This is a bad design. Explicit dependencies detected on concrete classes.");
        for (Component component : componentMap.values()) {
            if (!component.getExplicitImplementation().isEmpty()) {
                System.out.println("Component " + component.getName() +
                        " has explicit dependencies on: " +
                        component.getExplicitImplementation());
            }
        }
    }

    public static void main(String[] args) {
        try {
            File xmlFile = new File("D:\\Licenta\\ComponentDiagramLicense\\src\\LicentaJAR.xml");
            String jarFileName = "D:\\Licenta\\ComponentDiagramLicense\\src\\Licenta.jar";

            DependencyParser parser = new DependencyParser();
            parser.parseXML(xmlFile, jarFileName);
            parser.printComponents();

            if (parser.hasExplicitImplementations()) {
                parser.generateBadDesignReport();
            } else {
                PlantUMLGenerator umlGenerator = new PlantUMLGenerator(parser.getComponents());
                String plantUMLText = umlGenerator.generatePlantUML(false);
                //String plantUMLText = umlGenerator.generatePlantUML(true);
                System.out.println("PlantUML Text:\n" + plantUMLText);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
