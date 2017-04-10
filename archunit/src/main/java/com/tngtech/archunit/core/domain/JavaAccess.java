package com.tngtech.archunit.core.domain;

import java.util.Objects;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.HasDescription;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JavaAccess<TARGET extends AccessTarget>
        implements HasName, HasDescription, HasOwner<JavaCodeUnit> {

    private static final String LOCATION_TEMPLATE = "(%s.java:%d)";

    private final JavaCodeUnit origin;
    private final TARGET target;
    private final int lineNumber;
    private final int hashCode;

    JavaAccess(DomainBuilders.JavaAccessBuilder<TARGET, ?> builder) {
        this.origin = checkNotNull(builder.getOrigin());
        this.target = checkNotNull(builder.getTarget());
        this.lineNumber = builder.getLineNumber();
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
        public static DescribedPredicate<JavaAccess<?>> originOwner(DescribedPredicate<? super JavaClass> predicate) {
            return origin(Get.<JavaClass>owner().is(predicate));
        }

        public static DescribedPredicate<JavaAccess<?>> origin(DescribedPredicate<? super JavaCodeUnit> predicate) {
            return predicate.onResultOf(Functions.Get.origin()).as("origin " + predicate.getDescription());
        }

        public static DescribedPredicate<JavaAccess<?>> originOwnerEqualsTargetOwner() {
            return new DescribedPredicate<JavaAccess<?>>("origin owner equals target owner") {
                @Override
                public boolean apply(JavaAccess<?> input) {
                    return input.getOriginOwner().equals(input.getTargetOwner());
                }
            };
        }

        public static DescribedPredicate<JavaAccess<?>> targetOwner(final DescribedPredicate<? super JavaClass> predicate) {
            return target(Get.<JavaClass>owner().is(predicate));
        }

        public static DescribedPredicate<JavaAccess<?>> target(final DescribedPredicate<? super AccessTarget> predicate) {
            return new DescribedPredicate<JavaAccess<?>>("target " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaAccess<?> input) {
                    return predicate.apply(input.getTarget());
                }
            };
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
