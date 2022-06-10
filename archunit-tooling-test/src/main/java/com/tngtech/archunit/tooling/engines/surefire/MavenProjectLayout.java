package com.tngtech.archunit.tooling.engines.surefire;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.tngtech.archunit.tooling.utils.JavaProjectLayout;

public class MavenProjectLayout extends JavaProjectLayout<MavenProjectLayout.MavenProject> {

    public MavenProjectLayout(
            String basePackage,
            String buildManifestLocation,
            String compiledClassesLocation,
            String sourcesLocation,
            String buildToolResourcesLocation) {
        super(basePackage, buildManifestLocation, compiledClassesLocation, sourcesLocation, buildToolResourcesLocation);
    }

    @Override
    protected String getCompiledTestClassesTargetDirectory() {
        return "target/test-classes";
    }

    @Override
    protected String getBuildToolWrapperDirectory() {
        return ".mvn";
    }

    @Override
    public String getTestReportDirectory() {
        return "target/surefire-reports";
    }

    @Override
    protected MavenProject buildResult(final Path projectRoot) {
        return new MavenProject(projectRoot.resolve("pom.xml"));
    }

    public static class MavenProject {
        private final Path pomXml;

        private MavenProject(Path pomXml) {
            this.pomXml = pomXml;
        }

        @Nonnull
        public Path getPomXml() {
            return pomXml;
        }
    }
}
