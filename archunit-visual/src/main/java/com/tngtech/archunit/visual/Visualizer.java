package com.tngtech.archunit.visual;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.JavaClasses;

public class Visualizer {
    public Visualizer(JavaClasses classes) {

    }

    public void write() {
        // FIXME: Declare path in some config
        File dir = new File(new File(Visualizer.class.getResource("/report").getFile()), "data.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(ImmutableList.of(new Foo("Hans", 55), new Foo("Karl", 22))));

        try {
            Files.write("{}", dir, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Foo {
        private String name;
        private int age;

        private Foo(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
