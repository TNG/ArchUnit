package com.tngtech.archunit.visual;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonElementTest {

    @Test
    public void testGetPathOf() {
        JsonElement element = new JsonElement("class1", "com.tngtech.pkg.class1", "class") {
            @Override
            Set<? extends JsonElement> getChildren() {
                return null;
            }

            @Override
            void insertJavaElement(JsonJavaElement el) {
            }
        };
        assertTrue("path of class is wrong", element.getPath().equals("com.tngtech.pkg"));
    }

    @Test
    public void testGetChild() {
        final String pkg = "com.tngtech.pkg";
        final String class1 = "com.tngtech.pkg.class1";
        final String subpkg = "com.tngtech.pkg.subpkg";
        final String notexistingClass = "com.tngtech.pkg.notexistingpkg";

        JsonElement element = new JsonElement("pkg", pkg, "package") {
            @Override
            Set<? extends JsonElement> getChildren() {
                return new HashSet<>(Arrays.asList(
                        new JsonJavaPackage("subpkg", subpkg, false),
                        new JsonJavaClazz("class1", class1, "class", "")));
            }

            @Override
            void insertJavaElement(JsonJavaElement el) {
            }
        };
        assertTrue("getting child-class not working", hasFullname(element.getChild(class1), class1));

        assertTrue("getting child-package not working", hasFullname(element.getChild(subpkg), subpkg));

        assertTrue("getting own package not working", hasFullname(element.getChild(pkg), pkg));

        assertFalse("getting not existing child is not working",
                hasFullname(element.getChild(notexistingClass), notexistingClass));
    }

    private static boolean hasFullname(Optional<? extends JsonElement> act, String expectedFullname) {
        return act.isPresent() && act.get().fullname.equals(expectedFullname);
    }
}