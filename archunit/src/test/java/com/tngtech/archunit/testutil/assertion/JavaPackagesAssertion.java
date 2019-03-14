package com.tngtech.archunit.testutil.assertion;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_RELATIVE_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaPackagesAssertion extends AbstractObjectAssert<JavaPackagesAssertion, JavaPackage[]> {
    public JavaPackagesAssertion(Iterable<JavaPackage> javaPackages) {
        super(sort(javaPackages), JavaPackagesAssertion.class);
    }

    private static JavaPackage[] sort(Iterable<JavaPackage> actual) {
        JavaPackage[] result = Iterables.toArray(actual, JavaPackage.class);
        sortByName(result);
        return result;
    }

    public void matchOnlyPackagesOf(Class<?>... classes) {
        HashSet<String> expectedNames = new HashSet<>();
        for (Class<?> clazz : classes) {
            expectedNames.add(clazz.getPackage().getName());
        }
        assertThat(getActualNames()).containsOnlyElementsOf(expectedNames);
    }

    public void containOnlyRelativeNames(String... relativeNames) {
        assertThat(getActualRelativeNames()).containsOnly(relativeNames);
    }

    public void containOnlyNames(String... names) {
        assertThat(getActualNames()).containsOnly(names);
    }

    private ImmutableSet<String> getActualNames() {
        return FluentIterable.from(actual).transform(toGuava(GET_NAME)).toSet();
    }

    private ImmutableSet<String> getActualRelativeNames() {
        return FluentIterable.from(actual).transform(toGuava(GET_RELATIVE_NAME)).toSet();
    }

    public static <T extends HasName> void sortByName(T[] result) {
        Arrays.sort(result, new Comparator<HasName>() {
            @Override
            public int compare(HasName o1, HasName o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }
}
