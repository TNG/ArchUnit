package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.core.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.properties.HasType.Functions.GET_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class GivenClassesThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ArchCondition<JavaClass> condition;

    @Captor
    private ArgumentCaptor<JavaClass> classesCaptor;

    @Test
    public void areNamed() {
        List<JavaClass> classes = filterResultOf(classes().that().areNamed(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void areNotNamed() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotNamed(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaClass> classes = filterResultOf(classes().that().dontHaveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaClass> classes = filterResultOf(classes().that().haveNameMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaClass> classes = filterResultOf(classes().that().haveNameNotMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void resideInAPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideInAPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(getClass());
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideOutsideOfPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(getClass(), String.class, Constructor.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), Constructor.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaClass> classes = filterResultOf(classes().that()
                .resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(getClass(), String.class, Constructor.class);

        assertThatClasses(classes).matchInAnyOrder(String.class);
    }

    @Test
    public void arePublic() {
        List<JavaClass> classes = filterResultOf(classes().that().arePublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass());
    }

    @Test
    public void areNotPublic() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaClass> classes = filterResultOf(classes().that().areProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ProtectedClass.class);
    }

    @Test
    public void areNotProtected() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().arePackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PackagePrivateClass.class);
    }

    @Test
    public void areNotPackagePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().arePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class);
    }

    @Test
    public void areNotPrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void haveModifiers() {
        List<JavaClass> classes = filterResultOf(classes().that().haveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class);
    }

    @Test
    public void dontHaveModifiers() {
        List<JavaClass> classes = filterResultOf(classes().that().dontHaveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    @Test
    public void implement_type() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void dontImplement_type() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void implement_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void dontImplement_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void implement_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void dontImplement_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    private DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private Evaluator filterResultOf(GivenClassesConjunction givenClasses) {
        return new Evaluator(givenClasses);
    }

    private class Evaluator {
        private final GivenClassesConjunction givenClasses;

        Evaluator(GivenClassesConjunction givenClasses) {
            this.givenClasses = givenClasses;
        }

        public List<JavaClass> on(Class<?>... toCheck) {
            JavaClasses classes = javaClassesViaReflection(toCheck);
            givenClasses.should(condition).check(classes);

            verify(condition, atLeastOnce()).check(classesCaptor.capture(), any(ConditionEvents.class));

            return classesCaptor.getAllValues();
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

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
    }
}