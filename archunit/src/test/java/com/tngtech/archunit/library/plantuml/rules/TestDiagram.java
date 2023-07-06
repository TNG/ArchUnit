package com.tngtech.archunit.library.plantuml.rules;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.rules.TemporaryFolder;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

class TestDiagram {
    private final List<String> lines = new ArrayList<>();
    private final TemporaryFolder temporaryFolder;

    private TestDiagram(TemporaryFolder temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    ComponentCreator component(String componentName) {
        return new ComponentCreator(componentName);
    }

    private TestDiagram addComponent(ComponentCreator creator) {
        String stereotypes = creator.stereotypes
                .stream().map(input -> "<<" + input + ">>")
                .collect(joining(" "));
        String line = String.format("[%s] %s", creator.componentName, stereotypes);
        if (creator.alias != null) {
            line += " as " + creator.alias;
        }
        if (creator.color != null) {
            line += " #" + creator.color;
        }
        lines.add(line);
        return this;
    }

    DatabaseCreator database(String databaseName) {
        return new DatabaseCreator(databaseName);
    }

    private TestDiagram addDatabase(DatabaseCreator creator) {
        String stereotypes = creator.stereotypes
                .stream().map(input -> "<<" + input + ">>")
                .collect(joining(" "));
        String line = String.format("database \"%s\" %s", creator.databaseName, stereotypes);
        if (creator.alias != null) {
            line += " as " + creator.alias;
        }
        if (creator.color != null) {
            line += " #" + creator.color;
        }
        lines.add(line);
        return this;
    }

    DependencyFromCreator dependencyFrom(String origin) {
        return new DependencyFromCreator(origin);
    }

    DependencyToCreator dependencyTo(String target) {
        return new DependencyToCreator(target);
    }

    TestDiagram rawLine(String line) {
        lines.add(line);
        return this;
    }

    File write() {
        File file = createTempFile();
        String diagram = "@startuml" + lineSeparator() +
                lines.stream().collect(joining(lineSeparator())) + lineSeparator() +
                "@enduml";

        try {
            Files.write(file.toPath(), diagram.getBytes(UTF_8));
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createTempFile() {
        try {
            return temporaryFolder.newFile("plantuml_diagram_" + UUID.randomUUID() + ".puml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static TestDiagram in(TemporaryFolder temporaryFolder) {
        return new TestDiagram(temporaryFolder);
    }

    class ComponentCreator {
        private final String componentName;
        private final List<String> stereotypes = new ArrayList<>();

        private String alias;
        private String color;

        private ComponentCreator(String componentName) {
            this.componentName = componentName;
        }

        ComponentCreator withAlias(String alias) {
            this.alias = alias;
            return this;
        }

        ComponentCreator withColor(String color) {
            this.color = color;
            return this;
        }

        TestDiagram withStereoTypes(String... stereoTypes) {
            this.stereotypes.addAll(ImmutableList.copyOf(stereoTypes));
            return addComponent(this);
        }
    }

    class DatabaseCreator {
        private final String databaseName;
        private final List<String> stereotypes = new ArrayList<>();
        private String alias;
        private String color;

        DatabaseCreator(String databaseName) {
            this.databaseName = databaseName;
        }

        DatabaseCreator withAlias(String alias) {
            this.alias = alias;
            return this;
        }

        DatabaseCreator withColor(String color) {
            this.color = color;
            return this;
        }

        TestDiagram withStereoTypes(String... stereoTypes) {
            this.stereotypes.addAll(ImmutableList.copyOf(stereoTypes));
            return addDatabase(this);
        }

        public TestDiagram build() {
            return addDatabase(this);
        }
    }

    class DependencyFromCreator {
        private final String origin;

        private DependencyFromCreator(String origin) {
            this.origin = origin;
        }

        TestDiagram to(String target) {
            String dependency = origin + " --> " + target;
            return TestDiagram.this.rawLine(dependency);
        }
    }

    class DependencyToCreator {
        private final String target;

        private DependencyToCreator(String target) {
            this.target = target;
        }

        TestDiagram from(String origin) {
            String dependency = target + " <-- " + origin;
            return TestDiagram.this.rawLine(dependency);
        }
    }
}
