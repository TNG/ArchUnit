package com.tngtech.archunit.junit.internal.filtering;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.junit.engine_api.FieldSelector;
import com.tngtech.archunit.junit.engine_api.FieldSource;
import com.tngtech.archunit.junit.internal.ArchUnitEngineDescriptor;
import com.tngtech.archunit.junit.internal.testutil.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchUnitPropsTestSourceFilterTest {

    @Mock
    public LauncherDiscoveryRequest discoveryRequest;

    @Mock
    public ConfigurationParameters configurationParameters;

    @BeforeEach
    public void setup() {
        when(discoveryRequest.getConfigurationParameters()).thenReturn(configurationParameters);
    }

    @Nested
    class Reads {

        @Test
        void filters_from_archunit_config() {
            testFiltering(() -> mockIncludeFilterInArchConfig("com.example.SomeClass.*"),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#someField")),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.AnotherClass#someField")));
        }

        @Test
        void filters_from_discovery_request() {
            testFiltering(() -> mockIncludeFilterInConfigParams("com.example.SomeClass.*"),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#someField")),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.AnotherClass#someField")));
        }
    }

    @Nested
    class Filters {

        @ParameterizedTest(name = "by {0} filter")
        @CsvSource(value = {
        //"test scenario                 include filter                       accepted selector                       rejected selector",
        "simple class name,              SomeClass,                           com.example.SomeClass#someField,        com.example.AnotherClass#someField",
        "FQ class name,                  com.example.SomeClass,               com.example.SomeClass#someField,        com.example.AnotherClass#someField",
        "FQ class and field name,        com.example.SomeClass.someField,     com.example.SomeClass#someField,        com.example.AnotherClass#someField",
        "simple class and field name,    SomeClass.someField,                 com.example.SomeClass#someField,        com.example.AnotherClass#someField",
        "package name,                   com.example.*,                       com.example.SomeClass#someField,        com.another.AnotherClass#someField",
        "partial wildcard class name,    com.example.An*Class,                com.example.AnotherClass#someField,     com.example.SomeClass#someField",
        "wildcard field name,            com.example.SomeClass.*,             com.example.SomeClass#someField,        com.example.AnotherClass#someField",
        "partial wildcard field name,    com.example.SomeClass.some*,         com.example.SomeClass#someField,        com.example.SomeClass#anotherField"
        })
        void by_filter_type(String testScenario,
                String includeFilter,
                @ConvertWith(TestSelectorConverter.class) TestSource acceptedSelector,
                @ConvertWith(TestSelectorConverter.class) TestSource rejectedSelector) {
            testFiltering(includeFilter, null, Collections.singleton(acceptedSelector), Collections.singleton(rejectedSelector));
        }

    }

    @Nested
    class Combines {
        @Test
        void multiple_includes_as_alternatives() {
            testFiltering("com.example.SomeClass.someField,com.example.SomeClass.anotherField", null,
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#someField"),
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#anotherField")),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.AnotherClass#someField")
                    ));
        }

        @Test
        void multiple_excludes_as_conjunction() {
            testFiltering(null, "com.example.SomeClass.someField,com.example.SomeClass.anotherField",
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.AnotherClass#someField")),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#someField"),
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#anotherField")));
        }

        @Test
        void includes_and_excludes_by_resolving_against_both() {
            testFiltering("com.example.SomeClass.*", "com.example.SomeClass.anotherField",
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#someField")),
                    List.of(
                            TestSelectorConverter.toTestSelector("com.example.SomeClass#anotherField"),
                            TestSelectorConverter.toTestSelector("com.example.AnotherClass#someField")));
        }
    }

    private void testFiltering(
            String includeFilter,
            String excludeFilter,
            Collection<TestSource> acceptedSelectors,
            Collection<TestSource> rejectedSelectors) {
        testFiltering(() -> {
            mockIncludeFilterInConfigParams(includeFilter);
            mockExcludeFilterInConfigParams(excludeFilter);
        }, acceptedSelectors, rejectedSelectors);
    }

    private void testFiltering(Runnable setup, Collection<TestSource> acceptedSelectors, Collection<TestSource> rejectedSelectors) {
        // given
        setup.run();
        ArchUnitPropsTestSourceFilter filter = new ArchUnitPropsTestSourceFilter(discoveryRequest,
                new ArchUnitEngineDescriptor(UniqueId.forEngine("archunit")));

        // when
        Collection<TestSource> acceptedResults = acceptedSelectors.stream().filter(source -> !filter.shouldRun(source)).collect(Collectors.toList());
        Collection<TestSource> rejectedResults = rejectedSelectors.stream().filter(filter::shouldRun).collect(Collectors.toList());

        // then
        assertThat(acceptedResults).as("Wrongly rejected results").isEmpty();
        assertThat(rejectedResults).as("Wrongly accepted results").isEmpty();
    }

    private void mockIncludeFilterInConfigParams(String filter) {
        when(discoveryRequest.getConfigurationParameters().get("archunit.junit.includeTestsMatching")).thenReturn(Optional.ofNullable(filter));
    }

    private void mockExcludeFilterInConfigParams(String filter) {
        when(discoveryRequest.getConfigurationParameters().get("archunit.junit.excludeTestsMatching")).thenReturn(Optional.ofNullable(filter));
    }

    private void mockIncludeFilterInArchConfig(String filter) {
        ArchConfiguration.get().setProperty("junit.includeTestsMatching", filter);
    }

    private static class TestSelectorConverter implements ArgumentConverter {

        @Override
        public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
            return toTestSelector((String) source);
        }

        static FieldSource toTestSelector(String source) {
            String[] segments = source.split("#");
            return FieldSource.from(FieldSelector.selectField(segments[0], segments[1]).getJavaField());
        }
    }
}
