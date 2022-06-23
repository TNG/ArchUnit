package com.tngtech.archunit.tooling.engines.gradle;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import com.tngtech.archunit.tooling.utils.AntReportParserAdapter;
import com.tngtech.archunit.tooling.utils.JUnitEngineResolver;
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
    private final JUnitEngineResolver engineResolver = new JUnitEngineResolver();
    private final GradleProjectLayout projectLayout;

    GradleEngine(GradleProjectLayout projectLayout) {
        this.projectLayout = projectLayout;
    }

    @Override
    public TestReport execute(TestFile testFile) throws Exception {
        return withProjectRoot(projectRoot -> {
            projectLayout.applyTo(projectRoot);
            GradleConnector gradleConnector = GradleConnector.newConnector()
                    .useGradleVersion("7.4.2")
                    .forProjectDirectory(projectRoot.toFile());
            try (ProjectConnection connection = gradleConnector.connect()) {
                BuildLauncher launcher = prepareLauncher(testFile, connection);
                launcher.run();
                return parser.parseReports(projectRoot, projectLayout.getTestReportDirectory());
            }
        });
    }

    private BuildLauncher prepareLauncher(TestFile testFile, ProjectConnection connection) {
        return connection
                .newBuild()
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .forTasks("test")
                .withArguments(
                        "--debug",
                        "--full-stacktrace",
                        "-Ppatterns=" + String.join(",", toTestFilterArgument(testFile)),
                        "-Pengines=" + String.join(",", engineResolver.resolveJUnitEngines(testFile)));
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
