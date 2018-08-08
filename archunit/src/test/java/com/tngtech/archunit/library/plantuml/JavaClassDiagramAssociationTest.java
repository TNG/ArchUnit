package com.tngtech.archunit.library.plantuml;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.diagramtests.confusingpackagenames.foopackage.barpackage.ClassInFooAndBarPackage;
import com.tngtech.archunit.library.diagramtests.simpledependency.origin.SomeOriginClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaClassDiagramAssociationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void get_package_identifier_associated_with_class() {
        String expectedPackageIdentifier = SomeOriginClass.class.getPackage().getName().replaceAll(".*\\.", "..");
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes(expectedPackageIdentifier)
                .component("B").withStereoTypes("..noclasshere")
                .write());

        JavaClass clazz = importClassWithContext(SomeOriginClass.class);

        assertThat(javaClassDiagramAssociation.getPackageIdentifiersFromComponentOf(clazz))
                .as("package identifiers of " + clazz.getName())
                .containsOnly(expectedPackageIdentifier);
    }

    @Test
    public void get_target_package_identifiers_of_class() {
        String expectedTarget1 = "..target1";
        String expectedTarget2 = "..target2";
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes(SomeOriginClass.class.getPackage().getName().replaceAll(".*\\.", ".."))
                .component("B").withStereoTypes(expectedTarget1)
                .component("C").withStereoTypes(expectedTarget2)
                .dependencyFrom("[A]").to("[B]")
                .dependencyFrom("[A]").to("[C]")
                .write());

        JavaClass clazz = importClassWithContext(SomeOriginClass.class);

        assertThat(javaClassDiagramAssociation.getTargetPackageIdentifiers(clazz))
                .as("package identifiers of " + clazz.getName())
                .containsOnly(expectedTarget1, expectedTarget2);
    }

    @Test
    public void rejects_class_not_contained_in_any_component() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("SomeComponent").withStereoTypes("..someStereotype.")
                .write());
        JavaClass classNotContained = importClassWithContext(Object.class);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(String.format("Class %s is not contained in any component", Object.class.getName()));

        javaClassDiagramAssociation.getTargetPackageIdentifiers(classNotContained);
    }

    @Test
    public void reports_if_class_is_contained_in_any_component() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("Object").withStereoTypes(Object.class.getPackage().getName())
                .write());

        assertThat(javaClassDiagramAssociation.contains(importClassWithContext(Object.class)))
                .as("association contains " + Object.class.getName()).isTrue();
        assertThat(javaClassDiagramAssociation.contains(importClassWithContext(File.class)))
                .as("association contains " + File.class.getName()).isFalse();
    }

    @Test
    public void class_resides_in_multiple_packages() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes("..foopackage..")
                .component("B").withStereoTypes("..barpackage")
                .write());
        JavaClass classContainedInTwoComponents = importClassWithContext(ClassInFooAndBarPackage.class);

        thrown.expect(ComponentIntersectionException.class);
        thrown.expectMessage(String.format(
                "Class %s may not be contained in more than one component, but is contained in [A, B]",
                ClassInFooAndBarPackage.class.getName()));

        javaClassDiagramAssociation.getTargetPackageIdentifiers(classContainedInTwoComponents);
    }

    @Test
    public void rejects_duplicate_stereotype() {
        File file = TestDiagram.in(temporaryFolder)
                .component("first").withStereoTypes("..identical..")
                .component("second").withStereoTypes("..identical..")
                .write();

        thrown.expect(IllegalDiagramException.class);
        thrown.expectMessage("Stereotype '..identical..' should be unique");

        createAssociation(file);
    }

    private JavaClassDiagramAssociation createAssociation(File file) {
        PlantUmlDiagram diagram = new PlantUmlParser().parse(toUrl(file));
        return new JavaClassDiagramAssociation(diagram);
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}