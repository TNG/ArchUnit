package com.tngtech.archunit.testutils;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.lang.ConditionEvent;

public interface ExpectedRelation {
    void associateLines(LineAssociation association);

    /**
     * @return True, if this expected dependency refers to the supplied object
     * (i.e. the object that was passed to the {@link ConditionEvent}, e.g. a {@link JavaAccess})
     */
    boolean correspondsTo(Object object);

    /**
     * How we can identify a line within the exception message as belonging to this relation
     */
    interface LineAssociation {
        void associateIfPatternMatches(String pattern);

        void associateIfStringIsContained(String string);
    }
}
