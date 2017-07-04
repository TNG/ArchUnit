package com.tngtech.archunit.visual;

import com.tngtech.archunit.base.Optional;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JsonElementTest {

    @DataProvider
    public static Object[][] elements_with_name() {
        try {
            Constructor<JsonJavaInterface> jsonJavaInterfaceConstructor = JsonJavaInterface.class.getDeclaredConstructor(String.class, String.class);
            jsonJavaInterfaceConstructor.setAccessible(true);
            Constructor<JsonJavaClass> jsonJavaClassConstructor = JsonJavaClass.class.getDeclaredConstructor(String.class, String.class);
            jsonJavaClassConstructor.setAccessible(true);
            return $$(
                    $(jsonJavaInterfaceConstructor.newInstance("class1", "com.tngtech.pkg.class1")),
                    $(jsonJavaClassConstructor.newInstance("class1", "com.tngtech.pkg.class1")),
                    $(new JsonJavaPackage("class1", "com.tngtech.pkg.class1"))
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @DataProvider
    public static Object[][] root_package() {
        return $$(
                $(new JsonJavaPackage("com", "com"))
        );
    }

    @Test
    @UseDataProvider("elements_with_name")
    public void testGetPathOf(JsonElement element) {
        assertThat(element.getPath()).as("path of element").isEqualTo("com.tngtech.pkg");
    }

    @Test
    @UseDataProvider("root_package")
    public void testGetPathOfRootPackage(JsonElement element) {
        assertThat(element.getPath()).as("path of element").isEqualTo("default");
    }

    private static boolean hasFullName(Optional<? extends JsonElement> act, String expectedFullName) {
        return act.isPresent() && act.get().fullName.equals(expectedFullName);
    }
}