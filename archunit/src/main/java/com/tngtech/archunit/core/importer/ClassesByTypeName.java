package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClass;

interface ClassesByTypeName {
    JavaClass get(String typeName);
}
