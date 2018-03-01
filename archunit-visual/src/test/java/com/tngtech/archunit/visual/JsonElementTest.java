package com.tngtech.archunit.visual;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.TestUtils;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.SomeInterface;
import com.tngtech.archunit.visual.testclasses.subpkg.SecondSubPkgClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import com.tngtech.archunit.visual.testclasses.subpkg.ThirdSubPkgClass;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JsonElementTest {
    @DataProvider
    public static Object[][] elements_with_name() {
        JavaClasses classes = TestUtils.importClasses(SomeInterface.class, SomeClass.class);
        return $$(
                $(new JsonJavaInterface(classes.get(SomeInterface.class)), SomeInterface.class.getPackage().getName()),
                $(new JsonJavaClass(classes.get(SomeClass.class), true), SomeClass.class.getPackage().getName()),
                $(new JsonJavaPackage("sub", "com.tngtech.pkg.sub"), "com.tngtech.pkg"));
    }

    @Test
    @UseDataProvider("elements_with_name")
    public void path_of_single_root(JsonElement element, String expectedPath) {
        assertThat(element.getPath()).as("path of element").isEqualTo(expectedPath);
    }

    @Test
    public void testGetPathOfRootPackage() {
        assertThat(new JsonJavaPackage("com", "com").getPath()).as("path of element").isEqualTo("default");
    }

    private static boolean hasFullName(Optional<? extends JsonElement> act, String expectedFullName) {
        return act.isPresent() && act.get().fullName.equals(expectedFullName);
    }
}