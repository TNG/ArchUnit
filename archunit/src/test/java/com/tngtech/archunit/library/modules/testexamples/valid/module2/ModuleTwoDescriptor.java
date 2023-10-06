package com.tngtech.archunit.library.modules.testexamples.valid.module2;

import com.tngtech.archunit.library.modules.testexamples.MyModule;

@SuppressWarnings("unused")
@MyModule(name = "Module Two")
public interface ModuleTwoDescriptor {
    String name = "Module Two";
}
