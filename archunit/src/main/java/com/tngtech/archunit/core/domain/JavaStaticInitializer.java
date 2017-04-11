package com.tngtech.archunit.core.domain;

import java.lang.reflect.Member;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaStaticInitializerBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.emptySet;

public class JavaStaticInitializer extends JavaCodeUnit {
    @PublicAPI(usage = ACCESS)
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    JavaStaticInitializer(JavaStaticInitializerBuilder builder) {
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
