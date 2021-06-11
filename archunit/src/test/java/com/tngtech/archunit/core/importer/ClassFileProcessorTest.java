package com.tngtech.archunit.core.importer;

import java.lang.reflect.Field;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolverFromClasspath;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static com.google.common.collect.Iterables.getLast;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ClassFileProcessorTest {
    @Test
    public void ClassResolverFromClassPath_resolves_robustly() {
        Optional<JavaClass> resolved = new ClassResolverFromClasspath()
                .tryResolve("not.There");

        assertThat(resolved).isAbsent();
    }

    @Test
    public void always_use_the_highest_ASM_API_version() throws IllegalAccessException {
        Pattern asmApiFieldNamePattern = Pattern.compile("ASM(\\d+)");
        SortedMap<Integer, Field> availableAsmApiFieldsBySortedVersion = new TreeMap<>();
        for (Field field : Opcodes.class.getFields()) {
            Matcher matcher = asmApiFieldNamePattern.matcher(field.getName());
            if (matcher.matches()) {
                availableAsmApiFieldsBySortedVersion.put(Integer.parseInt(matcher.group(1)), field);
            }
        }

        Field maxAvailableAsmApiVersionField = getLast(availableAsmApiFieldsBySortedVersion.values());
        int maxAvailableAsmApiVersion = (int) maxAvailableAsmApiVersionField.get(null);

        assertThat(ClassFileProcessor.ASM_API_VERSION).as("used ASM API version").isEqualTo(maxAvailableAsmApiVersion);
    }
}
