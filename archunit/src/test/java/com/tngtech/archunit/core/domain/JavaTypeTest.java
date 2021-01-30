package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaType.Functions.TO_ERASURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaTypeTest {

    @Test
    public void function_TO_ERASURE() {
        JavaType javaType = mock(JavaType.class);
        JavaClass erasure = new ClassFileImporter().importClass(getClass());
        when(javaType.toErasure()).thenReturn(erasure);

        assertThat(TO_ERASURE.apply(javaType)).isEqualTo(erasure);
    }
}
