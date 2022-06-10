package com.tngtech.archunit.tooling.utils;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;

public abstract class ResourcesUtils {

    public static void copyResources(Path projectRoot, String parentDirectory, String resourcePattern, String target) throws IOException, URISyntaxException {
        for (Path path : findResourcesByPattern(parentDirectory, resourcePattern)) {
            copyResource(projectRoot, path.toString(), target, path);
        }
    }

    public static void copyResourceDirectory(Path projectRoot, String directoryName, String target) throws URISyntaxException, IOException {
        Path targetDirectory = projectRoot.resolve(target);
        Files.createDirectories(targetDirectory);
        FileUtils.copyDirectory(getResourceDirectory(directoryName).toFile(), targetDirectory.toFile());
    }

    public static void copyResources(Path projectRoot, String resourcePattern, String target) throws IOException, URISyntaxException {
        copyResources(projectRoot, "", resourcePattern, target);
    }

    private static Iterable<Path> findResourcesByPattern(String parentDirectory, String resourcePattern) throws URISyntaxException, IOException {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(resourcePattern);
        Path resourceRoot = getResourceDirectory(parentDirectory);
        return Files.find(resourceRoot, 10, (path, attributes) -> pathMatcher.matches(resourceRoot.relativize(path)))::iterator;
    }

    private static Path getResourceDirectory(String name) throws URISyntaxException {
        return Paths.get(getResourceUri(name));
    }

    public static URI getResourceUri(String name) throws URISyntaxException {
        return ResourcesUtils.class.getClassLoader().getResource(name).toURI();
    }

    private static void copyResource(Path projectRoot, String resourceName, String target, Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            copyResource(projectRoot, inputStream, resourceName, target);
        }
    }

    public static void copyResource(Path projectRoot, String resourceName, String target) throws IOException {
        try (InputStream inputStream = ResourcesUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            copyResource(projectRoot, inputStream, resourceName, target);
        }
    }

    private static void copyResource(Path projectRoot, InputStream source, String resourceName, String target) throws IOException {
        Path targetPath = projectRoot.resolve(target);
        Files.createDirectories(targetPath);
        Files.copy(Objects.requireNonNull(source), targetPath.resolve(Paths.get(resourceName).getFileName()));
    }
}
