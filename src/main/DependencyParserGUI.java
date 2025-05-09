package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DependencyParserGUI extends Application {

    private TextArea outputArea;
    private Button btnLoadJar, btnAnalyze, btnPrintStructure, btnGenerateDiagram;
    private ComboBox<String> visualizationMode;
    private Spinner<Integer> grayBoxLevel;
    private DependencyParser parser;
    private File selectedJarFile;
    private ExecutorService executor;
    private int currentMaxDepth = 1;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        parser = new DependencyParser();
        executor = Executors.newSingleThreadExecutor();

        primaryStage.setTitle("Dependency Parser");

        createUIComponents();
        Scene scene = createMainScene();
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        primaryStage.show();
    }

    private void createUIComponents() {
        // Button initialization
        btnLoadJar = new Button("Load JAR");
        btnAnalyze = new Button("Analyze");
        btnPrintStructure = new Button("Print Structure");
        btnGenerateDiagram = new Button("Generate Diagram");

        // Visualization mode dropdown
        visualizationMode = new ComboBox<>();
        visualizationMode.getItems().addAll("White-Box", "Gray-Box", "Black-Box");
        visualizationMode.getSelectionModel().selectFirst();
        visualizationMode.setDisable(true);

        // Gray level spinner
        grayBoxLevel = new Spinner<>(1, 1, 1);
        grayBoxLevel.setEditable(true);
        grayBoxLevel.setDisable(true);

        // Output area setup
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: monospace;");
        redirectSystemOut();

        // Initial button states
        setControlsDisabled(false);
        btnAnalyze.setDisable(true);
        btnPrintStructure.setDisable(true);
        btnGenerateDiagram.setDisable(true);

        setupEventHandlers();
    }

    private Scene createMainScene() {
        HBox fileControls = new HBox(10, btnLoadJar, btnAnalyze);
        fileControls.setPadding(new Insets(5));

        HBox settingsControls = new HBox(10,
                new Label("Visualization Mode:"), visualizationMode,
                new Label("Gray Level:"), grayBoxLevel
        );
        settingsControls.setPadding(new Insets(5));

        HBox actionsControls = new HBox(10, btnPrintStructure, btnGenerateDiagram);
        actionsControls.setPadding(new Insets(5));

        VBox controlPanel = new VBox(10, fileControls, settingsControls, actionsControls);
        controlPanel.setPadding(new Insets(10));

        // Output area with scroll
        ScrollPane outputScroll = new ScrollPane(outputArea);
        outputScroll.setFitToWidth(true);
        outputScroll.setFitToHeight(true);

        VBox root = new VBox(10, controlPanel, new Separator(), outputScroll);
        root.setPadding(new Insets(10));

        return new Scene(root, 900, 600);
    }

    private void setupEventHandlers() {
        btnLoadJar.setOnAction(e -> loadJarFile());
        btnAnalyze.setOnAction(e -> analyzeInBackground());
        btnPrintStructure.setOnAction(e -> printStructure());
        btnGenerateDiagram.setOnAction(e -> generateDiagramInBackground());

        visualizationMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            grayBoxLevel.setDisable(!"Gray-Box".equals(newVal));
        });
    }

    private void handleWindowClose(WindowEvent event) {
        if (executor != null) {
            executor.shutdownNow();
        }
        Platform.exit();
        System.exit(0);
    }

    private void redirectSystemOut() {
        OutputStream out = new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                buffer.append((char)b);
                if (b == '\n') {
                    flushBuffer();
                }
            }

            @Override
            public void write(byte[] b, int off, int len) {
                buffer.append(new String(b, off, len));
                if (buffer.indexOf("\n") >= 0) {
                    flushBuffer();
                }
            }

            private void flushBuffer() {
                String content = buffer.toString();
                buffer.setLength(0);
                Platform.runLater(() -> outputArea.appendText(content));
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void loadJarFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select JAR File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            selectedJarFile = file;
            parser = new DependencyParser();
            outputArea.clear();
            System.out.println("Loaded: " + selectedJarFile.getAbsolutePath());

            btnAnalyze.setDisable(false);
            btnPrintStructure.setDisable(true);
            btnGenerateDiagram.setDisable(true);
            visualizationMode.setDisable(true);
            grayBoxLevel.setDisable(true);

            grayBoxLevel.getValueFactory().setValue(1);
        }
    }

    private void analyzeInBackground() {
        setControlsDisabled(true);
        outputArea.clear();
        System.out.println("Starting analysis of: " + selectedJarFile.getName());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String xmlPath = selectedJarFile.getAbsolutePath().replace(".jar", "_dependencies.xml");
                    System.out.println("Generating dependency XML...");
                    parser.runDependencyFinder(selectedJarFile.getAbsolutePath(), xmlPath);

                    System.out.println("Parsing XML...");
                    parser.parseXML(new File(xmlPath), selectedJarFile.getAbsolutePath());

                    Platform.runLater(() -> {
                        if (parser.hasExplicitImplementations()) {
                            showBadDesignReport();
                        } else {
                            currentMaxDepth = parser.getGlobalMaxDepth();
                            SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                                    new SpinnerValueFactory.IntegerSpinnerValueFactory(
                                            1, Math.max(1, currentMaxDepth - 1), 1);
                            grayBoxLevel.setValueFactory(valueFactory);

                            btnPrintStructure.setDisable(false);
                            btnGenerateDiagram.setDisable(false);
                            visualizationMode.setDisable(false);
                            grayBoxLevel.setDisable(!"Gray-Box".equals(visualizationMode.getValue()));
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> System.err.println("Error: " + e.getMessage()));
                    e.printStackTrace();
                } finally {
                    System.out.println("Parsing finished");
                    Platform.runLater(() -> setControlsDisabled(false));
                }
                return null;
            }
        };

        executor.submit(task);
    }

    private void printStructure() {
        outputArea.clear();
        System.out.println("\n=== Detailed Program Structure ===");
        parser.printComponents();
    }

    private void generateDiagramInBackground() {
        setControlsDisabled(true);
        outputArea.clear();
        System.out.println("Generating diagram...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    PlantUMLGenerator.VisualizationMode visMode = switch (visualizationMode.getValue()) {
                        case "White-Box" -> PlantUMLGenerator.VisualizationMode.WHITE_BOX;
                        case "Gray-Box" -> PlantUMLGenerator.VisualizationMode.GRAY_BOX;
                        default -> PlantUMLGenerator.VisualizationMode.BLACK_BOX;
                    };

                    PlantUMLGenerator generator = new PlantUMLGenerator(
                            parser.getComponents(),
                            visMode,
                            grayBoxLevel.getValue(),
                            parser.getGlobalMaxDepth()
                    );

                    String plantUML = generator.generatePlantUML();
                    String pumlPath = parser.saveAndGenerateDiagram(plantUML, "component_diagram");

                    // Show the generated diagram
                    Platform.runLater(() -> {
                        String imagePath = pumlPath.replace(".puml", ".png");
                        showDiagramPopup(imagePath);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        System.err.println("Diagram error: " + e.getMessage());
                        e.printStackTrace();
                    });
                } finally {
                    Platform.runLater(() -> setControlsDisabled(false));
                }
                return null;
            }
        };

        executor.submit(task);
    }

    private void showDiagramPopup(String imagePath) {
        try {
            // Load and display the generated diagram image
            FileInputStream fis = new FileInputStream(imagePath);
            Image img = new Image(fis);

            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(1000);
            imageView.setFitHeight(800);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setPrefSize(1000, 800);

            Stage dialog = new Stage();
            dialog.setTitle("Component Diagram");
            dialog.initModality(Modality.NONE);
            dialog.setScene(new Scene(scrollPane));
            dialog.show();
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
        }
    }

    private void showBadDesignReport() {
        Stage dialog = new Stage();
        dialog.setTitle("Bad Design Report");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setStyle("-fx-font-family: monospace;");
        reportArea.setWrapText(true);

        // Redirect output to capture the report
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);

        parser.generateBadDesignReport();

        System.out.flush();
        System.setOut(oldOut);
        reportArea.setText(baos.toString());

        ScrollPane scrollPane = new ScrollPane(reportArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        dialog.setScene(new Scene(scrollPane, 800, 600));
        dialog.showAndWait();
    }

    private void setControlsDisabled(boolean disabled) {
        btnLoadJar.setDisable(disabled);
        btnAnalyze.setDisable(disabled || selectedJarFile == null);
        btnPrintStructure.setDisable(disabled);
        btnGenerateDiagram.setDisable(disabled);
        visualizationMode.setDisable(disabled);
        grayBoxLevel.setDisable(disabled || !"Gray-Box".equals(visualizationMode.getValue()));
    }
}