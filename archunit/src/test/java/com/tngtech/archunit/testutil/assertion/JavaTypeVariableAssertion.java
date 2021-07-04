package com.tngtech.archunit.testutil.assertion;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static org.assertj.guava.api.Assertions.assertThat;

public class JavaTypeVariableAssertion extends AbstractObjectAssert<JavaTypeVariableAssertion, JavaTypeVariable<?>> {
    public JavaTypeVariableAssertion(JavaTypeVariable<?> actual) {
        super(actual, JavaTypeVariableAssertion.class);
    }

    public void hasBoundsMatching(Class<?>... bounds) {
        hasBoundsMatching(ExpectedConcreteClass.wrap(bounds));
    }

    public void hasBoundsMatching(ExpectedConcreteType... bounds) {
        DescriptionContext context = new DescriptionContext(actual.getName()).step("bounds").describeUpperBounds();
        new JavaTypesAssertion(actual.getBounds()).matchExactly(bounds, context);
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "unchecked"}) // optional checked via AssertJ | cast is okay because Optional and JavaTypeVariable are covariant
    static <OWNER extends HasDescription> JavaTypeVariable<OWNER> getTypeVariableWithName(String name, List<? extends JavaTypeVariable<? extends OWNER>> typeVariables) {
        Optional<? extends JavaTypeVariable<? extends OWNER>> variable = FluentIterable.from(typeVariables).firstMatch(toGuava(name(name)));
        assertThat(variable).as("Type variable with name '%s'", name).isPresent();
        return (JavaTypeVariable<OWNER>) variable.get();
    }
}
