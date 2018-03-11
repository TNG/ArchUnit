package com.tngtech.archunit.junit;

import java.util.Set;
import java.util.stream.Stream;

import com.tngtech.archunit.junit.testexamples.SimpleRuleField;
import com.tngtech.archunit.junit.testexamples.SimpleRuleLibrary;
import com.tngtech.archunit.junit.testexamples.SimpleRuleMethod;
import com.tngtech.archunit.junit.testexamples.SimpleRules;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;

@SuppressWarnings("ConstantConditions")
class ArchUnitTestEngineTest {
    private final ArchUnitTestEngine testEngine = new ArchUnitTestEngine();

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

            UniqueId expectedId = uniqueId
                    .append("class", SimpleRuleField.class.getName())
                    .append("field", SimpleRuleField.SIMPLE_RULE_FIELD_NAME)
                    .append("rule", SimpleRuleField.simple_rule.getDescription());
            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(expectedId);
        }

        @Test
        void a_simple_rule_method() {
            UniqueId uniqueId = uniqueTestId();
            EngineDiscoveryTestRequest discoveryRequest = new EngineDiscoveryTestRequest().withClass(SimpleRuleMethod.class);

            TestDescriptor descriptor = testEngine.discover(discoveryRequest, uniqueId);

            TestDescriptor ruleDescriptor = getOnlyTest(descriptor);

            UniqueId expectedId = uniqueId
                    .append("class", SimpleRuleMethod.class.getName())
                    .append("method", SimpleRuleMethod.SIMPLE_RULE_METHOD_NAME);
            assertThat(ruleDescriptor.getUniqueId()).isEqualTo(expectedId);
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

            Set<UniqueId> simpleRuleField = singleton(simpleRuleLibrary.append("field", SimpleRuleLibrary.RULES_TWO_FIELD)
                    .append("class", SimpleRuleField.class.getName())
                    .append("field", SimpleRuleField.SIMPLE_RULE_FIELD_NAME)
                    .append("rule", getDescription(SimpleRuleField.class, SimpleRuleField.SIMPLE_RULE_FIELD_NAME)));

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
    }

    private UniqueId uniqueTestId() {
        return UniqueId.root("test", "id");
    }
}