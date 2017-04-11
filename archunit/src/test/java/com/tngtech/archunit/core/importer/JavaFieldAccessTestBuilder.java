package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;

public class JavaFieldAccessTestBuilder {
    private JavaFieldAccessBuilder delegate = new JavaFieldAccessBuilder();

    public JavaFieldAccessTestBuilder withOrigin(JavaMethod origin) {
        delegate.withOrigin(origin);
        return this;
    }

    public JavaFieldAccessTestBuilder withTarget(FieldAccessTarget target) {
        delegate.withTarget(target);
        return this;
    }

    public JavaFieldAccessTestBuilder withAccessType(AccessType accessType) {
        delegate.withAccessType(accessType);
        return this;
    }

    public JavaFieldAccessTestBuilder withLineNumber(int lineNumber) {
        delegate.withLineNumber(lineNumber);
        return this;
    }

    public JavaFieldAccess build() {
        return delegate.build();
    }
}
