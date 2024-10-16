import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class DependencyParser {

    private List<Component> components = new ArrayList<>();
    private Map<String, Component> componentMap = new HashMap<>();

    public void parseXML(File xmlFile, File jarFile) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        NodeList packageList = document.getElementsByTagName("package");

        //load classes from the JAR file
        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl});

        //process each package node
        for (int i = 0; i < packageList.getLength(); i++) {
            Node packageNode = packageList.item(i);

            if (packageNode.getNodeType() == Node.ELEMENT_NODE) {
                Element packageElement = (Element) packageNode;
                String confirmedAttribute = packageElement.getAttribute("confirmed");

                if (!"yes".equals(confirmedAttribute))
                    continue;

                String packageName = packageElement.getElementsByTagName("name").item(0).getTextContent();

                //create a new component for the package
                Component component = new Component();
                component.setName(packageName);
                List<String> composedClasses = new ArrayList<>();
                List<String> providedInterfaces = new ArrayList<>();

                //process each class from the package
                NodeList classList = packageElement.getElementsByTagName("class");
                for (int j = 0; j < classList.getLength(); j++) {
                    Node classNode = classList.item(j);
                    if (classNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element classElement = (Element) classNode;
                        String className = classElement.getElementsByTagName("name").item(0).getTextContent();

                        try {
                            Class<?> clazz = loader.loadClass(className);
                            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                                continue;
                            }

                            composedClasses.add(className);

                            //checking if the class implements interfaces
                            Class<?>[] interfaces = clazz.getInterfaces();
                            for (Class<?> iface : interfaces) {
                                providedInterfaces.add(iface.getName());
                            }

                        } catch (ClassNotFoundException e) {
                            System.err.println("Class not found: " + className);
                            e.printStackTrace();
                        }
                    }
                }

                //setting component attributes
                component.setComposedClasses(composedClasses);
                component.setProvidedInterfaces(providedInterfaces);

                //add component to the list and map for dependency linking
                components.add(component);
                componentMap.put(packageName, component);
            }
        }

        //finding dependencies between components
        for (int i = 0; i < packageList.getLength(); i++) {
            Node packageNode = packageList.item(i);

            if (packageNode.getNodeType() == Node.ELEMENT_NODE) {
                Element packageElement = (Element) packageNode;
                String confirmedAttribute = packageElement.getAttribute("confirmed");

                if (!"yes".equals(confirmedAttribute))
                    continue;

                String packageName = packageElement.getElementsByTagName("name").item(0).getTextContent();
                Component component = componentMap.get(packageName);

                if (component != null) {
                    //creating the list of required interfaces for this component
                    List<String> requiredInterfaces = new ArrayList<>();

                    NodeList classList = packageElement.getElementsByTagName("class");
                    for (int j = 0; j < classList.getLength(); j++) {
                        Node classNode = classList.item(j);
                        if (classNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element classElement = (Element) classNode;

                            NodeList outboundNodes = classElement.getElementsByTagName("outbound");
                            for (int k = 0; k < outboundNodes.getLength(); k++) {
                                Node outboundNode = outboundNodes.item(k);
                                if (outboundNode.getNodeType() == Node.ELEMENT_NODE) {
                                    String outboundClass = outboundNode.getTextContent();

                                    //check if the outbound class belongs to another component
                                    String outboundPackage = getPackageName(outboundClass);
                                    if (componentMap.containsKey(outboundPackage))
                                        requiredInterfaces.add(outboundClass);
                                }
                            }
                        }
                    }
                    component.setRequiredInterfaces(requiredInterfaces);
                }
            }
        }
    }

    //method to get the package name from a fully qualified class name
    private String getPackageName(String className) {
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return className.substring(0, lastDotIndex);
        }
        return ""; //default empty string if no package is found
    }

    public void printComponents() {
        for (Component component : components) {
            System.out.println("Component: " + component.getName());
            System.out.println("Composed Classes: " + component.getComposedClasses());
            System.out.println("Provided Interfaces: " + component.getProvidedInterfaces());
            System.out.println("Required Interfaces: " + component.getRequiredInterfaces());
            System.out.println();
        }
    }

    public static void main(String[] args) {
        try {
            File xmlFile = new File("D:\\Licenta\\ComponentDiagramLicense\\src\\firstTryLicenceJAR.xml");
            File jarFile = new File("D:\\Licenta\\ComponentDiagramLicense\\src\\FirstTryLicence.jar");

            DependencyParser parser = new DependencyParser();
            parser.parseXML(xmlFile, jarFile);
            parser.printComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
