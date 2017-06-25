package com.tngtech.archunit.visual;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class JsonElementTest {

    @DataProvider
    public static Object[][] elements_with_name() {
        return $$(
                $(new JsonJavaInterface("class1", "com.tngtech.pkg.class1")),
                $(new JsonJavaClass("class1", "com.tngtech.pkg.class1", "", "")),
                $(new JsonJavaPackage("class1", "com.tngtech.pkg.class1"))
        );
    }

    @Test
    @UseDataProvider("elements_with_name")
    public void testGetPathOf(JsonElement element) {
        assertThat(element.getPath()).as("path of element").isEqualTo("com.tngtech.pkg");
    }

    @Test
    public void testGetChild() {
        // FIXME: Already put this in JsonJavaPackageTest, test this for JsonJavaClass and JsonJavaInterface, too??
        // FIXME: Don't test abstract classes, since it's unrealistic. Either use DataProvider on all subclasses
        //        satisfying the assumption, or test individually (which I'd suggest for this test here)
        final String pkg = "com.tngtech.pkg";
        final String class1 = "com.tngtech.pkg.class1";
        final String subpkg = "com.tngtech.pkg.subpkg";
        final String notexistingClass = "com.tngtech.pkg.notexistingpkg";

        JsonElement element = new JsonElement("pkg", pkg, "package") {
            @Override
            Set<? extends JsonElement> getChildren() {
                return new HashSet<>(Arrays.asList(
                        new JsonJavaPackage("subpkg", subpkg),
                        new JsonJavaClass("class1", class1, "class", "")));
            }

            @Override
            void insertJavaElement(JsonJavaElement el) {
            }
        };
        assertTrue("getting child-class not working", hasFullName(element.getChild(class1), class1));

        assertTrue("getting child-package not working", hasFullName(element.getChild(subpkg), subpkg));

        assertTrue("getting own package not working", hasFullName(element.getChild(pkg), pkg));

        assertFalse("getting not existing child is not working",
                hasFullName(element.getChild(notexistingClass), notexistingClass));
    }

    private static boolean hasFullName(Optional<? extends JsonElement> act, String expectedFullName) {
        return act.isPresent() && act.get().fullName.equals(expectedFullName);
    }
}