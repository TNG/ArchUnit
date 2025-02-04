package com.tngtech.archunit.core.domain;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class JavaClassVersionTest {

    @ParameterizedTest
    @MethodSource
    void derives_Java_version_from_class_file_version(int major, int minor, int expectedJavaVersion) {
        JavaClassVersion version = new JavaClassVersion(major, minor);

        assertThat(version.getJavaVersion()).isEqualTo(expectedJavaVersion);
        assertThat(version.getBytecodeMajorVersion()).isEqualTo(major);
        assertThat(version.getBytecodeMinorVersion()).isEqualTo(minor);
    }

    static Stream<Arguments> derives_Java_version_from_class_file_version() {
        return Stream.of(
                arguments(52, 0, 8),
                arguments(55, 0, 11),
                arguments(61, 0, 17),
                arguments(65, 0, 21),
                arguments(69, 0, 25));
    }

    @ParameterizedTest
    @MethodSource
    void unpacks_version_as_provided_by_Asm(int major, int minor, int expectedJavaVersion) {
        // Asm packs the class file version into an int as minor<<16 | major
        int asmVersion = minor << 16 | major;

        JavaClassVersion version = JavaClassVersion.of(asmVersion);

        assertThat(version.getJavaVersion()).isEqualTo(expectedJavaVersion);
        assertThat(version.getBytecodeMajorVersion()).isEqualTo(major);
        assertThat(version.getBytecodeMinorVersion()).isEqualTo(minor);
    }

    static Stream<Arguments> unpacks_version_as_provided_by_Asm() {
        return Stream.of(
                arguments(52, 0, 8),
                arguments(55, 0, 11),
                arguments(61, 0, 17),
                arguments(65, 0, 21),
                arguments(69, 0, 25),
                arguments(52, 1, 8),
                arguments(69, 2, 25));
    }
}
