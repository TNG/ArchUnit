package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static com.tngtech.archunit.core.domain.SourceTest.urlOf;
import static org.assertj.core.api.Assertions.assertThat;

public class ModuleLocationFactoryTest {
    private ModuleLocationFactory locationFactory = new ModuleLocationFactory();

    @Test
    public void iterates_package_of_jrt() throws URISyntaxException {
        URI jrtJavaIoFile = uriOf(File.class);
        Location jrtJavaIo = locationFactory.create(parentOf(jrtJavaIoFile));

        assertThat(jrtJavaIo.iterateEntries())
                .contains(NormalizedResourceName.from(File.class.getName().replace('.', '/') + ".class"));
    }

    @Test
    public void iterates_entire_jrt() throws URISyntaxException {
        Location jrtContainingFile = locationFactory.create(createModuleUriContaining(File.class));

        assertThat(jrtContainingFile.iterateEntries())
                .contains(NormalizedResourceName.from(File.class.getName().replace('.', '/') + ".class"));
    }

    @Test
    public void respects_import_options() throws URISyntaxException {
        Location jrtContainingFile = locationFactory.create(createModuleUriContaining(File.class));

        ClassFileSource fileSource = jrtContainingFile.asClassFileSource(new ImportOptions()
                .with(location -> !location.contains("/" + File.class.getSimpleName() + ".class")));

        Set<URI> urisToImport = new HashSet<>();
        fileSource.forEach(classFileLocation -> urisToImport.add(classFileLocation.getUri()));

        assertThat(urisToImport)
                .contains(uriOf(FileReader.class))
                .doesNotContain(uriOf(File.class));
    }

    private URI createModuleUriContaining(Class<?> clazz) throws URISyntaxException {
        URI someJrt = uriOf(clazz);
        String moduleUri = someJrt.toString().replaceAll("(jrt:/[^/]+).*", "$1");
        return URI.create(moduleUri);
    }

    private URI parentOf(URI uri) {
        return URI.create(uri.toString().replaceAll("/[^/]+$", ""));
    }

    private URI uriOf(Class<?> clazz) throws URISyntaxException {
        return urlOf(clazz).toURI();
    }
}
