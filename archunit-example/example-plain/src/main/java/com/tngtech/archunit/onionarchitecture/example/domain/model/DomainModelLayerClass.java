package com.tngtech.archunit.onionarchitecture.example.domain.model;

import com.tngtech.archunit.onionarchitecture.example.adapter.cli.CliAdapterLayerClass;
import com.tngtech.archunit.onionarchitecture.example.adapter.persistence.PersistenceAdapterLayerClass;
import com.tngtech.archunit.onionarchitecture.example.adapter.rest.RestAdapterLayerClass;
import com.tngtech.archunit.onionarchitecture.example.application.ApplicationLayerClass;
import com.tngtech.archunit.onionarchitecture.example.domain.service.DomainServiceLayerClass;

public class DomainModelLayerClass {
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
