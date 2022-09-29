package com.tngtech.archunit.tooling.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class JavaProjectLayout<T> {
    private static final String DOT = "\\.";

    private final String basePackage;
    private final String buildManifestLocation;
    private final String compiledClassesLocation;
    private final String sourcesLocation;
    private final String buildToolResourcesLocation;

    public JavaProjectLayout(
            String basePackage,
            String buildManifestLocation,
            String compiledClassesLocation,
            String sourcesLocation,
            String buildToolResourcesLocation) {
        this.basePackage = basePackage;
        this.buildManifestLocation = buildManifestLocation;
        this.compiledClassesLocation = compiledClassesLocation;
        this.sourcesLocation = sourcesLocation;
        this.buildToolResourcesLocation = buildToolResourcesLocation;
    }

    public T applyTo(final Path projectRoot) throws IOException, URISyntaxException {
        ResourcesUtils.copyResource(projectRoot, buildManifestLocation, "");
        ResourcesUtils.copyResourceDirectory(projectRoot, Paths.get(buildToolResourcesLocation, getBuildToolWrapperDirectory()).toString(), getBuildToolWrapperDirectory());
        ResourcesUtils.copyResources(projectRoot, basePackageDirectoryPath(sourcesLocation), "glob:**.java", basePackageDirectoryPath("src/test/java"));
        ResourcesUtils.copyResources(projectRoot, basePackageDirectoryPath(compiledClassesLocation), "glob:**.class", basePackageDirectoryPath(
                getCompiledTestClassesTargetDirectory()));
        return buildResult(projectRoot);
    }

    protected abstract String getCompiledTestClassesTargetDirectory();

    protected abstract String getBuildToolWrapperDirectory();

    public abstract String getTestReportDirectory();

    protected abstract T buildResult(Path projectRoot);

    private String basePackageDirectoryPath(String prefix) {
        return Paths.get(prefix, basePackage.split(DOT)).toString();
    }
}
