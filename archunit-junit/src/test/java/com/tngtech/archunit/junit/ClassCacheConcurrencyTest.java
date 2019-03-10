package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.ClassCache.CacheClassFileImporter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@Category(Slow.class)
public class ClassCacheConcurrencyTest {
    private static final int NUM_THREADS = 20;
    private static final List<Class<?>> TEST_CLASSES = Arrays.asList(
            TestClass1.class, TestClass2.class, TestClass3.class, TestClass4.class, TestClass5.class, TestClass6.class
    );

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private CacheClassFileImporter classFileImporter;

    @InjectMocks
    private ClassCache cache = new ClassCache();

    private ExecutorService executorService;

    @Before
    public void setUp() {
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @After
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void concurrent_access() throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executorService.submit(repeatGetClassesToAnalyze(1000)));
        }
        for (Future<?> future : futures) {
            future.get(1, MINUTES);
        }
        verify(classFileImporter, atMost(TEST_CLASSES.size())).importClasses(any(ImportOptions.class), ArgumentMatchers.<Location>anyCollection());
        verifyNoMoreInteractions(classFileImporter);
    }

    private Runnable repeatGetClassesToAnalyze(final int times) {
        return new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < times; j++) {
                    cache.getClassesToAnalyzeFor(TEST_CLASSES.get(j % TEST_CLASSES.size()),
                            new TestAnalysisRequest().withLocationProviders(LocationOfClass.Provider.class));
                }
            }
        };
    }

    @LocationOfClass(FirstClass.class)
    public static class TestClass1 {
    }

    private static class FirstClass {
    }

    @LocationOfClass(SecondClass.class)
    public static class TestClass2 {
    }

    private static class SecondClass {
    }

    @LocationOfClass(ThirdClass.class)
    public static class TestClass3 {
    }

    private static class ThirdClass {
    }

    @LocationOfClass(FourthClass.class)
    public static class TestClass4 {
    }

    private static class FourthClass {
    }

    @LocationOfClass(FifthClass.class)
    public static class TestClass5 {
    }

    private static class FifthClass {
    }

    @LocationOfClass(SixthClass.class)
    public static class TestClass6 {
    }

    private static class SixthClass {
    }
}
