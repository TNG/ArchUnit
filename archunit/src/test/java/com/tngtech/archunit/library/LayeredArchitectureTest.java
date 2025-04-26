package com.tngtech.archunit.library;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.tngtech.archunit.library.testclasses.coveringallclasses.first.First;
import com.tngtech.archunit.library.testclasses.coveringallclasses.second.Second;
import com.tngtech.archunit.library.testclasses.coveringallclasses.third.Third;
import com.tngtech.archunit.library.testclasses.dependencysettings.DependencySettingsOutsideOfLayersAccessingLayers;
import com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_backwards.DependencySettingsForbiddenByMayOnlyBeAccessed;
import com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_forwards.DependencySettingsForbiddenByMayOnlyAccess;
import com.tngtech.archunit.library.testclasses.dependencysettings.origin.DependencySettingsOriginClass;
import com.tngtech.archunit.library.testclasses.dependencysettings_outside.DependencySettingsOutsideOfLayersBeingAccessedByLayers;
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyClass;
import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.forbidden.MayOnlyAccessLayersForbiddenClass;
import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.origin.MayOnlyAccessLayersOriginClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.SomePkgClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubclass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.archunit.testutil.TestUtils.union;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class LayeredArchitectureTest {
    private static final String NEW_LINE_REPLACE = "###";

    static Stream<LayeredArchitecture> layeredArchitectureDefinitions() {
        return Stream.of(
                layeredArchitecture()
                        .consideringAllDependencies()
                        .layer("One").definedBy("..library.testclasses.some.pkg..")
                        .layer("Two").definedBy("..library.testclasses.first.any.pkg..", "..library.testclasses.second.any.pkg..")
                        .optionalLayer("Three").definedBy("..library.testclasses..three..")
                        .whereLayer("One").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                        .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two"),
                layeredArchitecture()
                        .consideringAllDependencies()
                        .layer("One").definedBy(
                                resideInAnyPackage("..library.testclasses.some.pkg..")
                                        .as("'..library.testclasses.some.pkg..'"))
                        .layer("Two").definedBy(
                                resideInAnyPackage("..library.testclasses.first.any.pkg..", "..library.testclasses.second.any.pkg..")
                                        .as("'..library.testclasses.first.any.pkg..', '..library.testclasses.second.any.pkg..'"))
                        .optionalLayer("Three").definedBy(
                                resideInAnyPackage("..library.testclasses..three..")
                                        .as("'..library.testclasses..three..'"))
                        .whereLayer("One").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Two").mayOnlyBeAccessedByLayers("One")
                        .whereLayer("Three").mayOnlyBeAccessedByLayers("One", "Two"));
    }

    @ParameterizedTest
    @MethodSource("layeredArchitectureDefinitions")
    void layered_architecture_description(LayeredArchitecture architecture) {
        assertThat(architecture.getDescription()).isEqualTo(
                "Layered architecture considering all dependencies, consisting of" + lineSeparator() +
                        "layer 'One' ('..library.testclasses.some.pkg..')" + lineSeparator() +
                        "layer 'Two' ('..library.testclasses.first.any.pkg..', '..library.testclasses.second.any.pkg..')" + lineSeparator() +
                        "optional layer 'Three' ('..library.testclasses..three..')" + lineSeparator() +
                        "where layer 'One' may not be accessed by any layer" + lineSeparator() +
                        "where layer 'Two' may only be accessed by layers ['One']" + lineSeparator() +
                        "where layer 'Three' may only be accessed by layers ['One', 'Two']");
    }

    @Test
    public void layered_architecture_overridden_description() {
        LayeredArchitecture architecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden");

        assertThat(architecture.getDescription()).isEqualTo("overridden");
    }

    @Test
    public void layered_architecture_because_clause() {
        ArchRule architecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("One").definedBy("some.pkg..")
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .as("overridden")
                .because("some reason");

        assertThat(architecture.getDescription()).isEqualTo("overridden, because some reason");
    }

    @Test
    public void layered_architecture_defining_constraint_on_non_existing_target_layer_is_rejected() {
        assertThatThrownBy(() -> layeredArchitecture()
                .consideringAllDependencies()
                .layer("Some").definedBy("any")
                .whereLayer("NotThere").mayNotBeAccessedByAnyLayer()
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no layer")
                .hasMessageContaining("NotThere");
    }

    @Test
    public void layered_architecture_defining_constraint_on_non_existing_origin_is_rejected() {
        assertThatThrownBy(() -> layeredArchitecture()
                .consideringAllDependencies()
                .layer("Some").definedBy("any")
                .whereLayer("Some").mayOnlyBeAccessedByLayers("NotThere")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no layer")
                .hasMessageContaining("NotThere");
    }

    @Test
    public void layered_architecture_rejects_empty_layers_by_default() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers();

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureLayeredArchitectureWithEmptyLayers(result);
    }

    @Test
    public void layered_architecture_allows_empty_layers_if_all_layers_are_optional() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers().withOptionalLayers(true);
        assertThat(architecture.getDescription()).startsWith("Layered architecture considering all dependencies, consisting of (optional)");

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        assertThatRule(architecture).checking(classes).hasNoViolation();
    }

    @Test
    public void layered_architecture_rejects_empty_layers_if_layers_are_explicitly_not_optional_by_default() {
        LayeredArchitecture architecture = aLayeredArchitectureWithEmptyLayers().withOptionalLayers(false);

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertFailureLayeredArchitectureWithEmptyLayers(result);
    }

    private LayeredArchitecture aLayeredArchitectureWithEmptyLayers() {
        return layeredArchitecture()
                .consideringAllDependencies()
                .layer("Some").definedBy(absolute("should.not.be.found.."))
                .layer("Other").definedBy(absolute("also.not.found"))
                .layer("Okay").definedBy("..testclasses..")
                .whereLayer("Other").mayOnlyBeAccessedByLayers("Some");
    }

    private void assertFailureLayeredArchitectureWithEmptyLayers(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result of evaluating empty layers has violation").isTrue();
        assertPatternMatches(result.getFailureReport().getDetails(),
                ImmutableSet.of(expectedEmptyLayerPattern("Some"), expectedEmptyLayerPattern("Other")));
    }

    @Test
    public void layered_architecture_allows_individual_empty_optionalLayer() {
        LayeredArchitecture architecture = layeredArchitecture()
                .consideringAllDependencies()
                .optionalLayer("can be absent").definedBy(absolute("should.not.be.found.."));

        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);
        assertThat(result.hasViolation()).as("result of evaluating empty optionalLayer has violation").isFalse();
        assertThat(result.getFailureReport().isEmpty()).as("failure report").isTrue();
    }

    @ParameterizedTest
    @MethodSource("layeredArchitectureDefinitions")
    void layered_architecture_gathers_all_layer_violations(LayeredArchitecture architecture) {
        JavaClasses classes = new ClassFileImporter().importPackages(absolute(""));

        EvaluationResult result = architecture.evaluate(classes);

        assertPatternMatches(result.getFailureReport().getDetails(),
                ImmutableSet.of(
                        expectedAccessViolationPattern(FirstAnyPkgClass.class, "call", SomePkgSubclass.class, "callMe"),
                        expectedAccessViolationPattern(SecondThreeAnyClass.class, "call", SomePkgClass.class, "callMe"),
                        expectedAccessViolationPattern(FirstThreeAnyClass.class, "call", FirstAnyPkgClass.class, "callMe"),
                        expectedFieldTypePattern(FirstAnyPkgClass.class, "illegalTarget", SomePkgSubclass.class),
                        expectedFieldTypePattern(FirstThreeAnyClass.class, "illegalTarget", FirstAnyPkgClass.class),
                        expectedFieldTypePattern(SecondThreeAnyClass.class, "illegalTarget", SomePkgClass.class)));
    }

    static Stream<RuleWithIgnore> toIgnore() {
        LayeredArchitecture layeredArchitecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("One").definedBy(absolute("some.pkg.."))
                .whereLayer("One").mayNotBeAccessedByAnyLayer();

        return Stream.of(
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(FirstAnyPkgClass.class, SomePkgSubclass.class),
                        "rule with ignore specified as class objects"),
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(FirstAnyPkgClass.class.getName(), SomePkgSubclass.class.getName()),
                        "rule with ignore specified as class names"),
                new RuleWithIgnore(
                        layeredArchitecture.ignoreDependency(name(FirstAnyPkgClass.class.getName()), name(SomePkgSubclass.class.getName())),
                        "rule with ignore specified as predicates"));
    }

    @ParameterizedTest
    @MethodSource("toIgnore")
    void layered_architecture_ignores_specified_violations(RuleWithIgnore layeredArchitectureWithIgnore) {
        JavaClasses classes = new ClassFileImporter().importClasses(
                FirstAnyPkgClass.class, SomePkgSubclass.class,
                SecondThreeAnyClass.class, SomePkgClass.class);

        EvaluationResult result = layeredArchitectureWithIgnore.rule.evaluate(classes);

        assertThat(singleLine(result))
                .doesNotMatch(String.format(".*%s[^%s]*%s.*",
                        quote(FirstAnyPkgClass.class.getName()), NEW_LINE_REPLACE, quote(SomePkgSubclass.class.getName())))
                .matches(String.format(".*%s[^%s]*%s.*",
                        quote(SecondThreeAnyClass.class.getName()), NEW_LINE_REPLACE, quote(SomePkgClass.class.getName())));
    }

    @Test
    public void layered_architecture_combines_multiple_ignores() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                FirstAnyPkgClass.class, SomePkgSubclass.class,
                SecondThreeAnyClass.class, SomePkgClass.class);

        LayeredArchitecture layeredArchitecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("One").definedBy(absolute("some.pkg.."))
                .whereLayer("One").mayNotBeAccessedByAnyLayer()
                .ignoreDependency(FirstAnyPkgClass.class, SomePkgSubclass.class);

        assertThat(layeredArchitecture.evaluate(classes).hasViolation()).as("result has violation").isTrue();

        layeredArchitecture = layeredArchitecture
                .ignoreDependency(SecondThreeAnyClass.class, SomePkgClass.class);

        assertThat(layeredArchitecture.evaluate(classes).hasViolation()).as("result has violation").isFalse();
    }

    static Stream<LayeredArchitecture> layeredArchitectureMayOnlyAccessLayersDefinitions() {
        return Stream.of(
                layeredArchitecture()
                        .consideringAllDependencies()
                        .layer("Allowed").definedBy("..library.testclasses.mayonlyaccesslayers.allowed..")
                        .layer("Forbidden").definedBy("..library.testclasses.mayonlyaccesslayers.forbidden..")
                        .layer("Origin").definedBy("..library.testclasses.mayonlyaccesslayers.origin..")
                        .whereLayer("Origin").mayOnlyAccessLayers("Allowed")
                        .whereLayer("Forbidden").mayNotAccessAnyLayer(),
                layeredArchitecture()
                        .consideringAllDependencies()
                        .layer("Allowed").definedBy(
                                resideInAnyPackage("..library.testclasses.mayonlyaccesslayers.allowed..")
                                        .as("'..library.testclasses.mayonlyaccesslayers.allowed..'"))
                        .layer("Forbidden").definedBy(
                                resideInAnyPackage("..library.testclasses.mayonlyaccesslayers.forbidden..")
                                        .as("'..library.testclasses.mayonlyaccesslayers.forbidden..'"))
                        .layer("Origin").definedBy(
                                resideInAnyPackage("..library.testclasses.mayonlyaccesslayers.origin..")
                                        .as("'..library.testclasses.mayonlyaccesslayers.origin..'"))
                        .whereLayer("Origin").mayOnlyAccessLayers("Allowed")
                        .whereLayer("Forbidden").mayNotAccessAnyLayer());
    }

    @ParameterizedTest
    @MethodSource("layeredArchitectureMayOnlyAccessLayersDefinitions")
    void layered_architecture_may_only_access_layers_description(LayeredArchitecture architecture) {
        assertThat(architecture.getDescription()).isEqualTo(
                "Layered architecture considering all dependencies, consisting of" + lineSeparator() +
                        "layer 'Allowed' ('..library.testclasses.mayonlyaccesslayers.allowed..')" + lineSeparator() +
                        "layer 'Forbidden' ('..library.testclasses.mayonlyaccesslayers.forbidden..')" + lineSeparator() +
                        "layer 'Origin' ('..library.testclasses.mayonlyaccesslayers.origin..')" + lineSeparator() +
                        "where layer 'Origin' may only access layers ['Allowed']" + lineSeparator() +
                        "where layer 'Forbidden' may not access any layer");
    }

    @ParameterizedTest
    @MethodSource("layeredArchitectureMayOnlyAccessLayersDefinitions")
    void layered_architecture_gathers_may_only_access_layers_violations(LayeredArchitecture architecture) {
        JavaClasses classes = new ClassFileImporter().importPackages(absolute("mayonlyaccesslayers"));

        EvaluationResult result = architecture.evaluate(classes);

        assertPatternMatches(result.getFailureReport().getDetails(),
                ImmutableSet.of(
                        expectedAccessViolationPattern(
                                MayOnlyAccessLayersOriginClass.class, "call", MayOnlyAccessLayersForbiddenClass.class, "callMe"),
                        expectedAccessViolationPattern(MayOnlyAccessLayersOriginClass.class, CONSTRUCTOR_NAME, Object.class, CONSTRUCTOR_NAME),
                        expectedAccessViolationPattern(MayOnlyAccessLayersForbiddenClass.class, CONSTRUCTOR_NAME, Object.class, CONSTRUCTOR_NAME),
                        expectedAccessViolationPattern(MayOnlyAccessLayersForbiddenClass.class, "callMe", MayOnlyAccessLayersOriginClass.class, CONSTRUCTOR_NAME),
                        expectedInheritancePattern(MayOnlyAccessLayersOriginClass.class, Object.class),
                        expectedInheritancePattern(MayOnlyAccessLayersForbiddenClass.class, Object.class),
                        expectedFieldTypePattern(MayOnlyAccessLayersOriginClass.class, "illegalTarget", MayOnlyAccessLayersForbiddenClass.class)));
    }

    @ParameterizedTest
    @MethodSource("layeredArchitectureMayOnlyAccessLayersDefinitions")
    void layered_architecture_can_ignore_may_only_access_layers_violations(LayeredArchitecture architecture) {
        JavaClasses classes = new ClassFileImporter().importPackages(absolute("mayonlyaccesslayers"));

        architecture = architecture.ignoreDependency(MayOnlyAccessLayersOriginClass.class, MayOnlyAccessLayersForbiddenClass.class)
                .ignoreDependency(MayOnlyAccessLayersOriginClass.class, Object.class)
                .ignoreDependency(MayOnlyAccessLayersForbiddenClass.class, Object.class)
                .ignoreDependency(MayOnlyAccessLayersForbiddenClass.class, MayOnlyAccessLayersOriginClass.class);

        assertThat(architecture.evaluate(classes).hasViolation()).as("result has violation").isFalse();
    }

    @Test
    public void layered_architecture_supports_dependency_setting_considering_all_dependencies() {
        LayeredArchitecture layeredArchitecture = defineLayeredArchitectureForDependencySettings(
                layeredArchitecture().consideringAllDependencies());

        EvaluationResult result = layeredArchitecture.evaluate(new ClassFileImporter().importPackages(absolute("dependencysettings")));

        assertThat(layeredArchitecture.getDescription()).startsWith("Layered architecture considering all dependencies, consisting of");
        assertPatternMatches(result.getFailureReport().getDetails(),
                union(
                        dependencySettingsViolationsInLayers(),
                        dependencySettingsViolationsOutsideOfLayers(),
                        dependencySettingsViolationsByJavaLang()
                ));
    }

    @Test
    public void layered_architecture_supports_dependency_setting_considering_only_dependencies_in_any_package() {
        LayeredArchitecture layeredArchitecture = defineLayeredArchitectureForDependencySettings(
                layeredArchitecture().consideringOnlyDependenciesInAnyPackage("..dependencysettings..", "..dependencysettings_outside.."));

        EvaluationResult result = layeredArchitecture.evaluate(new ClassFileImporter().importPackages(absolute("dependencysettings")));

        assertThat(layeredArchitecture.getDescription()).startsWith(
                "Layered architecture considering only dependencies in any package ['..dependencysettings..', '..dependencysettings_outside..'], consisting of");
        assertPatternMatches(result.getFailureReport().getDetails(),
                union(
                        dependencySettingsViolationsInLayers(),
                        dependencySettingsViolationsOutsideOfLayers()
                ));
    }

    @Test
    public void layered_architecture_supports_dependency_setting_considering_only_dependencies_in_layers() {
        LayeredArchitecture layeredArchitecture = defineLayeredArchitectureForDependencySettings(
                layeredArchitecture().consideringOnlyDependenciesInLayers());

        EvaluationResult result = layeredArchitecture.evaluate(new ClassFileImporter().importPackages(absolute("dependencysettings")));

        assertThat(layeredArchitecture.getDescription()).startsWith(
                "Layered architecture considering only dependencies in layers, consisting of");
        assertPatternMatches(result.getFailureReport().getDetails(), dependencySettingsViolationsInLayers());
    }

    @Test
    public void layered_architecture_ensure_all_classes_are_contained_in_architecture() {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class);

        LayeredArchitecture architectureNotCoveringAllClasses = layeredArchitecture().consideringAllDependencies()
                .layer("One").definedBy("..first..")
                .ensureAllClassesAreContainedInArchitecture();

        assertThatRule(architectureNotCoveringAllClasses).checking(classes)
                .hasOnlyOneViolation(
                        object -> object instanceof JavaClass && ((JavaClass) object).isEquivalentTo(Second.class),
                        "Class <" + Second.class.getName() + "> is not contained in architecture");

        LayeredArchitecture architectureCoveringAllClasses = architectureNotCoveringAllClasses
                .layer("Two").definedBy("..second..");
        assertThatRule(architectureCoveringAllClasses).checking(classes).hasNoViolation();
    }

    @Test
    public void layered_architecture_ensure_all_classes_are_contained_in_architecture_ignoring_packages() {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class, Third.class);

        LayeredArchitecture architecture = layeredArchitecture().consideringAllDependencies()
                .layer("One").definedBy("..first..")
                .ensureAllClassesAreContainedInArchitectureIgnoring("..second..");

        assertThatRule(architecture).checking(classes)
                .hasOnlyOneViolation("Class <" + Third.class.getName() + "> is not contained in architecture");
    }

    @Test
    public void layered_architecture_ensure_all_classes_are_contained_in_architecture_ignoring_predicate() {
        JavaClasses classes = new ClassFileImporter().importClasses(First.class, Second.class, Third.class);

        LayeredArchitecture architecture = layeredArchitecture().consideringAllDependencies()
                .layer("One").definedBy("..first..")
                .ensureAllClassesAreContainedInArchitectureIgnoring(simpleName("Second"));

        assertThatRule(architecture).checking(classes)
                .hasOnlyOneViolation("Class <" + Third.class.getName() + "> is not contained in architecture");
    }

    private LayeredArchitecture defineLayeredArchitectureForDependencySettings(LayeredArchitecture layeredArchitecture) {
        return layeredArchitecture
                .layer("Origin").definedBy("..library.testclasses.dependencysettings.origin..")
                .layer("Allowed").definedBy("..library.testclasses.dependencysettings.allowed..")
                .layer("ForbiddenByMayOnlyAccess").definedBy("..library.testclasses.dependencysettings.forbidden_forwards..")
                .layer("ForbiddenByMayOnlyBeAccessed").definedBy("..library.testclasses.dependencysettings.forbidden_backwards..")
                .whereLayer("Origin").mayOnlyAccessLayers("Allowed")
                .whereLayer("Origin").mayNotBeAccessedByAnyLayer()
                .whereLayer("ForbiddenByMayOnlyBeAccessed").mayNotBeAccessedByAnyLayer()
                .whereLayer("ForbiddenByMayOnlyBeAccessed").mayNotAccessAnyLayer();
    }

    private Set<String> dependencySettingsViolationsByJavaLang() {
        return ImmutableSet.of(
                expectedInheritancePattern(DependencySettingsOriginClass.class, Object.class),
                expectedAccessViolationPattern(DependencySettingsOriginClass.class, CONSTRUCTOR_NAME, Object.class, CONSTRUCTOR_NAME),
                expectedInheritancePattern(DependencySettingsForbiddenByMayOnlyBeAccessed.class, Object.class),
                expectedAccessViolationPattern(DependencySettingsForbiddenByMayOnlyBeAccessed.class, CONSTRUCTOR_NAME, Object.class, CONSTRUCTOR_NAME)
        );
    }

    private Set<String> dependencySettingsViolationsOutsideOfLayers() {
        return ImmutableSet.of(
                expectedFieldTypePattern(
                        DependencySettingsOutsideOfLayersAccessingLayers.class, "origin", DependencySettingsOriginClass.class),
                expectedFieldTypePattern(
                        DependencySettingsOriginClass.class, "beingAccessedByLayers", DependencySettingsOutsideOfLayersBeingAccessedByLayers.class)
        );
    }

    private Set<String> dependencySettingsViolationsInLayers() {
        return ImmutableSet.of(
                expectedFieldTypePattern(
                        DependencySettingsOriginClass.class, "forbiddenByMayOnlyAccess", DependencySettingsForbiddenByMayOnlyAccess.class),
                expectedFieldTypePattern(
                        DependencySettingsForbiddenByMayOnlyAccess.class, "forbiddenByMayOnlyBeAccessed", DependencySettingsForbiddenByMayOnlyBeAccessed.class),
                expectedFieldTypePattern(
                        DependencySettingsForbiddenByMayOnlyAccess.class, "origin", DependencySettingsOriginClass.class));
    }

    static String[] absolute(String... pkgSuffix) {
        return Arrays.stream(pkgSuffix)
                .map(s -> OnionArchitectureTest.class.getPackage().getName() + ".testclasses." + s)
                .map(absolute -> absolute.replaceAll("\\.\\.\\.+", ".."))
                .toArray(String[]::new);
    }

    private String singleLine(EvaluationResult result) {
        return Joiner.on(NEW_LINE_REPLACE).join(result.getFailureReport().getDetails()).replace("\n", NEW_LINE_REPLACE);
    }

    static String expectedAccessViolationPattern(Class<?> from, String fromMethod, Class<?> to, String toMethod) {
        return String.format(".*%s.%s().*%s.%s().*", quote(from.getName()), fromMethod, quote(to.getName()), toMethod);
    }

    static String expectedFieldTypePattern(Class<?> owner, String fieldName, Class<?> fieldType) {
        return String.format("Field .*%s\\.%s.* has type .*<%s>.*", owner.getSimpleName(), fieldName, fieldType.getName());
    }

    @SuppressWarnings("SameParameterValue")
    private static String expectedInheritancePattern(Class<?> child, Class<?> parent) {
        return String.format("Class .*%s.* extends class .*.%s.*", child.getSimpleName(), parent.getSimpleName());
    }

    static String expectedEmptyLayerPattern(String layerName) {
        return String.format("Layer '%s' is empty", layerName);
    }

    static void assertPatternMatches(List<String> input, Set<String> expectedRegexes) {
        Set<String> toMatch = new HashSet<>(expectedRegexes);
        for (String line : input) {
            if (!matchIteratorAndRemove(toMatch, line)) {
                fail("Line '" + line + "' didn't match any pattern in " + expectedRegexes);
            }
        }
        assertThat(toMatch).as("Unmatched Patterns").isEmpty();
    }

    static boolean matchIteratorAndRemove(Set<String> toMatch, String line) {
        for (Iterator<String> toMatchIterator = toMatch.iterator(); toMatchIterator.hasNext(); ) {
            if (line.matches(toMatchIterator.next())) {
                toMatchIterator.remove();
                return true;
            }
        }
        return false;
    }

    private static class RuleWithIgnore {
        private final ArchRule rule;
        private final String description;

        private RuleWithIgnore(ArchRule rule, String description) {
            this.rule = rule;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
