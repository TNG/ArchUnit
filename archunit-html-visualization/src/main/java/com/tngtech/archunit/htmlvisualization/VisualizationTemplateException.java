package com.tngtech.archunit.htmlvisualization;

class VisualizationTemplateException extends RuntimeException {
    VisualizationTemplateException(Throwable cause) {
        super("Failed to process HTML visualization template", cause);
    }

    VisualizationTemplateException(String message) {
        super(message);
    }
}
