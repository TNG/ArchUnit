package com.tngtech.archunit.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitTestEngine.SharedCache;
import com.tngtech.archunit.junit.testexamples.FullAnalyzeClassesSpec;
import com.tngtech.archunit.junit.testexamples.SimpleRuleField;
import com.tngtech.archunit.junit.testexamples.SimpleRuleLibrary;
import com.tngtech.archunit.junit.testexamples.SimpleRuleMethod;
import com.tngtech.archunit.junit.testexamples.SimpleRules;
import com.tngtech.archunit.junit.testexamples.TestClassWithTags;
import com.tngtech.archunit.junit.testexamples.TestMethodWithTags;
import com.tngtech.archunit.junit.testexamples.UnwantedClass;
import com.tngtech.archunit.junit.testexamples.wrong.WrongRuleMethodNotStatic;
import com.tngtech.archunit.junit.testexamples.wrong.WrongRuleMethodWrongParameters;
import com.tngtech.archunit.junit.testutil.MockitoExtension;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.junit.EngineExecutionTestListener.onlyElement;
import static com.tngtech.archunit.junit.testexamples.SimpleRuleField.SIMPLE_RULE_FIELD_NAME;
import static com.tngtech.archunit.junit.testexamples.SimpleRuleMethod.SIMPLE_RULE_METHOD_NAME;
import static com.tngtech.archunit.junit.testexamples.TestMethodWithTags.METHOD_WITH_TAG_NAME;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
@ExtendWith(MockitoExtension.class)
class ArchUnitTestEngineTest {
    @Mock
    private ClassCache classCache;
    @Mock
    private SharedCache sharedCache;
    @Captor
    private ArgumentCaptor<ClassAnalysisRequest> classAnalysisRequestCaptor;

    @InjectMocks
    private ArchUnitTestEngine testEngine;

    @BeforeEach
    void setUp() {
        when(sharedCache.get()).thenReturn(classCache);
    }

    @Nested
    class Discovers {
        @Test
        void a_root_that_is_a_test_container() {
            TestDescriptor descriptor = testEngine.discover(new EngineDiscoveryTestRequest(), uniqueTestId());

            assertThat(descriptor.getType().isContainer()).as("Root descriptor is container").isTrue();
        }

        @Test
        void a_root_with_the_correct_unique_id() {
            UniqueId uniqueId = uniqueTestId();

            TestDescriptor descriptor = testEngine.discover(new EngineDiscoveryTestRequest(), uniqueId);

            assertThat(descriptor.getUniqueId()).isEqualTo(uniqueId);
        }

        @Test
        void a_single_test_class() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor child = getOnlyElement(descriptor.getChildren());
            assertThat(child).isInstanceOf(ArchUnitTestDescriptor.class);
            assertThat(child.getUniqueId()).isEqualTo(uniqueId.append("class", SimpleRuleField.class.getName()));
            assertThat(child.getDisplayName()).isEqualTo(SimpleRuleField.class.getSimpleName());
            assertThat(child.getType()).isEqualTo(CONTAINER);
            assertThat(child.getParent().get()).isEqualTo(descriptor);
        }

