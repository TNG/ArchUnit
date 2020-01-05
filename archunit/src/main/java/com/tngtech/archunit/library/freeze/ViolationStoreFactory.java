/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.toByteArray;
import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;
import static java.nio.charset.StandardCharsets.UTF_8;

class ViolationStoreFactory {
    static final String FREEZE_STORE_PROPERTY_NAME = "freeze.store";

    static ViolationStore create() {
        return ArchConfiguration.get().containsProperty(FREEZE_STORE_PROPERTY_NAME)
                ? createInstance(ArchConfiguration.get().getProperty(FREEZE_STORE_PROPERTY_NAME))
                : new TextFileBasedViolationStore();
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static ViolationStore createInstance(String violationStoreClassName) {
        try {
            return (ViolationStore) newInstanceOf(Class.forName(violationStoreClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    ViolationStore.class.getSimpleName(), FREEZE_STORE_PROPERTY_NAME, violationStoreClassName);
            throw new StoreInitializationFailedException(message, e);
        }
    }

    @VisibleForTesting
    static class TextFileBasedViolationStore implements ViolationStore {
        private static final Logger log = LoggerFactory.getLogger(TextFileBasedViolationStore.class);

        private static final Pattern UNESCAPED_LINE_BREAK_PATTERN = Pattern.compile("(?<!\\\\)\n");
        private static final String STORE_PATH_PROPERTY_NAME = "default.path";
        private static final String STORE_PATH_DEFAULT = "archunit_store";
        private static final String STORED_RULES_FILE_NAME = "stored.rules";
        private static final String ALLOW_STORE_CREATION_PROPERTY_NAME = "default.allowStoreCreation";
        private static final String ALLOW_STORE_CREATION_DEFAULT = "false";
        private static final String ALLOW_STORE_UPDATE_PROPERTY_NAME = "default.allowStoreUpdate";
        private static final String ALLOW_STORE_UPDATE_DEFAULT = "true";

        private boolean storeCreationAllowed;
        private boolean storeUpdateAllowed;
        private File storeFolder;
        private FileSyncedProperties storedRules;

        @Override
        public void initialize(Properties properties) {
            storeCreationAllowed = Boolean.parseBoolean(properties.getProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, ALLOW_STORE_CREATION_DEFAULT));
            storeUpdateAllowed = Boolean.parseBoolean(properties.getProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, ALLOW_STORE_UPDATE_DEFAULT));
            String path = properties.getProperty(STORE_PATH_PROPERTY_NAME, STORE_PATH_DEFAULT);
            storeFolder = new File(path);
            ensureExistence(storeFolder);
            File storedRulesFile = getStoredRulesFile();
            log.info("Initializing {} at {}", TextFileBasedViolationStore.class.getSimpleName(), storedRulesFile.getAbsolutePath());
            storedRules = new FileSyncedProperties(storedRulesFile);
            checkInitialization(storedRules.initializationSuccessful(), "Cannot create rule store at %s", storedRulesFile.getAbsolutePath());
        }

        private File getStoredRulesFile() {
            File rulesFile = new File(storeFolder, STORED_RULES_FILE_NAME);
            if (!rulesFile.exists() && !storeCreationAllowed) {
                throw new StoreInitializationFailedException(String.format(
                        "Creating new violation store is disabled (enable by configuration %s.%s=true)",
                        FREEZE_STORE_PROPERTY_NAME, ALLOW_STORE_CREATION_PROPERTY_NAME));
            }
            return rulesFile;
        }

        private void ensureExistence(File folder) {
            checkState(folder.exists() && folder.isDirectory() || folder.mkdirs(), "Cannot create folder %s", folder.getAbsolutePath());
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
            log.debug("Storing evaluated rule '{}' with {} violations: {}", rule.getDescription(), violations.size(), violations);
            if (!storeUpdateAllowed) {
                throw new StoreUpdateFailedException(String.format(
                        "Updating frozen violations is disabled (enable by configuration %s.%s=true)",
                        FREEZE_STORE_PROPERTY_NAME, ALLOW_STORE_UPDATE_PROPERTY_NAME));
            }
            String ruleFileName = ensureRuleFileName(rule);
            write(violations, new File(storeFolder, ruleFileName));
        }

        private void write(List<String> violations, File ruleDetails) {
            String updatedViolations = Joiner.on("\n").join(escape(violations));
            try {
                Files.write(updatedViolations, ruleDetails, UTF_8);
            } catch (IOException e) {
                throw new StoreUpdateFailedException(e);
            }
        }

        private List<String> escape(List<String> violations) {
            return replaceCharacter(violations, "\n", "\\\n");
        }

        private List<String> unescape(List<String> violations) {
            return replaceCharacter(violations, "\\\n", "\n");
        }

        private List<String> replaceCharacter(List<String> violations, String characterToReplace, String replacement) {
            List<String> result = new ArrayList<>();
            for (String violation : violations) {
                result.add(violation.replace(characterToReplace, replacement));
            }
            return result;
        }

        private String ensureRuleFileName(ArchRule rule) {
            String ruleFileName;
            if (storedRules.containsKey(rule.getDescription())) {
                ruleFileName = storedRules.getProperty(rule.getDescription());
                log.debug("Rule '{}' is already stored in file {}", rule.getDescription(), ruleFileName);
            } else {
                ruleFileName = createNewRuleId(rule).toString();
            }
            return ruleFileName;
        }

        private UUID createNewRuleId(ArchRule rule) {
            UUID ruleId = UUID.randomUUID();
            log.debug("Assigning new ID {} to rule '{}'", ruleId, rule.getDescription());
            storedRules.setProperty(rule.getDescription(), ruleId.toString());
            return ruleId;
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            String ruleDetailsFileName = storedRules.getProperty(rule.getDescription());
            checkArgument(ruleDetailsFileName != null, "No rule stored with description '%s'", rule.getDescription());
            List<String> result = readLines(ruleDetailsFileName);
            log.debug("Retrieved stored rule '{}' with {} violations: {}", rule.getDescription(), result.size(), result);
            return result;
        }

        private List<String> readLines(String ruleDetailsFileName) {
            String violationsText = readStoreFile(ruleDetailsFileName);
            List<String> lines = Splitter.on(UNESCAPED_LINE_BREAK_PATTERN).splitToList(violationsText);
            return unescape(lines);
        }

        private String readStoreFile(String fileName) {
            try {
                String result = new String(toByteArray(new File(storeFolder, fileName)), UTF_8);
                return ensureUnixLineBreaks(result);
            } catch (IOException e) {
                throw new StoreReadException(e);
            }
        }

        private String ensureUnixLineBreaks(String string) {
            return string.replaceAll("\r\n", "\n");
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
