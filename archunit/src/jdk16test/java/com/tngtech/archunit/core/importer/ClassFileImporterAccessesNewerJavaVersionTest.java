package com.tngtech.archunit.core.importer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.ChildOverridesAllMethods;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.DeterminesMethodAnalogouslyToReflectionApi;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.ExpectedMethod;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.LeftAncestorPrecedesRightAncestor;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.OnlyDefinedInCommonAncestor;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.OnlyLeftAncestorOverridesRootMethod;
import com.tngtech.archunit.core.importer.testexamples.methodresolution.OnlyRightAncestorOverridesRootMethod;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterAccessesNewerJavaVersionTest {

    @DataProvider
    public static Object[][] method_resolution_scenarios() {
        return testForEach(
                OnlyDefinedInCommonAncestor.class,
                OnlyLeftAncestorOverridesRootMethod.class,
                OnlyRightAncestorOverridesRootMethod.class,
                LeftAncestorPrecedesRightAncestor.class,
                ChildOverridesAllMethods.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftLeftHasPrecedence.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftRightHasPrecedenceOverRight.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.RightLeftHasPrecedenceOverRightRight.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.RightRightIsPickedIfThereIsNoAlternative.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftOverriddenHasPrecedenceOverParents.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.RightOverriddenHasPrecedenceOverParents.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftLeftHasPrecedenceOverOverriddenRight.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftRightHasPrecedenceOverOverriddenRight.class,
                DeterminesMethodAnalogouslyToReflectionApi.EquivalentMethodsAreChosenDepthFirst.LeftOverriddenHasPrecedenceOverRightOverridden.class,
                DeterminesMethodAnalogouslyToReflectionApi.ClassHasPrecedenceOverInterface.ParentClassHasPrecedenceOverChildInterfaces.class,
                DeterminesMethodAnalogouslyToReflectionApi.ClassHasPrecedenceOverInterface.GrandParentClassHasPrecedenceOverChildInterfaces.class,
                DeterminesMethodAnalogouslyToReflectionApi.InterfaceOnParentHasPrecedenceOverInterfaceOnChild.LeftLeftOnGrandparentHasPrecedenceOverAllOthers.class,
                DeterminesMethodAnalogouslyToReflectionApi.InterfaceOnParentHasPrecedenceOverInterfaceOnChild.LeftRightOnGrandparentHasPrecedenceOverLeftOnParent.class,
                DeterminesMethodAnalogouslyToReflectionApi.InterfaceOnParentHasPrecedenceOverInterfaceOnChild.RightLeftOnGrandparentHasPrecedenceOverLeftAndRightOnParent.class,
                DeterminesMethodAnalogouslyToReflectionApi.InterfaceOnParentHasPrecedenceOverInterfaceOnChild.RightRightOnGrandparentHasPrecedenceOverLeftAndRightOnParent.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.RightWithMoreSpecificReturnOnParentTypeHasPrecedenceOverAllOthers.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.LeftWithMoreSpecificReturnTypeHasPrecedenceRightWithMoreSpecificReturnType.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.ParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllOthers.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.InterfaceWithMoreSpecificReturnTypeHasPrecedenceOverGrandParentClass.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.ParentInterfaceOnParentClassWithMoreSpecificReturnTypeHasPrecedenceOverGrandParentClass.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllOthers.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverFirstParentInterfaceWithMoreSpecificReturnType.class,
                DeterminesMethodAnalogouslyToReflectionApi.MoreSpecificReturnTypeHasPrecedence.GrandParentInterfaceWithMoreSpecificReturnTypeHasPrecedenceOverAllParentInterfacesWithMoreSpecificReturnType.class,
                DeterminesMethodAnalogouslyToReflectionApi.StaticMethodsInInterfacesAreIgnored.class
        );
    }

    @Test
    @UseDataProvider("method_resolution_scenarios")
    public void resolves_method_call_targets(Class<?> scenario) throws NoSuchMethodException {
        JavaMethod origin = new ClassFileImporter().importPackagesOf(scenario).get(scenario).getMethod("scenario");

        MethodCallTarget target = getOnlyElement(origin.getMethodCallsFromSelf()).getTarget();

        Method methodWithAnnotation = findMethodWithAnnotation(scenario, ExpectedMethod.class);
        // Sanity check that the expected method really is the one that would be found via Reflection, too
        assertThat(target.getOwner().reflect().getMethod(target.getName())).as("Method resolved via Reflection").isEqualTo(methodWithAnnotation);

        assertThat(target.resolveMember().get()).isEquivalentTo(methodWithAnnotation);
    }

    private static Method findMethodWithAnnotation(Class<?> scenario, Class<? extends Annotation> annotationType) {
        for (Class<?> nestedClass : scenario.getDeclaredClasses()) {
            for (Method method : nestedClass.getMethods()) {
                if (method.isAnnotationPresent(annotationType)) {
                    return method;
                }
            }
        }
        throw new IllegalStateException(String.format("No method of nested type in scenario %s is marked as @%s", scenario.getSimpleName(), annotationType.getSimpleName()));
    }
}
