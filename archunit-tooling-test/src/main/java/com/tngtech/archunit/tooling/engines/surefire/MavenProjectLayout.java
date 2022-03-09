package com.tngtech.archunit.tooling.engines.surefire;

import com.tngtech.archunit.tooling.utils.ResourcesUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenProjectLayout {

    private static final String DOT = "\\.";

    private final String basePackage;
    private final String pomLocation;
    private final String compiledClassesLocation;
    private final String sourcesLocation;
    private final String mavenResourcesLocation;

    public MavenProjectLayout(
            String basePackage,
            String pomLocation,
            String compiledClassesLocation,
            String sourcesLocation,
            String mavenResourcesLocation) {
        this.basePackage = basePackage;
        this.pomLocation = pomLocation;
        this.compiledClassesLocation = compiledClassesLocation;
        this.sourcesLocation = sourcesLocation;
        this.mavenResourcesLocation = mavenResourcesLocation;
    }

    public MavenProject applyTo(final Path projectRoot) throws IOException, URISyntaxException {
        ResourcesUtils.copyResource(projectRoot, pomLocation, "");
        ResourcesUtils.copyResourceDirectory(projectRoot, mavenResourcesLocation + "/.mvn", ".mvn");
        ResourcesUtils.copyResources(projectRoot, basePackageDirectoryPath(sourcesLocation), "glob:**.java", basePackageDirectoryPath("src/test/java"));
        ResourcesUtils.copyResources(projectRoot, basePackageDirectoryPath(compiledClassesLocation), "glob:**.class", basePackageDirectoryPath("target/test-classes"));
        return new MavenProject(projectRoot.resolve("pom.xml"));
    }

    private String basePackageDirectoryPath(String prefix) {
        return Paths.get(prefix, basePackage.split(DOT)).toString();
    }

    public static class MavenProject {
        private final Path pomXml;

        private MavenProject(Path pomXml) {
            this.pomXml = pomXml;
        }

        public Path getPomXml() {
            return pomXml;
        }
    }
}
