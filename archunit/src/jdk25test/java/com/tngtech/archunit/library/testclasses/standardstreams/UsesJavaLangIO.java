package com.tngtech.archunit.library.testclasses.standardstreams;

public class UsesJavaLangIO {
    void useIo() {
        IO.println("foo");
        IO.println();
        IO.print("bar");
        IO.readln();
        IO.readln("prompt");
    }
}
