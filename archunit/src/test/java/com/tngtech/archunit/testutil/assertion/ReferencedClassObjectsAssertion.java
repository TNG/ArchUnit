package com.tngtech.archunit.testutil.assertion;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class ReferencedClassObjectsAssertion extends AbstractIterableAssert<ReferencedClassObjectsAssertion, Set<ReferencedClassObject>, ReferencedClassObject, ReferencedClassObjectsAssertion.ReferencedClassObjectAssertion> {
    public ReferencedClassObjectsAssertion(Set<ReferencedClassObject> referencedClassObjects) {
        super(referencedClassObjects, ReferencedClassObjectsAssertion.class);
    }

    @Override
    protected ReferencedClassObjectAssertion toAssert(ReferencedClassObject value, String description) {
        return new ReferencedClassObjectAssertion(value).as(description);
    }

    @Override
    protected ReferencedClassObjectsAssertion newAbstractIterableAssert(Iterable<? extends ReferencedClassObject> iterable) {
        return new ReferencedClassObjectsAssertion(ImmutableSet.copyOf(iterable));
    }

    public void containReferencedClassObjects(ExpectedReferencedClassObject... expectedReferencedClassObjects) {
        containReferencedClassObjects(ImmutableList.copyOf(expectedReferencedClassObjects));
    }

    public void containReferencedClassObjects(Iterable<ExpectedReferencedClassObject> expectedReferencedClassObjects) {
        Set<ExpectedReferencedClassObject> unmatchedClassObjects = stream(expectedReferencedClassObjects.spliterator(), false)
                .filter(expected -> actual.stream().noneMatch(expected))
                .collect(toSet());
        assertThat(unmatchedClassObjects).as("Referenced class objects not contained in " + actual).isEmpty();
    }

    static class ReferencedClassObjectAssertion extends AbstractObjectAssert<ReferencedClassObjectAssertion, ReferencedClassObject> {
        public ReferencedClassObjectAssertion(ReferencedClassObject referencedClassObject) {
            super(referencedClassObject, ReferencedClassObjectAssertion.class);
        }
    }

    public static ExpectedReferencedClassObject referencedClassObject(Class<?> type, int lineNumber) {
        return new ExpectedReferencedClassObject(type, lineNumber);
    }

    public static class ExpectedReferencedClassObject implements Predicate<ReferencedClassObject> {
        private final Class<?> type;
        private final int lineNumber;
        private final boolean declaredInLambda;

        private ExpectedReferencedClassObject(Class<?> type, int lineNumber) {
            this(type, lineNumber, false);
        }

        private ExpectedReferencedClassObject(Class<?> type, int lineNumber, boolean declaredInLambda) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.declaredInLambda = declaredInLambda;
        }

        public ExpectedReferencedClassObject declaredInLambda() {
            return new ExpectedReferencedClassObject(type, lineNumber, true);
        }

        @Override
        public boolean test(ReferencedClassObject input) {
            return input.getValue().isEquivalentTo(type) && input.getLineNumber() == lineNumber && input.isDeclaredInLambda() == declaredInLambda;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("type", type)
                    .add("lineNumber", lineNumber)
                    .add("declaredInLambda", declaredInLambda)
                    .toString();
        }
    }
}
