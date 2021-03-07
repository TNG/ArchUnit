package com.tngtech.archunit.htmlvisualization;

import java.io.Serializable;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Predicates.containsPattern;
import static com.tngtech.archunit.htmlvisualization.JsonSerializableAssertion.ExpectedJsonNode.anonymousClassNode;
import static com.tngtech.archunit.htmlvisualization.JsonSerializableAssertion.ExpectedJsonNode.classNode;
import static com.tngtech.archunit.htmlvisualization.JsonSerializableAssertion.ExpectedJsonNode.flattenedPackageNode;
import static com.tngtech.archunit.htmlvisualization.JsonSerializableAssertion.ExpectedJsonNode.relativePackageNode;
import static com.tngtech.archunit.htmlvisualization.JsonSerializableAssertion.assertThatJsonSerializable;

public class JsonGraphTest {

    @Rule
    public final ArchConfigurationRule rule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    @Test
    public void creates_json_serializable_structure_containing_simple_classes() {
        JsonGraph graph = JsonGraph.from(new ClassFileImporter().importPackages("com.tngtech.archunit.core"));

        JsonSerializable root = graph.toJsonSerializable().get("root");

        // @formatter:off
        assertThatJsonSerializable(root).isRoot().containsPath(
          flattenedPackageNode("com").containing(
            relativePackageNode("com.tngtech").containing(
              relativePackageNode("com.tngtech.archunit").containing(
                relativePackageNode("com.tngtech.archunit.core").containing(
                  relativePackageNode("com.tngtech.archunit.core.importer").containing(
                    classNode(ClassFileImporter.class)
                  ),
                  relativePackageNode("com.tngtech.archunit.core.domain").containing(
                    classNode(JavaClass.class)
                  )
                )
              )
            )
          ));
        // @formatter:on
    }

    @Test
    public void creates_json_serializable_structure_containing_nested_classes() {
        JsonGraph graph = JsonGraph.from(new ClassFileImporter().importPackagesOf(getClass()));

        JsonSerializable root = graph.toJsonSerializable().get("root");

        // @formatter:off
        assertThatJsonSerializable(root).isRoot().containsPath(
          flattenedPackageNode("com").containing(
            relativePackageNode("com.tngtech").containing(
              relativePackageNode("com.tngtech.archunit").containing(
                relativePackageNode("com.tngtech.archunit.htmlvisualization").containing(
                  classNode(getClass()).containing(
                    classNode(NestedClass.class).containing(
                      classNode(NestedClass.EvenMoreNestedClass.class).containing(
                        classNode(NestedClass.EvenMoreNestedClass.InterfaceNestedDeeplyInClasses.class),
                        classNode(NestedClass.EvenMoreNestedClass.ClassNestedDeeplyInClasses.class)
                      )
                    ),
                    classNode(NestedInterface.class).containing(
                      classNode(NestedInterface.EvenMoreNestedInterface.class).containing(
                        classNode(NestedInterface.EvenMoreNestedInterface.ClassNestedDeeplyInInterfaces.class)
                      )
                    )
                  )
                )
              )
            )
          ));
        // @formatter:on
    }

    @Test
    public void creates_json_serializable_structure_containing_array_dependency_component_types() {
        class SomeLocalClass {
        }
        @SuppressWarnings("unused")
        class DependsOnArray {
            String[] arrayType;
            SomeLocalClass[][] localArrayType;
        }

        JsonGraph graph = JsonGraph.from(new ClassFileImporter().importClasses(DependsOnArray.class, SomeLocalClass.class));

        JsonSerializable root = graph.toJsonSerializable().get("root");

        // @formatter:off
        assertThatJsonSerializable(root).isRoot()
          .containsPath(
            flattenedPackageNode("com.tngtech.archunit.htmlvisualization").containing(
              classNode(getClass()).containing(
                classNode(DependsOnArray.class),
                classNode(SomeLocalClass.class)
              )
            ),
            flattenedPackageNode("java.lang").containing(
              classNode(String.class)
            )
          )
          .doesNotContainNodeWithNameThat(containsPattern("\\[\\]"));
        // @formatter:on
    }

    @Test
    public void creates_json_serializable_structure_containing_anonymous_classes() throws ClassNotFoundException {
        @SuppressWarnings("unused")
        class DependsOnAnonymousClass {
            {
                new Serializable() {
                };
            }
        }

        JsonGraph graph = JsonGraph.from(new ClassFileImporter().importClasses(
                DependsOnAnonymousClass.class, Serializable.class,
                Class.forName("com.tngtech.archunit.htmlvisualization.JsonGraphTest$1DependsOnAnonymousClass$1")
        ));

        JsonSerializable root = graph.toJsonSerializable().get("root");

        // @formatter:off
        assertThatJsonSerializable(root).isRoot()
          .containsPath(
            flattenedPackageNode("com.tngtech.archunit.htmlvisualization").containing(
              classNode(getClass()).containing(
                classNode(DependsOnAnonymousClass.class).containing(
                  anonymousClassNode(DependsOnAnonymousClass.class, 1)
                )
              )
            ),
            flattenedPackageNode("java").containing(
              relativePackageNode("java.io").containing(
                classNode(Serializable.class)
              )
            )
          )
          .doesNotContainNodeWithNameThat(containsPattern("\\[\\]"));
        // @formatter:on
    }

    private static class NestedClass {
        static class EvenMoreNestedClass {
            interface InterfaceNestedDeeplyInClasses {
            }

            static class ClassNestedDeeplyInClasses {
            }
        }
    }

    private interface NestedInterface {
        interface EvenMoreNestedInterface {
            class ClassNestedDeeplyInInterfaces {
            }
        }
    }
}
