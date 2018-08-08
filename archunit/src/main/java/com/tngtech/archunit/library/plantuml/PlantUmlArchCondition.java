/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.plantuml;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatchers;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesInAnyPackage;
import static java.util.Collections.singleton;

public class PlantUmlArchCondition extends ArchCondition<JavaClass> {
    private final DescribedPredicate<Dependency> ignorePredicate;
    private final JavaClassDiagramAssociation javaClassDiagramAssociation;

    private PlantUmlArchCondition(
            String description,
            DescribedPredicate<Dependency> ignorePredicate,
            JavaClassDiagramAssociation javaClassDiagramAssociation) {

        super(description);
        this.ignorePredicate = ignorePredicate;
        this.javaClassDiagramAssociation = javaClassDiagramAssociation;
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependenciesWithOrigin(DescribedPredicate<JavaClass> ignorePredicate) {
        return ignoreDependencies(GET_ORIGIN_CLASS.is(ignorePredicate)
                .as("ignoring dependencies with origin " + ignorePredicate.getDescription()));
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependenciesWithTarget(DescribedPredicate<JavaClass> ignorePredicate) {
        return ignoreDependencies(GET_TARGET_CLASS.is(ignorePredicate)
                .as("ignoring dependencies with target " + ignorePredicate.getDescription()));
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(final Class<?> from, final Class<?> to) {
        return ignoreDependencies(from.getName(), to.getName());
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(final String from, final String to) {
        return ignoreDependencies(
                GET_ORIGIN_CLASS.is(name(from)).and(GET_TARGET_CLASS.is(name(to)))
                        .as("ignoring dependencies from %s to %s", from, to));
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(DescribedPredicate<Dependency> ignorePredicate) {
        String description = getDescription() + ", " + ignorePredicate.getDescription();
        return new PlantUmlArchCondition(description,
                this.ignorePredicate.or(ignorePredicate),
                javaClassDiagramAssociation);
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        if (allDependenciesAreIgnored(item)) {
            return;
        }

        String[] allAllowedTargets = FluentIterable
                .from(javaClassDiagramAssociation.getPackageIdentifiersFromComponentOf(item))
                .append(javaClassDiagramAssociation.getTargetPackageIdentifiers(item))
                .toArray(String.class);

        ArchCondition<JavaClass> delegate = onlyHaveDependenciesInAnyPackage(allAllowedTargets)
                .ignoreDependency(ignorePredicate);

        delegate.check(item, events);
    }

    private boolean allDependenciesAreIgnored(JavaClass item) {
        return FluentIterable.from(item.getDirectDependenciesFromSelf()).allMatch(toGuava(ignorePredicate));
    }

    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(URL url, Configuration configuration) {
        return create(url, configuration);
    }

    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(String fileName, Configuration configuration) {
        return create(toUrl(Paths.get(fileName)), configuration);
    }

    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(Path path, Configuration configuration) {
        return create(toUrl(path), configuration);
    }

    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(File file, Configuration configuration) {
        return create(toUrl(file.toPath()), configuration);
    }

    private static PlantUmlArchCondition create(URL url, Configuration configuration) {
        PlantUmlDiagram diagram = new PlantUmlParser().parse(url);
        JavaClassDiagramAssociation javaClassDiagramAssociation = new JavaClassDiagramAssociation(diagram);
        DescribedPredicate<Dependency> ignorePredicate = configuration.asIgnorePredicate(javaClassDiagramAssociation);
        return new PlantUmlArchCondition(getDescription(url, ignorePredicate.getDescription()), ignorePredicate, javaClassDiagramAssociation);
    }

    private static String getDescription(URL plantUmlUrl, String ignoreDescription) {
        return String.format("adhere to PlantUML diagram <%s>%s", getFileNameOf(plantUmlUrl), ignoreDescription);
    }

    private static String getFileNameOf(URL url) {
        try {
            return new File(url.toURI()).getName();
        } catch (URISyntaxException e) {
            throw new PlantUmlParseException(e);
        }
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new PlantUmlParseException(e);
        }
    }

    public static final class Configurations {
        private Configurations() {
        }

        @PublicAPI(usage = ACCESS)
        public static Configuration consideringAllDependencies() {
            return new Configuration() {
                @Override
                public DescribedPredicate<Dependency> asIgnorePredicate(JavaClassDiagramAssociation javaClassDiagramAssociation) {
                    return DescribedPredicate.<Dependency>alwaysFalse().as("");
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static Configuration consideringOnlyDependenciesInDiagram() {
            return new Configuration() {
                @Override
                public DescribedPredicate<Dependency> asIgnorePredicate(final JavaClassDiagramAssociation javaClassDiagramAssociation) {
                    return new NotContainedInDiagramPredicate(javaClassDiagramAssociation);
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static Configuration consideringOnlyDependenciesInAnyPackage(String packageIdentifier, final String... furtherPackageIdentifiers) {
            final List<String> packageIdentifiers = FluentIterable.from(singleton(packageIdentifier))
                    .append(furtherPackageIdentifiers)
                    .toList();

            return new Configuration() {
                @Override
                public DescribedPredicate<Dependency> asIgnorePredicate(final JavaClassDiagramAssociation javaClassDiagramAssociation) {
                    return new NotContainedInPackagesPredicate(packageIdentifiers);
                }
            };
        }

        private static class NotContainedInDiagramPredicate extends DescribedPredicate<Dependency> {
            private final JavaClassDiagramAssociation javaClassDiagramAssociation;

            NotContainedInDiagramPredicate(JavaClassDiagramAssociation javaClassDiagramAssociation) {
                super(" while ignoring dependencies not contained in the diagram");
                this.javaClassDiagramAssociation = javaClassDiagramAssociation;
            }

            @Override
            public boolean apply(Dependency input) {
                return !javaClassDiagramAssociation.contains(input.getTargetClass());
            }
        }

        private static class NotContainedInPackagesPredicate extends DescribedPredicate<Dependency> {
            private final List<String> packageIdentifiers;

            NotContainedInPackagesPredicate(List<String> packageIdentifiers) {
                super(" while ignoring dependencies outside of packages ['%s']", Joiner.on("', '").join(packageIdentifiers));
                this.packageIdentifiers = packageIdentifiers;
            }

            @Override
            public boolean apply(Dependency input) {
                return !PackageMatchers.of(packageIdentifiers).apply(input.getTargetClass().getPackage());
            }
        }
    }

    interface Configuration {
        DescribedPredicate<Dependency> asIgnorePredicate(JavaClassDiagramAssociation javaClassDiagramAssociation);
    }
}