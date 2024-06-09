package com.tngtech.archunit.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import com.google.common.base.Splitter;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.testutil.TestUtils.newTemporaryFolder;
import static com.tngtech.archunit.testutil.TestUtils.toUri;
import static com.tngtech.archunit.testutil.TestUtils.unchecked;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OutsideOfClassPathRule implements TestRule {
    private static final Pattern PACKAGE_DECLARATION_PATTERN = Pattern.compile("package (.*);");

    private final TemporaryFolder temporaryFolder = new TemporaryFolder(newTemporaryFolder());

    @Override
    public Statement apply(Statement base, Description description) {
        return temporaryFolder.apply(base, description);
    }

    /**
     * Compiles all classes from the directory at {@code url} to a temporary folder.
     * Doesn't traverse the given directory, but only takes the direct children.
     * @param url The folder where the Java source files reside
     * @return The path of the remaining compiled class files
     */
    public CompiledClasses compileClassesFrom(URL url) {
        return unchecked(() -> {
            Path sourceFileDir = Paths.get(toUri(url));
            Path classFileDir = temporaryFolder.newFolder().toPath();

            try (Stream<Path> files = Files.list(sourceFileDir)) {
                files.filter(it -> it.toString().endsWith(".java"))
                        .forEach(javaSourceFile -> unchecked(() -> compileJavaSourceFile(javaSourceFile, classFileDir)));
            }

            return new CompiledClasses(classFileDir);
        });
    }

    private void compileJavaSourceFile(Path originalJavaSourceFile, Path targetDir) throws Exception {
        String packageName = readPackageNameOf(originalJavaSourceFile);
        Path packagePath = convertToRelativePath(packageName);

        Path javaSourceDir = temporaryFolder.newFolder().toPath();
        Path javaSourceFile = Files.copy(originalJavaSourceFile, javaSourceDir.resolve(originalJavaSourceFile.getFileName())).toAbsolutePath();
        executeCompile(javaSourceFile);

        Path targetClassFileDir = Files.createDirectories(targetDir.resolve(packagePath));
        copy(javaSourceDir, targetClassFileDir, it -> it.endsWith(".class"));
    }

    private static String readPackageNameOf(Path originalJavaSourceFile) throws IOException {
        return Files.readAllLines(originalJavaSourceFile, UTF_8).stream()
                .flatMap(OutsideOfClassPathRule::tryMatchPackage)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Couldn't find package declaration in source file " + originalJavaSourceFile.toAbsolutePath()));
    }

    private static Stream<String> tryMatchPackage(String input) {
        Matcher matcher = PACKAGE_DECLARATION_PATTERN.matcher(input);
        return matcher.matches() ? Stream.of(matcher.group(1)) : Stream.empty();
    }

    private static Path convertToRelativePath(String packageName) {
        List<String> parts = Splitter.on(".").splitToList(packageName);
        return Paths.get(parts.get(0), parts.subList(1, parts.size()).toArray(new String[0]));
    }

    private static void executeCompile(Path javaSourceFile) throws UnsupportedEncodingException {
        ByteArrayOutputStream toolSysOut = new ByteArrayOutputStream();
        ByteArrayOutputStream toolSysErr = new ByteArrayOutputStream();
        int exitCode = ToolProvider.getSystemJavaCompiler().run(null, toolSysOut, toolSysErr, javaSourceFile.toString());
        if (exitCode != 0) {
            throw new AssertionError(String.format(
                    "Couldn't compile java file. file: %s%nOUT: %s%nERR: %s",
                    javaSourceFile, toolSysOut.toString(UTF_8.name()), toolSysErr.toString(UTF_8.name())));
        }
    }

    private static void copy(Path sourceDir, Path targetDir, Predicate<String> fileNamePredicate) {
        try (Stream<Path> files = unchecked(() -> Files.list(sourceDir))) {
            files.filter(it -> fileNamePredicate.test(it.getFileName().toString()))
                    .forEach(it -> unchecked(() -> Files.copy(it, targetDir.resolve(it.getFileName()))));
        }
    }

    public static class CompiledClasses {
        private final Path classFileDir;

        CompiledClasses(Path classFileDir) {
            this.classFileDir = classFileDir;
        }

        /**
         * Removes all files in the class file folder where the file name doesn't match {@code onlyKeepFiles}
         * @param onlyKeepFiles A predicate to determine which classes to keep in the class file folder
         * @return The compiled classes
         */
        public CompiledClasses onlyKeep(Predicate<String> onlyKeepFiles) {
            unchecked(() -> {
                Files.walkFileTree(classFileDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (Files.isRegularFile(file) && !onlyKeepFiles.test(file.getFileName().toString())) {
                            Files.delete(file);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            });
            return this;
        }

        public Path getPath() {
            return classFileDir;
        }
    }
}
