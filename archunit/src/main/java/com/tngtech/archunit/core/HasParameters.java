package com.tngtech.archunit.core;

import java.util.List;

public interface HasParameters {
    List<TypeDetails> getParameters();
}
