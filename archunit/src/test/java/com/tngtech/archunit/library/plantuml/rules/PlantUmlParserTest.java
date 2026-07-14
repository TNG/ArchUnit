package com.tngtech.archunit.library.plantuml.rules;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlantUmlParserTest {
    private static final PlantUmlParser parser = new PlantUmlParser();

    @TempDir
    public File temporaryFolder;

    @Test
    public void parses_correct_number_of_components() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin..")
                .component("SomeTarget").withStereoTypes("..target..")
                .write());

        assertThat(diagram.getAllComponents()).hasSize(2);
    }

    @Test
    public void parses_a_simple_component() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin..")
                .write());

        PlantUmlComponent origin = getComponentWithName("SomeOrigin", diagram);
        assertThat(getOnlyElement(origin.getStereotypes())).as("Stereotype")
                .isEqualTo(new Stereotype("..origin.."));
        assertThat(origin.getAlias().isPresent()).as("alias is present").isFalse();
    }

    @Test
    public void parses_a_complex_component() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withAlias("origin").withColor("Blue").withStereoTypes("..origin..")
                .write());

        PlantUmlComponent origin = getComponentWithName("SomeOrigin", diagram);
        assertThat(origin.getAlias()).as("Alias").contains(new Alias("origin"));
        assertThat(getOnlyElement(origin.getStereotypes())).as("Stereotype")
                .isEqualTo(new Stereotype("..origin.."));
    }

    static Stream<Function<TestDiagram, TestDiagram>> simple_diagrams() {
        return Stream.of(
                new Function<TestDiagram, TestDiagram>() {
                    @Override
                    public TestDiagram apply(TestDiagram diagram) {
                        return diagram.dependencyFrom("[SomeOrigin]").to("[SomeTarget]");
                    }

                    @Override
                    public String toString() {
                        return "[SomeOrigin] --> [SomeTarget]";
                    }
                },
                new Function<TestDiagram, TestDiagram>() {
                    @Override
                    public TestDiagram apply(TestDiagram diagram) {
                        return diagram.dependencyTo("[SomeTarget]").from("[SomeOrigin]");
                    }

                    @Override
                    public String toString() {
                        return "[SomeTarget] <-- [SomeOrigin]";
                    }
                }
        );
    }

    @ParameterizedTest
    @MethodSource("simple_diagrams")
    void parses_dependency_of_simple_component_diagram(Function<TestDiagram, TestDiagram> testCase) {
        TestDiagram initialDiagram = TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin..")
                .component("SomeTarget").withStereoTypes("..target..");
        PlantUmlDiagram diagram = createDiagram(testCase.apply(initialDiagram).write());

        PlantUmlComponent origin = getComponentWithName("SomeOrigin", diagram);
        PlantUmlComponent target = getOnlyElement(origin.getDependencies());

        assertThat(target.getComponentName()).as("dependency component name").isEqualTo(new ComponentName("SomeTarget"));
        assertThat(target.getDependencies()).as("dependency component's dependencies").isEmpty();
        assertThat(getOnlyElement(target.getStereotypes())).as("dependency component's stereotype")
                .isEqualTo(new Stereotype("..target.."));
        assertThat(target.getAlias()).as("dependency component's alias is present").isEmpty();
    }

    static List<String> dependency_arrow_testcases() {
        List<String> arrowCenters = rangeClosed(1, 10)
                .mapToObj(i -> repeat("-", i))
                .collect(toList());
        for (int i = 2; i <= 10; i++) {
            for (String infix : ImmutableList.of("left", "right", "up", "down", "[#green]")) {
                arrowCenters.add(repeat("-", i - 1) + infix + "-");
            }
        }
        List<String> testCase = new ArrayList<>();
        for (String arrowCenter : arrowCenters) {
            testCase.add("[SomeOrigin] " + arrowCenter + "> [SomeTarget]");
            testCase.add("[SomeTarget] <" + arrowCenter + " [SomeOrigin]");
        }
        return testCase;
    }

    @ParameterizedTest
    @MethodSource("dependency_arrow_testcases")
    void parses_various_types_of_dependency_arrows(String dependency) {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("SomeOrigin").withStereoTypes("..origin..")
                .component("SomeTarget").withStereoTypes("..target..")
                .rawLine(dependency)
                .write());

        PlantUmlComponent target = getOnlyElement(getComponentWithName("SomeOrigin", diagram).getDependencies());

        assertThat(target.getComponentName())
                .as("dependency component name")
                .isEqualTo(new ComponentName("SomeTarget"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Chartreuse",
            "dodgerblue",
            "483D8b",
            "F0808080",
            "123",
            "transparent",
            "red|green",
            "red/green",
            "red\\green",
            "red-green"
    })
    void parses_various_colored_components(String color) {
        File diagramFile = TestDiagram.in(temporaryFolder)
                .component("SomeComponent").withColor(color).withStereoTypes("..stereotype..")
                .write();

        PlantUmlComponent component = getComponentWithName("SomeComponent", createDiagram(diagramFile));

        assertThat(component.getStereotypes()).containsOnly(new Stereotype("..stereotype.."));
    }

    @Test
    public void does_not_include_commented_out_lines() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("uncommentedComponent").withAlias("uncommentedAlias").withStereoTypes("..uncommentedPackage..")
                .rawLine("  '  [commentedComponent] <<..commentedPackage..>> as commentedAlias")
                .rawLine("")
                .rawLine(" ' [uncommentedComponent] --> [commentedComponent]")
                .write());

        PlantUmlComponent uncommentedComponent = getComponentWithName("uncommentedComponent", diagram);

        assertThat(getOnlyElement(diagram.getAllComponents())).isEqualTo(uncommentedComponent);
        assertThat(uncommentedComponent.getDependencies().isEmpty()).isTrue();
    }

    @Test
    public void does_not_include_dependency_descriptions() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("component").withStereoTypes("..somePackage..")
                .component("otherComponent").withStereoTypes("..somePackage2..")
                .rawLine("[component] --> [otherComponent] : this part should be ignored, no matter the comment tick ' ")
                .write());

        PlantUmlComponent component = getComponentWithName("component", diagram);
        PlantUmlComponent targetOfDescribedDependency = getOnlyElement(component.getDependencies());
        assertThat(targetOfDescribedDependency.getComponentName())
                .as("target of dependency with description")
                .isEqualTo(new ComponentName("otherComponent"));
    }

    @Test
    public void throws_exception_with_components_without_stereotypes() {
        File file = TestDiagram.in(temporaryFolder)
                .rawLine("[componentWithoutStereotype]")
                .write();

        assertThatThrownBy(() -> createDiagram(file))
                .isInstanceOf(IllegalDiagramException.class)
                .hasMessageContaining("componentWithoutStereotype")
                .hasMessageContaining("at least one stereotype specifying the package identifier(<<..>>)");
    }

    @Test
    public void parses_two_identical_components_no_dependency() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("someName").withAlias("someAlias").withStereoTypes("someStereotype")
                .component("someName").withAlias("someAlias").withStereoTypes("someStereotype")
                .write());

        assertThat(diagram.getAllComponents()).containsOnly(getComponentWithName("someName", diagram));
    }

    @Test
    public void rejects_a_component_with_an_illegal_alias() {
        File file = TestDiagram.in(temporaryFolder)
                .component("irrelevant").withAlias("ill[]egal").withStereoTypes("..irrelevant..")
                .write();

        assertThatThrownBy(() -> createDiagram(file))
                .isInstanceOf(IllegalDiagramException.class)
                .hasMessageContaining("Alias 'ill[]egal' should not contain character(s): '[' or ']' or '\"'");
    }

    @Test
    public void parses_component_name_that_clashes_with_alias_definition() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("tricky as hell cause of as keyword").withAlias("alias").withStereoTypes("..any..")
                .write());

        PlantUmlComponent trickyAsHell = getComponentWithName("tricky as hell cause of as keyword", diagram);

        assertThat(trickyAsHell.getComponentName()).isEqualTo(new ComponentName("tricky as hell cause of as keyword"));
        assertThat(trickyAsHell.getAlias().get()).isEqualTo(new Alias("alias"));
    }

    @Test
    public void parses_component_diagram_with_multiple_stereotypes() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("someComponent")
                .withStereoTypes("..firstPackage..", "..secondPackage..", "..thirdPackage..")
                .write());

        PlantUmlComponent component = getOnlyElement(diagram.getAllComponents());
        assertThat(component.getStereotypes()).containsOnly(
                new Stereotype("..firstPackage.."),
                new Stereotype("..secondPackage.."),
                new Stereotype("..thirdPackage.."));
    }

    @Test
    public void parses_component_diagram_with_multiple_stereotypes_and_alias() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("someComponent").withAlias("someAlias")
                .withStereoTypes("..firstPackage..", "..secondPackage..", "..thirdPackage..")
                .write());

        PlantUmlComponent component = getOnlyElement(diagram.getAllComponents());

        assertThat(component.getAlias()).contains(new Alias("someAlias"));
    }

    @Test
    public void parses_diagram_with_dependencies_that_use_alias() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("A").withAlias("aliasForA").withStereoTypes("..controller..")
                .component("B").withAlias("aliasForB").withStereoTypes("..service..")
                .dependencyFrom("aliasForA").to("aliasForB")
                .write());

        PlantUmlComponent aliasForA = getComponentWithAlias(new Alias("aliasForA"), diagram);
        PlantUmlComponent aliasForB = getComponentWithAlias(new Alias("aliasForB"), diagram);

        assertThat(getComponentWithName("A", diagram)).isEqualTo(aliasForA);
        assertThat(getComponentWithName("B", diagram)).isEqualTo(aliasForB);

        assertThat(aliasForA.getDependencies()).containsOnly(aliasForB);
    }

    @Test
    public void parses_dependencies_between_components_without_brackets() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes("..origin..")
                .component("B").withStereoTypes("..target..")
                .dependencyFrom("A").to("B")
                .write());

        PlantUmlComponent a = getComponentWithName("A", diagram);
        PlantUmlComponent b = getComponentWithName("B", diagram);

        assertThat(a.getDependencies()).containsOnly(b);
    }

    @Test
    public void parses_multiple_components_and_dependencies() {
        File file = TestDiagram.in(temporaryFolder)
                .component("Component1").withStereoTypes("..origin1..")
                .component("Component2").withStereoTypes("..target1..")
                .component("Component3").withStereoTypes("..origin2..")
                .component("Component4").withStereoTypes("..target2..")
                .dependencyFrom("Component1").to("Component2")
                .dependencyFrom("Component3").to("Component4")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent component1 = getComponentWithName("Component1", diagram);
        PlantUmlComponent component2 = getComponentWithName("Component2", diagram);
        PlantUmlComponent component3 = getComponentWithName("Component3", diagram);
        PlantUmlComponent component4 = getComponentWithName("Component4", diagram);

        assertThat(diagram.getAllComponents()).containsOnly(component1, component2, component3, component4);
        assertThat(component1.getDependencies()).containsOnly(component2);
        assertThat(component2.getDependencies().isEmpty()).isTrue();
        assertThat(component3.getDependencies()).containsOnly(component4);
        assertThat(component4.getDependencies().isEmpty()).isTrue();
    }

    @Test
    public void parses_a_diagram_with_non_unique_origins() {
        File file = TestDiagram.in(temporaryFolder)
                .component("Component1").withStereoTypes("..origin..")
                .component("Component2").withStereoTypes("..target1..")
                .component("Component3").withStereoTypes("..target2..")
                .dependencyFrom("[Component1]").to("[Component2]")
                .dependencyFrom("[Component1]").to("[Component3]")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent component1 = getComponentWithName("Component1", diagram);
        PlantUmlComponent component2 = getComponentWithName("Component2", diagram);
        PlantUmlComponent component3 = getComponentWithName("Component3", diagram);

        assertThat(component1.getDependencies()).containsOnly(component2, component3);
    }

    @Test
    public void parse_a_diagram_with_non_unique_targets() {
        File file = TestDiagram.in(temporaryFolder)
                .component("Component1").withStereoTypes("..origin1..")
                .component("Component2").withStereoTypes("..origin2..")
                .component("Component3").withStereoTypes("..target..")
                .dependencyFrom("[Component1]").to("[Component3]")
                .dependencyFrom("[Component2]").to("[Component3]")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent component1 = getComponentWithName("Component1", diagram);
        PlantUmlComponent component2 = getComponentWithName("Component2", diagram);
        PlantUmlComponent component3 = getComponentWithName("Component3", diagram);

        assertThat(component1.getDependencies()).containsOnly(component3);
        assertThat(component2.getDependencies()).containsOnly(component3);
    }

    @Test
    public void parse_a_component_diagram_with_both_alias_and_names_used() {
        PlantUmlDiagram diagram = createDiagram(TestDiagram.in(temporaryFolder)
                .component("A").withAlias("foo").withStereoTypes("..service..")
                .component("B").withStereoTypes("..controller..")
                .dependencyFrom("[B]").to("foo")
                .dependencyFrom("foo").to("[B]")
                .write());

        PlantUmlComponent componentB = getComponentWithName("B", diagram);
        PlantUmlComponent componentFoo = getComponentWithAlias(new Alias("foo"), diagram);

        assertThat(componentB.getDependencies()).containsOnly(componentFoo);
        assertThat(componentFoo.getDependencies()).containsOnly(componentB);
    }

    @Test
    public void parses_a_component_diagram_that_uses_alias_with_and_without_brackets() {
        File file = TestDiagram.in(temporaryFolder)
                .component("A").withAlias("foo").withStereoTypes("..origin..")
                .component("B").withAlias("bar").withStereoTypes("..target..")
                .dependencyFrom("foo").to("bar")
                .dependencyFrom("[foo]").to("[bar]")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent foo = getComponentWithAlias(new Alias("foo"), diagram);
        PlantUmlComponent bar = getComponentWithAlias(new Alias("bar"), diagram);

        assertThat(foo.getDependencies()).containsOnly(bar);
        assertThat(bar.getDependencies()).isEmpty();
    }

    @Test
    public void ignores_components_that_are_not_yet_defined() {
        File file = TestDiagram.in(temporaryFolder)
                .dependencyFrom("[NotYetDefined]").to("[AlsoNotYetDefined]")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        assertThat(diagram.getComponentsWithAlias()).isEmpty();
    }

    @Test
    public void ignores_database_components() {
        File file = TestDiagram.in(temporaryFolder)
                .rawLine("database \"DB\"")
                .component("componentA").withAlias("aliasA").withStereoTypes("..packageA..")
                .component("componentB").withAlias("aliasB").withStereoTypes("..packageB..")
                .dependencyFrom("aliasA").to("aliasB")
                .dependencyFrom("aliasB").to("DB")
                .dependencyFrom("componentA").to("DB")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent a = getComponentWithAlias(new Alias("aliasA"), diagram);
        PlantUmlComponent b = getComponentWithAlias(new Alias("aliasB"), diagram);

        assertThat(a.getDependencies()).containsOnly(b);
        assertThat(b.getDependencies()).isEmpty();
    }

    @Test
    public void supports_components_declared_with_component_keyword() {
        File file = TestDiagram.in(temporaryFolder)
                .rawLine("component [CompA] <<..c1..>>")
                .rawLine("component [Comp B] <<..c2..>> as CompB")
                .dependencyFrom("CompA").to("CompB")
                .write();

        PlantUmlDiagram diagram = createDiagram(file);

        PlantUmlComponent a = getComponentWithName("CompA", diagram);
        PlantUmlComponent b = getComponentWithAlias(new Alias("CompB"), diagram);

        assertThat(a.getDependencies()).containsOnly(b);
        assertThat(b.getDependencies()).isEmpty();
    }

    private PlantUmlComponent getComponentWithName(String componentName, PlantUmlDiagram diagram) {
        PlantUmlComponent component = diagram.getAllComponents().stream()
                .filter(c -> c.getComponentName().asString().equals(componentName))
                .collect(onlyElement());
        assertThat(component).as("Component with name " + componentName).isNotNull();
        return component;
    }

    private PlantUmlComponent getComponentWithAlias(Alias alias, PlantUmlDiagram diagram) {
        PlantUmlComponent component = diagram.getComponentsWithAlias()
                .stream().filter(a -> a.getAlias().get().equals(alias))
                .collect(onlyElement());
        assertThat(component).as("Component with alias " + alias.asString()).isNotNull();
        return component;
    }

    private URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private PlantUmlDiagram createDiagram(File file) {
        return parser.parse(toUrl(file));
    }
}
