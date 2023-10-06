package com.tngtech.archunit.library.modules.testexamples.valid.module1;

import com.tngtech.archunit.library.modules.testexamples.MyModule;

@MyModule(name = "Module One")
public interface ModuleOneDescriptor {
    String name = "Module One";
}
