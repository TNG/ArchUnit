package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass;
import org.assertj.core.api.AbstractObjectAssert;

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
}
