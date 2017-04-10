package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.JavaClass;

public interface ClassesByTypeName {
    JavaClass get(String typeName);
}
