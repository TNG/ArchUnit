package com.tngtech.archunit.junit;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitTestEngine.SharedCache;
import com.tngtech.archunit.junit.testexamples.ClassWithPrivateTests;
import com.tngtech.archunit.junit.testexamples.ComplexRuleLibrary;
import com.tngtech.archunit.junit.testexamples.ComplexTags;
import com.tngtech.archunit.junit.testexamples.FullAnalyzeClassesSpec;
import com.tngtech.archunit.junit.testexamples.LibraryWithPrivateTests;
import com.tngtech.archunit.junit.testexamples.SimpleRuleLibrary;
import com.tngtech.archunit.junit.testexamples.TestClassWithTags;
import com.tngtech.archunit.junit.testexamples.TestFieldWithTags;
import com.tngtech.archunit.junit.testexamples.TestMethodWithTags;
import com.tngtech.archunit.junit.testexamples.UnwantedClass;
import com.tngtech.archunit.junit.testexamples.ignores.IgnoredClass;
import com.tngtech.archunit.junit.testexamples.ignores.IgnoredField;
import com.tngtech.archunit.junit.testexamples.ignores.IgnoredLibrary;
import com.tngtech.archunit.junit.testexamples.ignores.IgnoredMethod;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleMethod;
import com.tngtech.archunit.junit.testexamples.subtwo.SimpleRules;
import com.tngtech.archunit.junit.testexamples.wrong.WrongRuleMethodNotStatic;
import com.tngtech.archunit.junit.testexamples.wrong.WrongRuleMethodWrongParameters;
import com.tngtech.archunit.junit.testutil.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.CLASS_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.FIELD_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.ArchUnitTestDescriptor.METHOD_SEGMENT_TYPE;
import static com.tngtech.archunit.junit.EngineExecutionTestListener.onlyElement;
import static com.tngtech.archunit.junit.testexamples.TestFieldWithTags.FIELD_WITH_TAG_NAME;
import static com.tngtech.archunit.junit.testexamples.TestMethodWithTags.METHOD_WITH_TAG_NAME;
import static com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField.SIMPLE_RULE_FIELD_NAME;
import static com.tngtech.archunit.junit.testexamples.subone.SimpleRuleMethod.SIMPLE_RULE_METHOD_NAME;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.PackageNameFilter.excludePackageNames;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;
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

    private final UniqueId engineId = createEngineId();

    @BeforeEach
    void setUp() {
        when(sharedCache.get()).thenReturn(classCache);
    }

    @Nested
    class Discovers {
        @Test
        void a_root_that_is_a_test_container() {
            TestDescriptor descriptor = testEngine.discover(new EngineDiscoveryTestRequest(), createEngineId());

            assertThat(descriptor.getType().isContainer()).as("Root descriptor is container").isTrue();
        }

        @Test
        void a_root_with_the_correct_unique_id() {

            TestDescriptor descriptor = testEngine.discover(new EngineDiscoveryTestRequest(), engineId);

            assertThat(descriptor.getUniqueId()).isEqualTo(engineId);
        }

        @Test
        void a_single_test_class() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor child = getOnlyElement(descriptor.getChildren());
            assertThat(child).isInstanceOf(ArchUnitTestDescriptor.class);
            assertThat(child.getUniqueId()).isEqualTo(engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()));
            assertThat(child.getDisplayName()).isEqualTo(SimpleRuleField.class.getSimpleName());
            assertThat(child.getType()).isEqualTo(CONTAINER);
            assertThat(child.getParent().get()).isEqualTo(descriptor);
        }

        @Test
        void source_of_a_single_test_class() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, createEngineId());

            TestDescriptor child = getOnlyElement(descriptor.getChildren());

            assertClassSource(child, SimpleRuleField.class);
        }

        @Test
        void multiple_test_classes() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClass(SimpleRuleField.class)
                    .withClass(SimpleRuleMethod.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, createEngineId());

            Set<String> displayNames = descriptor.getChildren().stream().map(TestDescriptor::getDisplayName).collect(toSet());
            assertThat(displayNames).containsOnly(SimpleRuleField.class.getSimpleName(), SimpleRuleMethod.class.getSimpleName());
        }

        @Test
        void a_class_with_simple_rule_field() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleField.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor ruleDescriptor = getOnlyTest(descriptor);

            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(simpleRuleFieldTestId(engineId));
            FieldSource testSource = ((FieldSource) ruleDescriptor.getSource().get());
            assertThat(testSource.getClassName()).isEqualTo(SimpleRuleField.class.getName());
            assertThat(testSource.getFieldName()).isEqualTo(SIMPLE_RULE_FIELD_NAME);
        }

        @Test
        void a_class_with_simple_rule_method() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleMethod.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor ruleDescriptor = getOnlyTest(descriptor);

            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(simpleRuleMethodTestId(engineId));
            MethodSource testSource = (MethodSource) ruleDescriptor.getSource().get();
            assertThat(testSource.getClassName()).isEqualTo(SimpleRuleMethod.class.getName());
            assertThat(testSource.getMethodName()).isEqualTo(SIMPLE_RULE_METHOD_NAME);
            assertThat(testSource.getMethodParameterTypes()).isEqualTo(JavaClasses.class.getName());
        }

        @Test
        void a_class_with_simple_hierarchy__descriptor_types() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            Stream<TestDescriptor> archRulesDescriptors = getArchRulesDescriptorsOfOnlyChild(descriptor);
            boolean allAreContainer = archRulesDescriptors.allMatch(d -> d.getType().equals(CONTAINER));
            assertThat(allAreContainer).as("all rules descriptor have type " + CONTAINER).isTrue();
        }

        @Test
        void a_class_with_simple_hierarchy__uniqueIds() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            Stream<TestDescriptor> archRulesDescriptors = getArchRulesDescriptorsOfOnlyChild(descriptor);

            Set<UniqueId> expectedIds = getExpectedIdsForSimpleRuleLibrary(engineId);
            Set<UniqueId> actualIds = archRulesDescriptors.flatMap(d -> d.getChildren().stream())
                    .map(TestDescriptor::getUniqueId).collect(toSet());
            assertThat(actualIds).isEqualTo(expectedIds);
        }

        @Test
        void a_class_with_simple_hierarchy__class_source() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleLibrary.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

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
        void a_class_with_complex_hierarchy() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(ComplexRuleLibrary.class);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("all leaf unique ids of complex hierarchy")
                    .containsOnlyElementsOf(getExpectedIdsForComplexRuleLibrary(engineId));
        }

        @Test
        void private_instance_members() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(ClassWithPrivateTests.class);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("all leaf unique ids of private members")
                    .containsOnly(privateRuleFieldId(engineId), privateRuleMethodId(engineId));
        }

        @Test
        void private_instance_libraries() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(LibraryWithPrivateTests.class);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("all leaf unique ids of private members")
                    .containsOnlyElementsOf(privateRuleLibraryIds(engineId));
        }

        @Test
        void an_unique_id() {
            UniqueId ruleIdToDiscover = simpleRulesId(engineId)
                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withUniqueId(ruleIdToDiscover);

            TestDescriptor descriptor = getOnlyTest(testEngine.discover(discoveryRequest, engineId));

            assertThat(descriptor.getUniqueId()).isEqualTo(ruleIdToDiscover);
            assertThat(descriptor.getDisplayName()).isEqualTo(SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            assertThat(descriptor.getChildren()).isEmpty();
            assertThat(descriptor.getDescendants()).isEmpty();
            assertThat(descriptor.getType()).isEqualTo(TEST);
            assertThat(descriptor.getSource().get()).isEqualTo(FieldSource.from(field(SimpleRules.class, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME)));
            assertThat(descriptor.getParent().get().getSource().get()).isEqualTo(ClassSource.from(SimpleRules.class));
        }

        @Test
        void multiple_unique_ids() {
            UniqueId testId = simpleRulesId(engineId);
            UniqueId firstRuleIdToDiscover = testId.append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            UniqueId secondRuleIdToDiscover = testId.append(METHOD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_METHOD_ONE_NAME);
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withUniqueId(firstRuleIdToDiscover)
                    .withUniqueId(secondRuleIdToDiscover);

            TestDescriptor test = getOnlyElement(testEngine.discover(discoveryRequest, engineId).getChildren());

            Set<UniqueId> discoveredRuleIds = toUniqueIds(test);
            assertThat(discoveredRuleIds).containsOnly(firstRuleIdToDiscover, secondRuleIdToDiscover);
        }

        @Test
        void no_redundant_descriptors() {
            UniqueId redundantId = simpleRulesId(engineId)
                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClass(SimpleRules.class)
                    .withUniqueId(redundantId);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor test = getOnlyElement(rootDescriptor.getChildren());
            assertThat(test.getChildren()).as("all children of test").hasSize(4);
            List<TestDescriptor> descriptorsWithSpecifiedId = test.getChildren().stream()
                    .filter(descriptor -> descriptor.getUniqueId().equals(redundantId))
                    .collect(toList());
            assertThat(descriptorsWithSpecifiedId)
                    .as("descriptors with id " + redundantId)
                    .hasSize(1);
        }

        @Test
        void no_redundant_library_descriptors() {
            UniqueId simpleRulesId = simpleRulesInLibraryId(engineId);
            UniqueId ruleIdOne = simpleRulesId.append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            UniqueId ruleIdTwo = simpleRulesId.append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_TWO_NAME);
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withUniqueId(ruleIdOne)
                    .withUniqueId(ruleIdTwo);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor simpleRules = getArchRulesDescriptorsOfOnlyChild(rootDescriptor).collect(onlyElement());

            assertThat(toUniqueIds(simpleRules))
                    .as("ids of requested children of " + SimpleRules.class.getSimpleName())
                    .containsOnly(ruleIdOne, ruleIdTwo);
        }

        @Test
        void classpath_roots() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClasspathRoot(rootOfClass(Test.class))
                    .withClasspathRoot(rootOfClass(SimpleRules.class))
                    .withClassNameFilter(excludeClassNamePatterns(".*(W|w)rong.*"));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("children discovered by " + ClasspathRootSelector.class.getSimpleName())
                    .contains(simpleRulesInLibraryId(engineId).append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME))
                    .contains(simpleRuleFieldTestId(engineId))
                    .contains(simpleRuleMethodTestId(engineId));
        }

        @Test
        void packages() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withPackage(SimpleRuleField.class.getPackage().getName())
                    .withPackage(SimpleRules.class.getPackage().getName());

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor))
                    .as("tests discovered by " + PackageSelector.class.getSimpleName())
                    .containsOnly(
                            engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()),
                            engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleMethod.class.getName()),
                            simpleRulesId(engineId));
        }

        @Test
        void subpackages() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withPackage(SimpleRuleLibrary.class.getPackage().getName())
                    .withClassNameFilter(excludeClassNamePatterns(".*(W|w)rong.*"));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor))
                    .as("tests discovered by " + PackageSelector.class.getSimpleName())
                    .contains(
                            engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleLibrary.class.getName()),
                            simpleRulesId(engineId));

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("children discovered by " + PackageSelector.class.getSimpleName())
                    .contains(
                            simpleRulesInLibraryId(engineId).append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME),
                            simpleRuleFieldTestId(engineId),
                            simpleRuleMethodTestId(engineId));
        }

        @Test
        void methods() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withMethod(SimpleRuleMethod.class, SimpleRuleMethod.SIMPLE_RULE_METHOD_NAME)
                    .withMethod(SimpleRules.class, SimpleRules.SIMPLE_RULE_METHOD_ONE_NAME);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("children discovered by " + MethodSelector.class.getSimpleName())
                    .containsOnly(
                            simpleRuleMethodTestId(engineId),
                            simpleRulesId(engineId)
                                    .append(METHOD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_METHOD_ONE_NAME));
        }

        @Test
        void fields() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withField(SimpleRuleField.class, SimpleRuleField.SIMPLE_RULE_FIELD_NAME)
                    .withField(SimpleRules.class, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("children discovered by " + FieldSelector.class.getSimpleName())
                    .containsOnly(
                            simpleRuleFieldTestId(engineId),
                            simpleRulesId(engineId)
                                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME));
        }

        @Test
        void mixed_class_methods_and_fields() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withField(SimpleRuleField.class, SimpleRuleField.SIMPLE_RULE_FIELD_NAME)
                    .withMethod(SimpleRuleMethod.class, SimpleRuleMethod.SIMPLE_RULE_METHOD_NAME)
                    .withClass(SimpleRules.class);

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            Set<UniqueId> expectedLeafIds = new HashSet<>();
            expectedLeafIds.add(simpleRuleFieldTestId(engineId));
            expectedLeafIds.add(simpleRuleMethodTestId(engineId));
            Stream.concat(
                    SimpleRules.RULE_FIELD_NAMES.stream().map(fieldName ->
                            simpleRulesId(engineId).append(FIELD_SEGMENT_TYPE, fieldName)),
                    SimpleRules.RULE_METHOD_NAMES.stream().map(methodName ->
                            simpleRulesId(engineId).append(METHOD_SEGMENT_TYPE, methodName)))
                    .forEach(expectedLeafIds::add);

            assertThat(getAllLeafUniqueIds(rootDescriptor))
                    .as("children discovered by mixed selectors")
                    .containsOnlyElementsOf(expectedLeafIds);
        }

        @Test
        void tags_of_test_classes() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(TestClassWithTags.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            TestDescriptor testClass = getOnlyElement(descriptor.getChildren());
            assertThat(testClass.getTags()).containsOnly(TestTag.create("tag-one"), TestTag.create("tag-two"));

            Set<? extends TestDescriptor> concreteRules = getAllLeafs(testClass);
            assertThat(concreteRules).as("concrete rules").hasSize(3);
            concreteRules.forEach(concreteRule ->
                    assertThat(concreteRule.getTags()).containsOnly(TestTag.create("tag-one"), TestTag.create("tag-two"))
            );
        }

        @Test
        void tags_of_rule_fields() {
            TestDescriptor testField = getOnlyChildWithDescriptorContaining(FIELD_WITH_TAG_NAME, TestFieldWithTags.class);

            assertThat(testField.getTags()).containsOnly(TestTag.create("field-tag-one"), TestTag.create("field-tag-two"));
        }

        @Test
        void tags_of_rule_methods() {
            TestDescriptor testMethod = getOnlyChildWithDescriptorContaining(METHOD_WITH_TAG_NAME, TestMethodWithTags.class);

            assertThat(testMethod.getTags()).containsOnly(TestTag.create("method-tag-one"), TestTag.create("method-tag-two"));
        }

        @Test
        void complex_tags() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(ComplexTags.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            Map<UniqueId, Set<TestTag>> tagsById = new HashMap<>();
            descriptor.accept(d -> tagsById.put(d.getUniqueId(), d.getTags()));

            assertThat(getTagsForIdEndingIn(ComplexTags.class.getSimpleName(), tagsById))
                    .containsOnly(TestTag.create("library-tag"));

            assertThat(getTagsForIdEndingIn(TestClassWithTags.class.getSimpleName(), tagsById))
                    .containsOnly(
                            TestTag.create("library-tag"),
                            TestTag.create("rules-tag"),
                            TestTag.create("tag-one"),
                            TestTag.create("tag-two"));

            assertThat(getTagsForIdEndingIn(TestClassWithTags.FIELD_RULE_NAME, tagsById))
                    .containsOnly(
                            TestTag.create("library-tag"),
                            TestTag.create("rules-tag"),
                            TestTag.create("tag-one"),
                            TestTag.create("tag-two"));

            assertThat(getTagsForIdEndingIn(ComplexTags.FIELD_RULE_NAME, tagsById))
                    .containsOnly(
                            TestTag.create("library-tag"),
                            TestTag.create("field-tag"));

            assertThat(getTagsForIdEndingIn(ComplexTags.METHOD_RULE_NAME, tagsById))
                    .containsOnly(
                            TestTag.create("library-tag"),
                            TestTag.create("method-tag"));
        }

        @Test
        void filtering_excluded_class_names() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClasspathRoot(uriOfClass(SimpleRuleField.class))
                    .withClasspathRoot(uriOfClass(SimpleRuleMethod.class))
                    .withClasspathRoot(uriOfClass(SimpleRules.class))
                    .withClasspathRoot(uriOfClass(SimpleRuleLibrary.class))
                    .withClassNameFilter(excludeClassNamePatterns(".*(Field|Rules).*"));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor)).containsOnly(
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleMethod.class.getName()),
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleLibrary.class.getName()));
        }

        @Test
        void filtering_included_class_names() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClasspathRoot(uriOfClass(SimpleRuleField.class))
                    .withClasspathRoot(uriOfClass(SimpleRuleMethod.class))
                    .withClasspathRoot(uriOfClass(SimpleRules.class))
                    .withClasspathRoot(uriOfClass(SimpleRuleLibrary.class))
                    .withClassNameFilter(includeClassNamePatterns(".*(Field|Rules).*"));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor)).containsOnly(
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()),
                    simpleRulesId(engineId));
        }

        @Test
        void filtering_excluded_packages() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withPackage(SimpleRuleLibrary.class.getPackage().getName())
                    .withPackageNameFilter(excludePackageNames(
                            SimpleRuleField.class.getPackage().getName(),
                            SimpleRules.class.getPackage().getName(),
                            WrongRuleMethodNotStatic.class.getPackage().getName()));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor))
                    .contains(engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleLibrary.class.getName()))
                    .doesNotContain(
                            engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()),
                            engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleMethod.class.getName()),
                            simpleRulesId(engineId));
        }

        @Test
        void filtering_included_packages() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withPackage(SimpleRuleLibrary.class.getPackage().getName())
                    .withPackageNameFilter(includePackageNames(
                            SimpleRuleField.class.getPackage().getName(),
                            SimpleRules.class.getPackage().getName()));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor)).containsOnly(
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()),
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleMethod.class.getName()),
                    simpleRulesId(engineId));
        }

        @Test
        void all_without_filters() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest()
                    .withClasspathRoot(uriOfClass(SimpleRuleField.class))
                    .withClasspathRoot(uriOfClass(SimpleRules.class));

            TestDescriptor rootDescriptor = testEngine.discover(discoveryRequest, engineId);

            assertThat(toUniqueIds(rootDescriptor)).containsOnly(
                    engineId.append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName()),
                    simpleRulesId(engineId));
        }

        private Set<TestTag> getTagsForIdEndingIn(String suffix, Map<UniqueId, Set<TestTag>> tagsById) {
            UniqueId matchingId = tagsById.keySet().stream()
                    .filter(id -> getLast(id.getSegments()).getValue().endsWith(suffix))
                    .collect(onlyElement());
            return tagsById.get(matchingId);
        }

        private TestDescriptor getOnlyChildWithDescriptorContaining(String idPart, Class<?> testClass) {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(testClass);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, engineId);

            return getArchRulesDescriptorsOfOnlyChild(descriptor)
                    .filter(d -> d.getUniqueId().toString().contains(idPart))
                    .collect(onlyElement());
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
    }

    @Nested
    class Executes {
        @Test
        void a_simple_rule_field_without_violation() {
            simulateCachedClassesForTest(SimpleRuleField.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleField.class);

            testListener.verifySuccessful(simpleRuleFieldTestId(engineId));
        }

        @Test
        void a_simple_rule_field_with_violation() {
            simulateCachedClassesForTest(SimpleRuleField.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleField.class);

            testListener.verifyViolation(simpleRuleFieldTestId(engineId), UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
        }

        @Test
        void a_simple_rule_method_without_violation() {
            simulateCachedClassesForTest(SimpleRuleMethod.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleMethod.class);

            testListener.verifySuccessful(simpleRuleMethodTestId(engineId));
        }

        @Test
        void a_simple_rule_method_with_violation() {
            simulateCachedClassesForTest(SimpleRuleMethod.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleMethod.class);

            testListener.verifyViolation(simpleRuleMethodTestId(engineId), UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
        }

        @Test
        void rule_library_without_violation() {
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleLibrary.class);

            getExpectedIdsForSimpleRuleLibrary(engineId).forEach(testListener::verifySuccessful);
        }

        @Test
        void rule_library_with_violation() {
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, SimpleRuleLibrary.class);

            getExpectedIdsForSimpleRuleLibrary(engineId).forEach(testId ->
                    testListener.verifyViolation(testId, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName()));
        }

        @Test
        void private_instance_libraries() {
            simulateCachedClassesForTest(LibraryWithPrivateTests.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, LibraryWithPrivateTests.class);

            privateRuleLibraryIds(engineId).forEach(testId ->
                    testListener.verifyViolation(testId, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName()));
        }

        @Test
        void rule_by_unique_id_without_violation() {
            UniqueId fieldRuleInLibrary = simpleRulesInLibraryId(engineId)
                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_SATISFYING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, new EngineDiscoveryTestRequest()
                    .withUniqueId(fieldRuleInLibrary));

            testListener.verifySuccessful(fieldRuleInLibrary);
            testListener.verifyNoOtherStartExceptHierarchyOf(fieldRuleInLibrary);
        }

        @Test
        void rule_by_unique_id_with_violation() {
            UniqueId fieldRuleInLibrary = simpleRulesInLibraryId(engineId)
                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, new EngineDiscoveryTestRequest()
                    .withUniqueId(fieldRuleInLibrary));

            testListener.verifyViolation(fieldRuleInLibrary, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
            testListener.verifyNoOtherStartExceptHierarchyOf(fieldRuleInLibrary);
        }

        @Test
        void mixed_rules_by_unique_id_and_class_with_violation() {
            UniqueId fieldRuleInLibrary = simpleRulesInLibraryId(engineId)
                    .append(FIELD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_FIELD_ONE_NAME);
            UniqueId methodRuleInLibrary = simpleRulesInLibraryId(engineId)
                    .append(METHOD_SEGMENT_TYPE, SimpleRules.SIMPLE_RULE_METHOD_ONE_NAME);
            simulateCachedClassesForTest(SimpleRuleLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);
            simulateCachedClassesForTest(SimpleRuleField.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, new EngineDiscoveryTestRequest()
                    .withClass(SimpleRuleField.class)
                    .withUniqueId(fieldRuleInLibrary)
                    .withUniqueId(methodRuleInLibrary));

            testListener.verifyViolation(simpleRuleFieldTestId(engineId), UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
            testListener.verifyViolation(fieldRuleInLibrary, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
            testListener.verifyViolation(methodRuleInLibrary, UnwantedClass.CLASS_VIOLATING_RULES.getSimpleName());
        }

        @Test
        void passes_AnalyzeClasses_to_cache() {
            execute(createEngineId(), FullAnalyzeClassesSpec.class);

            verify(classCache).getClassesToAnalyzeFor(eq(FullAnalyzeClassesSpec.class), classAnalysisRequestCaptor.capture());
            ClassAnalysisRequest request = classAnalysisRequestCaptor.getValue();
            AnalyzeClasses expected = FullAnalyzeClassesSpec.class.getAnnotation(AnalyzeClasses.class);
            assertThat(request.getPackageNames()).isEqualTo(expected.packages());
            assertThat(request.getPackageRoots()).isEqualTo(expected.packagesOf());
            assertThat(request.getLocationProviders()).isEqualTo(expected.locations());
            assertThat(request.getImportOptions()).isEqualTo(expected.importOptions());
        }

        @Test
        void cache_is_cleared_afterwards() {
            execute(createEngineId(), SimpleRuleLibrary.class);

            verify(classCache, times(1)).clear(SimpleRuleLibrary.class);
            verify(classCache, atLeastOnce()).getClassesToAnalyzeFor(any(Class.class), any(ClassAnalysisRequest.class));
            verifyNoMoreInteractions(classCache);
        }
    }

    @Nested
    class Rejects {
        @Test
        void rule_method_with_wrong_parameters() {
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(WrongRuleMethodWrongParameters.class);

            assertThatThrownBy(() -> testEngine.discover(discoveryRequest, engineId))
                    .isInstanceOf(ArchTestInitializationException.class)
                    .hasMessageContaining(ArchTest.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodWrongParameters.class.getSimpleName())
                    .hasMessageContaining(WrongRuleMethodWrongParameters.WRONG_PARAMETERS_METHOD_NAME)
                    .hasMessageContaining("must have exactly one parameter of type " + JavaClasses.class.getName());
        }
    }

    @Nested
    class Ignores {
        @Test
        void fields() {
            simulateCachedClassesForTest(IgnoredField.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredField.class);

            testListener.verifySkipped(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredField.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredField.IGNORED_RULE_FIELD));

            testListener.verifyViolation(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredField.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredField.UNIGNORED_RULE_FIELD));
        }

        @Test
        void methods() {
            simulateCachedClassesForTest(IgnoredMethod.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredMethod.class);

            testListener.verifySkipped(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredMethod.class.getName())
                    .append(METHOD_SEGMENT_TYPE, IgnoredMethod.IGNORED_RULE_METHOD));

            testListener.verifyViolation(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredMethod.class.getName())
                    .append(METHOD_SEGMENT_TYPE, IgnoredMethod.UNIGNORED_RULE_METHOD));
        }

        @Test
        void classes() {
            simulateCachedClassesForTest(IgnoredClass.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredClass.class);

            testListener.verifySkipped(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredClass.class.getName()));

            testListener.verifyNoOtherStartExceptHierarchyOf(engineId);
        }

        @Test
        void libraries() {
            simulateCachedClassesForTest(IgnoredLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredLibrary.class);

            testListener.verifySkipped(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredLibrary.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredLibrary.IGNORED_LIB_FIELD)
                    .append(CLASS_SEGMENT_TYPE, SimpleRules.class.getName()));
        }

        @Test
        void library_referenced_classes() {
            simulateCachedClassesForTest(IgnoredLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredLibrary.class);

            testListener.verifySkipped(engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredLibrary.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredLibrary.UNIGNORED_LIB_ONE_FIELD)
                    .append(CLASS_SEGMENT_TYPE, IgnoredClass.class.getName()));
        }

        @Test
        void library_sub_rules() {
            simulateCachedClassesForTest(IgnoredLibrary.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredLibrary.class);

            UniqueId classWithIgnoredMethod = engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredLibrary.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredLibrary.UNIGNORED_LIB_TWO_FIELD)
                    .append(CLASS_SEGMENT_TYPE, IgnoredMethod.class.getName());

            testListener.verifySkipped(classWithIgnoredMethod
                    .append(METHOD_SEGMENT_TYPE, IgnoredMethod.IGNORED_RULE_METHOD));

            testListener.verifyViolation(classWithIgnoredMethod
                    .append(METHOD_SEGMENT_TYPE, IgnoredMethod.UNIGNORED_RULE_METHOD));
        }

        @Test
        void with_reason() {
            simulateCachedClassesForTest(IgnoredField.class, UnwantedClass.CLASS_VIOLATING_RULES);

            EngineExecutionTestListener testListener = execute(engineId, IgnoredField.class);

            UniqueId ignoredId = engineId
                    .append(CLASS_SEGMENT_TYPE, IgnoredField.class.getName())
                    .append(FIELD_SEGMENT_TYPE, IgnoredField.IGNORED_RULE_FIELD);
            testListener.verifySkipped(ignoredId, "some example description");
        }
    }

    private UniqueId createEngineId() {
        return UniqueId.forEngine(ArchUnitTestEngine.UNIQUE_ID);
    }

    private UniqueId simpleRuleFieldTestId(UniqueId uniqueId) {
        return uniqueId
                .append(CLASS_SEGMENT_TYPE, SimpleRuleField.class.getName())
                .append(FIELD_SEGMENT_TYPE, SIMPLE_RULE_FIELD_NAME);
    }

    private UniqueId simpleRuleMethodTestId(UniqueId uniqueId) {
        return uniqueId
                .append(CLASS_SEGMENT_TYPE, SimpleRuleMethod.class.getName())
                .append(METHOD_SEGMENT_TYPE, SIMPLE_RULE_METHOD_NAME);
    }

    private UniqueId simpleRulesId(UniqueId rootId) {
        return rootId.append(CLASS_SEGMENT_TYPE, SimpleRules.class.getName());
    }

    private UniqueId simpleRulesInLibraryId(UniqueId uniqueId) {
        return simpleRulesId(uniqueId
                .append(CLASS_SEGMENT_TYPE, SimpleRuleLibrary.class.getName())
                .append(FIELD_SEGMENT_TYPE, SimpleRuleLibrary.RULES_ONE_FIELD));
    }

    private Set<UniqueId> getExpectedIdsForSimpleRuleLibrary(UniqueId uniqueId) {
        UniqueId simpleRuleLibrary = uniqueId.append(CLASS_SEGMENT_TYPE, SimpleRuleLibrary.class.getName());
        Set<UniqueId> simpleRulesIds = getExpectedIdsForSimpleRules(
                simpleRuleLibrary.append(FIELD_SEGMENT_TYPE, SimpleRuleLibrary.RULES_ONE_FIELD));
        Set<UniqueId> simpleRuleFieldIds = singleton(simpleRuleFieldTestId(
                simpleRuleLibrary.append(FIELD_SEGMENT_TYPE, SimpleRuleLibrary.RULES_TWO_FIELD)));

        return Stream.of(simpleRulesIds, simpleRuleFieldIds)
                .flatMap(Set::stream).collect(toSet());
    }

    private Set<UniqueId> getExpectedIdsForSimpleRules(UniqueId append) {
        UniqueId simpleRules = simpleRulesId(append);
        Set<UniqueId> simpleRulesFields = SimpleRules.RULE_FIELD_NAMES.stream().map(fieldName -> simpleRules
                .append(FIELD_SEGMENT_TYPE, fieldName)).collect(toSet());
        Set<UniqueId> simpleRulesMethods = SimpleRules.RULE_METHOD_NAMES.stream().map(methodName -> simpleRules
                .append(METHOD_SEGMENT_TYPE, methodName)).collect(toSet());
        return Stream.of(simpleRulesFields, simpleRulesMethods).flatMap(Set::stream).collect(toSet());
    }

    private Set<UniqueId> getExpectedIdsForComplexRuleLibrary(UniqueId uniqueId) {
        UniqueId complexRuleLibrary = uniqueId.append(CLASS_SEGMENT_TYPE, ComplexRuleLibrary.class.getName());
        Set<UniqueId> simpleRuleLibraryIds = getExpectedIdsForSimpleRuleLibrary(complexRuleLibrary
                .append(FIELD_SEGMENT_TYPE, ComplexRuleLibrary.RULES_ONE_FIELD));
        Set<UniqueId> simpleRulesIds = getExpectedIdsForSimpleRules(complexRuleLibrary
                .append(FIELD_SEGMENT_TYPE, ComplexRuleLibrary.RULES_TWO_FIELD));

        return Stream.of(simpleRuleLibraryIds, simpleRulesIds).flatMap(Set::stream).collect(toSet());
    }

    private Set<UniqueId> privateRuleLibraryIds(UniqueId uniqueId) {
        UniqueId libraryId = uniqueId
                .append(CLASS_SEGMENT_TYPE, LibraryWithPrivateTests.class.getName())
                .append(FIELD_SEGMENT_TYPE, LibraryWithPrivateTests.PRIVATE_RULES_FIELD_NAME)
                .append(CLASS_SEGMENT_TYPE, LibraryWithPrivateTests.SubRules.class.getName())
                .append(FIELD_SEGMENT_TYPE, LibraryWithPrivateTests.SubRules.PRIVATE_RULES_FIELD_NAME);
        return ImmutableSet.of(privateRuleFieldId(libraryId), privateRuleMethodId(libraryId));
    }

    private UniqueId privateRuleFieldId(UniqueId uniqueId) {
        return uniqueId
                .append(CLASS_SEGMENT_TYPE, ClassWithPrivateTests.class.getName())
                .append(FIELD_SEGMENT_TYPE, ClassWithPrivateTests.PRIVATE_RULE_FIELD_NAME);
    }

    private UniqueId privateRuleMethodId(UniqueId uniqueId) {
        return uniqueId
                .append(CLASS_SEGMENT_TYPE, ClassWithPrivateTests.class.getName())
                .append(METHOD_SEGMENT_TYPE, ClassWithPrivateTests.PRIVATE_RULE_METHOD_NAME);
    }

    private Set<UniqueId> getAllLeafUniqueIds(TestDescriptor rootDescriptor) {
        return getAllLeafs(rootDescriptor).stream().map(TestDescriptor::getUniqueId).collect(toSet());
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

    private Set<UniqueId> toUniqueIds(TestDescriptor rootDescriptor) {
        return rootDescriptor.getChildren().stream().map(TestDescriptor::getUniqueId).collect(toSet());
    }

    private void simulateCachedClassesForTest(Class<?> testClass, Class<?> classToReturn) {
        when(classCache.getClassesToAnalyzeFor(eq(testClass), classAnalysisRequestOf(testClass)))
                .thenReturn(importClasses(classToReturn));
    }

    private ClassAnalysisRequest classAnalysisRequestOf(Class<?> testClass) {
        return argThat(r -> Arrays.equals(r.getPackageNames(), testClass.getAnnotation(AnalyzeClasses.class).packages()));
    }

    private EngineExecutionTestListener execute(UniqueId uniqueId, Class<?> testClass) {
        return execute(uniqueId, new EngineDiscoveryTestRequest().withClass(testClass));
    }

    private EngineExecutionTestListener execute(UniqueId uniqueId, EngineDiscoveryTestRequest discoveryRequest) {
        TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

        EngineExecutionTestListener listener = new EngineExecutionTestListener();
        testEngine.execute(new ExecutionRequest(descriptor, listener, discoveryRequest.getConfigurationParameters()));
        return listener;
    }

    private static URI rootOfClass(Class<?> clazz) {
        String resourceName = classFileResource(clazz);
        URL classResource = clazz.getResource(resourceName);
        String rootPath = classResource.toExternalForm()
                .replace(resourceName, "")
                .replaceAll("^jar:", "")
                .replaceAll("!$", "");
        return URI.create(rootPath);
    }

    private static URI uriOfClass(Class<?> clazz) {
        String resourceName = classFileResource(clazz);
        return URI.create(clazz.getResource(resourceName).toExternalForm());
    }

    private static String classFileResource(Class<?> clazz) {
        return String.format("/%s.class", clazz.getName().replace('.', '/'));
    }
}