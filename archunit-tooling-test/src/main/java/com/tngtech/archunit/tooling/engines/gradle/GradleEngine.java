package com.tngtech.archunit.tooling.engines.gradle;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import com.tngtech.archunit.tooling.utils.ResourcesUtils;
import com.tngtech.archunit.tooling.utils.AntReportParserAdapter;
import com.tngtech.archunit.tooling.utils.TemporaryDirectoryUtils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

public enum GradleEngine implements TestEngine {
    FOR_TESTS_LOCATED_IN_EXAMPLES(new GradleProjectLayout(
            "com.tngtech.archunit.tooling.examples",
            "project/build.gradle",
            "",
            "@sources",
            "project"
    ));

    private static final Set<String> WILDCARD = Collections.singleton("");

    private static final String DOT = ".";

    private final AntReportParserAdapter parser = new AntReportParserAdapter();
    private final GradleProjectLayout projectLayout;

    GradleEngine(GradleProjectLayout projectLayout) {
        this.projectLayout = projectLayout;
    }

    @Override
    public TestReport execute(Set<TestFile> testFiles) throws Exception {
        return withProjectRoot(projectRoot -> {
            projectLayout.applyTo(projectRoot);
            GradleConnector gradleConnector = GradleConnector.newConnector()
                    .useDistribution(ResourcesUtils.getResourceUri("gradle-7.5-bin.zip"))
                    .forProjectDirectory(projectRoot.toFile());
            try (ProjectConnection connection = gradleConnector.connect()) {
                BuildLauncher launcher = prepareLauncher(testFiles, connection);
                launcher.run();
                return parser.parseReports(projectRoot, projectLayout.getTestReportDirectory());
            }
        });
    }

    private BuildLauncher prepareLauncher(Set<TestFile> testFiles, ProjectConnection connection) {
        return connection
                .newBuild()
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .forTasks("test")
                .withArguments(
                        "--full-stacktrace",
                        "-Ppatterns=" + String.join(",", toTestFilterArguments(testFiles)));
    }

    private Collection<String> toTestFilterArguments(Set<TestFile> testFiles) {
        return testFiles.stream()
                .map(this::toTestFilterArgument)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Collection<String> toTestFilterArgument(TestFile testFile) {
        String prefix = testFile.getFixture().getName();
        Collection<String> suffixes = testFile.hasTestCasesFilter() ? toTestCaseSuffixes(testFile) : WILDCARD;
        return suffixes.stream()
                .map(prefix::concat)
                .collect(Collectors.toList());
    }

    private Set<String> toTestCaseSuffixes(TestFile testFile) {
        return testFile.getTestCases().stream()
                .map(DOT::concat)
                .collect(Collectors.toSet());
    }

    private <R> R withProjectRoot(TemporaryDirectoryUtils.ThrowableFunction<Path, R> action) throws Exception {
        return TemporaryDirectoryUtils.withTemporaryDirectory(action, "project-root");
    }

    @Override
    public String toString() {
        return "Gradle";
    }
}
