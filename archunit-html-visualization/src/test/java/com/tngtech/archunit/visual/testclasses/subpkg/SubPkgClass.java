package com.tngtech.archunit.visual.testclasses.subpkg;

import java.io.File;

public class SubPkgClass {
    public SubPkgClass() {
        new InnerSubPkgClass().dependencyOnFile = new File("/");
    }

    public static class InnerSubPkgClass {
        File dependencyOnFile;
    }
}
