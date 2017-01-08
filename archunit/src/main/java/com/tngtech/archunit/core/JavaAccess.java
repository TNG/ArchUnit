package com.tngtech.archunit.core;

import java.util.Objects;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.properties.HasDescription;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasOwner;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JavaAccess<TARGET extends AccessTarget>
        implements HasName, HasDescription, HasOwner<JavaCodeUnit> {

    private static final String LOCATION_TEMPLATE = "(%s.java:%d)";

    private final JavaCodeUnit origin;
    private final TARGET target;
    private final int lineNumber;
    private final int hashCode;

    JavaAccess(AccessRecord<TARGET> record) {
        this(record.getCaller(), record.getTarget(), record.getLineNumber());
    }

    JavaAccess(JavaCodeUnit origin, TARGET target, int lineNumber) {
        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
        this.hashCode = Objects.hash(origin.getFullName(), target.getFullName(), lineNumber);
    }

    @Override
    public String getName() {
        return target.getName();
    }

    public JavaCodeUnit getOrigin() {
        return origin;
    }

    public JavaClass getOriginOwner() {
        return getOrigin().getOwner();
    }

    public JavaClass getTargetOwner() {
        return getTarget().getOwner();
    }

    public TARGET getTarget() {
        return target;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public JavaCodeUnit getOwner() {
        return getOrigin();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaAccess<?> other = (JavaAccess<?>) obj;
        return Objects.equals(this.origin.getFullName(), other.origin.getFullName()) &&
                Objects.equals(this.target.getFullName(), other.target.getFullName()) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{origin=" + origin + ", target=" + target + ", lineNumber=" + lineNumber + additionalToStringFields() + '}';
    }

    String additionalToStringFields() {
        return "";
    }

    @Override
    public String getDescription() {
        return getDescriptionWithTemplate(descriptionTemplate());
    }

    public String getDescriptionWithTemplate(String template) {
        String description = String.format(template, getOwner().getFullName(), getTarget().getFullName());
        String location = String.format(LOCATION_TEMPLATE, getLocationClass().getSimpleName(), getLineNumber());
        return String.format("%s in %s", description, location);
    }

    private JavaClass getLocationClass() {
        JavaClass location = getOriginOwner();
        while (location.getEnclosingClass().isPresent()) {
            location = location.getEnclosingClass().get();
        }
        return location;
    }

    protected abstract String descriptionTemplate();

    public static class Predicates {
        public static DescribedPredicate<JavaAccess<?>> withOrigin(DescribedPredicate<? super JavaCodeUnit> predicate) {
            return predicate.onResultOf(Functions.Get.origin());
        }
    }

    public static class Functions {
        public static class Get {
            public static ChainableFunction<JavaAccess<?>, JavaCodeUnit> origin() {
                return new ChainableFunction<JavaAccess<?>, JavaCodeUnit>() {
                    @Override
                    public JavaCodeUnit apply(JavaAccess<?> input) {
                        return input.getOrigin();
                    }
                };
            }

            public static <A extends JavaAccess<? extends T>, T extends AccessTarget> ChainableFunction<A, T> target() {
                return new ChainableFunction<A, T>() {
                    @Override
                    public T apply(A input) {
                        return input.getTarget();
                    }
                };
            }
        }
    }
}
