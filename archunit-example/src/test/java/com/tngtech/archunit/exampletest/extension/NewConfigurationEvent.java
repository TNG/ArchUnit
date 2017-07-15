package com.tngtech.archunit.exampletest.extension;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;

public class NewConfigurationEvent {
    private final ImmutableMap<String, String> properties;

    NewConfigurationEvent(Properties properties) {
        this.properties = copy(properties);
    }

    private ImmutableMap<String, String> copy(Properties props) {
        ImmutableMap.Builder<String, String> result = ImmutableMap.builder();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            checkArgument(entry.getKey() instanceof String, "Found non String key in properties");
            checkArgument(entry.getValue() instanceof String, "Found non String value in properties");
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result.build();
    }

    public ImmutableMap<String, String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NewConfigurationEvent other = (NewConfigurationEvent) obj;
        return Objects.equals(this.properties, other.properties);
    }

    @Override
    public String toString() {
        return "NewConfigurationEvent{" +
                "props=" + properties +
                '}';
    }
}
