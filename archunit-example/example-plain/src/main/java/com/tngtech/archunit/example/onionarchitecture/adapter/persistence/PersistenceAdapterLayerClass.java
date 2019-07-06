package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import com.tngtech.archunit.example.onionarchitecture.adapter.cli.CliAdapterLayerClass;
import com.tngtech.archunit.example.onionarchitecture.adapter.rest.RestAdapterLayerClass;
import com.tngtech.archunit.example.onionarchitecture.application.ApplicationLayerClass;
import com.tngtech.archunit.example.onionarchitecture.domain.model.DomainModelLayerClass;
import com.tngtech.archunit.example.onionarchitecture.domain.service.DomainServiceLayerClass;

public class PersistenceAdapterLayerClass {
    private DomainModelLayerClass domainModelLayerClass;
    private DomainServiceLayerClass domainServiceLayerClass;
    private ApplicationLayerClass applicationLayerClass;
    private CliAdapterLayerClass cliAdapterLayerClass;
    private PersistenceAdapterLayerClass persistenceAdapterLayerClass;
    private RestAdapterLayerClass restAdapterLayerClass;

    private void call() {
        domainModelLayerClass.callMe();
        domainServiceLayerClass.callMe();
        applicationLayerClass.callMe();
        cliAdapterLayerClass.callMe();
        persistenceAdapterLayerClass.callMe();
        restAdapterLayerClass.callMe();
    }

    public void callMe() {
    }
}