        @Test
        void source_of_a_single_test_class() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueTestId());

            TestDescriptor child = getOnlyElement(descriptor.getChildren());

            assertClassSource(child, SimpleRuleField.class);
        }

        @Test
        void multiple_test_classes() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClass(SimpleRuleField.class)
                    .withClass(SimpleRuleMethod.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueTestId());

            Set<String> displayNames = descriptor.getChildren().stream().map(TestDescriptor::getDisplayName).collect(toSet());
            assertThat(displayNames).containsOnly(SimpleRuleField.class.getSimpleName(), SimpleRuleMethod.class.getSimpleName());
        }

        @Test
        void a_simple_rule_field() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor ruleDescriptor = getOnlyTest(descriptor);

            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(simpleRuleFieldTestId(uniqueId));
            FieldSource testSource = ((FieldSource) ruleDescriptor.getSource().get());
            assertThat(testSource.getClassName()).isEqualTo(SimpleRuleField.class.getName());
            assertThat(testSource.getFieldName()).isEqualTo(SIMPLE_RULE_FIELD_NAME);
        }

        @Test
        void a_simple_rule_method() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleMethod.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor ruleDescriptor = getOnlyTest(descriptor);

            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(simpleRuleMethodTestId(uniqueId));
            MethodSource testSource = (MethodSource) ruleDescriptor.getSource().get();
            assertThat(testSource.getClassName()).isEqualTo(SimpleRuleMethod.class.getName());
            assertThat(testSource.getMethodName()).isEqualTo(SIMPLE_RULE_METHOD_NAME);
            assertThat(testSource.getMethodParameterTypes()).isEqualTo(JavaClasses.class.getName());
        }

        @Test
        void a_simple_hierarchy__descriptor_types() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            Stream<TestDescriptor> archRulesDescriptors = getArchRulesDescriptorsOfOnlyChild(descriptor);
            boolean allAreContainer = archRulesDescriptors.allMatch(d -> d.getType().equals(CONTAINER));
            assertThat(allAreContainer).as("all rules descriptor have type " + CONTAINER).isTrue();
        }

        @Test
        void a_simple_hierarchy__uniqueIds() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            Stream<TestDescriptor> archRulesDescriptors = getArchRulesDescriptorsOfOnlyChild(descriptor);

            Set<UniqueId> expectedIds = getExpectedIdsForSimpleRuleLibrary(uniqueId);
            Set<UniqueId> actualIds = archRulesDescriptors.flatMap(d -> d.getChildren().stream())
                    .map(TestDescriptor::getUniqueId).collect(toSet());
            assertThat(actualIds).isEqualTo(expectedIds);
        }

        @Test
        void a_simple_hierarchy__class_source() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            assertClassSource(getOnlyElement(descriptor.getChildren()), SimpleRuleLibrary.class);

            List<TestDescriptor> archRulesDescriptors = getArchRulesDescriptorsOfOnlyChild(descriptor).collect(toList());

            TestDescriptor testDescriptor = findRulesDescriptor(archRulesDescriptors, SimpleRules.class);
            assertClassSource(testDescriptor, SimpleRules.class);
            testDescriptor.getChildren().forEach(d ->
                    assertThat(d.getSource().isPresent()).as("source is present").isTrue());

            testDescriptor = findRulesDescriptor(archRulesDescriptors, SimpleRuleField.class);
            assertClassSource(testDescriptor, SimpleRuleField.class);
            testDescriptor.getChildren().forEach(d ->
                    assertThat(d.getSource().isPresent()).as("source is present").isTrue());
        }

        @Test
        void tags_of_test_classes() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(TestClassWithTags.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor testClass = getOnlyElement(descriptor.getChildren());
            assertThat(testClass.getTags()).containsOnly(TestTag.create("tag-one"), TestTag.create("tag-two"));

            Set<? extends TestDescriptor> concreteRules = getAllLeafs(testClass);
            assertThat(concreteRules).as("concrete rules").hasSize(3);
            concreteRules.forEach(concreteRule ->
                    assertThat(concreteRule.getTags()).containsOnly(TestTag.create("tag-one"), TestTag.create("tag-two"))
            );
        }

        @Test
        void tags_of_rule_methods() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(TestMethodWithTags.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor testMethod = getArchRulesDescriptorsOfOnlyChild(descriptor)
                    .filter(d -> d.getUniqueId().toString().contains(METHOD_WITH_TAG_NAME))
                    .collect(onlyElement());

            assertThat(testMethod.getTags()).containsOnly(TestTag.create("method-tag-one"), TestTag.create("method-tag-two"));
        }

        private TestDescriptor getOnlyTest(TestDescriptor descriptor) {
            TestDescriptor testClass = getOnlyElement(descriptor.getChildren());
            TestDescriptor ruleDescriptor = getOnlyElement(testClass.getChildren());
            assertThat(ruleDescriptor.getType()).isEqualTo(TEST);
            return ruleDescriptor;
        }

        private Stream<TestDescriptor> getArchRulesDescriptorsOfOnlyChild(TestDescriptor descriptor) {
            TestDescriptor testClass = getOnlyElement(descriptor.getChildren());
            Set<? extends TestDescriptor> archRulesDescriptors = testClass.getChildren();
            return archRulesDescriptors.stream().map(identity());
        }

        private void assertClassSource(TestDescriptor child, Class<?> aClass) {
            ClassSource classSource = (ClassSource) child.getSource().get();
            assertThat(classSource.getClassName()).isEqualTo(aClass.getName());
            assertThat(classSource.getJavaClass()).isEqualTo(aClass);
            assertThat(classSource.getPosition().isPresent()).as("position is present").isFalse();
        }

        private TestDescriptor findRulesDescriptor(Collection<TestDescriptor> archRulesDescriptors, Class<?> clazz) {
            return archRulesDescriptors.stream().filter(d -> d.getUniqueId().toString().contains(clazz.getSimpleName())).findFirst().get();
        }

        private Set<? extends TestDescriptor> getAllLeafs(TestDescriptor descriptor) {
            Set<TestDescriptor> result = new HashSet<>();
            descriptor.accept(possibleLeaf -> {
                if (possibleLeaf.getChildren().isEmpty()) {
                    result.add(possibleLeaf);
                }
            });
            return result;
        }
    }

    @Nested
    class Executes {
        @Test
        void a_simple_rule_field_without_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleField.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleField.class);

            testListener.verifySuccessful(simpleRuleFieldTestId(uniqueId));
        }

        @Test
        void a_simple_rule_field_with_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleField.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleField.class);

            testListener.verifyViolation(simpleRuleFieldTestId(uniqueId), UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
        }

        @Test
        void a_simple_rule_method_without_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleMethod.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleMethod.class);

            testListener.verifySuccessful(simpleRuleMethodTestId(uniqueId));
        }

        @Test
        void a_simple_rule_method_with_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleMethod.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleMethod.class);

            testListener.verifyViolation(simpleRuleMethodTestId(uniqueId), UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
        }

        @Test
        void rule_library_without_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleLibrary.class);

            getExpectedIdsForSimpleRuleLibrary(uniqueId).forEach(testListener::verifySuccessful);
        }

        @Test
        void rule_library_with_violation() {
            UniqueId uniqueId = uniqueTestId();
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(uniqueId, SimpleRuleLibrary.class);

            getExpectedIdsForSimpleRuleLibrary(uniqueId).forEach(testId ->
                    testListener.verifyViolation(testId, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName()));
        }

        @Test
        void passes_AnalyzeClasses_to_cache() {
            execute(uniqueTestId(), FullAnalyzeClassesSpec.class);

            verify(classCache).getClassesToAnalyzeFor(eq(FullAnalyzeClassesSpec.class), classAnalysisRequestCaptor.capture());
            ClassAnalysisRequest request = classAnalysisRequestCaptor.getValue();
            AnalyzeClasses expected = FullAnalyzeClassesSpec.class.getAnnotation(AnalyzeClasses.class);
            assertThat(request.getPackages()).isEqualTo(expected.packages());
            assertThat(request.getPackageRoots()).isEqualTo(expected.packagesOf());
            assertThat(request.getLocationProviders()).isEqualTo(expected.locations());
            assertThat(request.getImportOptions()).isEqualTo(expected.importOptions());
        }

        @Test
        void cache_is_cleared_afterwards() {
            execute(uniqueTestId(), SimpleRuleLibrary.class);

            verify(classCache, times(1)).clear(SimpleRuleLibrary.class);
            verify(classCache, atLeastOnce()).getClassesToAnalyzeFor(any(Class.class), any(ClassAnalysisRequest.class));
            verifyNoMoreInteractions(classCache);
        }

        @Test
        void rule_method_that_is_not_static_is_rejected() {
            UniqueId uniqueId = uniqueTestId();

            assertThatThrownBy(() -> execute(uniqueId, WrongRuleMethodNotStatic.class))
                    .isInstanceOf(ArchUnitTestInitializationException.class)
                    .hasMessageContaining(ArchTest.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodNotStatic.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodNotStatic.NOT_STATIC_METHOD_NAME)
                    .hasMessageContaining("must be static");
        }

        @Test
        void rule_method_with_wrong_parameters_is_rejected() {
            UniqueId uniqueId = uniqueTestId();

            assertThatThrownBy(() -> execute(uniqueId, WrongRuleMethodWrongParameters.class))
                    .isInstanceOf(ArchUnitTestInitializationException.class)
                    .hasMessageContaining(ArchTest.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodWrongParameters.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodWrongParameters.WRONG_PARAMETERS_METHOD_NAME)
                    .hasMessageContaining("must have exactly one parameter of type " + JavaClasses.class.getName());
        }

        private void simulateCachedClassesForTest(Class<?> testClass, Class<?> classToReturn) {
            when(classCache.getClassesToAnalyzeFor(eq(testClass), classAnalysisRequestOf(testClass)))
                    .thenReturn(importClasses(classToReturn));
        }

        private ClassAnalysisRequest classAnalysisRequestOf(Class<?> testClass) {
            return argThat(r -> Arrays.equals(r.getPackages(), testClass.getAnnotation(AnalyzeClasses.class).packages()));
        }

        private EngineExecutionTestListener execute(UniqueId uniqueId, Class<?> testClass) {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(testClass);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            EngineExecutionTestListener listener = new EngineExecutionTestListener();
            testEngine.execute(new ExecutionRequest(descriptor, listener, discoveryRequest.getConfigurationParameters()));
            return listener;
        }
    }

    private UniqueId uniqueTestId() {
        return UniqueId.root("test", "id");
    }

    private UniqueId simpleRuleFieldTestId(UniqueId uniqueId) {
        return uniqueId
                .append("class", SimpleRuleField.class.getName())
                .append("field", SIMPLE_RULE_FIELD_NAME)
                .append("rule", SimpleRuleField.simple_rule.getDescription());
    }

    private UniqueId simpleRuleMethodTestId(UniqueId uniqueId) {
        return uniqueId
                .append("class", SimpleRuleMethod.class.getName())
                .append("method", SIMPLE_RULE_METHOD_NAME);
    }

    private Set<UniqueId> getExpectedIdsForSimpleRuleLibrary(UniqueId uniqueId) {
        UniqueId simpleRuleLibrary = uniqueId.append("class", SimpleRuleLibrary.class.getName());
        UniqueId simpleRules = simpleRuleLibrary
                .append("field", SimpleRuleLibrary.RULES_ONE_FIELD)
                .append("class", SimpleRules.class.getName());
        Set<UniqueId> simpleRulesFields = SimpleRules.RULE_FIELD_NAMES.stream().map(fieldName -> simpleRules
                .append("field", fieldName)
                .append("rule", getDescription(SimpleRules.class, fieldName))).collect(toSet());
        Set<UniqueId> simpleRulesMethods = SimpleRules.RULE_METHOD_NAMES.stream().map(methodName -> simpleRules
                .append("method", methodName)).collect(toSet());

        Set<UniqueId> simpleRuleField = singleton(simpleRuleFieldTestId(
                simpleRuleLibrary.append("field", SimpleRuleLibrary.RULES_TWO_FIELD)));

        return Stream.of(simpleRulesFields, simpleRulesMethods, simpleRuleField)
                .flatMap(Set::stream).collect(toSet());
    }

    private String getDescription(Class<?> clazz, String fieldName) {
        try {
            return ((ArchRule) clazz.getField(fieldName).get(null)).getDescription();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}