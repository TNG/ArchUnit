package com.tngtech.archunit.testutil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.testutil.TestUtils.newTemporaryFolder;

public class OutsideOfClassPathRule extends ExternalResource {
    private final TemporaryFolder temporaryFolder = new TemporaryFolder(newTemporaryFolder());
    private Path backup;
    private Path originFolder;
    private Predicate<String> fileNamePredicate = Predicates.alwaysTrue();

    @Override
    public Statement apply(Statement base, Description description) {
        return temporaryFolder.apply(super.apply(base, description), description);
    }

    @SuppressWarnings("unchecked")
    public OutsideOfClassPathRule onlyKeep(Predicate<? super String> fileNamePredicate) {
        this.fileNamePredicate = (Predicate<String>) fileNamePredicate;
        return this;
    }

    public Path setUp(URL folder) throws IOException {
        this.originFolder = new File(folder.getFile()).toPath();
        return moveOutOfClassPath();
    }

    private Path moveOutOfClassPath() throws IOException {
        String classNameToCheck = rememberClassNameForSanityCheck();
        Path result = setupClassesOutsideOfClasspathWithMissingDependencies();
        assertNotPresent(classNameToCheck);
        return result;
    }

    private String rememberClassNameForSanityCheck() throws IOException {
        ClassNameRetrievingVisitor classNameRetriever = new ClassNameRetrievingVisitor();
        Files.walkFileTree(this.originFolder, classNameRetriever);
        return classNameRetriever.className;
    }

    private Path setupClassesOutsideOfClasspathWithMissingDependencies() throws IOException {
        File sourceDir = originFolder.toFile();
        checkFolderFlat(sourceDir);
        backup = temporaryFolder.newFolder().toPath();
        copy(sourceDir, backup);
        Path targetDir = temporaryFolder.newFolder().toPath();
        moveMatchingFilesDeleteRest(sourceDir, targetDir, fileNamePredicate);
        return targetDir;
    }

    private void copy(File sourceDir, Path targetDir) throws IOException {
        for (File file : sourceDir.listFiles()) {
            Files.copy(file.toPath(), targetDir.resolve(file.getName()));
        }
    }

    private void moveMatchingFilesDeleteRest(File sourceDir, Path targetDir, Predicate<String> predicate) throws IOException {
        for (File file : sourceDir.listFiles()) {
            if (predicate.apply(file.getName())) {
                Files.move(file.toPath(), targetDir.resolve(file.getName()));
            } else {
                checkState(file.delete());
            }
        }
    }

    private void checkFolderFlat(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                throw new UnsupportedOperationException(String.format(
                        "Dealing with folders like %s is not implemented yet", file.getAbsolutePath()));
            }
        }
    }

    private void assertNotPresent(String className) {
        Preconditions.checkNotNull(className,
                "Couldn't find any class file in given folder %s", originFolder.toAbsolutePath());
        try {
            Class.forName(className, false, getClass().getClassLoader());
            Assert.fail("Should not be able to load class " + className);
        } catch (ClassNotFoundException expected) {
        }
    }

    @Override
    protected void after() {
        if (backup != null) {
            try {
                moveMatchingFilesDeleteRest(backup.toFile(), originFolder, Predicates.<String>alwaysTrue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ClassNameRetrievingVisitor extends SimpleFileVisitor<Path> {
        private String className;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".class")) {
                className = getClassName(file.toAbsolutePath().toString());
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

        private String getClassName(String filePath) {
            return filePath.replaceAll(".*/com/tngtech/archunit", "com/tngtech/archunit")
                    .replace(".class", "")
                    .replace('/', '.');
        }
    }
}
