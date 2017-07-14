package com.tngtech.archunit.lang.extension;

import java.io.IOException;
import java.util.Comparator;
import java.util.regex.Pattern;

import com.tngtech.archunit.lang.extension.examples.DummyTestExtension;
import com.tngtech.archunit.lang.extension.examples.TestExtension;
import com.tngtech.archunit.lang.extension.examples.TestExtensionWithNullIdentifier;
import com.tngtech.archunit.lang.extension.examples.TestExtensionWithSameIdentifier;
import com.tngtech.archunit.lang.extension.examples.YetAnotherDummyTestExtension;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchUnitExtensionLoaderTest {
    @Rule
    public final TestServicesFile testServicesFile = new TestServicesFile();
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private ArchUnitExtensionLoader extensionLoader = new ArchUnitExtensionLoader();

    @Test
    public void loads_a_single_extension() throws IOException {
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
    public void loads_multiple_extensions() throws IOException {
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

        thrown.expect(ExtensionLoadingException.class);
        thrown.expectMessage("identifier");
        thrown.expectMessage("null");
        thrown.expectMessage(containingWord(TestExtensionWithNullIdentifier.class.getName()));
        extensionLoader.getAll();
    }

    @Test
    public void rejects_non_unique_extension_identifier() {
        testServicesFile.addService(TestExtension.class);
        testServicesFile.addService(TestExtensionWithSameIdentifier.class);

        thrown.expect(ExtensionLoadingException.class);
        thrown.expectMessage("must be unique");
        thrown.expectMessage(TestExtension.UNIQUE_IDENTIFIER);
        thrown.expectMessage(containingWord(TestExtension.class.getName()));
        thrown.expectMessage(containingWord(TestExtensionWithSameIdentifier.class.getName()));
        extensionLoader.getAll();
    }

    private Matcher<String> containingWord(final String word) {
        final Pattern wordPattern = Pattern.compile(" " + quote(word) + "[: ]");
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return wordPattern.matcher(item).find();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("containing word '%s'", word));
            }
        };
    }

    private static Comparator<Object> sameInstance() {
        return new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return o1 == o2 ? 0 : -1;
            }
        };
    }
}