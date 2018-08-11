package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Simple adapter to separate the JUnit version specific @AnalyzeClasses from infrastructure like {@link ClassCache}.
 */
interface ClassAnalysisRequest {
    String[] getPackageNames();

    Class<?>[] getPackageRoots();

    Class<? extends LocationProvider>[] getLocationProviders();

    Class<? extends ImportOption>[] getImportOptions();

    CacheMode getCacheMode();
}
