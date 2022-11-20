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
import java.util.function.Predicate;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.packageexamples.annotated.PackageLevelAnnotation;
import com.tngtech.archunit.core.domain.packageexamples.first.First1;
import com.tngtech.archunit.core.domain.packageexamples.first.First2;
import com.tngtech.archunit.core.domain.packageexamples.second.ClassDependingOnOtherSecondClass;
import com.tngtech.archunit.core.domain.packageexamples.second.Second1;
import com.tngtech.archunit.core.domain.packageexamples.second.Second2;
import com.tngtech.archunit.core.domain.packageexamples.second.sub.SecondSub1;
import com.tngtech.archunit.core.domain.packageexamples.third.sub.ThirdSub1;
import com.tngtech.archunit.core.domain.packageexamples.unrelated.AnyClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_CLASSES;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_RELATIVE_NAME;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_SUB_PACKAGES;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatPackages;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaPackageTest {

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
        assertThatThrownBy(
                () -> importDefaultPackage().getPackage("some.pkg")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain")
                .hasMessageContaining("some.pkg");
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_class_object() {
        assertThatThrownBy(
                () -> importDefaultPackage().getClass(Object.class)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain")
                .hasMessageContaining(Object.class.getName());
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_fully_qualified_name() {
        assertThatThrownBy(
                () -> importDefaultPackage().getClassWithFullyQualifiedName(Object.class.getName())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain")
                .hasMessageContaining(Object.class.getName());
    }

    @Test
    public void rejects_retrieving_non_existing_classes_by_simple_name() {
        assertThatThrownBy(
                () -> importDefaultPackage().getClassWithSimpleName(Object.class.getSimpleName())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not contain")
                .hasMessageContaining(Object.class.getSimpleName());
    }

    @Test
    public void creates_single_package() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class);

        assertThat(defaultPackage.containsPackage("java.lang"))
                .as("default package contains 'java.lang'").isTrue();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.getName()).isEqualTo("java.lang");
        assertThat(javaPackage.getDescription()).isEqualTo("Package <java.lang>");
        assertThat(javaPackage.getRelativeName()).isEqualTo("lang");
        assertThatTypes(javaPackage.getClasses()).contain(Object.class, String.class);
    }

    @Test
    public void keeps_packages_unique() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class);

        JavaPackage javaLang = defaultPackage.getPackage("java.lang");
        assertThat(javaLang).isSameAs(javaLang.getClass(Object.class).getPackage());
    }

    @Test
    public void retrieves_JavaClasses() {
        JavaClasses classes = new ClassFileImporter().importClasses(Object.class, String.class);
        JavaPackage defaultPackage = classes.getDefaultPackage();
        JavaClass javaLangObject = classes.get(Object.class);

        assertThat(defaultPackage.getPackage("java").containsClass(javaLangObject))
                .as("package 'java' contains " + javaLangObject.getName()).isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClass(javaLangObject))
                .as(javaLangObject.getName() + " is contained").isTrue();
        assertThat(javaPackage.getClass(Object.class))
                .as(javaLangObject.getName() + "is returned").isEqualTo(javaLangObject);
    }

    @Test
    public void retrieves_class_by_class_object() {
        Class<?> javaLangObject = Object.class;
        JavaPackage defaultPackage = importDefaultPackage(javaLangObject, String.class);

        assertThat(defaultPackage.getPackage("java").containsClass(javaLangObject))
                .as("package 'java' contains " + javaLangObject.getName()).isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClass(javaLangObject))
                .as(javaLangObject + " is reported contained by class object").isTrue();
        assertThat(javaPackage.getClass(javaLangObject).isEquivalentTo(javaLangObject))
                .as(javaLangObject + " is returned by class object").isTrue();
    }

    @Test
    public void retrieves_class_by_fully_qualified_name() {
        Class<Object> javaLangObject = Object.class;
        JavaPackage defaultPackage = importDefaultPackage(javaLangObject, String.class);

        assertThat(defaultPackage.getPackage("java").containsClassWithFullyQualifiedName(javaLangObject.getName()))
                .as("package 'java' contains " + javaLangObject.getName()).isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClassWithFullyQualifiedName(javaLangObject.getName()))
                .as(javaLangObject.getName() + " is reported contained by fully qualified name").isTrue();
        assertThat(javaPackage.getClassWithFullyQualifiedName(javaLangObject.getName()).isEquivalentTo(javaLangObject))
                .as(javaLangObject.getName() + " is returned by fully qualified name").isTrue();
    }

    @Test
    public void retrieves_class_by_simple_class_name() {
        Class<Object> javaLangObject = Object.class;
        JavaPackage defaultPackage = importDefaultPackage(javaLangObject, String.class);

        assertThat(defaultPackage.getPackage("java").containsClassWithSimpleName(javaLangObject.getSimpleName()))
                .as("package 'java' contains " + javaLangObject.getName()).isFalse();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");

        assertThat(javaPackage.containsClassWithSimpleName(javaLangObject.getSimpleName()))
                .as(javaLangObject + " is reported contained by simple name").isTrue();
        assertThat(javaPackage.getClassWithSimpleName(javaLangObject.getSimpleName()).isEquivalentTo(javaLangObject))
                .as(javaLangObject + " is returned by simple name").isTrue();
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
        assertThat(defaultPackage.getParent()).as("parent of default package").isEmpty();

        JavaPackage javaLang = defaultPackage.getPackage("java.lang");

        JavaPackage java = javaLang.getParent().get();
        assertThat(java.getName()).isEqualTo("java");
        assertThat(java.containsPackage("lang")).as("package 'java' contains 'lang'").isTrue();
    }

    @Test
    public void iterates_sub_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Collection.class, File.class, Security.class);

        JavaPackage java = defaultPackage.getPackage("java");

        assertThatPackages(java.getSubpackages()).containRelativeNames("lang", "util", "io", "security");
        assertThatPackages(java.getSubpackages()).containNames("java.lang", "java.util", "java.io", "java.security");
    }

    @Test
    public void iterates_all_classes() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class, Annotation.class, Field.class, Security.class);

        JavaPackage javaLang = defaultPackage.getPackage("java.lang");

        assertThatTypes(javaLang.getClassesInPackageTree()).contain(Object.class, String.class, Annotation.class, Field.class);
    }

    @Test
    public void iterates_all_sub_packages() {
        JavaPackage defaultPackage = importDefaultPackage(
                Object.class, Annotation.class, Collection.class, BlockingQueue.class, Security.class, getClass());

        JavaPackage java = defaultPackage.getPackage("java");

        assertThatPackages(java.getSubpackagesInTree()).containPackagesOf(
                Object.class, Annotation.class, Collection.class, BlockingQueue.class, Security.class);
    }

    @Test
    public void visits_classes() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, String.class, File.class, Serializable.class, Security.class);

        List<JavaClass> visitedClasses = new ArrayList<>();
        defaultPackage.traversePackageTree(startsWith("S"), visitedClasses::add);

        assertThatTypes(visitedClasses).contain(String.class, Serializable.class, Security.class);
        for (JavaClass visitedClass : visitedClasses) {
            assertThat(visitedClass.getSimpleName()).startsWith("S");
        }
    }

    @Test
    public void visits_packages() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Annotation.class, File.class, Security.class);

        List<JavaPackage> visitedPackages = new ArrayList<>();
        defaultPackage.traversePackageTree(nameContains(".lang"), visitedPackages::add);

        assertThatPackages(visitedPackages).containPackagesOf(Object.class, Annotation.class);
        for (JavaPackage visitedPackage : visitedPackages) {
            assertThat(visitedPackage.getName()).contains(".lang");
        }
    }

    @Test
    public void has_class_dependencies_from_this_package() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThatDependencies(examplePackage.getPackage("second").getClassDependenciesFromThisPackage())
                .contain(Second1.class, First2.class)
                .contain(Second2.class, SecondSub1.class)
                .doesNotContain(ClassDependingOnOtherSecondClass.class, Second1.class)
                .doesNotContain(SecondSub1.class, ThirdSub1.class)
                .doesNotContain(SecondSub1.class, First1.class);

        assertThatDependencies(examplePackage.getPackage("third").getClassDependenciesFromThisPackage())
                .isEmpty();

        assertThatDependencies(examplePackage.getPackage("unrelated").getClassDependenciesFromThisPackage())
                .containOnly(AnyClass.class, Object.class);
    }

    @Test
    public void has_class_dependencies_from_this_package_tree() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThatDependencies(examplePackage.getPackage("second").getClassDependenciesFromThisPackageTree())
                .contain(Second1.class, First2.class)
                .contain(SecondSub1.class, ThirdSub1.class)
                .contain(SecondSub1.class, First1.class)
                .doesNotContain(Second2.class, SecondSub1.class)
                .doesNotContain(ClassDependingOnOtherSecondClass.class, Second1.class);

        assertThatDependencies(examplePackage.getPackage("third").getClassDependenciesFromThisPackageTree())
                .contain(ThirdSub1.class, First1.class);

        assertThatDependencies(examplePackage.getPackage("unrelated").getClassDependenciesFromThisPackageTree())
                .containOnly(AnyClass.class, Object.class);
    }

    @Test
    public void has_class_dependencies_to_this_package() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThatDependencies(examplePackage.getPackage("first").getClassDependenciesToThisPackage())
                .contain(Second1.class, First2.class)
                .contain(SecondSub1.class, First1.class)
                .contain(ThirdSub1.class, First1.class);

        assertThatDependencies(examplePackage.getPackage("second").getClassDependenciesToThisPackage())
                .contain(First1.class, Second1.class)
                .contain(SecondSub1.class, Second2.class)
                .doesNotContain(First2.class, SecondSub1.class)
                .doesNotContain(ClassDependingOnOtherSecondClass.class, Second1.class);

        assertThatDependencies(examplePackage.getPackage("third").getClassDependenciesToThisPackage())
                .isEmpty();

        assertThatDependencies(examplePackage.getPackage("unrelated").getClassDependenciesToThisPackage())
                .isEmpty();
    }

    @Test
    public void has_class_dependencies_to_this_package_tree() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThatDependencies(examplePackage.getPackage("first").getClassDependenciesToThisPackageTree())
                .contain(Second1.class, First2.class)
                .contain(ThirdSub1.class, First1.class)
                .contain(SecondSub1.class, First1.class);

        assertThatDependencies(examplePackage.getPackage("second").getClassDependenciesToThisPackageTree())
                .contain(First1.class, Second1.class)
                .contain(First2.class, SecondSub1.class)
                .doesNotContain(SecondSub1.class, Second2.class)
                .doesNotContain(ClassDependingOnOtherSecondClass.class, Second1.class);

        assertThatDependencies(examplePackage.getPackage("third").getClassDependenciesToThisPackageTree())
                .contain(SecondSub1.class, ThirdSub1.class);

        assertThatDependencies(examplePackage.getPackage("unrelated").getClassDependenciesToThisPackageTree())
                .isEmpty();
    }

    @Test
    public void has_package_dependencies_from_this_package() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThat(examplePackage.getPackage("second").getPackageDependenciesFromThisPackage())
                .containsOnly(
                        getRoot(examplePackage).getPackage("java.lang"),
                        examplePackage.getPackage("second.sub"),
                        examplePackage.getPackage("first"));

        assertThat(examplePackage.getPackage("third").getPackageDependenciesFromThisPackage())
                .isEmpty();

        assertThatPackages(examplePackage.getPackage("unrelated").getPackageDependenciesFromThisPackage())
                .containOnlyNames("java.lang");
    }

    @Test
    public void has_package_dependencies_from_this_package_tree() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThat(examplePackage.getPackage("second").getPackageDependenciesFromThisPackageTree())
                .containsOnly(
                        getRoot(examplePackage).getPackage("java.lang"),
                        examplePackage.getPackage("first"),
                        examplePackage.getPackage("third.sub"));

        assertThat(examplePackage.getPackage("third").getPackageDependenciesFromThisPackageTree())
                .containsOnly(examplePackage.getPackage("first"));

        assertThatPackages(examplePackage.getPackage("unrelated").getPackageDependenciesFromThisPackageTree())
                .containOnlyNames("java.lang");
    }

    @Test
    public void has_package_dependencies_to_this_package() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThat(examplePackage.getPackage("first").getPackageDependenciesToThisPackage())
                .containsOnly(
                        examplePackage.getPackage("second"),
                        examplePackage.getPackage("second.sub"),
                        examplePackage.getPackage("third.sub"));

        assertThat(examplePackage.getPackage("second").getPackageDependenciesToThisPackage())
                .containsOnly(
                        examplePackage.getPackage("first"),
                        examplePackage.getPackage("second.sub"));

        assertThat(examplePackage.getPackage("third").getPackageDependenciesToThisPackage())
                .isEmpty();

        assertThat(examplePackage.getPackage("unrelated").getPackageDependenciesToThisPackage())
                .isEmpty();
    }

    @Test
    public void has_package_dependencies_to_this_package_tree() {
        JavaPackage examplePackage = importPackage("packageexamples");

        assertThat(examplePackage.getPackage("first").getPackageDependenciesToThisPackageTree())
                .containsOnly(
                        examplePackage.getPackage("second"),
                        examplePackage.getPackage("second.sub"),
                        examplePackage.getPackage("third.sub"));

        assertThat(examplePackage.getPackage("second").getPackageDependenciesToThisPackageTree())
                .containsOnly(examplePackage.getPackage("first"));

        assertThat(examplePackage.getPackage("third").getPackageDependenciesToThisPackageTree())
                .containsOnly(examplePackage.getPackage("second.sub"));

        assertThat(examplePackage.getPackage("unrelated").getPackageDependenciesToThisPackageTree())
                .isEmpty();
    }

    @Test
    public void test_getPackageInfo() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.getPackageInfo()).isNotNull();

        assertThatThrownBy(nonAnnotatedPackage::getPackageInfo)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(nonAnnotatedPackage.getDescription() + " does not contain a package-info.java");
    }

    @Test
    public void test_tryGetPackageInfo() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.tryGetPackageInfo()).isPresent();
        assertThat(nonAnnotatedPackage.tryGetPackageInfo()).isEmpty();
    }

    @Test
    public void test_getAnnotations() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        JavaAnnotation<JavaPackage> annotation = getOnlyElement(annotatedPackage.getAnnotations());
        assertThatType(annotation.getRawType()).matches(PackageLevelAnnotation.class);
        assertThat(annotation.getOwner()).isEqualTo(annotatedPackage);

        assertThat(nonAnnotatedPackage.getAnnotations()).isEmpty();
    }

    @Test
    public void test_getAnnotationOfType_type() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.getAnnotationOfType(PackageLevelAnnotation.class)).isInstanceOf(PackageLevelAnnotation.class);

        assertThatThrownBy(() -> annotatedPackage.getAnnotationOfType(Deprecated.class)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(annotatedPackage.getDescription() + " is not annotated with @" + Deprecated.class.getName());

        assertThatThrownBy(() -> nonAnnotatedPackage.getAnnotationOfType(Deprecated.class)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(nonAnnotatedPackage.getDescription() + " is not annotated with @" + Deprecated.class.getName());
    }

    @Test
    public void test_getAnnotationOfType_typeName() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThatType(annotatedPackage.getAnnotationOfType(PackageLevelAnnotation.class.getName())
                .getRawType()).matches(PackageLevelAnnotation.class);

        assertThatThrownBy(() -> annotatedPackage.getAnnotationOfType("not.There")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(annotatedPackage.getDescription() + " is not annotated with @not.There");

        assertThatThrownBy(() -> nonAnnotatedPackage.getAnnotationOfType("not.There")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(nonAnnotatedPackage.getDescription() + " is not annotated with @not.There");
    }

    @Test
    public void test_tryGetAnnotationOfType_type() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.tryGetAnnotationOfType(PackageLevelAnnotation.class)).isPresent();
        assertThat(annotatedPackage.tryGetAnnotationOfType(Deprecated.class)).isEmpty();

        assertThat(nonAnnotatedPackage.tryGetAnnotationOfType(Deprecated.class)).isEmpty();
    }

    @Test
    public void test_tryGetAnnotationOfType_typeName() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.tryGetAnnotationOfType(PackageLevelAnnotation.class.getName())).isPresent();
        assertThat(annotatedPackage.tryGetAnnotationOfType(Deprecated.class.getName())).isEmpty();

        assertThat(nonAnnotatedPackage.tryGetAnnotationOfType(Deprecated.class.getName())).isEmpty();
    }

    @Test
    public void test_isAnnotatedWith_type() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.isAnnotatedWith(PackageLevelAnnotation.class)).isTrue();
        assertThat(annotatedPackage.isAnnotatedWith(Deprecated.class)).isFalse();

        assertThat(nonAnnotatedPackage.isAnnotatedWith(Deprecated.class)).isFalse();
    }

    @Test
    public void test_isAnnotatedWith_typeName() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.isAnnotatedWith(PackageLevelAnnotation.class.getName())).isTrue();
        assertThat(annotatedPackage.isAnnotatedWith(Deprecated.class.getName())).isFalse();

        assertThat(nonAnnotatedPackage.isAnnotatedWith(Deprecated.class.getName())).isFalse();
    }

    @Test
    public void test_isAnnotatedWith_predicate() {
        JavaPackage annotatedPackage = importPackage("packageexamples.annotated");
        JavaPackage nonAnnotatedPackage = importPackage("packageexamples");

        assertThat(annotatedPackage.isAnnotatedWith(alwaysTrue())).isTrue();
        assertThat(annotatedPackage.isAnnotatedWith(alwaysFalse())).isFalse();

        assertThat(nonAnnotatedPackage.isAnnotatedWith(alwaysTrue())).isFalse();
        assertThat(nonAnnotatedPackage.isAnnotatedWith(alwaysFalse())).isFalse();
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

        assertThatTypes(classes).contain(Object.class, String.class);
        for (JavaClass javaClass : classes) {
            assertThat(javaClass.getPackageName()).startsWith("java.lang");
        }
    }

    @Test
    public void function_GET_SUB_PACKAGES() {
        JavaPackage defaultPackage = importDefaultPackage(Object.class, Annotation.class, Field.class, Collection.class);

        Iterable<JavaPackage> packages = GET_SUB_PACKAGES.apply(defaultPackage.getPackage("java.lang"));

        assertThatPackages(packages).containPackagesOf(Annotation.class, Field.class);
    }

    private JavaPackage getRoot(JavaPackage javaPackage) {
        JavaPackage result = javaPackage;
        while (result.getParent().isPresent()) {
            result = result.getParent().get();
        }
        return result;
    }

    private Predicate<? super JavaPackage> nameContains(String infix) {
        return HasName.Predicates.nameMatching(".*" + quote(infix) + ".*");
    }

    private DescribedPredicate<JavaClass> startsWith(String prefix) {
        return GET_SIMPLE_NAME.is(new DescribedPredicate<String>("starts with '%s'", prefix) {
            @Override
            public boolean test(String input) {
                return input.startsWith(prefix);
            }
        });
    }

    private JavaPackage importDefaultPackage(Class<?>... classes) {
        return new ClassFileImporter().importClasses(classes).getDefaultPackage();
    }

    private JavaPackage importPackage(String subpackageName) {
        String packageName = getClass().getPackage().getName() + "." + subpackageName;
        JavaClasses classes = new ClassFileImporter().importPackages(packageName);
        return classes.getPackage(packageName);
    }
}
