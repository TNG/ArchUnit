package com.tngtech.archunit.testutil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.util.Files;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.util.Files.temporaryFolderPath;
import static org.assertj.core.util.Strings.concat;

public class TestUtils {
    private static final Random random = new Random();

    /**
     * NOTE: The resolution of {@link Files#newTemporaryFolder()}, using {@link System#currentTimeMillis()}
     * is not good enough and makes tests flaky.
     */
    public static File newTemporaryFolder() {
        String folderName = "archtmp" + System.nanoTime() + random.nextLong();
        File folder = new File(concat(temporaryFolderPath(), folderName));
        if (folder.exists()) {
            Files.delete(folder);
        }
        checkArgument(folder.mkdirs(), "Folder %s already exists", folder.getAbsolutePath());
        folder.deleteOnExit();
        return folder;
    }

    public static Object invoke(Method method, Object owner, Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(owner, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties singleProperty(String key, String value) {
        Properties result = new Properties();
        result.setProperty(key, value);
        return result;
    }

    public static Properties properties(String... keyValues) {
        Properties result = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    public static Set<String> namesOf(HasName[] thingsWithNames) {
        return namesOf(ImmutableSet.copyOf(thingsWithNames));
    }

    public static Set<String> namesOf(Iterable<? extends HasName> thingsWithNames) {
        Set<String> result = new LinkedHashSet<>();
        for (HasName hasName : thingsWithNames) {
            result.add(hasName.getName());
        }
        return result;
    }
}
