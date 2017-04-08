package com.tngtech.archunit.core;

import com.tngtech.archunit.base.Optional;

interface ClassResolver {
    Optional<JavaClass> tryResolve(String typeName);
}
