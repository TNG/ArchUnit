package com.tngtech.archunit.core.domain.testobjects;

import java.io.File;
import java.io.FilterInputStream;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class DependenciesOnClassObjects {
    static {
        List<Class<?>> referencedClassObjectsInStaticInitializer = ImmutableList.of(FilterInputStream.class, Buffer.class);
    }

    List<Class<?>> referencedClassObjectsInConstructor = ImmutableList.<Class<?>>of(File.class, Path.class);

    List<Class<?>> referencedClassObjectsInMethod() {
        return ImmutableList.of(FileSystem.class, Charset.class);
    }

    public static class MoreDependencies {
        Class<?> moreReferencedClassObjectsInMethod() {
            return Charset.class;
        }
    }
}
