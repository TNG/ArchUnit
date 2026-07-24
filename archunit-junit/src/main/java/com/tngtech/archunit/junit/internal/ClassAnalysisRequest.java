package com.tngtech.archunit.junit.internal;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.CacheMode;
import com.tngtech.archunit.junit.LocationProvider;

/**
 * Simple adapter to separate the JUnit version specific @AnalyzeClasses from infrastructure like {@link ClassCache}.
 */
interface ClassAnalysisRequest {
    String[] getPackageNames();

    Class<?>[] getPackageRoots();

    Class<? extends LocationProvider>[] getLocationProviders();

    Class<? extends ImportOption>[] getImportOptions();

    CacheMode getCacheMode();

    boolean scanWholeClasspath();

    Class<?>[] getClassesToAnalyze();
}
