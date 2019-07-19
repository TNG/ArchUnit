package com.tngtech.archunit.example.layers;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class ClassViolatingCodingRules {
    static Logger log = Logger.getLogger("Wrong Logger"); // Violates rules not to use java.util.logging & that loggers should be private static final

    public void printToStandardStream() throws FileNotFoundException {
        System.out.println("I'm gonna print to the command line"); // Violates rule not to write to standard streams
        System.err.println("I'm gonna print to the command line"); // Violates rule not to write to standard streams
        new SomeCustomException().printStackTrace(); // Violates rule not to write to standard streams

        new SomeCustomException().printStackTrace(new PrintStream("/some/file")); // this is okay, since it's a custom PrintStream
        new SomeCustomException().printStackTrace(new PrintWriter("/some/file")); // this is okay, since it's a custom PrintWriter
    }

    public void throwGenericExceptions() throws Throwable {
        if (Math.random() > 0.75) {
            throw new Throwable(); // Violates rule not to throw generic exceptions
        } else if (Math.random() > 0.25) {
            throw new Exception("Bummer"); // Violates rule not to throw generic exceptions
        } else {
            throw new RuntimeException("I have some cause", new Exception("I'm the cause")); // Violates rule not to throw generic exceptions
        }
    }

    public org.joda.time.DateTime jodaTimeIsBad() { // Violates rule not to use the JodaTime library
      return org.joda.time.DateTime.now();
    }

    public void thisIsOkay() {
        throw new SomeCustomException();
    }
}
