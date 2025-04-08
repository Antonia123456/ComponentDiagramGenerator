import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public class DependencyParserGUI {
    private JFrame frame;
    private JTextArea outputArea;
    private JButton btnLoadJar, btnAnalyze, btnPrintStructure, btnGenerateDiagram;
    private JComboBox<String> visualizationMode;
    private JSpinner grayBoxLevel;
    private DependencyParser parser;
    private File selectedJarFile;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int currentMaxDepth = 1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DependencyParserGUI().createAndShowGUI());
    }

    public void createAndShowGUI() {
        parser = new DependencyParser();

        frame = new JFrame("Dependency Parser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        createComponents();
        setupLayout();
        frame.setVisible(true);
    }

    private void createComponents() {
        btnLoadJar = new JButton("Load JAR");
        btnAnalyze = new JButton("Analyze");
        btnPrintStructure = new JButton("Print Structure");
        btnGenerateDiagram = new JButton("Generate Diagram");

        visualizationMode = new JComboBox<>(new String[]{"White-Box", "Gray-Box", "Black-Box"});

        // Initialize spinner with proper range
        grayBoxLevel = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        grayBoxLevel.setEnabled(false);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        redirectSystemOutToTextArea();

        setupEventListeners();

        btnAnalyze.setEnabled(false);
        btnPrintStructure.setEnabled(false);
        btnGenerateDiagram.setEnabled(false);
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel: File Operations ---
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filePanel.add(new JLabel("JAR File:"));
        filePanel.add(btnLoadJar);
        filePanel.add(btnAnalyze);  // Moved Analyze next to Load

        // --- Middle Panel: Visualization Settings ---
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        settingsPanel.add(new JLabel("Visualization Mode:"));
        settingsPanel.add(visualizationMode);
        settingsPanel.add(new JLabel("Gray Level:"));
        settingsPanel.add(grayBoxLevel);

        // --- Bottom Panel: Actions ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionPanel.add(btnPrintStructure);
        actionPanel.add(btnGenerateDiagram);

        // --- Combine All ---
        JPanel controlPanel = new JPanel(new GridLayout(3, 1, 5, 5));  // 3 rows
        controlPanel.add(filePanel);      // Row 1: Load + Analyze
        controlPanel.add(settingsPanel);  // Row 2: Mode + Gray Level
        controlPanel.add(actionPanel);    // Row 3: Print + Generate

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setPreferredSize(new Dimension(400, 300));

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(outputScroll, BorderLayout.CENTER);

        frame.add(mainPanel);
    }

    private void redirectSystemOutToTextArea() {
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void loadJarFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select JAR File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JAR Files", "jar"));

        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            // Reset everything for new analysis
            selectedJarFile = fileChooser.getSelectedFile();
            parser = new DependencyParser(); // Create fresh parser instance
            outputArea.setText(""); // Clear previous output
            System.out.println("Loaded: " + selectedJarFile.getAbsolutePath());

            // Reset UI state
            btnAnalyze.setEnabled(true);
            btnPrintStructure.setEnabled(false);
            btnGenerateDiagram.setEnabled(false);
            visualizationMode.setEnabled(false);
            grayBoxLevel.setEnabled(false);

            // Reset spinner to default
            SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 1, 1);
            grayBoxLevel.setModel(model);
        }
    }

    private void analyzeInBackground() {
        setButtonsEnabled(false);
        outputArea.setText(""); // Clear previous output
        System.out.println("Starting analysis of: " + selectedJarFile.getName());

        executor.execute(() -> {
            try {
                String xmlPath = selectedJarFile.getAbsolutePath().replace(".jar", "_dependencies.xml");
                System.out.println("Generating dependency XML...");
                parser.runDependencyFinder(selectedJarFile.getAbsolutePath(), xmlPath);

                System.out.println("Parsing XML...");
                parser.parseXML(new File(xmlPath), selectedJarFile.getAbsolutePath());

                SwingUtilities.invokeLater(() -> {
                    if (parser.hasExplicitImplementations()) {
                        showBadDesignReport();
                    } else {
                        currentMaxDepth = parser.getGlobalMaxDepth();
                        System.out.println("Maximum package depth: " + currentMaxDepth);

                        // Initialize gray level spinner with proper range
                        SpinnerNumberModel model = new SpinnerNumberModel(
                                1, // initial value
                                1, // min
                                Math.max(1, currentMaxDepth - 1), // max (using your logic)
                                1  // step
                        );
                        grayBoxLevel.setModel(model);

                        // Enable controls
                        btnPrintStructure.setEnabled(true);
                        btnGenerateDiagram.setEnabled(true);
                        visualizationMode.setEnabled(true);

                        // If Gray-Box is already selected, enable the spinner
                        if (visualizationMode.getSelectedItem().equals("Gray-Box")) {
                            grayBoxLevel.setEnabled(true);
                        }
                    }
                    setButtonsEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Error: " + e.getMessage());
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void setupEventListeners() {
        btnLoadJar.addActionListener(e -> loadJarFile());
        btnAnalyze.addActionListener(e -> analyzeInBackground());
        btnPrintStructure.addActionListener(e -> printStructure());
        btnGenerateDiagram.addActionListener(e -> generateDiagramInBackground());

        visualizationMode.addActionListener(e -> {
            boolean isGrayBox = visualizationMode.getSelectedItem().equals("Gray-Box");
            grayBoxLevel.setEnabled(isGrayBox);
        });
    }

    private void showBadDesignReport() {
        JDialog reportDialog = new JDialog(frame, "Bad Design Report", true);
        reportDialog.setSize(600, 400);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);

        // Redirect output to capture the report
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);

        parser.generateBadDesignReport();

        System.out.flush();
        System.setOut(oldOut);
        reportArea.setText(baos.toString());

        reportDialog.add(new JScrollPane(reportArea));
        reportDialog.setLocationRelativeTo(frame);
        reportDialog.setVisible(true);
    }

    private void printStructure() {
        outputArea.setText(""); // Clear previous output first
        System.out.println("\n=== Detailed Program Structure ===");
        parser.printComponents();
    }

    private void generateDiagramInBackground() {
        setButtonsEnabled(false);
        outputArea.setText(""); // Clear previous output first
        System.out.println("Generating diagram...");

        executor.execute(() -> {
            try {
                int mode = visualizationMode.getSelectedItem().equals("White-Box") ? 1 :
                        visualizationMode.getSelectedItem().equals("Gray-Box") ? 2 : 3;

                PlantUMLGenerator.VisualizationMode visMode =
                        mode == 1 ? PlantUMLGenerator.VisualizationMode.WHITE_BOX :
                                mode == 2 ? PlantUMLGenerator.VisualizationMode.GRAY_BOX :
                                        PlantUMLGenerator.VisualizationMode.BLACK_BOX;

                PlantUMLGenerator generator = new PlantUMLGenerator(
                        parser.getComponents(),
                        visMode,
                        (Integer)grayBoxLevel.getValue(),
                        parser.getGlobalMaxDepth()
                );

                String plantUML = generator.generatePlantUML();
                String pumlPath = parser.saveAndGenerateDiagram(plantUML, "component_diagram");

                SwingUtilities.invokeLater(() -> {
                    try {
                        String imagePath = pumlPath.replace(".puml", ".png");
                        showDiagramPopup(imagePath);
                        System.out.println("Diagram generated successfully!");
                    } catch (Exception e) {
                        System.err.println("Error displaying diagram: " + e.getMessage());
                    }
                    setButtonsEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Diagram error: " + e.getMessage());
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void showDiagramPopup(String imagePath) throws IOException {
        BufferedImage img = ImageIO.read(new File(imagePath));
        if (img == null) {
            throw new IOException("Could not load diagram image");
        }

        JDialog dialog = new JDialog(frame, "Component Diagram", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        Image scaledImage = img.getScaledInstance(
                Math.min(1000, img.getWidth()),
                Math.min(800, img.getHeight()),
                Image.SCALE_SMOOTH
        );

        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(1000, 800));

        dialog.add(scrollPane);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLoadJar.setEnabled(enabled);
        btnAnalyze.setEnabled(enabled && selectedJarFile != null);
        btnPrintStructure.setEnabled(enabled);
        btnGenerateDiagram.setEnabled(enabled);
        visualizationMode.setEnabled(enabled);
        grayBoxLevel.setEnabled(enabled && visualizationMode.getSelectedItem().equals("Gray-Box"));
    }
}

class CustomOutputStream extends OutputStream {
    private JTextArea textArea;

    public CustomOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        SwingUtilities.invokeLater(() -> textArea.append(String.valueOf((char) b)));
    }
}