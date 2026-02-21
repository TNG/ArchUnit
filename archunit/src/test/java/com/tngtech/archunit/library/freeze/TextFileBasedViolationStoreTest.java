package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.tngtech.archunit.lang.ArchRule;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TextFileBasedViolationStoreTest {

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
    public void throws_exception_when_there_are_obsolete_entries_in_storedRules_files() throws Exception {
        // given
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));
        Properties properties = readProperties(new File(configuredFolder, "stored.rules"));
        File ruleViolationsFile = new File(configuredFolder, properties.getProperty(defaultRule().getDescription()));
        assertThat(ruleViolationsFile.delete()).isTrue();

        // when && then
        ThrowableAssert.ThrowingCallable storeInitialization = () -> store.initialize(propertiesOf(
                "default.path", configuredFolder.getAbsolutePath(),
                "default.allowStoreUpdate", String.valueOf(false)));
        assertThatThrownBy(storeInitialization)
                .isInstanceOf(StoreUpdateFailedException.class)
                .hasMessage("Failed to remove 1 obsolete stored rule(s). Updating frozen violations is disabled (enable by configuration freeze.store.default.allowStoreUpdate=true)");
        assertThat(store.contains(defaultRule())).isTrue();
    }

    @Test
    public void deletes_obsolete_entries_from_storedRules_files() throws Exception {
        // given
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));
        Properties properties = readProperties(new File(configuredFolder, "stored.rules"));
        File ruleViolationsFile = new File(configuredFolder, properties.getProperty(defaultRule().getDescription()));
        assertThat(ruleViolationsFile.delete()).isTrue();

        // when
        store.initialize(propertiesOf("default.path", configuredFolder.getAbsolutePath()));

        // then
        assertThat(store.contains(defaultRule())).isFalse();
    }

    @Test
    public void throws_exception_when_there_are_unreferenced_files_in_store_directory() throws Exception {
        // given
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));
        File propertiesFile = new File(configuredFolder, "stored.rules");
        Properties properties = readProperties(propertiesFile);
        File ruleViolationsFile = new File(configuredFolder, properties.getProperty(defaultRule().getDescription()));
        assertThat(ruleViolationsFile).exists();
        properties.remove(defaultRule().getDescription());
        storeProperties(propertiesFile, properties);

        // when && then
        ThrowableAssert.ThrowingCallable storeInitialization = () -> store.initialize(propertiesOf(
                "default.path", configuredFolder.getAbsolutePath(),
                "default.allowStoreUpdate", String.valueOf(false)));
        assertThatThrownBy(storeInitialization)
                .isInstanceOf(StoreUpdateFailedException.class)
                .hasMessage("Failed to remove 1 unreferenced rule files. Updating frozen store is disabled (enable by configuration freeze.store.default.allowStoreUpdate=true)");
        assertThat(ruleViolationsFile).exists();
    }

    @Test
    public void deletes_files_not_referenced_in_storedRules() throws Exception {
        // given
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));
        File propertiesFile = new File(configuredFolder, "stored.rules");
        Properties properties = readProperties(propertiesFile);
        File ruleViolationsFile = new File(configuredFolder, properties.getProperty(defaultRule().getDescription()));
        assertThat(ruleViolationsFile).exists();
        properties.remove(defaultRule().getDescription());
        storeProperties(propertiesFile, properties);

        // when
        store.initialize(propertiesOf("default.path", configuredFolder.getAbsolutePath()));

        // then
        assertThat(store.contains(defaultRule())).isFalse();
        assertThat(ruleViolationsFile).doesNotExist();
    }

    @Test
    public void reports_unknown_rule_as_unstored() {
        assertThat(store.contains(defaultRule())).as("store contains random rule").isFalse();
    }

    @Test
    public void throws_an_exception_if_violations_of_unstored_rule_are_requested() {
        ArchRule rule = defaultRule();

        assertThatThrownBy(() -> store.getViolations(rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No rule stored with description '%s'", rule.getDescription());
    }

    @Test
    public void stores_violations_of_single_rule_in_configured_folder() throws IOException {
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));

        Properties properties = readProperties(new File(configuredFolder, "stored.rules"));
        String ruleViolationsFile = properties.getProperty(defaultRule().getDescription());
        assertThat(ruleViolationsFile).isNotBlank();

        String contents = Files.asCharSource(new File(configuredFolder, ruleViolationsFile), UTF_8).read();
        assertThat(contents).isEqualTo("first violation\nsecond violation\n");
    }

    @Test
    public void updates_stored_violations_of_single_rule() throws IOException {
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));
        store.save(defaultRule(), ImmutableList.of("first overwritten violation", "second overwritten violation"));

        Properties properties = readProperties(new File(configuredFolder, "stored.rules"));
        String ruleViolationsFile = properties.getProperty(defaultRule().getDescription());
        List<String> violationLines = Files.readLines(new File(configuredFolder, ruleViolationsFile), UTF_8);
        assertThat(violationLines).containsOnly("first overwritten violation", "second overwritten violation");
    }

    @Test
    public void reads_violations_of_single_rule_from_configured_folder() {
        store.save(defaultRule(), ImmutableList.of("first violation", "second violation"));

        assertThat(store.contains(defaultRule())).as("store contains rule").isTrue();

        List<String> storedViolations = store.getViolations(defaultRule());
        assertThat(storedViolations).containsOnly("first violation", "second violation");
    }

    @Test
    public void reads_empty_list_of_violations() {
        store.save(defaultRule(), ImmutableList.of());

        List<String> storedViolations = store.getViolations(defaultRule());

        assertThat(storedViolations).isEmpty();
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

    private static void storeProperties(File propertiesFile, Properties properties) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(propertiesFile)) {
            properties.store(outputStream, "");
        }
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
