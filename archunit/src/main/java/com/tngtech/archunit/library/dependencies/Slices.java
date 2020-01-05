/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.PackageMatcher.TO_GROUPS;
import static com.tngtech.archunit.core.domain.Dependency.toTargetClasses;

/**
 * Basic collection of {@link Slice} for tests of dependencies between different domain packages, e.g. to avoid cycles.
 * Refer to {@link SlicesRuleDefinition} for further info on how to form an {@link ArchRule} to test slices.
 */
public final class Slices implements DescribedIterable<Slice>, CanOverrideDescription<Slices> {
    private final Iterable<Slice> slices;
    private final String description;

    private Slices(Iterable<Slice> slices) {
        this(slices, "Slices");
    }

    private Slices(Iterable<Slice> slices, String description) {
        this.slices = slices;
        this.description = description;
    }

    @Override
    public Iterator<Slice> iterator() {
        return slices.iterator();
    }

    @Override
    public Slices as(String description) {
        return new Slices(slices, description);
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Allows the naming of single slices, where back references to the matching pattern can be denoted by '$' followed
     * by capturing group number. <br>
     * E.g. {@code namingSlices("Slice $1")} would name a slice matching {@code '*..service.(*)..*'}
     * against {@code 'com.some.company.service.hello.something'} as 'Slice hello'.<br>
     * Likewise, if the slices were created by a {@link SliceAssignment} (compare
     * {@link #assignedFrom(SliceAssignment)}),
     * then the back reference refers to the n-th element of the identifier.
     *
     * @param pattern The naming pattern, e.g. 'Slice $1'
     * @return <b>New</b> (equivalent) slices with adjusted description for each single slice
     */
    @PublicAPI(usage = ACCESS)
    public Slices namingSlices(String pattern) {
        List<Slice> newSlices = new ArrayList<>();
        for (Slice slice : slices) {
            newSlices.add(slice.as(pattern));
        }
        return new Slices(newSlices, description);
    }

    /**
     * Supports partitioning a set of {@link JavaClasses} into different slices by matching the supplied
     * package identifier. For identifier syntax, see {@link PackageMatcher}.<br>
     * The slicing is done according to capturing groups (thus if none are contained in the identifier, no more than
     * a single slice will be the result). For example
     * <p>
     * Suppose there are three classes:<br><br>
     * {@code com.example.slice.one.SomeClass}<br>
     * {@code com.example.slice.one.AnotherClass}<br>
     * {@code com.example.slice.two.YetAnotherClass}<br><br>
     * If slices are created by specifying<br><br>
     * {@code Slices.of(classes).byMatching("..slice.(*)..")}<br><br>
     * then the result will be two slices, the slice where the capturing group is 'one' and the slice where the
     * capturing group is 'two'.
     * </p>
     *
     * @param packageIdentifier The identifier to match against
     * @return Slices partitioned according the supplied package identifier
     */
    @PublicAPI(usage = ACCESS)
    public static Transformer matching(String packageIdentifier) {
        PackageMatchingSliceIdentifier sliceIdentifier = new PackageMatchingSliceIdentifier(packageIdentifier);
        String description = "slices matching " + sliceIdentifier.getDescription();
        return new Transformer(sliceIdentifier, description);
    }

    /**
     * Supports partitioning a set of {@link JavaClasses} into different {@link Slices} by the supplied
     * {@link SliceAssignment}. This is basically a mapping {@link JavaClass} -&gt; {@link SliceIdentifier},
     * i.e. if the {@link SliceAssignment} returns the same
     * {@link SliceIdentifier} for two classes they will end up in the same slice.
     * A {@link JavaClass} will be ignored within the slices, if its {@link SliceIdentifier} is
     * {@link SliceIdentifier#ignore()}. For example
     * <p>
     * Suppose there are four classes:<br><br>
     * {@code com.somewhere.SomeClass}<br>
     * {@code com.somewhere.AnotherClass}<br>
     * {@code com.other.elsewhere.YetAnotherClass}<br>
     * {@code com.randomly.anywhere.AndYetAnotherClass}<br><br>
     * If slices are created by specifying<br><br>
     * {@code Slices.of(classes).assignedFrom(customAssignment)}<br><br>
     *
     * and the {@code customAssignment} maps<br><br>
     *
     * {@code com.somewhere -> SliceIdentifier.of("somewhere")}<br>
     * {@code com.other.elsewhere -> SliceIdentifier.of("elsewhere")}<br>
     * {@code com.randomly -> SliceIdentifier.ignore()}<br><br>
     * then the result will be two slices, identified by the single strings 'somewhere' (containing {@code SomeClass}
     * and {@code AnotherClass}) and 'elsewhere' (containing {@code YetAnotherClass}). The class {@code AndYetAnotherClass}
     * will be missing from all slices.
     *
     * @param sliceAssignment The assignment of {@link JavaClass} to {@link SliceIdentifier}
     * @return Slices partitioned according the supplied assignment
     */
    @PublicAPI(usage = ACCESS)
    public static Transformer assignedFrom(SliceAssignment sliceAssignment) {
        String description = "slices assigned from " + sliceAssignment.getDescription();
        return new Transformer(sliceAssignment, description);
    }

    /**
     * Specifies how to transform a set of {@link JavaClass} into a set of {@link Slice}, e.g. to test that
     * no cycles between certain package slices appear.
     *
     * @see Slices
     */
    public static class Transformer implements ClassesTransformer<Slice> {
        private final SliceAssignment sliceAssignment;
        private final String description;
        private final Optional<String> namingPattern;
        private final SlicesPredicateAggregator predicate;

        Transformer(SliceAssignment sliceAssignment, String description) {
            this(sliceAssignment, description, new SlicesPredicateAggregator("that"));
        }

        private Transformer(SliceAssignment sliceAssignment, String description, SlicesPredicateAggregator predicate) {
            this(sliceAssignment, description, Optional.<String>absent(), predicate);
        }

        private Transformer(SliceAssignment sliceAssignment,
                String description,
                Optional<String> namingPattern,
                SlicesPredicateAggregator predicate) {

            this.sliceAssignment = checkNotNull(sliceAssignment);
            this.description = checkNotNull(description);
            this.namingPattern = checkNotNull(namingPattern);
            this.predicate = predicate;
        }

        /**
         * @see Slices#namingSlices(String)
         */
        Transformer namingSlices(String pattern) {
            return namingSlices(Optional.of(pattern));
        }

        private Transformer namingSlices(Optional<String> pattern) {
            return new Transformer(sliceAssignment, description, pattern, predicate);
        }

        @Override
        public Transformer as(String description) {
            return new Transformer(sliceAssignment, description, predicate).namingSlices(namingPattern);
        }

        public Slices of(JavaClasses classes) {
            return new Slices(transform(classes));
        }

        public Slices transform(Iterable<Dependency> dependencies) {
            return new Slices(transform(toTargetClasses(dependencies)));
        }

        @Override
        public Slices transform(JavaClasses classes) {
            Slices slices = createSlices(classes);
            if (namingPattern.isPresent()) {
                slices = slices.namingSlices(namingPattern.get());
            }
            if (predicate.isPresent()) {
                slices = new Slices(Guava.Iterables.filter(slices, predicate.get()));
            }
            return slices.as(getDescription());
        }

        private Slices createSlices(JavaClasses classes) {
            SliceBuilders sliceBuilders = new SliceBuilders(sliceAssignment);
            for (JavaClass clazz : classes) {
                sliceBuilders.add(clazz);
            }
            return new Slices(sliceBuilders.build());
        }

        @Override
        public Slices.Transformer that(final DescribedPredicate<? super Slice> predicate) {
            String newDescription = this.predicate.joinDescription(getDescription(), predicate.getDescription());
            return new Transformer(sliceAssignment, newDescription, namingPattern, this.predicate.add(predicate));
        }

        @Override
        public String getDescription() {
            return description;
        }

        Transformer thatANDsPredicates() {
            return new Transformer(sliceAssignment, description, namingPattern, predicate.thatANDs());
        }

        Transformer thatORsPredicates() {
            return new Transformer(sliceAssignment, description, namingPattern, predicate.thatORs());
        }
    }

    // Since Slices can be renamed with 'as' in the middle (e.g. slices().that(foo).as("bar").should()... -> "bar should")
    // we need this workaround for now
    private static class SlicesPredicateAggregator {
        private final PredicateAggregator<Slice> predicate;
        private final String descriptionJoinWord;

        SlicesPredicateAggregator(String descriptionJoinWord) {
            this(new PredicateAggregator<Slice>(), descriptionJoinWord);
        }

        private SlicesPredicateAggregator(PredicateAggregator<Slice> predicate, String descriptionJoinWord) {
            this.predicate = checkNotNull(predicate);
            this.descriptionJoinWord = checkNotNull(descriptionJoinWord);
        }

        boolean isPresent() {
            return predicate.isPresent();
        }

        DescribedPredicate<Slice> get() {
            return predicate.get();
        }

        SlicesPredicateAggregator add(DescribedPredicate<? super Slice> predicate) {
            return new SlicesPredicateAggregator(this.predicate.add(predicate), descriptionJoinWord);
        }

        SlicesPredicateAggregator thatANDs() {
            return new SlicesPredicateAggregator(predicate.thatANDs(), "and");
        }

        SlicesPredicateAggregator thatORs() {
            return new SlicesPredicateAggregator(predicate.thatORs(), "or");
        }

        String joinDescription(String first, String second) {
            return Joiner.on(" ").join(first, descriptionJoinWord, second);
        }
    }

    private static class SliceBuilders {
        private final Map<List<String>, Slice.Builder> sliceBuilders = new HashMap<>();
        private final SliceAssignment sliceAssignment;

        SliceBuilders(SliceAssignment sliceAssignment) {
            this.sliceAssignment = sliceAssignment;
        }

        void add(JavaClass clazz) {
            List<String> identifierParts = sliceAssignment.getIdentifierOf(clazz).getParts();
            if (identifierParts.isEmpty()) {
                return;
            }

            if (!sliceBuilders.containsKey(identifierParts)) {
                sliceBuilders.put(identifierParts, Slice.Builder.from(identifierParts, sliceAssignment));
            }
            sliceBuilders.get(identifierParts).addClass(clazz);
        }

        Set<Slice> build() {
            Set<Slice> result = new HashSet<>();
            for (Slice.Builder builder : sliceBuilders.values()) {
                result.add(builder.build());
            }
            return result;
        }
    }

    private static class PackageMatchingSliceIdentifier implements SliceAssignment {
        private final String packageIdentifier;

        private PackageMatchingSliceIdentifier(String packageIdentifier) {
            this.packageIdentifier = checkNotNull(packageIdentifier);
        }

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            PackageMatcher matcher = PackageMatcher.of(packageIdentifier);
            Optional<List<String>> result = matcher.match(javaClass.getPackageName()).transform(TO_GROUPS);
            List<String> parts = result.or(Collections.<String>emptyList());
            return parts.isEmpty() ? SliceIdentifier.ignore() : SliceIdentifier.of(parts);
        }

        @Override
        public String getDescription() {
            return slicesMatchingDescription(packageIdentifier);
        }

        private static String slicesMatchingDescription(String packageIdentifier) {
            return "'" + packageIdentifier + "'";
        }
    }
}
