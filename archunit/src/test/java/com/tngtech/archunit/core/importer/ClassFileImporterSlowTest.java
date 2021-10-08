package com.tngtech.archunit.core.importer;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.testutil.ContextClassLoaderRule;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import com.tngtech.archunit.testutil.TransientCopyRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.jarFileOf;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.importer.UrlSourceTest.JAVA_CLASS_PATH_PROP;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.TestUtils.urlOf;
import static java.util.jar.Attributes.Name.CLASS_PATH;

@Category(Slow.class)
public class ClassFileImporterSlowTest {
    @Rule
    public final TransientCopyRule copyRule = new TransientCopyRule();
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();
    @Rule
    public final ContextClassLoaderRule contextClassLoaderRule = new ContextClassLoaderRule();

    @Test
    public void imports_the_classpath() {
        JavaClasses classes = new ClassFileImporter().importClasspath();

        assertThatTypes(classes).contain(ClassFileImporter.class, getClass());
        assertThatTypes(classes).doNotContain(Rule.class); // Default does not import jars
        assertThatTypes(classes).doNotContain(File.class); // Default does not import JDK classes

        classes = new ClassFileImporter().importClasspath(new ImportOptions().with(importJavaBaseOrRtAndJUnitJarAndFilesOnTheClasspath()));

        assertThatTypes(classes).contain(ClassFileImporter.class, getClass(), Rule.class, File.class);
    }

    @Test
    public void respects_ImportOptions_when_using_the_default_importClasspath_method() {
        JavaClasses classes = new ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importClasspath();

        assertThatTypes(classes).contain(ClassFileImporter.class);
        assertThatTypes(classes).doNotContain(getClass(), Rule.class, String.class);
    }

    @Test
    public void imports_packages() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                getClass().getPackage().getName(), Rule.class.getPackage().getName());
        assertThatTypes(classes).contain(ImmutableSet.of(getClass(), Rule.class));

        classes = new ClassFileImporter().importPackages(
                ImmutableSet.of(getClass().getPackage().getName(), Rule.class.getPackage().getName()));
        assertThatTypes(classes).contain(ImmutableSet.of(getClass(), Rule.class));
    }

    @Test
    public void imports_packages_of_classes() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(getClass(), Rule.class);
        assertThatTypes(classes).contain(ImmutableSet.of(getClass(), Rule.class));

        classes = new ClassFileImporter().importPackagesOf(ImmutableSet.of(getClass(), Rule.class));
        assertThatTypes(classes).contain(ImmutableSet.of(getClass(), Rule.class));
    }

    @Test
    public void imports_jars() throws Exception {
        JavaClasses classes = new ClassFileImporter().importJar(jarFileOf(Rule.class));
        assertThatTypes(classes).contain(Rule.class);
        assertThatTypes(classes).doNotContain(Object.class, ImmutableList.class);

        classes = new ClassFileImporter().importJars(jarFileOf(Rule.class), jarFileOf(ImmutableList.class));
        assertThatTypes(classes).contain(Rule.class, ImmutableList.class);
        assertThatTypes(classes).doNotContain(Object.class);

        classes = new ClassFileImporter().importJars(ImmutableList.of(
                jarFileOf(Rule.class), jarFileOf(ImmutableList.class)));
        assertThatTypes(classes).contain(Rule.class, ImmutableList.class);
        assertThatTypes(classes).doNotContain(Object.class);
    }

    @Test
    public void imports_duplicate_classes() {
        String existingClass = urlOf(JavaClass.class).getFile();
        copyRule.copy(
                new File(existingClass),
                new File(getClass().getResource(".").getFile()));

        JavaClasses classes = new ClassFileImporter().importPackages(getClass().getPackage().getName());

        assertThatType(classes.get(JavaClass.class)).isNotNull();
    }

    @Test
    public void imports_classes_from_classpath_specified_in_manifest_file() {
        TestClassFile testClassFile = new TestClassFile().create();
        String manifestClasspath = testClassFile.getClasspathRoot().getAbsolutePath();
        String jarPath = new TestJarFile()
                .withManifestAttribute(CLASS_PATH, manifestClasspath)
                .create()
                .getName();

        verifyCantLoadWithCurrentClasspath(testClassFile);
        System.setProperty(JAVA_CLASS_PATH_PROP, jarPath);

        JavaClasses javaClasses = new ClassFileImporter().importPackages(testClassFile.getPackageName());

        assertThat(javaClasses).extracting("name").contains(testClassFile.getClassName());
    }

    private void verifyCantLoadWithCurrentClasspath(TestClassFile testClassFile) {
        try {
            new ClassFileImporter().importPackages(testClassFile.getPackageName()).get(testClassFile.getClassName());
            Assert.fail(String.format("Should not have been able to load class %s with the current classpath", testClassFile.getClassName()));
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void creates_JavaPackages() {
        JavaClasses javaClasses = importJavaBase();

        JavaPackage defaultPackage = javaClasses.getDefaultPackage();

        assertThat(defaultPackage.containsPackage("java"))
                .as("Created default package contains 'java'").isTrue();

        JavaPackage javaPackage = defaultPackage.getPackage("java.lang");
        assertThatTypes(javaPackage.getClasses()).contain(Object.class, String.class, Integer.class);
        assertThatTypes(javaPackage.getAllClasses()).contain(Object.class, Annotation.class, Field.class);

        assertThat(javaClasses.containPackage("java.util"))
                .as("Classes contain package 'java.util'").isTrue();
        assertThatTypes(javaClasses.getPackage("java.util").getClasses()).contain(List.class);
    }

    private JavaClasses importJavaBase() {
        return new ClassFileImporter().importClasspath(new ImportOptions().with(new ImportOption() {
            @Override
            public boolean includes(Location location) {
                return
                        // before Java 9 package like java.lang were in rt.jar
                        location.contains("rt.jar") ||
                                // from Java 9 on those packages were in a JRT with name 'java.base'
                                (location.asURI().getScheme().equals("jrt") && location.contains("java.base"));
            }
        }));
    }

    private ImportOption importJavaBaseOrRtAndJUnitJarAndFilesOnTheClasspath() {
        return new ImportOption() {
            @Override
            public boolean includes(Location location) {
                if (!location.isArchive()) {
                    return true;
                }
                if (location.isJar() && (location.contains("junit") || location.contains("/rt.jar"))) {
                    return true;
                }
                return location.asURI().getScheme().equals("jrt") && location.contains("java.base");
            }
        };
    }
}
