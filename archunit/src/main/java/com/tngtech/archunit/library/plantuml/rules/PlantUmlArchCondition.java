/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.plantuml.rules;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.domain.PackageMatchers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesInAnyPackage;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;

/**
 * Allows to evaluate <a href="http://plantuml.com/component-diagram">PlantUML Component Diagrams</a>
 * as ArchUnit rules.
 * <br><br>
 * The general syntax to use is
 * <br><br>
 * <pre><code>
 * classes().should(adhereToPlantUmlDiagram(someDiagramUrl, consideringAllDependencies()));
 * </code></pre>
 * The supported diagram syntax uses component diagram stereotypes to associate package patterns
 * (compare {@link PackageMatcher}) with components. An example could look like
 * <pre><code>
 * [Some Source] &lt;&lt;..some.source..&gt;&gt;
 * [Some Target] &lt;&lt;..some.target..&gt;&gt;
 *
 * [Some Source] --&gt; [Some Target]
 * </code></pre>
 * Applying such a diagram as an ArchUnit rule would demand dependencies only from <code>..some.source..</code>
 * to <code>..some.target..</code>, but forbid them vice versa.<br>
 * There are various factory method for different input formats (file, url, ...), compare
 * <ul>
 *     <li>{@link #adhereToPlantUmlDiagram(URL, Configuration)}</li>
 *     <li>{@link #adhereToPlantUmlDiagram(File, Configuration)}</li>
 *     <li>{@link #adhereToPlantUmlDiagram(Path, Configuration)}</li>
 *     <li>{@link #adhereToPlantUmlDiagram(String, Configuration)}</li>
 * </ul>
 * Which dependencies should be considered by the rule can be configured via {@link Configuration}.
 * Candidates are
 * <ul>
 *     <li>{@link Configuration#consideringAllDependencies()}</li>
 *     <li>{@link Configuration#consideringOnlyDependenciesInDiagram()}</li>
 *     <li>{@link Configuration#consideringOnlyDependenciesInAnyPackage(String, String...)}</li>
 * </ul>
 * <br>
 * A PlantUML diagram used with ArchUnit must abide by a certain set of rules:
 * <ol>
 *     <li>Components must have a name</li>
 *     <li>Components must have at least one stereotype. Each stereotype in the diagram must be unique</li>
 *     <li>Components may have an optional alias</li>
 *     <li>Components may have an optional color</li>
 *     <li>Components must be defined before declaring dependencies</li>
 *     <li>Dependencies must use arrows only consisting of dashes, pointing right, e.g. <code>--&gt;</code></li>
 * </ol>
 */
@PublicAPI(usage = ACCESS)
public final class PlantUmlArchCondition extends ArchCondition<JavaClass> {
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

    /**
     * Ignores all {@link Dependency dependencies} that have an {@link Dependency#getOriginClass() origin class}
     * matching the supplied {@link DescribedPredicate ignorePredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaClass} can be found within {@link JavaClass.Predicates} or one of the respective ancestors like {@link HasName.Predicates}.
     */
    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependenciesWithOrigin(DescribedPredicate<? super JavaClass> ignorePredicate) {
        return ignoreDependencies(GET_ORIGIN_CLASS.is(ignorePredicate)
                .as("ignoring dependencies with origin " + ignorePredicate.getDescription()));
    }

    /**
     * Ignores all {@link Dependency dependencies} that have an {@link Dependency#getTargetClass() target class}
     * matching the supplied {@link DescribedPredicate ignorePredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaClass} can be found within {@link JavaClass.Predicates} or one of the respective ancestors like {@link HasName.Predicates}.
     */
    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependenciesWithTarget(DescribedPredicate<? super JavaClass> ignorePredicate) {
        return ignoreDependencies(GET_TARGET_CLASS.is(ignorePredicate)
                .as("ignoring dependencies with target " + ignorePredicate.getDescription()));
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(Class<?> origin, Class<?> target) {
        return ignoreDependencies(origin.getName(), target.getName());
    }

    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(String origin, String target) {
        return ignoreDependencies(
                GET_ORIGIN_CLASS.is(name(origin)).and(GET_TARGET_CLASS.is(name(target)))
                        .as("ignoring dependencies from %s to %s", origin, target));
    }

    /**
     * Ignores all {@link Dependency dependencies} matching the supplied {@link DescribedPredicate ignorePredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link Dependency} can be found within {@link Dependency.Predicates}.
     */
    @PublicAPI(usage = ACCESS)
    public PlantUmlArchCondition ignoreDependencies(DescribedPredicate<? super Dependency> ignorePredicate) {
        String description = getDescription() + ", " + ignorePredicate.getDescription();
        return new PlantUmlArchCondition(description,
                this.ignorePredicate.or(ignorePredicate),
                javaClassDiagramAssociation);
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        if (allDependenciesAreIgnored(javaClass)) {
            return;
        }

        if (!javaClassDiagramAssociation.contains(javaClass)) {
            events.add(violated(javaClass, String.format("Class %s is not contained in any component", javaClass.getName())));
            return;
        }

        Set<PlantUmlComponent> components = javaClassDiagramAssociation.getAssociatedComponents(javaClass);
        if (components.size() > 1) {
            events.add(violated(javaClass, String.format(
                    "Class %s may not be contained in more than one component, but is contained in [%s]",
                    javaClass.getName(), joinSortedNames(components))));
            return;
        }

        checkDependencies(javaClass, events);
    }

    private static String joinSortedNames(Set<PlantUmlComponent> components) {
        return components.stream()
                .map(it -> it.getComponentName().asString())
                .sorted()
                .collect(joining(", "));
    }

    private void checkDependencies(JavaClass javaClass, ConditionEvents events) {
        String[] allAllowedTargets = Sets.union(
                javaClassDiagramAssociation.getPackageIdentifiersFromComponentOf(javaClass),
                javaClassDiagramAssociation.getTargetPackageIdentifiers(javaClass)
        ).toArray(new String[0]);

        ArchCondition<JavaClass> delegate = onlyHaveDependenciesInAnyPackage(allAllowedTargets)
                .ignoreDependency(ignorePredicate);

        delegate.check(javaClass, events);
    }

    private boolean allDependenciesAreIgnored(JavaClass item) {
        return item.getDirectDependenciesFromSelf().stream().allMatch(ignorePredicate);
    }

    /**
     * @see PlantUmlArchCondition
     */
    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(URL url, Configuration configuration) {
        return create(url, configuration);
    }

    /**
     * @see PlantUmlArchCondition
     */
    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(String fileName, Configuration configuration) {
        return create(toUrl(Paths.get(fileName)), configuration);
    }

    /**
     * @see PlantUmlArchCondition
     */
    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(Path path, Configuration configuration) {
        return create(toUrl(path), configuration);
    }

    /**
     * @see PlantUmlArchCondition
     */
    @PublicAPI(usage = ACCESS)
    public static PlantUmlArchCondition adhereToPlantUmlDiagram(File file, Configuration configuration) {
        return create(toUrl(file.toPath()), configuration);
    }

    private static PlantUmlArchCondition create(URL url, Configuration configuration) {
        PlantUmlDiagram diagram = new PlantUmlParser().parse(url);
        checkState(!diagram.getAllComponents().isEmpty(), "No components defined in diagram <%s>", url);

        JavaClassDiagramAssociation javaClassDiagramAssociation = new JavaClassDiagramAssociation(diagram);
        DescribedPredicate<Dependency> ignorePredicate = configuration.asIgnorePredicate(javaClassDiagramAssociation);
        return new PlantUmlArchCondition(getDescription(url, ignorePredicate.getDescription()), ignorePredicate, javaClassDiagramAssociation);
    }

    private static String getDescription(URL plantUmlUrl, String ignoreDescription) {
        return String.format("adhere to PlantUML diagram <%s>%s", getFileNameOf(plantUmlUrl), ignoreDescription);
    }

    private static String getFileNameOf(URL url) {
        return new File(url.getFile()).getName();
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new PlantUmlParseException(e);
        }
    }

