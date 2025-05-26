package main;

import java.util.Set;

public interface UMLGenerator {
    enum VisualizationMode { WHITE_BOX, GRAY_BOX, BLACK_BOX }

    String generateUML(Set<Component> components, VisualizationMode mode, int grayBoxLevel, int globalMaxDepth);
}