package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasType;
import com.tngtech.archunit.lang.FailureReport;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.properties.HasType.Functions.GET_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;

public class ClassesShouldThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void areNamed() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNamed(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotNamed() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotNamed(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().haveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().dontHaveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().haveNameMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().haveNameNotMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void resideInPackage() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().resideInPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().resideOutsideOfPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access()
                .classesThat().resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingConstructor.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access()
                .classesThat().resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @Test
    public void arePublic() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().arePublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    public void areNotPublic() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotPublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingProtectedClass.class);
    }

    @Test
    public void areNotProtected() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                ClassAccessingPackagePrivateClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().arePackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPackagePrivateClass.class);
    }

    @Test
    public void areNotPackagePrivate() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotPackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().arePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    public void areNotPrivate() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotPrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void haveModifiers() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().haveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    public void dontHaveModifiers() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().dontHaveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(noClasses().should().access().classesThat().areNotAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        DescribedPredicate<HasName> hasNamePredicate = GET_NAME.is(equalTo(Collection.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableTo(hasNamePredicate))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        DescribedPredicate<HasName> hasNamePredicate = GET_NAME.is(equalTo(Collection.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableTo(hasNamePredicate))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        DescribedPredicate<HasName> predicate = GET_NAME.is(equalTo(Collection.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areAssignableFrom(predicate))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        DescribedPredicate<HasName> predicate = GET_NAME.is(equalTo(Collection.class.getName()));
        List<JavaClass> classes = filterClassesHitBy(
                noClasses().should().access().classesThat().areNotAssignableFrom(predicate))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    private Evaluator filterClassesHitBy(ClassesShouldConjunction classesShould) {
        return new Evaluator(classesShould);
    }

    private class Evaluator {
        private final ClassesShouldConjunction classesShould;

        Evaluator(ClassesShouldConjunction classesShould) {
            this.classesShould = classesShould;
        }

        public List<JavaClass> on(Class<?>... toCheck) {
            JavaClasses classes = importClasses(toCheck);
            String report = getRelevantFailures(classes);
            List<JavaClass> result = new ArrayList<>();
            for (JavaClass clazz : classes) {
                if (report.contains(clazz.getName())) {
                    result.add(clazz);
                }
            }
            return result;
        }

        private String getRelevantFailures(JavaClasses classes) {
            List<String> relevant = new ArrayList<>();
            for (String line : linesIn(classesShould.evaluate(classes).getFailureReport())) {
                if (!isDefaultConstructor(line)) {
                    relevant.add(line);
                }
            }
            return Joiner.on(" ").join(relevant);
        }

        private boolean isDefaultConstructor(String line) {
            return line.contains(Object.class.getName());
        }

        private List<String> linesIn(FailureReport failureReport) {
            List<String> result = new ArrayList<>();
            for (String details : failureReport.getDetails()) {
                result.addAll(Splitter.on(System.lineSeparator()).splitToList(details));
            }
            return result;
        }
    }

    private JavaClasses importClasses(Class<?>... classes) {
        try {
            ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);
            return new ClassFileImporter().importClasses(ImmutableSet.copyOf(classes));
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private static class ClassAccessingList {
        void call(List list) {
            list.size();
        }
    }

    private static class ClassAccessingString {
        void call() {
            "string".length();
        }
    }

    private static class ClassAccessingCollection {
        void call(Collection collection) {
            collection.size();
        }
    }

    private static class ClassAccessingIterable {
        void call(Iterable iterable) {
            iterable.iterator();
        }
    }

    private static class ClassAccessingConstructor {
        void call(Constructor<?> constructor) {
            constructor.getModifiers();
        }
    }

    private static class ClassAccessingSimpleClass {
        void call() {
            new SimpleClass();
        }
    }

    private static class ClassAccessingPrivateClass {
        void call() {
            new PrivateClass();
        }
    }

    private static class ClassAccessingPackagePrivateClass {
        void call() {
            new PackagePrivateClass();
        }
    }

    private static class ClassAccessingProtectedClass {
        void call() {
            new ProtectedClass();
        }
    }

    private static class ClassAccessingPublicClass {
        void call() {
            new PublicClass();
        }
    }

    private static class ClassAccessingAnnotatedClass {
        void call() {
            new AnnotatedClass();
        }
    }

    private static class SimpleClass {
    }

    private static class PrivateClass {
    }

    static class PackagePrivateClass {
    }

    protected static class ProtectedClass {
    }

    public static class PublicClass {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
    }
}