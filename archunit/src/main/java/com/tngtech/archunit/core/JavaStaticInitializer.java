package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.Set;

import com.tngtech.archunit.core.importer.DomainBuilders;

import static java.util.Collections.emptySet;

public class JavaStaticInitializer extends JavaCodeUnit {
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public JavaStaticInitializer(DomainBuilders.JavaStaticInitializerBuilder builder) {
        super(builder);
    }

    @Override
    public Set<? extends JavaAccess<?>> getAccessesToSelf() {
        return emptySet();
    }

    @Override
    public Member reflect() {
        throw new UnsupportedOperationException("Can't reflect on a static initializer");
    }

}
