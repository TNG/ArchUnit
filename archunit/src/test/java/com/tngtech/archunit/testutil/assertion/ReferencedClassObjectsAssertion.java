package com.tngtech.archunit.testutil.assertion;

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.assertj.core.api.Assertions.assertThat;

public class ReferencedClassObjectsAssertion extends AbstractIterableAssert<ReferencedClassObjectsAssertion, Set<ReferencedClassObject>, ReferencedClassObject, ReferencedClassObjectsAssertion.ReferencedClassObjectAssertion> {
    public ReferencedClassObjectsAssertion(Set<ReferencedClassObject> referencedClassObjects) {
        super(referencedClassObjects, ReferencedClassObjectsAssertion.class);
    }

    @Override
    protected ReferencedClassObjectAssertion toAssert(ReferencedClassObject value, String description) {
        return new ReferencedClassObjectAssertion(value).as(description);
    }

    public void containReferencedClassObjects(Iterable<ExpectedReferencedClassObject> expectedReferencedClassObjects) {
        final FluentIterable<ReferencedClassObject> actualReferencedClassObjects = FluentIterable.from(actual);
        Set<ExpectedReferencedClassObject> unmatchedClassObjects = FluentIterable.from(expectedReferencedClassObjects)
                .filter(new Predicate<ExpectedReferencedClassObject>() {
                    @Override
                    public boolean apply(ExpectedReferencedClassObject expectedReferencedClassObject) {
                        return !actualReferencedClassObjects.anyMatch(expectedReferencedClassObject);
                    }
                }).toSet();
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

        private ExpectedReferencedClassObject(Class<?> type, int lineNumber) {
            this.type = type;
            this.lineNumber = lineNumber;
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        public boolean apply(ReferencedClassObject input) {
            return input.getValue().isEquivalentTo(type) && input.getLineNumber() == lineNumber;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("type", type)
                    .add("lineNumber", lineNumber)
                    .toString();
        }
    }
}
