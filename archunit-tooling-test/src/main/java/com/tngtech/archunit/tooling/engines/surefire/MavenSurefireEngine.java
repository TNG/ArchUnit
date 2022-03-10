package com.tngtech.archunit.tooling.engines.surefire;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.tngtech.archunit.tooling.ExecutedTestFile;
import com.tngtech.archunit.tooling.ExecutedTestFile.TestResult;
import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import com.tngtech.archunit.tooling.engines.surefire.MavenProjectLayout.MavenProject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.surefire.shared.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MavenSurefireEngine implements TestEngine {
    FOR_TESTS_LOCATED_IN_EXAMPLES(new MavenProjectLayout(
            "com.tngtech.archunit.tooling.examples",
            "project/pom.xml",
            "",
            "@sources",
            "project")
    );

    private static final Logger LOG = LoggerFactory.getLogger(MavenSurefireEngine.class);

    private final MavenProjectLayout projectLayout;

    private final Invoker invoker;
    private final String goal;

    MavenSurefireEngine(final MavenProjectLayout projectLayout, final String goal) {
        try {
            this.invoker = initInvoker();
            this.goal = goal;
            this.projectLayout = projectLayout;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    MavenSurefireEngine(final MavenProjectLayout projectLayout) {
        this(projectLayout, "surefire:test");
    }

    private Invoker initInvoker() throws URISyntaxException {
        final Invoker invoker = new DefaultInvoker();
        File mavenExecutable = getMavenExecutable();
        invoker.setMavenExecutable(mavenExecutable);
        invoker.setMavenHome(mavenExecutable.getParentFile());
        return invoker;
    }

    @Override
    public TestReport execute(Set<TestFile> testFiles) throws Exception {
        return withProjectRoot(projectRoot -> {
            MavenProject mavenProject = prepareProjectDirectory(projectRoot);
            InvocationRequest request = prepareInvocationRequest(mavenProject, testFiles);
            LOG.info("Executing request with properties {}", request.getProperties());
            invokeRequest(request);
            SurefireReportParser parser = createParser(projectRoot);
            List<ReportTestSuite> reportTestSuites = parser.parseXMLReportFiles();
            return toTestReport(reportTestSuites);
        });
    }

    @Override
    public boolean reportsErrors() {
        return true;
    }

    private File getMavenExecutable() throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(getMavenExecutableName()).toURI());
    }

    private TestReport toTestReport(List<ReportTestSuite> reportTestSuites) {
        TestReport result = new TestReport();
        reportTestSuites.stream()
                .map(this::toTestFile)
                .forEach(result::addFile);
        return result;
    }

    private ExecutedTestFile toTestFile(ReportTestSuite reportTestSuite) {
        ExecutedTestFile result = new ExecutedTestFile(reportTestSuite.getFullClassName());
        reportTestSuite.getTestCases()
                .stream()
                .filter(reportTestCase -> !Strings.isNullOrEmpty(reportTestCase.getName()))
                .forEach(reportTestCase -> result.addResult(reportTestCase.getName(), resolveResult(reportTestCase)));
        return result;
    }

    private TestResult resolveResult(ReportTestCase reportTestCase) {
        if (reportTestCase.isSuccessful()) {
            return TestResult.SUCCESS;
        }
        if (reportTestCase.hasFailure()) {
            return TestResult.FAILURE;
        }
        if (reportTestCase.hasError()) {
            return TestResult.ERROR;
        }
        if (reportTestCase.hasSkipped()) {
            return TestResult.SKIPPED;
        }
        throw new IllegalArgumentException("Cannot determine test result for " + reportTestCase.getFullName());
    }

    private SurefireReportParser createParser(Path projectRoot) {
        return new SurefireReportParser(
                Collections.singletonList(projectRoot.resolve("target/surefire-reports").toFile()),
                Locale.US,
                new NullConsoleLogger());
    }

    private MavenProject prepareProjectDirectory(Path projectRoot) throws IOException, URISyntaxException {
        return projectLayout.applyTo(projectRoot);
    }

    private void invokeRequest(InvocationRequest request) throws MavenInvocationException, CommandLineException {
        InvocationResult result = invoker.execute(request);
        if (result.getExecutionException() != null) {
            throw result.getExecutionException();
        }
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private InvocationRequest prepareInvocationRequest(MavenProject mavenProject, Set<TestFile> testFiles) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(Objects.requireNonNull(mavenProject.getPomXml().getParent()).toFile());
        request.setPomFile(mavenProject.getPomXml().toFile());
        request.setGoals(Collections.singletonList(goal));
        request.setProperties(propertiesWithTest(toTestProperty(testFiles)));
        return request;
    }

    private Properties propertiesWithTest(String filter) {
        Properties result = new Properties();
        result.setProperty("test", filter);
        return result;
    }

    private String toTestProperty(Set<TestFile> testFiles) {
        return testFiles.stream()
                .map(this::toIncludePattern)
                .collect(Collectors.joining(","));
    }

    private String toIncludePattern(TestFile testFile) {
        String result = testFile.getFixture().getSimpleName();
        if (testFile.hasTestCasesFilter()) {
            result = result + "#" + String.join("+", testFile.getTestCases());
        }
        return result;
    }

    private <R> R withProjectRoot(ThrowableFunction<Path, R> action) throws Exception {
        Path projectRoot = null;
        try {
            projectRoot = Files.createTempDirectory("project-root");
            return action.apply(projectRoot);
        } finally {
            FileUtils.deleteDirectory(Objects.requireNonNull(projectRoot).toFile());
        }
    }

    private String getMavenExecutableName() {
        return "project/mvnw" + (isWindows() ? ".cmd" : "");
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
    }

    @Override
    public String toString() {
        return "Maven Surefire";
    }

    @FunctionalInterface
    private interface ThrowableFunction<T, R> {

        R apply(T t) throws Exception;

    }
}
