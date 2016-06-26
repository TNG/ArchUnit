package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import static com.google.common.collect.Sets.newHashSet;
import static org.reflections.ReflectionUtils.getAllConstructors;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;

public class IdentifiedTarget<T extends Member> {
    private Optional<T> target;

    public IdentifiedTarget(Optional<T> target) {
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public static IdentifiedTarget<Field> ofField(Class<?> owner, Predicate<Field> predicate) {
        return identifyTarget(getAllFields(owner, predicate));
    }

    @SuppressWarnings("unchecked")
    public static IdentifiedTarget<Method> ofMethod(Class<?> owner, Predicate<Method> predicate) {
        return identifyTarget(getAllMethods(owner, predicate));
    }

    public static IdentifiedTarget<Constructor<?>> ofConstructor(Class<?> owner, Predicate<Constructor<?>> predicate) {
        @SuppressWarnings("unchecked")
        Set<Constructor<?>> allConstructors = (Set) getAllConstructors(owner, (Predicate) predicate);
        return identifyTarget(allConstructors);
    }

    private static <T extends Member> IdentifiedTarget<T> identifyTarget(Set<T> matchingMembers) {
        // NOTE: Don't use Set here, because our Comparator is very special and can't compare arbitrary T,
        //       thus we can't check the SortedSets for equality. Unique elements are guaranteed by the algorithm.
        ImmutableList.Builder<SortedSet<T>> builder = ImmutableList.builder();
        Set<Set<T>> orderableSubsets = findOrderableSubsets(matchingMembers);
        for (Set<T> subset : orderableSubsets) {
            builder.add(sortedByTypeHierarchy(subset));
        }
        return new IdentifiedTarget<>(tryToDetermineUniqueTarget(builder.build()));
    }

    private static <T extends Member> Set<Set<T>> findOrderableSubsets(Set<T> members) {
        Set<Set<T>> result = new HashSet<>();
        for (T member : members) {
            boolean added = false;
            for (Set<T> set : result) {
                if (comparableToAllElements(member, set)) {
                    added |= set.add(member);
                }
            }
            if (!added) {
                result.add(newSet(member));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Member> Set<T> newSet(T member) {
        return newHashSet(member);
    }

    private static <T extends Member> boolean comparableToAllElements(T member, Set<T> set) {
        for (T m : set) {
            if (!member.getDeclaringClass().isAssignableFrom(m.getDeclaringClass())
                    && !m.getDeclaringClass().isAssignableFrom(member.getDeclaringClass())) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Member> TreeSet<T> sortedByTypeHierarchy(Set<T> members) {
        TreeSet<T> result = new TreeSet<>(SORT_BY_DECLARING_CLASSES_SUB_TO_SUPER);
        result.addAll(members);
        return result;
    }

    private static <T extends Member> Optional<T> tryToDetermineUniqueTarget(List<SortedSet<T>> totallyOrdered) {
        SortedSet<T> minOfEachSubset = new TreeSet<>(SORT_BY_DECLARING_CLASSES_SUB_TO_SUPER);
        for (SortedSet<T> ts : totallyOrdered) {
            if (comparableToAllElements(ts.first(), minOfEachSubset)) {
                minOfEachSubset.add(ts.first());
            } else {
                return Optional.absent();
            }
        }
        return minOfEachSubset.isEmpty() ? Optional.<T>absent() : Optional.of(minOfEachSubset.first());
    }


    public boolean wasIdentified() {
        return target.isPresent();
    }

    public T get() {
        return getOrThrow("Target could not be identified");
    }

    public T getOrThrow(String message, Object... args) {
        if (!wasIdentified()) {
            throw new UnidentifiableTargetException(String.format(message, args));
        }
        return target.get();
    }

    private static final Comparator<Member> SORT_BY_DECLARING_CLASSES_SUB_TO_SUPER = new Comparator<Member>() {
        @Override
        public int compare(Member o1, Member o2) {
            if (o1.getDeclaringClass() == o2.getDeclaringClass()) {
                return 0;
            }
            if (o1.getDeclaringClass().isAssignableFrom(o2.getDeclaringClass())) {
                return 1;
            }
            if (o2.getDeclaringClass().isAssignableFrom(o1.getDeclaringClass())) {
                return -1;
            }
            throw new IllegalStateException(String.format(
                    "Can't compare %s and %s, because the type hierarchy or their declaring classes is disjoint",
                    o1, o2));
        }
    };
}
