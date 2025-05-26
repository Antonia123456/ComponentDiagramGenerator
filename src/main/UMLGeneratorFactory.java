package main;

public class UMLGeneratorFactory {
    public enum GeneratorType {
        PLANT_UML
    }

    public static UMLGenerator createGenerator(GeneratorType type) {
        switch (type) {
            case PLANT_UML:
                return new PlantUMLGenerator();
            //Open for extension by adding new cases for other generator types here
            default:
                throw new IllegalArgumentException("Unknown generator type: " + type);
        }
    }
}