package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.importer.ImportOption;

@SuppressWarnings("unchecked")
class TestAnalysisRequest implements ClassAnalysisRequest {
    private String[] packages = new String[0];
    private Class<?>[] packageRoots = new Class<?>[0];
    private Class<? extends LocationProvider>[] locationProviders = new Class[0];
    private Class<? extends ImportOption>[] importOptions = new Class[0];
    private CacheMode cacheMode = CacheMode.FOREVER;

    @Override
    public String[] getPackageNames() {
        return packages;
    }

    @Override
    public Class<?>[] getPackageRoots() {
        return packageRoots;
    }

    @Override
    public Class<? extends LocationProvider>[] getLocationProviders() {
        return locationProviders;
    }

    @Override
    public Class<? extends ImportOption>[] getImportOptions() {
        return importOptions;
    }

    @Override
    public CacheMode getCacheMode() {
        return cacheMode;
    }

    TestAnalysisRequest withPackages(String... packages) {
        this.packages = packages;
        return this;
    }

    TestAnalysisRequest withPackagesRoots(Class<?>... classes) {
        this.packageRoots = classes;
        return this;
    }

    @SafeVarargs
    final TestAnalysisRequest withLocationProviders(Class<? extends LocationProvider>... locationProviders) {
        this.locationProviders = locationProviders;
        return this;
    }

    @SafeVarargs
    final TestAnalysisRequest withImportOptions(Class<? extends ImportOption>... importOptions) {
        this.importOptions = importOptions;
        return this;
    }

    TestAnalysisRequest withCacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
        return this;
    }
}
