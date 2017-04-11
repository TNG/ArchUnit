package com.tngtech.archunit.maventest;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.maventest", importOption = ArchUnitSmokeTest.NoTests.class)
public class ArchUnitSmokeTest {
    @ArchTest
    public static void runs_without_exception(JavaClasses classes) {
        int count = 0;
        for (JavaClass javaClass : classes) {
            count++;
        }
        assertEquals("Expected 2 classes", 2, count);

        assertEquals("Number of fields in ClassOne", classes.get(ClassOne.class).getFields().size(), 1);
        assertEquals("Number of methods in ClassOne", classes.get(ClassOne.class).getMethods().size(), 0);
        assertEquals("Number of fields in ClassTwo", classes.get(ClassTwo.class).getFields().size(), 0);
        assertEquals("Number of methods in ClassTwo", classes.get(ClassTwo.class).getMethods().size(), 1);
    }

    public static class NoTests implements ImportOption {
        public boolean includes(Location location) {
            return !location.asURI().toString().contains("test-classes");
        }
    }
}
