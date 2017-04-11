package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface CollectsLines {
    void add(String line);
}
