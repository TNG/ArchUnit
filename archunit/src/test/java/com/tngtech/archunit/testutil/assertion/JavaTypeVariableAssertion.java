package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

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

    @SuppressWarnings("unchecked") // cast is okay because Optional and JavaTypeVariable are covariant
    static <OWNER extends HasDescription> JavaTypeVariable<OWNER> getTypeVariableWithName(String name, List<? extends JavaTypeVariable<? extends OWNER>> typeVariables) {
        Optional<? extends JavaTypeVariable<? extends OWNER>> variable = typeVariables.stream().filter(name(name)).findFirst();
        assertThat(variable).as("Type variable with name '%s'", name).isPresent();
        return (JavaTypeVariable<OWNER>) variable.get();
    }
}
