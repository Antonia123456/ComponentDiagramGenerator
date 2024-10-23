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

    private Set<Component> components = new HashSet<>();

    public void parseXML(File xmlFile, String jarFileName) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        NodeList packageList = document.getElementsByTagName("package");

        //load classes from the JAR file
        JarFile jarFile = new JarFile(new File(jarFileName));
        URLClassLoader loader = new URLClassLoader(new URL[]{new File(jarFileName).toURI().toURL()});

        jarFile.stream().forEach(entry -> {
            if (entry.getName().endsWith(".class")) {
                try {
                    //convert to fully qualified class name format used in Java + remove the ".class" extension to get actual class name
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    Class<?> clazz = loader.loadClass(className);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });



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
                Set<String> composedParts = new HashSet<>();
                Set<String> providedInterfaces = new HashSet<>();
                Set<String> requiredInterfaces = new HashSet<>();
                Set<String> explicitImplementation = new HashSet<>();

                //process each class from the package
                NodeList classList = packageElement.getElementsByTagName("class");
                for (int j = 0; j < classList.getLength(); j++) {
                    Node classNode = classList.item(j);
                    if (classNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element classElement = (Element) classNode;
                        String className = classElement.getElementsByTagName("name").item(0).getTextContent();

                        try {
                            Class<?> clazz = loader.loadClass(className);
                            composedParts.add(className);

                            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())){
                                providedInterfaces.add(clazz.getName());
                            }

                            //check if a class explicitly extends a concrete class
                            if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                                Class<?> superclass = clazz.getSuperclass();
                                if (superclass!=null && !superclass.isInterface() && !Modifier.isAbstract(superclass.getModifiers()) && !superclass.getName().equals("java.lang.Object"))
                                    explicitImplementation.add(className);
                            }

                        } catch (ClassNotFoundException e) {
                            System.err.println("Class not found: " + className);
                            e.printStackTrace();
                        }

                        NodeList outboundNodes = classElement.getElementsByTagName("outbound");
                        for (int k = 0; k < outboundNodes.getLength(); k++) {
                            Node outboundNode = outboundNodes.item(k);
                            if (outboundNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element outboundElement = (Element) outboundNode;
                                String outboundName = outboundNode.getTextContent();
                                String outboundType = outboundElement.getAttribute("type");

                                if (outboundType.equals("class")) {
                                    try {
                                        Class<?> outboundClass =loader.loadClass(outboundName);

                                        if (outboundClass.isInterface() || Modifier.isAbstract(outboundClass.getModifiers())) {
                                            //ignore classes from the java.lang package
                                            if (!outboundClass.getPackage().getName().startsWith("java.lang")) {
                                                //if (!providedInterfaces.contains(outboundName))
                                                    requiredInterfaces.add(outboundName);

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

                requiredInterfaces.removeAll(providedInterfaces);

                //setting component attributes
                component.setComposedParts(composedParts);
                component.setProvidedInterfaces(providedInterfaces);
                component.setRequiredInterfaces(requiredInterfaces);
                component.setExplicitImplementation(explicitImplementation);

                components.add(component);
            }
        }

    }

    public void printComponents() {
        for (Component component : components) {
            System.out.println("Component: " + component.getName());
            System.out.println("Composed Parts: " + component.getComposedParts());
            System.out.println("Provided Interfaces: " + component.getProvidedInterfaces());
            System.out.println("Required Interfaces: " + component.getRequiredInterfaces());
            System.out.println("Explicit Implementations: " + component.getExplicitImplementation());
            System.out.println();
        }
    }

    public static void main(String[] args) {
        try {
            File xmlFile = new File("D:\\Licenta\\ComponentDiagramLicense\\src\\firstTryLicenceJAR.xml");
            String jarFileName="D:\\Licenta\\ComponentDiagramLicense\\src\\FirstTryLicence.jar";

            DependencyParser parser = new DependencyParser();
            parser.parseXML(xmlFile, jarFileName);
            parser.printComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
