package com.tngtech.archunit.tooling.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.tngtech.archunit.tooling.engines.surefire.MavenSurefireEngine;
import org.apache.maven.surefire.shared.io.FileUtils;

public abstract class TemporaryDirectoryUtils {

    private TemporaryDirectoryUtils() {}

    public static <R> R withTemporaryDirectory(ThrowableFunction<Path, R> action, String prefix) throws Exception {
        Path projectRoot = null;
        try {
            projectRoot = Files.createTempDirectory(prefix);
            return action.apply(projectRoot);
        } finally {
            FileUtils.deleteDirectory(Objects.requireNonNull(projectRoot).toFile());
        }
    }

    @FunctionalInterface
    public interface ThrowableFunction<T, R> {

        R apply(T t) throws Exception;

    }
}
