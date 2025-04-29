package com.tngtech.archunit.library.plantuml.rules;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.diagramtests.multipledependencies.origin.SomeOrigin;
import com.tngtech.archunit.library.diagramtests.multipledependencies.target.SomeTarget;
import com.tngtech.archunit.library.diagramtests.simpledependency.origin.SomeOriginClass;
import com.tngtech.archunit.library.diagramtests.simpledependency.target.SomeTargetClass;
import com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.ArchRule.Assertions.assertNoViolation;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringAllDependencies;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInDiagram;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.adhereToPlantUmlDiagram;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlantUmlArchConditionTest {
    @TempDir
    File temporaryFolder;

    static Stream<PlantUmlCreationTestCase> possibleInputTypes() {
        return Stream.of(
                new PlantUmlCreationTestCase("file") {
                    @Override
                    PlantUmlArchCondition createConditionFrom(File file) {
                        return adhereToPlantUmlDiagram(file, consideringOnlyDependenciesInDiagram());
                    }
                }, new PlantUmlCreationTestCase("URL") {
                    @Override
                    PlantUmlArchCondition createConditionFrom(File file) {
                        return adhereToPlantUmlDiagram(toUrl(file), consideringOnlyDependenciesInDiagram());
                    }
                }, new PlantUmlCreationTestCase("path") {
                    @Override
                    PlantUmlArchCondition createConditionFrom(File file) {
                        return adhereToPlantUmlDiagram(file.toPath(), consideringOnlyDependenciesInDiagram());
                    }
                }, new PlantUmlCreationTestCase("file name") {
                    @Override
                    PlantUmlArchCondition createConditionFrom(File file) {
                        return adhereToPlantUmlDiagram(file.getAbsolutePath(), consideringOnlyDependenciesInDiagram());
                    }
                }
        );
    }

    @ParameterizedTest
    @MethodSource("possibleInputTypes")
    void can_handle_all_possible_user_inputs_without_violations(PlantUmlCreationTestCase testCase) {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin")
                .component("SomeTarget").withStereoTypes("..target")
                .dependencyFrom("[SomeOrigin]").to("[SomeTarget]")
                .write();

        PlantUmlArchCondition condition = testCase.createConditionFrom(file);
        EvaluationResult result = createEvaluationResult(condition, "simpledependency");
        assertNoViolation(result);
    }

    @ParameterizedTest
    @MethodSource("possibleInputTypes")
    void can_handle_all_possible_user_inputs_with_violations(PlantUmlCreationTestCase testCase) {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin")
                .component("SomeTarget").withStereoTypes("..target")
                .dependencyFrom("[SomeTarget]").to("[SomeOrigin]")
                .write();

        PlantUmlArchCondition condition = testCase.createConditionFrom(file);
        EvaluationResult result = createEvaluationResult(condition, "simpledependency");

        assertThat(result.getFailureReport().toString())
                .containsPattern(String.format(".*%s.*(calls|extends).*%s.*",
                        SomeOriginClass.class.getSimpleName(),
                        SomeTargetClass.class.getSimpleName()));
    }

    @Test
    public void diagram_with_dependencies_to_java_lang() {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin")
                .component("SomeTarget").withStereoTypes("..target")
                .dependencyFrom("[SomeOrigin]").to("[SomeTarget]")
                .write();

        String reportedDependencyOnJavaLangPattern = String.format(".*%s.*(calls|extends).*%s.*",
                SomeTargetClass.class.getSimpleName(), Object.class.getSimpleName());

        assertThatEvaluatedConditionWithConfiguration(file, consideringAllDependencies())
                .has(lineMatching(reportedDependencyOnJavaLangPattern));

        assertThatEvaluatedConditionWithConfiguration(file, consideringOnlyDependenciesInDiagram())
                .doesNotHave(lineMatching(reportedDependencyOnJavaLangPattern));
    }

    @Test
    public void diagram_with_no_dependencies() {
        File file = TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes("..independent")
                .component("B").withStereoTypes("..somepackage")
                .write();

        assertNoViolation(createEvaluationResult(file, "nodependencies"));
    }

    @Test
    public void defined_but_unused_dependency_is_allowed() {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..independent")
                .component("SomeTarget").withStereoTypes("..somepackage")
                .dependencyFrom("SomeOrigin").to("SomeTarget")
                .write();

        assertNoViolation(createEvaluationResult(file, "nodependencies"));
    }

    @Test
    public void class_must_be_contained_within_the_diagram_if_it_has_relevant_dependencies() {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeComponent").withStereoTypes("..someStereotype.")
                .write();
        JavaClasses notContained = importClasses(Object.class);
        PlantUmlArchCondition condition = adhereToPlantUmlDiagram(file, consideringAllDependencies());

        assertThatRule(classes().should(condition))
                .checking(notContained)
                .hasOnlyOneViolation(String.format(
                        "Class %s is not contained in any component",
                        getOnlyElement(notContained).getName()));

        assertThatRule(classes().should(condition.ignoreDependenciesWithOrigin(equivalentTo(Object.class))))
                .checking(notContained)
                .hasNoViolation();
    }

    @Test
    public void class_must_not_be_contained_in_multiple_components_if_it_has_relevant_dependencies() {
        File file = TestDiagram.in(temporaryFolder)
                .component("First").withStereoTypes("java..")
                .component("Second").withStereoTypes("..lang..")
                .write();
        JavaClasses containedInTwoComponents = importClasses(Object.class);
        PlantUmlArchCondition condition = adhereToPlantUmlDiagram(file, consideringAllDependencies());

        assertThatRule(classes().should(condition))
                .checking(containedInTwoComponents)
                .hasOnlyOneViolation(String.format(
                        "Class %s may not be contained in more than one component, but is contained in [First, Second]",
                        getOnlyElement(containedInTwoComponents).getName()));

        assertThatRule(classes().should(condition.ignoreDependenciesWithOrigin(equivalentTo(Object.class))))
                .checking(containedInTwoComponents)
                .hasNoViolation();
    }

    @Test
    public void diagram_with_multiple_dependencies_that_considers_only_certain_packages() {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin")
                .component("SomeIntermediary").withStereoTypes("..intermediary")
                .component("SomeTarget").withStereoTypes("..target")
                .dependencyFrom("SomeOrigin").to("SomeIntermediary")
                .dependencyFrom("SomeIntermediary").to("SomeTarget")
                .write();

        JavaClasses classes = getClassesFrom("multipledependencies");

        PlantUmlArchCondition condition = adhereToPlantUmlDiagram(file,
                consideringOnlyDependenciesInAnyPackage("..origin", "..intermediary", "..target"));
        assertConditionHasNumberOfFailures(classes, condition, 3);

        condition = adhereToPlantUmlDiagram(file,
                consideringOnlyDependenciesInAnyPackage("..origin", "..intermediary"));
        assertConditionHasNumberOfFailures(classes, condition, 2);

        condition = adhereToPlantUmlDiagram(file,
                consideringOnlyDependenciesInAnyPackage("..origin"));
        assertConditionHasNumberOfFailures(classes, condition, 1);
    }

    @Test
    public void diagram_with_multiple_dependencies_that_ignores_violations() {
        File file = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin")
                .component("SomeIntermediary").withStereoTypes("..intermediary")
                .component("SomeTarget").withStereoTypes("..target")
                .dependencyFrom("SomeOrigin").to("SomeIntermediary")
                .dependencyFrom("SomeIntermediary").to("SomeTarget")
                .write();

        JavaClasses classes = getClassesFrom("multipledependencies");
        PlantUmlArchCondition condition = adhereToPlantUmlDiagram(file, consideringOnlyDependenciesInDiagram());

        assertConditionHasNumberOfFailures(classes, condition,
                3);

        assertConditionHasNumberOfFailures(classes, condition
                        .ignoreDependenciesWithOrigin(equivalentTo(SomeTarget.class)),
                2);

        assertConditionHasNumberOfFailures(classes, condition
                        .ignoreDependenciesWithOrigin(equivalentTo(SomeTarget.class))
                        .ignoreDependenciesWithTarget(equivalentTo(SomeOrigin.class)),
                1);

        assertConditionHasNumberOfFailures(classes, condition
                        .ignoreDependenciesWithOrigin(equivalentTo(SomeTarget.class))
                        .ignoreDependenciesWithTarget(equivalentTo(SomeOrigin.class))
                        .ignoreDependencies(SomeOrigin.class, SomeTarget.class),
                0);
    }

    @Test
    public void diagram_with_unparseable_content() throws IOException {
        File file = new File(temporaryFolder, "plantuml_diagram_" + UUID.randomUUID() + ".puml");
        Files.write(file.toPath(), "XXX-someUnparseableContent-XXX".getBytes(UTF_8));

        assertThatThrownBy(() -> adhereToPlantUmlDiagram(file, consideringOnlyDependenciesInDiagram()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No components defined in diagram <%s>", toUrl(file));
    }

    private ListAssert<String> assertThatEvaluatedConditionWithConfiguration(
            File diagramFile, Configuration configuration) {
        PlantUmlArchCondition condition = adhereToPlantUmlDiagram(diagramFile, configuration);
        EvaluationResult result = createEvaluationResult(condition, "simpledependency");
        return assertThat(result.getFailureReport().getDetails());
    }

    private Condition<List<? extends String>> lineMatching(String pattern) {
        return new Condition<List<? extends String>>(String.format("line matching '%s'", pattern)) {
            @Override
            public boolean matches(List<? extends String> lines) {
                return lines.stream().anyMatch(line -> line.matches(pattern));
            }
        };
    }

    private void assertConditionHasNumberOfFailures(JavaClasses classes, PlantUmlArchCondition condition, int expectedNumberOfFailures) {
        EvaluationResult result = classes().should(condition).evaluate(classes);
        assertThat(result.getFailureReport().getDetails())
                .as("number of failures").hasSize(expectedNumberOfFailures);
    }

    private static JavaClasses getClassesFrom(String pkg) {
        String packageName = "com.tngtech.archunit.library.diagramtests." + pkg;
        JavaClasses classes = new ClassFileImporter().importPackages(packageName);
        if (isEmpty(classes)) {
            throw new IllegalStateException(
                    String.format("No classes were imported from '%s'", packageName));
        }
        return classes;
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private EvaluationResult createEvaluationResult(File file, String packageToImport) {
        return createEvaluationResult(adhereToPlantUmlDiagram(file, consideringOnlyDependenciesInDiagram()), packageToImport);
    }

    private EvaluationResult createEvaluationResult(PlantUmlArchCondition condition, String packageToImport) {
        ArchRule rule = classes().should(condition);
        JavaClasses classes = getClassesFrom(packageToImport);
        return rule.evaluate(classes);
    }

    abstract static class PlantUmlCreationTestCase {
        private final String description;

        PlantUmlCreationTestCase(String description) {
            this.description = description;
        }

        abstract PlantUmlArchCondition createConditionFrom(File file);

        @Override
        public String toString() {
            return PlantUmlArchCondition.class.getSimpleName() + " created from " + description;
        }
    }
}
