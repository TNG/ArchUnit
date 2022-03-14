package com.tngtech.archunit.tooling.utils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Strings;
import com.tngtech.archunit.tooling.ExecutedTestFile;
import com.tngtech.archunit.tooling.TestReport;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

public class AntReportParserAdapter {

    public TestReport parseReports(Path projectRoot, String reportDirectory) throws MavenReportException {
        SurefireReportParser parser = createParser(projectRoot, reportDirectory);
        List<ReportTestSuite> reportTestSuites = parser.parseXMLReportFiles();
        return toTestReport(reportTestSuites);
    }

    public SurefireReportParser createParser(Path projectRoot, String reportDirectory) {
        return new SurefireReportParser(
                Collections.singletonList(projectRoot.resolve(reportDirectory).toFile()),
                Locale.US,
                new NullConsoleLogger());
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

    private ExecutedTestFile.TestResult resolveResult(ReportTestCase reportTestCase) {
        if (reportTestCase.isSuccessful()) {
            return ExecutedTestFile.TestResult.SUCCESS;
        }
        if (reportTestCase.hasFailure()) {
            return ExecutedTestFile.TestResult.FAILURE;
        }
        if (reportTestCase.hasError()) {
            return ExecutedTestFile.TestResult.ERROR;
        }
        if (reportTestCase.hasSkipped()) {
            return ExecutedTestFile.TestResult.SKIPPED;
        }
        throw new IllegalArgumentException("Cannot determine test result for " + reportTestCase.getFullName());
    }
}