    /**
     * Used to specify which dependencies should be checked by the condition. Possible options are:
     * <ul>
     *     <li>{@link #consideringAllDependencies()}</li>
     *     <li>{@link #consideringOnlyDependenciesInDiagram()}</li>
     *     <li>{@link #consideringOnlyDependenciesInAnyPackage(String, String...)}</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public static final class Configuration {
        private final Function<JavaClassDiagramAssociation, DescribedPredicate<Dependency>> createIgnorePredicate;

        private Configuration(Function<JavaClassDiagramAssociation, DescribedPredicate<Dependency>> createIgnorePredicate) {
            this.createIgnorePredicate = createIgnorePredicate;
        }

        DescribedPredicate<Dependency> asIgnorePredicate(JavaClassDiagramAssociation javaClassDiagramAssociation) {
            return createIgnorePredicate.apply(javaClassDiagramAssociation);
        }

        /**
         * Considers all dependencies of every imported class, including basic Java classes like {@link Object}
         */
        @PublicAPI(usage = ACCESS)
        public static Configuration consideringAllDependencies() {
            return new Configuration(javaClassDiagramAssociation -> DescribedPredicate.<Dependency>alwaysFalse().as(""));
        }

        /**
         * Considers only dependencies of the imported classes that are contained within diagram components.
         * This makes it easy to ignore dependencies to irrelevant classes like {@link Object}, but bears the
         * danger of missing dependencies to components that have simply been forgotten to be added to the diagram.
         */
        @PublicAPI(usage = ACCESS)
        public static Configuration consideringOnlyDependenciesInDiagram() {
            return new Configuration(NotContainedInDiagramPredicate::new);
        }

        /**
         * Considers only dependencies of the imported classes that have targets in the package identifiers.
         * This can for example be used to limit checked dependencies to those contained in the own project,
         * e.g. '<code>com.myapp..</code>'.
         */
        @PublicAPI(usage = ACCESS)
        public static Configuration consideringOnlyDependenciesInAnyPackage(String packageIdentifier, String... furtherPackageIdentifiers) {
            List<String> packageIdentifiers = FluentIterable.from(singleton(packageIdentifier))
                    .append(furtherPackageIdentifiers)
                    .toList();

            return new Configuration(javaClassDiagramAssociation -> new NotContainedInPackagesPredicate(packageIdentifiers));
        }

        private static class NotContainedInDiagramPredicate extends DescribedPredicate<Dependency> {
            private final JavaClassDiagramAssociation javaClassDiagramAssociation;

            NotContainedInDiagramPredicate(JavaClassDiagramAssociation javaClassDiagramAssociation) {
                super(" while ignoring dependencies not contained in the diagram");
                this.javaClassDiagramAssociation = javaClassDiagramAssociation;
            }

            @Override
            public boolean test(Dependency input) {
                return !javaClassDiagramAssociation.contains(input.getTargetClass());
            }
        }

        private static class NotContainedInPackagesPredicate extends DescribedPredicate<Dependency> {
            private final List<String> packageIdentifiers;

            NotContainedInPackagesPredicate(List<String> packageIdentifiers) {
                super(" while ignoring dependencies outside of packages [%s]", joinSingleQuoted(packageIdentifiers));
                this.packageIdentifiers = packageIdentifiers;
            }

            @Override
            public boolean test(Dependency input) {
                return !PackageMatchers.of(packageIdentifiers).test(input.getTargetClass().getPackageName());
            }
        }
    }
}
