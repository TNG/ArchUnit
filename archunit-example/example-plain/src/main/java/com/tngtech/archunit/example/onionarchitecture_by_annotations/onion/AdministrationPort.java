package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Application;

@Application
public interface AdministrationPort {
    <T> T getInstanceOf(Class<T> type);
}
