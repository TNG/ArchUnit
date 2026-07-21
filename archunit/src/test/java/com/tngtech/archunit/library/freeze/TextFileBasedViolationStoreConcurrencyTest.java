package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class TextFileBasedViolationStoreConcurrencyTest {

    @TempDir
    Path temporaryFolder;

    private File configuredFolder;

    @BeforeEach
    public void setUp() {
        configuredFolder = new File(temporaryFolder.toFile(), "notyetthere");
    }

    @RepeatedTest(5)
    public void is_safe_when_multiple_instances_initialize_concurrently() throws Exception {
        runConcurrently(8, (i, startSignal) -> {
            ViolationStore instanceStore = new TextFileBasedViolationStore();
            startSignal.await();
            instanceStore.initialize(defaultStoreProperties());
        });

        // The real test here is that StoreInitializationFailedException is not thrown
        assertThat(configuredFolder).exists();
    }

    @Test
    public void is_safe_when_multiple_instances_save_to_the_same_store_concurrently() throws Exception {
        int ruleCount = 20;

        runConcurrently(ruleCount, (i, startSignal) -> {
            ViolationStore instanceStore = new TextFileBasedViolationStore();
            instanceStore.initialize(defaultStoreProperties());
            startSignal.await();
            instanceStore.save(rule("concurrent rule " + i), ImmutableList.of("violation " + i));
        });

        Properties storedRules = readProperties(new File(configuredFolder, "stored.rules"));
        assertThat(storedRules).hasSize(ruleCount);

        ViolationStore readerStore = new TextFileBasedViolationStore();
        readerStore.initialize(defaultStoreProperties());
        for (int i = 0; i < ruleCount; i++) {
            ArchRule concurrentRule = rule("concurrent rule " + i);
            assertThat(readerStore.contains(concurrentRule)).as("store contains " + concurrentRule.getDescription()).isTrue();
            assertThat(readerStore.getViolations(concurrentRule)).containsExactly("violation " + i);
        }
    }

    @SuppressWarnings("resource") // only available in Java 19 and later
    private void runConcurrently(int taskCount, ConcurrentTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<?>> futures = IntStream.range(0, taskCount)
              .mapToObj(i -> executor.submit(task.toCallable(i, startSignal)))
              .collect(Collectors.toList());

        startSignal.countDown();
        executor.shutdown();

        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // exceptions thrown inside submitted tasks are swallowed unless we call get() on each future
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private Properties defaultStoreProperties() {
        return propertiesOf(
                "default.path", configuredFolder.getAbsolutePath(),
                "default.allowStoreCreation", String.valueOf(true));
    }

    private Properties propertiesOf(String... keyValuePairs) {
        Properties result = new Properties();
        LinkedList<String> keyValues = new LinkedList<>(asList(keyValuePairs));
        while (!keyValues.isEmpty()) {
            result.setProperty(keyValues.poll(), keyValues.poll());
        }
        return result;
    }

    private Properties readProperties(File file) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private ArchRule rule(String description) {
        return classes().should().bePublic().as(description);
    }

    @FunctionalInterface
    private interface ConcurrentTask {
        void run(int ruleIndex, CountDownLatch startSignal) throws Exception;

        default Callable<Void> toCallable(int ruleIndex, CountDownLatch startSignal) {
            return () -> {
                this.run(ruleIndex, startSignal);
                return null;
            };
        }
    }
}
