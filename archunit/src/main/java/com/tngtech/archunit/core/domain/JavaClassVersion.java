package com.tngtech.archunit.core.domain;

/**
 * Represents the version of a JVM class file.
 */
public class JavaClassVersion {

    private final int major;
    private final int minor;

    /**
     * Create a new instance of {@link JavaClassVersion}.
     *
     * @param major The major version of the class file.
     * @param minor The minor version of the class file.
     */
    public JavaClassVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Get the Java version corresponding to the major version of the class file.
     *
     * @return The Java version
     */
    public int getJavaVersion() {
        return major - 44;
    }

    /**
     * Get the major version of the class file.
     *
     * @return The major version
     */
    public int getBytecodeMajorVersion() {
        return major;
    }

    /**
     * Get the minor version of the class file.
     *
     * @return The minor version
     */
    public int getBytecodeMinorVersion() {
        return minor;
    }

    /**
     * Create a new instance of {@link JavaClassVersion} from the version as provided by ASM.
     *
     * @return A representation of the class file version
     */
    public static JavaClassVersion of(int asmVersion) {
        int major = asmVersion & 0xFF;
        int minor = asmVersion >> 16;

        return new JavaClassVersion(major, minor);
    }

}
