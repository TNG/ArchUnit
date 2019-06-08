package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.lang.ArchRule;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.Files.toByteArray;
import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;
import static java.nio.charset.StandardCharsets.UTF_8;

class ViolationStoreFactory {
    static final String FREEZE_STORE_PROPERTY = "freeze.store";

    static ViolationStore create() {
        return ArchConfiguration.get().containsProperty(FREEZE_STORE_PROPERTY)
                ? createInstance(ArchConfiguration.get().getProperty(FREEZE_STORE_PROPERTY))
                : new TextFileBasedViolationStore();
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static ViolationStore createInstance(String violationStoreClassName) {
        try {
            return (ViolationStore) newInstanceOf(Class.forName(violationStoreClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    ViolationStore.class.getSimpleName(), FREEZE_STORE_PROPERTY, violationStoreClassName);
            throw new StoreInitializationFailedException(message, e);
        }
    }

    @VisibleForTesting
    static class TextFileBasedViolationStore implements ViolationStore {
        private static final Pattern UNESCAPED_LINE_BREAK_PATTERN = Pattern.compile("(?<!\\\\)\n");
        private File storeFolder;
        private FileSyncedProperties storedRules;

        @Override
        public void initialize(Properties properties) {
            String path = properties.getProperty("default.path", "archunit_store");
            storeFolder = new File(path);
            File storedRulesFile = new File(storeFolder, "stored.rules");
            storedRules = new FileSyncedProperties(storedRulesFile);
            checkInitialization(storedRules.initializationSuccessful(), "Cannot create rule store at %s", storedRulesFile.getAbsolutePath());
        }

        private void checkInitialization(boolean initializationSuccessful, String message, Object... args) {
            if (!initializationSuccessful) {
                throw new StoreInitializationFailedException(String.format(message, args));
            }
        }

        @Override
        public boolean contains(ArchRule rule) {
            return storedRules.containsKey(rule.getDescription());
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            UUID ruleId = ensureRuleId(rule);
            File ruleDetails = new File(storeFolder, ruleId.toString());
            write(violations, ruleDetails);
        }

        private void write(List<String> violations, File ruleDetails) {
            String updatedViolations = Joiner.on("\n").join(escaped(violations));
            try {
                Files.write(updatedViolations, ruleDetails, UTF_8);
            } catch (IOException e) {
                throw new StoreUpdateFailedException(e);
            }
        }

        private List<String> escaped(List<String> violations) {
            return replaceCharacter(violations, "\n", "\\\n");
        }

        // FIXME: Correct word for 'deescaped'?
        private List<String> deescaped(List<String> violations) {
            return replaceCharacter(violations, "\\\n", "\n");
        }

        private List<String> replaceCharacter(List<String> violations, String characterToReplace, String replacement) {
            List<String> result = new ArrayList<>();
            for (String violation : violations) {
                result.add(violation.replace(characterToReplace, replacement));
            }
            return result;
        }

        private UUID ensureRuleId(ArchRule rule) {
            UUID ruleId;
            if (!storedRules.containsKey(rule.getDescription())) {
                ruleId = createNewRuleId(rule);
            } else {
                ruleId = UUID.fromString(storedRules.getProperty(rule.getDescription()));
            }
            return ruleId;
        }

        private UUID createNewRuleId(ArchRule rule) {
            UUID ruleId = UUID.randomUUID();
            storedRules.setProperty(rule.getDescription(), ruleId.toString());
            return ruleId;
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            String ruleDetailsFileName = storedRules.getProperty(rule.getDescription());
            checkArgument(ruleDetailsFileName != null, "No rule stored with description '%s'", rule.getDescription());
            return readLines(ruleDetailsFileName);
        }

        private List<String> readLines(String ruleDetailsFileName) {
            try {
                String violationsText = new String(toByteArray(new File(storeFolder, ruleDetailsFileName)), UTF_8);
                List<String> lines = Splitter.on(UNESCAPED_LINE_BREAK_PATTERN).splitToList(violationsText);
                return deescaped(lines);
            } catch (IOException e) {
                throw new StoreReadException(e);
            }
        }

        private static class FileSyncedProperties {
            private final File propertiesFile;
            private final Properties loadedProperties;

            FileSyncedProperties(File file) {
                propertiesFile = initializePropertiesFile(file);
                loadedProperties = initializationSuccessful() ? loadRulesFrom(propertiesFile) : null;
            }

            boolean initializationSuccessful() {
                return propertiesFile != null;
            }

            private File initializePropertiesFile(File file) {
                boolean fileAvailable;
                try {
                    fileAvailable = file.exists() || file.createNewFile();
                } catch (IOException e) {
                    fileAvailable = false;
                }
                return fileAvailable ? file : null;
            }

            private Properties loadRulesFrom(File file) {
                Properties result = new Properties();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    result.load(inputStream);
                } catch (IOException e) {
                    throw new StoreInitializationFailedException(e);
                }
                return result;
            }

            boolean containsKey(String propertyName) {
                return loadedProperties.containsKey(propertyName);
            }

            String getProperty(String propertyName) {
                return loadedProperties.getProperty(propertyName);
            }

            void setProperty(String propertyName, String value) {
                loadedProperties.setProperty(propertyName, value);
                syncFileSystem();
            }

            private void syncFileSystem() {
                try (FileOutputStream outputStream = new FileOutputStream(propertiesFile)) {
                    loadedProperties.store(outputStream, "");
                } catch (IOException e) {
                    throw new StoreUpdateFailedException(e);
                }
            }
        }
    }
}
