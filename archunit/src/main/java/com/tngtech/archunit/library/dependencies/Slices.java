package com.tngtech.archunit.library.dependencies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.PackageMatcher.TO_GROUPS;
import static com.tngtech.archunit.core.domain.Dependency.toTargetClasses;

/**
 * Basic collection of {@link Slice} for tests on dependencies of package slices, e.g. to avoid cycles.
 * Example to specify a {@link ClassesTransformer} to run {@link ArchRule ArchRules} against {@link Slices}:
 * <pre><code>
 * Slices.matching("some.pkg.(*)..")
 * </code></pre>
 * would group the packages
 * <ul>
 * <li><code>some.pkg.one.any</code></li>
 * <li><code>some.pkg.one.other</code></li>
 * </ul>
 * in the same slice, the package
 * <ul>
 * <li><code>some.pkg.two.any</code></li>
 * </ul>
 * in a different slice.<br/>
 * The resulting {@link ClassesTransformer} can be used to specify an {@link ArchRule} on slices.
 */
public final class Slices implements DescribedIterable<Slice> {
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

    public Slices as(String description) {
        return new Slices(slices, description);
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Allows the naming of single slices, where back references to the matching pattern can be denoted by '$' followed
     * by capturing group number. <br/>
     * E.g. {@code namingSlices("Slice $1")} would name a slice matching {@code '*..service.(*)..*'}
     * against {@code 'com.some.company.service.hello.something'} as 'Slice hello'.
     *
     * @param pattern The naming pattern, e.g. 'Slice $1'
     * @return The same slices with adjusted naming for each single slice
     */
    public Slices namingSlices(String pattern) {
        for (Slice slice : slices) {
            slice.as(pattern);
        }
        return this;
    }

    /**
     * @see Creator#matching(String)
     */
    public static Transformer matching(String packageIdentifier) {
        return new Transformer(packageIdentifier, slicesMatchingDescription(packageIdentifier));
    }

    /**
     * Specifies how to transform a set of {@link JavaClass} into a set of {@link Slice}, e.g. to test that
     * no cycles between certain package slices appear.
     *
     * @see Slices
     */
    public static class Transformer implements ClassesTransformer<Slice> {
        private final String packageIdentifier;
        private final String description;
        private final Optional<String> namingPattern;
        private final PredicateAggregator<Slice> predicate;

        private Transformer(String packageIdentifier, String description) {
            this(packageIdentifier, description, new PredicateAggregator<Slice>());
        }

        private Transformer(String packageIdentifier, String description, PredicateAggregator<Slice> predicate) {
            this(packageIdentifier, description, Optional.<String>absent(), predicate);
        }

        private Transformer(String packageIdentifier,
                            String description,
                            Optional<String> namingPattern,
                            PredicateAggregator<Slice> predicate) {

            this.packageIdentifier = checkNotNull(packageIdentifier);
            this.description = checkNotNull(description);
            this.namingPattern = checkNotNull(namingPattern);
            this.predicate = checkNotNull(predicate);
        }

        /**
         * @see Slices#namingSlices(String)
         */
        Transformer namingSlices(String pattern) {
            return namingSlices(Optional.of(pattern));
        }

        private Transformer namingSlices(Optional<String> pattern) {
            return new Transformer(packageIdentifier, description, pattern, predicate);
        }

        @Override
        public Transformer as(String description) {
            return new Transformer(packageIdentifier, description).namingSlices(namingPattern);
        }

        public Slices of(JavaClasses classes) {
            return new Slices(transform(classes));
        }

        public Slices transform(Iterable<Dependency> dependencies) {
            return new Slices(transform(toTargetClasses(dependencies)));
        }

        @Override
        public Slices transform(JavaClasses classes) {
            Slices slices = new Creator(classes).matching(packageIdentifier);
            if (predicate.isPresent()) {
                slices = new Slices(Guava.Iterables.filter(slices, predicate.get()));
            }
            if (namingPattern.isPresent()) {
                slices.namingSlices(namingPattern.get());
            }
            return slices.as(getDescription());
        }

        @Override
        public Slices.Transformer that(final DescribedPredicate<? super Slice> predicate) {
            String newDescription = getDescription() + " that " + predicate.getDescription();
            return new Transformer(packageIdentifier, newDescription, namingPattern, this.predicate.add(predicate));
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public static final class Creator {
        private final JavaClasses classes;

        private Creator(JavaClasses classes) {
            this.classes = classes;
        }

        /**
         * Supports partitioning a set of {@link JavaClasses} into different slices by matching the supplied
         * package identifier. For identifier syntax, see {@link PackageMatcher}.<br/>
         * The slicing is done according to capturing groups (thus if none are contained in the identifier, no more than
         * a single slice will be the result). For example
         * <p>
         * Suppose there are three classes:<br/><br/>
         * {@code com.example.slice.one.SomeClass}<br/>
         * {@code com.example.slice.one.AnotherClass}<br/>
         * {@code com.example.slice.two.YetAnotherClass}<br/><br/>
         * If slices are created by specifying<br/><br/>
         * {@code Slices.of(classes).byMatching("..slice.(*)..")}<br/><br/>
         * then the result will be two slices, the slice where the capturing group is 'one' and the slice where the
         * capturing group is 'two'.
         * </p>
         *
         * @param packageIdentifier The identifier to match against
         * @return Slices partitioned according the supplied package identifier
         */
        @PublicAPI(usage = ACCESS)
        public Slices matching(String packageIdentifier) {
            SliceBuilders sliceBuilders = new SliceBuilders();
            PackageMatcher matcher = PackageMatcher.of(packageIdentifier);
            for (JavaClass clazz : classes) {
                Optional<List<String>> groups = matcher.match(clazz.getPackage()).transform(TO_GROUPS);
                sliceBuilders.add(groups, clazz);
            }
            return new Slices(sliceBuilders.build()).as(slicesMatchingDescription(packageIdentifier));
        }
    }

    private static String slicesMatchingDescription(String packageIdentifier) {
        return String.format("slices matching '%s'", packageIdentifier);
    }

    private static class SliceBuilders {
        Map<List<String>, Slice.Builder> sliceBuilders = new HashMap<>();

        void add(Optional<List<String>> matchingGroups, JavaClass clazz) {
            if (matchingGroups.isPresent()) {
                put(matchingGroups.get(), clazz);
            }
        }

        private void put(List<String> matchingGroups, JavaClass clazz) {
            if (!sliceBuilders.containsKey(matchingGroups)) {
                sliceBuilders.put(matchingGroups, Slice.Builder.from(matchingGroups));
            }
            sliceBuilders.get(matchingGroups).addClass(clazz);
        }

        Set<Slice> build() {
            Set<Slice> result = new HashSet<>();
            for (Slice.Builder builder : sliceBuilders.values()) {
                result.add(builder.build());
            }
            return result;
        }
    }
}
