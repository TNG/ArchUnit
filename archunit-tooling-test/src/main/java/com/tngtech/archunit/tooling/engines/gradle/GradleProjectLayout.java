package com.tngtech.archunit.tooling.engines.gradle;

import java.nio.file.Path;

import com.tngtech.archunit.tooling.utils.JavaProjectLayout;

public class GradleProjectLayout extends JavaProjectLayout<GradleProjectLayout.GradleProject> {

    public GradleProjectLayout(
            String basePackage,
            String buildManifestLocation,
            String compiledClassesLocation,
            String sourcesLocation,
            String buildToolResourcesLocation) {
        super(basePackage, buildManifestLocation, compiledClassesLocation, sourcesLocation, buildToolResourcesLocation);
    }

    @Override
    protected String getCompiledTestClassesTargetDirectory() {
        return "build/classes/java/test";
    }

    @Override
    protected String getBuildToolWrapperDirectory() {
        return "gradle";
    }

    @Override
    public String getTestReportDirectory() {
        return "build/test-results/test";
    }

    @Override
    protected GradleProject buildResult(Path projectRoot) {
        return new GradleProject(projectRoot.resolve("build.gradle"));
    }

    public static class GradleProject {
        private final Path buildGradle;

        public GradleProject(Path buildGradle) {
            this.buildGradle = buildGradle;
        }

        public Path getBuildGradle() {
            return buildGradle;
        }
    }
}
