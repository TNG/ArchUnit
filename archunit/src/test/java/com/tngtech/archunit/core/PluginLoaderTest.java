package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_14;
import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_9;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginLoaderTest {
    private static final Class<?> pluginTypeBeforeJava9 = HashSet.class;
    private static final Class<?> pluginTypeBetweenJava9AndJava13 = ArrayList.class;
    private static final Class<?> pluginTypeAfterJava13 = HashMap.class;

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void loads_correct_plugin_for_version() {
        System.setProperty("java.version", "1.7.0_55");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBeforeJava9);

        System.setProperty("java.version", "1.8.0_122");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBeforeJava9);

        System.setProperty("java.version", "9");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBetweenJava9AndJava13);

        System.setProperty("java.version", "9.0.1");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBetweenJava9AndJava13);

        System.setProperty("java.version", "11-ea");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBetweenJava9AndJava13);

        System.setProperty("java.version", "13");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeBetweenJava9AndJava13);

        System.setProperty("java.version", "14");
        assertThat(createPluginLoader().load()).isInstanceOf(pluginTypeAfterJava13);
    }

    // PluginLoader memoizes the loaded plugin
    private PluginLoader<Object> createPluginLoader() {
        return PluginLoader.forType(Object.class)
                .ifVersionGreaterOrEqualTo(JAVA_9).load(pluginTypeBetweenJava9AndJava13.getName())
                .ifVersionGreaterOrEqualTo(JAVA_14).load(pluginTypeAfterJava13.getName())
                .fallback(newInstance(pluginTypeBeforeJava9));
    }

    private Object newInstance(Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
