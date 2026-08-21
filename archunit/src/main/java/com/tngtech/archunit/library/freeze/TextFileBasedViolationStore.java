/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.Files.toByteArray;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.ensureUnixLineBreaks;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * A text file based implementation of a {@link ViolationStore}.<br>
 * This {@link ViolationStore} will store the violations of every single {@link FreezingArchRule} in a dedicated file.<br>
 * It will keep an index of all stored rules as well as a mapping to the individual rule violation files in the same folder.<br>
 * By default, the layout within the configured store folder will look like:
 * <pre><code>
 * storeFolder
 *   |-- stored.rules (the index file of all stored rules)
 *   |-- 6fc2fd04-b3ab-44e0-8f78-215c66f2174a (a rule violation file named randomly by UUID and referenced from stored.rules)
 *   |-- 2186b43a-c24c-417d-bd96-547e2dfdba1c (another rule violation file)
 *   |-- ... (more rule violation files for every rule that has been stored so far)
 * </code></pre>
 * To adjust the strategy how the individual rule violation files are named use the constructor
 * {@link TextFileBasedViolationStore#TextFileBasedViolationStore(RuleViolationFileNameStrategy) TextFileBasedViolationStore(RuleViolationFileNameStrategy)}.<br>
 * This {@link ViolationStore} can be configured through the following properties:
 * <pre><code>
 * default.path=...               # string: the path of the folder where violation files will be stored
 * default.allowStoreCreation=... # boolean: whether to allow creating a new index file
 * default.allowStoreUpdate=...   # boolean: whether to allow updating any store file
 * </code></pre>
 */
@PublicAPI(usage = ACCESS)
public final class TextFileBasedViolationStore implements ViolationStore {
    private static final Logger log = LoggerFactory.getLogger(TextFileBasedViolationStore.class);

    private static final Pattern UNESCAPED_LINE_BREAK_PATTERN = Pattern.compile("(?<!\\\\)\n");
    private static final String STORE_PATH_PROPERTY_NAME = "default.path";
    private static final String STORE_PATH_DEFAULT = "archunit_store";
    private static final String STORED_RULES_FILE_NAME = "stored.rules";
    private static final String ALLOW_STORE_CREATION_PROPERTY_NAME = "default.allowStoreCreation";
    private static final String ALLOW_STORE_CREATION_DEFAULT = "false";
    private static final String ALLOW_STORE_UPDATE_PROPERTY_NAME = "default.allowStoreUpdate";
    private static final String ALLOW_STORE_UPDATE_DEFAULT = "true";
    private static final String DELETE_EMPTY_RULE_VIOLATION_PROPERTY_NAME = "default.deleteEmptyRuleViolation";
    private static final String DELETE_EMPTY_RULE_VIOLATION_DEFAULT = "false";
    private static final String WARN_EMPTY_RULE_VIOLATION_PROPERTY_NAME = "default.warnEmptyRuleViolation";
    private static final String WARN_EMPTY_RULE_VIOLATION_DEFAULT = "false";

    private static final ConcurrentHashMap<String, FileSyncedProperties> STORED_RULES_BY_PATH = new ConcurrentHashMap<>();

    private final RuleViolationFileNameStrategy ruleViolationFileNameStrategy;

    private boolean storeCreationAllowed;
    private boolean storeUpdateAllowed;
    private boolean deleteEmptyRule;
    private boolean warnEmptyRuleViolation;
    private File storeFolder;
    private FileSyncedProperties storedRules;

    /**
     * Creates a standard {@link TextFileBasedViolationStore} that names rule violation files by random {@link UUID}s
     *
     * @see #TextFileBasedViolationStore(RuleViolationFileNameStrategy)
     */
    public TextFileBasedViolationStore() {
        this(__ -> UUID.randomUUID().toString());
    }

    /**
     * Creates a {@link TextFileBasedViolationStore} with a custom strategy for rule violation file naming
     *
     * @param ruleViolationFileNameStrategy controls how the rule violation file name is derived from the rule description
     */
    public TextFileBasedViolationStore(RuleViolationFileNameStrategy ruleViolationFileNameStrategy) {
        this.ruleViolationFileNameStrategy = ruleViolationFileNameStrategy;
    }

    @Override
    public void initialize(Properties properties) {
        storeCreationAllowed = Boolean.parseBoolean(properties.getProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, ALLOW_STORE_CREATION_DEFAULT));
        storeUpdateAllowed = Boolean.parseBoolean(properties.getProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, ALLOW_STORE_UPDATE_DEFAULT));
        deleteEmptyRule = Boolean.parseBoolean(properties.getProperty(DELETE_EMPTY_RULE_VIOLATION_PROPERTY_NAME, DELETE_EMPTY_RULE_VIOLATION_DEFAULT));
        warnEmptyRuleViolation = Boolean.parseBoolean(properties.getProperty(WARN_EMPTY_RULE_VIOLATION_PROPERTY_NAME, WARN_EMPTY_RULE_VIOLATION_DEFAULT));
        String path = properties.getProperty(STORE_PATH_PROPERTY_NAME, STORE_PATH_DEFAULT);
        storeFolder = new File(path);
        File storedRulesFile = getStoredRulesFile();
        log.trace("Initializing {} at {}", TextFileBasedViolationStore.class.getSimpleName(), storedRulesFile.getAbsolutePath());
        storedRules = getOrCreateStoredRules(storedRulesFile);
        checkInitialization(storedRules.initializationSuccessful(), "Cannot create rule store at %s", storedRulesFile.getAbsolutePath());
    }

    private FileSyncedProperties getOrCreateStoredRules(File storedRulesFile) {
        try {
            return STORED_RULES_BY_PATH.computeIfAbsent(storedRulesFile.getCanonicalPath(), path -> new FileSyncedProperties(storedRulesFile));
        } catch (IOException e) {
            throw new StoreInitializationFailedException(e);
        }
    }

    private File getStoredRulesFile() {
        File rulesFile = new File(storeFolder, STORED_RULES_FILE_NAME);
        if (!rulesFile.exists() && !storeCreationAllowed) {
            throw new StoreInitializationFailedException(String.format(
                    "Creating new violation store is disabled (enable by configuration %s.%s=true)",
                    ViolationStoreFactory.FREEZE_STORE_PROPERTY_NAME, ALLOW_STORE_CREATION_PROPERTY_NAME));
        }
        return rulesFile;
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
        log.trace("Storing evaluated rule '{}' with {} violations: {}", rule.getDescription(), violations.size(), violations);
        if (violations.isEmpty() && warnEmptyRuleViolation) {
            throw new StoreEmptyException(String.format("Saving empty violations for freezing rule is disabled (enable by configuration %s.%s=true)",
                    ViolationStoreFactory.FREEZE_STORE_PROPERTY_NAME, WARN_EMPTY_RULE_VIOLATION_PROPERTY_NAME));
        }
        if (violations.isEmpty() && deleteEmptyRule && !contains(rule)) {
            // do nothing, new rule file should not be created
            return;
        }
        if (!storeUpdateAllowed) {
            throw new StoreUpdateFailedException(String.format(
                    "Updating frozen violations is disabled (enable by configuration %s.%s=true)",
                    ViolationStoreFactory.FREEZE_STORE_PROPERTY_NAME, ALLOW_STORE_UPDATE_PROPERTY_NAME));
        }
        if (violations.isEmpty() && deleteEmptyRule) {
            deleteRuleFile(rule);
            return;
        }

        String ruleFileName = ensureRuleFileName(rule);
        write(violations, new File(storeFolder, ruleFileName));
    }

    private void deleteRuleFile(ArchRule rule) {
        try {
            String ruleFileName = storedRules.getProperty(rule.getDescription());
            Files.delete(storeFolder.toPath().resolve(ruleFileName));
        } catch (IOException e) {
            throw new StoreUpdateFailedException(e);
        }
        storedRules.removeProperty(rule.getDescription());
    }

    private void write(List<String> violations, File ruleDetails) {
        StringBuilder builder = new StringBuilder();
        for (String violation : violations) {
            builder.append(escape(violation)).append("\n");
        }
        try {
            Files.write(ruleDetails.toPath(), builder.toString().getBytes(UTF_8));
        } catch (IOException e) {
            throw new StoreUpdateFailedException(e);
        }
    }

    private String escape(String violation) {
        return violation.replace("\n", "\\\n");
    }

    private String unescape(String violation) {
        return violation.replace("\\\n", "\n");
    }

    private String ensureRuleFileName(ArchRule rule) {
        String ruleDescription = rule.getDescription();
        String candidateFileName = ruleViolationFileNameStrategy.createRuleFileName(ruleDescription);
        String existingFileName = storedRules.putIfAbsent(ruleDescription, candidateFileName);
        if (existingFileName == null) {
            log.trace("Assigning new file {} to rule '{}'", candidateFileName, ruleDescription);
            return candidateFileName;
        }
        log.trace("Rule '{}' is already stored in file {}", ruleDescription, existingFileName);
        return existingFileName;
    }

    @Override
    public List<String> getViolations(ArchRule rule) {
        String ruleDetailsFileName = storedRules.getProperty(rule.getDescription());
        checkArgument(ruleDetailsFileName != null, "No rule stored with description '%s'", rule.getDescription());
        List<String> result = readLines(ruleDetailsFileName);
        log.trace("Retrieved stored rule '{}' with {} violations: {}", rule.getDescription(), result.size(), result);
        return result;
    }

    private List<String> readLines(String ruleDetailsFileName) {
        String violationsText = readStoreFile(ruleDetailsFileName);
        return Splitter.on(UNESCAPED_LINE_BREAK_PATTERN).omitEmptyStrings().splitToStream(violationsText)
                .map(this::unescape)
                .collect(toList());
    }

    private String readStoreFile(String fileName) {
        try {
            String result = new String(toByteArray(new File(storeFolder, fileName)), UTF_8);
            return ensureUnixLineBreaks(result);
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
            try {
                File directory = file.getParentFile();

                // mkdirs() returns false both on failure and if another process concurrently
                // created the directory, so isDirectory() distinguishes the two
                if (!directory.mkdirs() && !directory.isDirectory()) {
                    return null;
                }

                if (!file.exists() && !file.createNewFile()) {
                    return null;
                }

                return file;
            } catch (IOException e) {
                return null;
            }
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
            return loadedProperties.containsKey(ensureUnixLineBreaks(propertyName));
        }

        String getProperty(String propertyName) {
            return loadedProperties.getProperty(ensureUnixLineBreaks(propertyName));
        }

        synchronized String putIfAbsent(String key, String value) {
            String normalizedKey = ensureUnixLineBreaks(key);
            String existing = loadedProperties.getProperty(normalizedKey);
            if (existing != null) {
                return existing;
            }
            loadedProperties.setProperty(normalizedKey, ensureUnixLineBreaks(value));
            syncFileSystem();
            return null;
        }

        void removeProperty(String propertyName) {
            loadedProperties.remove(ensureUnixLineBreaks(propertyName));
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

    /**
     * Allows to adjust the rule violation file names of {@link TextFileBasedViolationStore}
     *
     * @see TextFileBasedViolationStore#TextFileBasedViolationStore(RuleViolationFileNameStrategy)
     */
    @FunctionalInterface
    @PublicAPI(usage = INHERITANCE)
    public interface RuleViolationFileNameStrategy {
        /**
         * Returns the file name to store violations of an {@link ArchRule}, possibly based on the rule description.<br>
         * The returned names <b>must</b> be sufficiently unique from any others;
         * as long as the descriptions themselves are unique, this can be achieved by sanitizing the description into some sort of file name.
         *
         * @param ruleDescription The description of the {@link ArchRule} to store
         * @return The file name the respective rule violation file will have (see {@link TextFileBasedViolationStore})
         */
        String createRuleFileName(String ruleDescription);
    }
}
