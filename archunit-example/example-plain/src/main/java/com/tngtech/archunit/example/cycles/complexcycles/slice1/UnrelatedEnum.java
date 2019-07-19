package com.tngtech.archunit.example.cycles.complexcycles.slice1;

// This was just added for the integration tests since it creates some synthetic byte code
@SuppressWarnings("SwitchStatementWithTooFewBranches")
public enum UnrelatedEnum {
    INSTANCE;

    static void doSomeSwitching(UnrelatedEnum unrelatedEnum) {
        switch (unrelatedEnum) {
            case INSTANCE:
                throw new UnsupportedOperationException();
        }
    }
}
