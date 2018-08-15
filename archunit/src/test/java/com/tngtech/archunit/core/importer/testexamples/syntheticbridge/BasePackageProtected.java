package com.tngtech.archunit.core.importer.testexamples.syntheticbridge;

class BasePackageProtected {
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private void privateMethod() {
    }

    void packageProtectedMethod() {
    }

    protected void protectedMethod() {
    }

    public void publicMethod() {
    }
}
