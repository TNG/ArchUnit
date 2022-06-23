package com.tngtech.archunit.tooling.engines.surefire;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import com.tngtech.archunit.tooling.engines.surefire.MavenProjectLayout.MavenProject;
import com.tngtech.archunit.tooling.utils.AntReportParserAdapter;
import com.tngtech.archunit.tooling.utils.JUnitEngineResolver;
import com.tngtech.archunit.tooling.utils.TemporaryDirectoryUtils;
import com.tngtech.archunit.tooling.utils.TemporaryDirectoryUtils.ThrowableFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.cli.CommandLineException;
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

    private final AntReportParserAdapter parser = new AntReportParserAdapter();
    private final JUnitEngineResolver engineResolver = new JUnitEngineResolver();
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
    public TestReport execute(TestFile testFiles) throws Exception {
        return withProjectRoot(projectRoot -> {
            MavenProject mavenProject = prepareProjectDirectory(projectRoot);
            InvocationRequest request = prepareInvocationRequest(mavenProject, testFiles);
            LOG.info("Executing request with properties {}", request.getProperties());
            invokeRequest(request);
            return parser.parseReports(projectRoot, projectLayout.getTestReportDirectory());
        });
    }

    @Override
    public boolean reportsErrors() {
        return true;
    }

    private File getMavenExecutable() throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(getMavenExecutableName()).toURI());
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
    private InvocationRequest prepareInvocationRequest(MavenProject mavenProject, TestFile testFile) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(Objects.requireNonNull(mavenProject.getPomXml().getParent()).toFile());
        request.setPomFile(mavenProject.getPomXml().toFile());
        request.setGoals(Collections.singletonList(goal));
        request.setProperties(propertiesForTest(testFile));
        request.setMavenOpts("-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000");
        request.setDebug(false);
        request.setShowErrors(true);
        return request;
    }

    private Properties propertiesForTest(TestFile testFile) {
        Properties result = new Properties();
        result.setProperty("test", toIncludePattern(testFile));
        result.setProperty("surefire.includeJUnit5Engines", String.join(",", engineResolver.resolveJUnitEngines(testFile)));
        return result;
    }

    private String toIncludePattern(TestFile testFile) {
        String result = testFile.getFixture().getSimpleName();
        if (testFile.hasTestCasesFilter()) {
            result = result + "#" + String.join("+", testFile.getTestCases());
        }
        return result;
    }

    private <R> R withProjectRoot(ThrowableFunction<Path, R> action) throws Exception {
        return TemporaryDirectoryUtils.withTemporaryDirectory(action, "project-root");
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
}
