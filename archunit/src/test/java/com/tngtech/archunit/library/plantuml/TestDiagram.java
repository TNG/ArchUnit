package com.tngtech.archunit.library.plantuml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.junit.rules.TemporaryFolder;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;

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
        String stereotypes = FluentIterable.from(creator.stereotypes)
                .transform(new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                        return "<<" + input + ">>";
                    }
                }).join(Joiner.on(" "));
        String line = String.format("[%s] %s", creator.componentName, stereotypes);
        if (creator.alias != null) {
            line += " as " + creator.alias;
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
        String diagram = FluentIterable.from(singleton("@startuml"))
                .append(lines)
                .append("@enduml")
                .join(Joiner.on(lineSeparator()));

        try {
            Files.write(diagram, file, UTF_8);
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

        private ComponentCreator(String componentName) {
            this.componentName = componentName;
        }

        ComponentCreator withAlias(String alias) {
            this.alias = alias;
            return this;
        }

        TestDiagram withStereoTypes(String... stereoTypes) {
            this.stereotypes.addAll(ImmutableList.copyOf(stereoTypes));
            return addComponent(this);
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
