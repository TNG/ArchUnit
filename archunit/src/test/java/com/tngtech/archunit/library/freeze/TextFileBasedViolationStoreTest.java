package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.ViolationStoreFactory.TextFileBasedViolationStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class TextFileBasedViolationStoreTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ViolationStore store = new TextFileBasedViolationStore();
    private File configuredFolder;

    @Before
    public void setUp() throws Exception {
        configuredFolder = new File(temporaryFolder.newFolder(), "notyetthere");

        store.initialize(propertiesOf(
                "default.path", configuredFolder.getAbsolutePath(),
                "default.allowStoreCreation", String.valueOf(true)));
    }

    @Test
    public void reports_unknown_rule_as_unstored() {
        assertThat(store.contains(defaultRule())).as("store contains random rule").isFalse();
    }

    @Test
    public void throws_an_exception_if_violations_of_unstored_rule_are_requested() {
        ArchRule rule = defaultRule();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No rule stored with description '" + rule.getDescription() + "'");

        store.getViolations(rule);
    }

    @Test
    public void stores_violations_of_single_rule_in_configured_folder() throws IOException {
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));

        Properties properties = readProperties(new File(configuredFolder, "stored.rules"));
        String ruleViolationsFile = properties.getProperty(defaultRule().getDescription());
        assertThat(ruleViolationsFile).isNotBlank();

        List<String> violationLines = Files.readLines(new File(configuredFolder, ruleViolationsFile), UTF_8);
        assertThat(violationLines).containsOnly("first violation", "second violation");
    }

    @Test
    public void reads_violations_of_single_rule_from_configured_folder() {
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));

        assertThat(store.contains(defaultRule())).as("store contains rule").isTrue();

        List<String> storedViolations = store.getViolations(defaultRule());
        assertThat(storedViolations).containsOnly("first violation", "second violation");
    }

    @Test
    public void stores_violations_of_multiple_rules() {
        ArchRule firstRule = rule("first rule");
        store.save(firstRule, ImmutableList.of("first violation1", "first violation2"));
        ArchRule secondRule = rule("second rule");
        store.save(secondRule, ImmutableList.of("second violation1", "second violation2"));
        ArchRule thirdRule = rule("third rule");
        store.save(thirdRule, ImmutableList.of("third violation1", "third violation2"));

        assertThat(store.getViolations(firstRule)).containsOnly("first violation1", "first violation2");
        assertThat(store.getViolations(secondRule)).containsOnly("second violation1", "second violation2");
        assertThat(store.getViolations(thirdRule)).containsOnly("third violation1", "third violation2");
    }

    @Test
    public void stores_violations_with_line_breaks() {
        List<String> expected = ImmutableList.of(String.format("first with%nlinebreak"), String.format("second with%nlinebreak"));

        store.save(defaultRule(), expected);

        List<String> violations = store.getViolations(defaultRule());
        assertThat(violations).as("stored violations").containsExactlyElementsOf(expected);
    }

    private Properties readProperties(File file) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private Properties propertiesOf(String... keyValuePairs) {
        Properties result = new Properties();
        LinkedList<String> keyValues = new LinkedList<>(asList(keyValuePairs));
        while (!keyValues.isEmpty()) {
            result.setProperty(keyValues.poll(), keyValues.poll());
        }
        return result;
    }

    private ArchRule defaultRule() {
        return rule("default rule");
    }

    private ArchRule rule(String description) {
        return classes().should().bePublic().as(description);
    }
}
