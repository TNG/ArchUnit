package com.tngtech.archunit.core.domain;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaPackage.ClassVisitor;
import com.tngtech.archunit.core.domain.JavaPackage.PackageVisitor;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_CLASSES;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_RELATIVE_NAME;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_SUB_PACKAGES;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThatPackages;
import static java.util.regex.Pattern.quote;

public class JavaPackageTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void creates_default_package() {
        JavaPackage defaultPackage = importDefaultPackage();

        assertThat(defaultPackage.getName()).isEmpty();
        assertThat(defaultPackage.getRelativeName()).isEmpty();
        assertThat(defaultPackage.containsPackage("any")).isFalse();
        assertThat(defaultPackage.containsClass(Object.class)).isFalse();
        assertThat(defaultPackage.containsClassWithFullyQualifiedName("some.SomeClass")).isFalse();
        assertThat(defaultPackage.containsClassWithSimpleName("SomeClass")).isFalse();
    }

    @Test
    public void rejects_retrieving_non_existing_subpackages() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("does not contain");
        thrown.expectMessage("some.pkg");

        importDefaultPackage().getPackage("some.pkg");
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_class_object() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("does not contain");
        thrown.expectMessage(Object.class.getName());

        importDefaultPackage().getClass(Object.class);
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_fully_qualified_name() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("does not contain");
        thrown.expectMessage(Object.class.getName());

        importDefaultPackage().getClassWithFullyQualifiedName(Object.class.getName());
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_simple_name() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("does not contain");
        thrown.expectMessage(Object.class.getSimpleName());

        importDefaultPackage().getClassWithSimpleName(Object.class.getSimpleName());
    }

    @Test
    public void creates_simple_package() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class);

        assertThat(defaultPackage.containsPackage("java.lang"))
                .as("default package contains 'java.lang'").isTrue();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.getName()).isEqualTo("java.lang");
        assertThat(javaPackage.getRelativeName()).isEqualTo("lang");
        assertThatClasses(javaPackage.getClasses()).contain(Object.class, String.class);
    }

    @Test
    public void retrieves_class_by_class_object() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class);

        assertThat(defaultPackage.getPackage("java").containsClass(Object.class))
                .as("package 'java' contains java.lang.Object").isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClass(Object.class))
                .as("java.lang.Object is reported contained by class object").isTrue();
        assertThat(javaPackage.getClass(Object.class).isEquivalentTo(Object.class))
                .as("java.lang.Object is returned by class object").isTrue();
    }

    @Test
    public void retrieves_class_by_fully_qualified_name() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class);

        assertThat(defaultPackage.getPackage("java").containsClassWithFullyQualifiedName(Object.class.getName()))
                .as("package 'java' contains java.lang.Object").isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClassWithFullyQualifiedName(Object.class.getName()))
                .as("java.lang.Object is reported contained by fully qualified name").isTrue();
        assertThat(javaPackage.getClassWithFullyQualifiedName(Object.class.getName()).isEquivalentTo(Object.class))
                .as("java.lang.Object is returned by fully qualified name").isTrue();
    }

    @Test
    public void retrieves_class_by_simple_class_name() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class);

        assertThat(defaultPackage.getPackage("java").containsClassWithSimpleName(Object.class.getSimpleName()))
                .as("package 'java' contains java.lang.Object").isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClassWithSimpleName(Object.class.getSimpleName()))
                .as("java.lang.Object is reported contained by simple name").isTrue();
        assertThat(javaPackage.getClassWithSimpleName(Object.class.getSimpleName()).isEquivalentTo(Object.class))
                .as("java.lang.Object is returned by simple name").isTrue();
    }

    @Test
    public void creates_empty_middle_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class);

        assertThat(defaultPackage.containsPackage("java")).as("default package contains 'java'").isTrue();

        JavaPackage java = defaultPackage.getPackage("java");
        assertThat(java.containsPackage("lang")).isTrue();
        assertThat(java.getPackage("lang").getName()).isEqualTo("java.lang");
    }

    @Test
    public void creates_parent_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class);
        assertThat(defaultPackage.getParent()).as("parent of default package").isAbsent();

        JavaPackage javaLang = defaultPackage.getPackage("java.lang");

        JavaPackage java = javaLang.getParent().get();
        assertThat(java.getName()).isEqualTo("java");
        assertThat(java.containsPackage("lang")).as("package 'java' contains 'lang'").isTrue();
    }

    @Test
    public void iterates_sub_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Collection.class, File.class, Security.class);

        JavaPackage java = defaultPackage.getPackage("java");

        assertThatPackages(java.getSubPackages()).containOnlyRelativeNames("lang", "util", "io", "security");
        assertThatPackages(java.getSubPackages()).containOnlyNames("java.lang", "java.util", "java.io", "java.security");
    }

    @Test
    public void iterates_all_classes() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class, Annotation.class, Field.class, Security.class);

        JavaPackage javaLang = defaultPackage.getPackage("java.lang");

        assertThatClasses(javaLang.getAllClasses()).contain(Object.class, String.class, Annotation.class, Field.class);
    }

    @Test
    public void iterates_all_sub_packages() {
        JavaPackage defaultPackage = importDefaultPackage(
                Object.class, Annotation.class, Collection.class, BlockingQueue.class, Security.class, getClass());

        JavaPackage java = defaultPackage.getPackage("java");

        assertThatPackages(java.getAllSubPackages()).matchOnlyPackagesOf(
                Object.class, Annotation.class, Collection.class, BlockingQueue.class, Security.class);
    }

    @Test
    public void visits_classes() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class, File.class, Serializable.class, Security.class);

        final List<JavaClass> visitedClasses = new ArrayList<>();
        defaultPackage.accept(startsWith("S"), new ClassVisitor() {
            @Override
            public void visit(JavaClass javaClass) {
                visitedClasses.add(javaClass);
            }
        });

        assertThatClasses(visitedClasses).matchInAnyOrder(String.class, Serializable.class, Security.class);
    }

    @Test
    public void visits_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Annotation.class, File.class, Security.class);

        final List<JavaPackage> visitedPackages = new ArrayList<>();
        defaultPackage.accept(nameContains(".lang"), new PackageVisitor() {
            @Override
            public void visit(JavaPackage javaPackage) {
                visitedPackages.add(javaPackage);
            }
        });

        assertThatPackages(visitedPackages).matchOnlyPackagesOf(Object.class, Annotation.class);
    }

    @Test
    public void function_GET_RELATIVE_NAME() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class);

        String name = GET_RELATIVE_NAME.apply(defaultPackage.getPackage("java.lang"));

        assertThat(name).isEqualTo("lang");
    }

    @Test
    public void function_GET_CLASSES() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class, Collection.class);

        Iterable<JavaClass> classes = GET_CLASSES.apply(defaultPackage.getPackage("java.lang"));

        assertThatClasses(classes).matchInAnyOrder(Object.class, String.class);
    }

    @Test
    public void function_GET_SUB_PACKAGES() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Annotation.class, Field.class, Collection.class);

        Iterable<JavaPackage> packages = GET_SUB_PACKAGES.apply(defaultPackage.getPackage("java.lang"));

        assertThatPackages(packages).matchOnlyPackagesOf(Annotation.class, Field.class);
    }

    private Predicate<? super JavaPackage> nameContains(String infix) {
        return HasName.Predicates.nameMatching(".*" + quote(infix) + ".*");
    }

    private DescribedPredicate<JavaClass> startsWith(final String prefix) {
        return GET_SIMPLE_NAME.is(new DescribedPredicate<String>("starts with '%s'", prefix) {
            @Override
            public boolean apply(String input) {
                return input.startsWith(prefix);
            }
        });
    }

    private JavaPackage importDefaultPackage(Class<?>... classes) {
        return JavaPackage.from(importClassesWithContext(classes));
    }
}