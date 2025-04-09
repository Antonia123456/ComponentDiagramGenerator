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
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int currentMaxDepth = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        parser = new DependencyParser();

        primaryStage.setTitle("Dependency Parser");

        btnLoadJar = new Button("Load JAR");
        btnAnalyze = new Button("Analyze");
        btnPrintStructure = new Button("Print Structure");
        btnGenerateDiagram = new Button("Generate Diagram");

        visualizationMode = new ComboBox<>();
        visualizationMode.getItems().addAll("White-Box", "Gray-Box", "Black-Box");
        visualizationMode.setDisable(true);

        grayBoxLevel = new Spinner<>(1, 1, 1);
        grayBoxLevel.setDisable(true);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        redirectSystemOut();

        btnAnalyze.setDisable(true);
        btnPrintStructure.setDisable(true);
        btnGenerateDiagram.setDisable(true);

        btnLoadJar.setOnAction(e -> loadJarFile(primaryStage));
        btnAnalyze.setOnAction(e -> analyzeInBackground());
        btnPrintStructure.setOnAction(e -> printStructure());
        btnGenerateDiagram.setOnAction(e -> generateDiagramInBackground());

        visualizationMode.setOnAction(e -> {
            boolean isGrayBox = "Gray-Box".equals(visualizationMode.getValue());
            grayBoxLevel.setDisable(!isGrayBox);
        });

        HBox fileControls = new HBox(10, btnLoadJar, btnAnalyze);
        HBox settingsControls = new HBox(10,
                new Label("Visualization Mode:"), visualizationMode,
                new Label("Gray Level:"), grayBoxLevel
        );
        HBox actionsControls = new HBox(10, btnPrintStructure, btnGenerateDiagram);
        VBox controlPanel = new VBox(10, fileControls, settingsControls, actionsControls);
        controlPanel.setPadding(new Insets(10));

        VBox root = new VBox(10, controlPanel, new Separator(), new ScrollPane(outputArea));
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void redirectSystemOut() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                Platform.runLater(() -> outputArea.appendText(String.valueOf((char) b)));
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void loadJarFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select JAR File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File file = fileChooser.showOpenDialog(stage);

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

            grayBoxLevel.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
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
                            grayBoxLevel.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                                    1, Math.max(1, currentMaxDepth - 1), 1));

                            btnPrintStructure.setDisable(false);
                            btnGenerateDiagram.setDisable(false);
                            visualizationMode.setDisable(false);
                            if ("Gray-Box".equals(visualizationMode.getValue())) {
                                grayBoxLevel.setDisable(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> System.err.println("Error: " + e.getMessage()));
                }
                Platform.runLater(() -> setControlsDisabled(false));
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

                    String imagePath = pumlPath.replace(".puml", ".png");
                    Platform.runLater(() -> showDiagramPopup(imagePath));
                } catch (Exception e) {
                    Platform.runLater(() -> System.err.println("Diagram error: " + e.getMessage()));
                }
                Platform.runLater(() -> setControlsDisabled(false));
                return null;
            }
        };

        executor.submit(task);
    }

    private void showDiagramPopup(String imagePath) {
        try {
            FileInputStream fis = new FileInputStream(imagePath);
            Image img = new Image(fis);

            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(1000);
            imageView.setFitHeight(800);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setPrefSize(1000, 800);

            Stage dialog = new Stage();
            dialog.setTitle("main.Component Diagram");
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

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);

        parser.generateBadDesignReport();

        System.out.flush();
        System.setOut(oldOut);
        reportArea.setText(baos.toString());

        dialog.setScene(new Scene(new ScrollPane(reportArea), 600, 400));
        dialog.initModality(Modality.APPLICATION_MODAL);
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
