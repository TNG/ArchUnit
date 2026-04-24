package com.tngtech.archunit.core.domain.testobjects;

import java.io.IOException;

@SuppressWarnings("unused")
public class ClassWithDependencyOnCaughtException {
    static {
        try {
            throw new IOException();
        } catch (IOException e) {
        }
    }

    ClassWithDependencyOnCaughtException() {
        try {
            throw new IOException();
        } catch (IOException e) {
        }
    }

    void simpleCatchClauseMethod() {
        try {
            throw new IOException();
        } catch (IOException e) {
        }
    }

    void unionCatchClauseMethod() {
        try {
            throw new IOException();
        } catch (IllegalStateException | IOException e) {
        }
    }

    void multipleCatchClausesMethod() {
        try {
            throw new IOException();
        } catch (IllegalStateException e) {
        } catch (RuntimeException e) {
        } catch (IOException e) {
        }
    }
}
