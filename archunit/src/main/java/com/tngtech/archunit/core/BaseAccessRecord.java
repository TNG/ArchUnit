package com.tngtech.archunit.core;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

class BaseAccessRecord<CALLER, TARGET> {
    final CALLER caller;
    final TARGET target;
    final int lineNumber;

    BaseAccessRecord(CALLER caller, TARGET target, int lineNumber) {
        this.caller = checkNotNull(caller);
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, target, lineNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BaseAccessRecord<?, ?> other = (BaseAccessRecord<?, ?>) obj;
        return Objects.equals(this.caller, other.caller) &&
                Objects.equals(this.target, other.target) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
    }

    String fieldsAsString() {
        return "caller=" + caller + ", target=" + target + ", lineNumber=" + lineNumber;
    }
}
