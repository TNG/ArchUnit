package com.tngtech.archunit.testutil.assertion;

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.testutil.TestUtils;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_RELATIVE_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaPackagesAssertion extends AbstractObjectAssert<JavaPackagesAssertion, JavaPackage[]> {
    public JavaPackagesAssertion(Iterable<JavaPackage> javaPackages) {
        super(sort(javaPackages), JavaPackagesAssertion.class);
    }

    private static JavaPackage[] sort(Iterable<JavaPackage> actual) {
        JavaPackage[] result = Iterables.toArray(actual, JavaPackage.class);
        TestUtils.sortByName(result);
        return result;
    }

    public void containPackagesOf(Class<?>... classes) {
        Set<String> expectedNames = getExpectedNames(classes);
        assertThat(getActualNames()).containsAll(expectedNames);
    }

    private Set<String> getExpectedNames(Class<?>[] classes) {
        return Arrays.stream(classes).map(clazz -> clazz.getPackage().getName()).collect(toSet());
    }

    public void containRelativeNames(String... relativeNames) {
        assertThat(getActualRelativeNames()).contains(relativeNames);
    }

    public void containNames(String... names) {
        assertThat(getActualNames()).contains(names);
    }

    public void containOnlyNames(String... names) {
        assertThat(getActualNames()).containsOnly(names);
    }

    private Set<String> getActualNames() {
        return Arrays.stream(actual).map(GET_NAME).collect(toSet());
    }

    private Set<String> getActualRelativeNames() {
        return Arrays.stream(actual).map(GET_RELATIVE_NAME).collect(toSet());
    }
}
