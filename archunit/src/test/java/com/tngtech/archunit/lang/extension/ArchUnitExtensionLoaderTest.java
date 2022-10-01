package com.tngtech.archunit.lang.extension;

import java.util.Comparator;
import java.util.regex.Pattern;

import com.tngtech.archunit.lang.extension.examples.DummyTestExtension;
import com.tngtech.archunit.lang.extension.examples.TestExtension;
import com.tngtech.archunit.lang.extension.examples.TestExtensionWithIllegalIdentifier;
import com.tngtech.archunit.lang.extension.examples.TestExtensionWithNullIdentifier;
import com.tngtech.archunit.lang.extension.examples.TestExtensionWithSameIdentifier;
import com.tngtech.archunit.lang.extension.examples.YetAnotherDummyTestExtension;
import com.tngtech.archunit.testutil.LogTestRule;
import org.apache.logging.log4j.Level;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchUnitExtensionLoaderTest {
    @Rule
    public final TestServicesFile testServicesFile = new TestServicesFile();
    @Rule
    public final LogTestRule logTestRule = new LogTestRule();

    private final ArchUnitExtensionLoader extensionLoader = new ArchUnitExtensionLoader();

    @Test
    public void loads_a_single_extension() {
        testServicesFile.addService(TestExtension.class);

        Iterable<ArchUnitExtension> extensions = extensionLoader.getAll();

        assertThat(getOnlyElement(extensions)).isInstanceOf(TestExtension.class);
    }

    @Test
    public void caches_loaded_extensions() {
        testServicesFile.addService(TestExtension.class);

        Iterable<ArchUnitExtension> first = extensionLoader.getAll();
        Iterable<ArchUnitExtension> second = extensionLoader.getAll();

        assertThat(first).usingElementComparator(sameInstance()).hasSameElementsAs(second);
    }

    @Test
    public void loads_multiple_extensions() {
        testServicesFile.addService(TestExtension.class);
        testServicesFile.addService(DummyTestExtension.class);
        testServicesFile.addService(YetAnotherDummyTestExtension.class);

        Iterable<ArchUnitExtension> extensions = extensionLoader.getAll();

        assertThat(extensions)
                .hasSize(3)
                .hasAtLeastOneElementOfType(TestExtension.class)
                .hasAtLeastOneElementOfType(DummyTestExtension.class)
                .hasAtLeastOneElementOfType(YetAnotherDummyTestExtension.class);
    }

    @Test
    public void rejects_null_extension_identifier() {
        testServicesFile.addService(TestExtensionWithNullIdentifier.class);

        assertThatThrownBy(extensionLoader::getAll)
                .isInstanceOf(ExtensionLoadingException.class)
                .hasMessageContaining("identifier")
                .hasMessageContaining("null")
                .is(containingWord(TestExtensionWithNullIdentifier.class.getName()));
    }

    @Test
    public void rejects_illegal_characters_in_extension_identifier() {
        testServicesFile.addService(TestExtensionWithIllegalIdentifier.class);

        assertThatThrownBy(extensionLoader::getAll)
                .isInstanceOf(ExtensionLoadingException.class)
                .hasMessageContaining("identifier")
                .hasMessageContaining("'.'")
                .is(containingWord(TestExtensionWithIllegalIdentifier.class.getName()));
    }

    @Test
    public void rejects_non_unique_extension_identifier() {
        testServicesFile.addService(TestExtension.class);
        testServicesFile.addService(TestExtensionWithSameIdentifier.class);

        assertThatThrownBy(extensionLoader::getAll)
                .isInstanceOf(ExtensionLoadingException.class)
                .hasMessageContaining("must be unique")
                .hasMessageContaining(TestExtension.UNIQUE_IDENTIFIER)
                .is(containingWord(TestExtension.class.getName()))
                .is(containingWord(TestExtensionWithSameIdentifier.class.getName()));
    }

    @Test
    public void logs_discovered_extensions() {
        testServicesFile.addService(TestExtension.class);
        testServicesFile.addService(DummyTestExtension.class);

        logTestRule.watch(ArchUnitExtensionLoader.class, Level.INFO);

        extensionLoader.getAll();

        logTestRule.assertLogMessage(Level.INFO, expectedExtensionLoadedMessage(TestExtension.UNIQUE_IDENTIFIER));
        logTestRule.assertLogMessage(Level.INFO, expectedExtensionLoadedMessage(DummyTestExtension.UNIQUE_IDENTIFIER));
    }

    private String expectedExtensionLoadedMessage(String identifier) {
        return "Loaded " + ArchUnitExtension.class.getSimpleName() + " with id '" + identifier + "'";
    }

    private Condition<Throwable> containingWord(final String word) {
        final Pattern wordPattern = Pattern.compile(" " + quote(word) + "[: ]");
        return new Condition<Throwable>(String.format("containing word '%s'", word)) {
            @Override
            public boolean matches(Throwable value) {
                return wordPattern.matcher(value.getMessage()).find();
            }
        };
    }

    private static Comparator<Object> sameInstance() {
        return (o1, o2) -> o1 == o2 ? 0 : -1;
    }
}
