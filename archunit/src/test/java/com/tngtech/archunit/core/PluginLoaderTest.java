package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.HashSet;

import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_9;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginLoaderTest {
    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void loads_correct_plugin_for_version() {
        System.setProperty("java.version", "1.7.0_55");
        assertThat(loadsArrayListForJava9FallbackHashSet().load()).isInstanceOf(HashSet.class);

        System.setProperty("java.version", "1.8.0_122");
        assertThat(loadsArrayListForJava9FallbackHashSet().load()).isInstanceOf(HashSet.class);

        System.setProperty("java.version", "9");
        assertThat(loadsArrayListForJava9FallbackHashSet().load()).isInstanceOf(ArrayList.class);

        System.setProperty("java.version", "9.0.1");
        assertThat(loadsArrayListForJava9FallbackHashSet().load()).isInstanceOf(ArrayList.class);

        System.setProperty("java.version", "11-ea");
        assertThat(loadsArrayListForJava9FallbackHashSet().load()).isInstanceOf(ArrayList.class);
    }

    // PluginLoader memoizes the loaded plugin
    private PluginLoader<Object> loadsArrayListForJava9FallbackHashSet() {
        return PluginLoader.forType(Object.class)
                .ifVersionGreaterOrEqualTo(JAVA_9).load(ArrayList.class.getName())
                .fallback(new HashSet<>());
    }
}