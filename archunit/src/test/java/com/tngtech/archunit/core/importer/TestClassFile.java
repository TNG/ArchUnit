package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.testutil.TestUtils.newTemporaryFolder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class TestClassFile {
    private static final String PACKAGE_NAME = "com.dummy";
    private static final String SOURCE_FOLDER = PACKAGE_NAME.replace(".", File.separator) + File.separator;
    private static final String CLASS_NAME = "Dummy";
    private final File classpathRoot = newTemporaryFolder();
    private final File sourceFile = new File(classpathRoot, SOURCE_FOLDER + CLASS_NAME + ".java");

    public TestClassFile() {
    }

    public TestClassFile create() {
        try {
            String sourceCode = String.format("package %s;public class %s {}", PACKAGE_NAME, CLASS_NAME);

            checkState(sourceFile.getParentFile().exists() || sourceFile.getParentFile().mkdirs(),
                    "Can't create directory %s", sourceFile.getParentFile().getAbsolutePath());
            Files.write(sourceCode, sourceFile, UTF_8);

            int result = getSystemJavaCompiler().run(null, null, null, sourceFile.getAbsolutePath());
            checkState(result == 0, "Compiler exit code should be 0, but it was " + result);

            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPackageName() {
        return PACKAGE_NAME;
    }

    public String getClassName() {
        return PACKAGE_NAME + "." + CLASS_NAME;
    }

    public File getClasspathRoot() {
        return classpathRoot;
    }
}
