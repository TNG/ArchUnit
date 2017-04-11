package com.tngtech.archunit.base;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.Internal;

@Internal
public class ArchUnitException extends RuntimeException {
    ArchUnitException(String message, Throwable cause) {
        super(message, cause);
    }

    ArchUnitException(String message) {
        super(message);
    }

    ArchUnitException(Throwable cause) {
        super(cause);
    }

    @Internal
    public static class LocationException extends ArchUnitException {
        public LocationException(Exception e) {
            super(e);
        }
    }

    @Internal
    public static class ReflectionException extends ArchUnitException {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    @Internal
    public static class UnsupportedUriSchemeException extends ArchUnitException {
        public UnsupportedUriSchemeException(URI uri) {
            super("The scheme of the following URI is not (yet) supported: " + uri);
        }
    }

    @Internal
    public static class InconsistentClassPathException extends ArchUnitException {
        public InconsistentClassPathException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Internal
    public static class ClassResolverConfigurationException extends ArchUnitException {
        private ClassResolverConfigurationException(String message) {
            super(message);
        }

        private ClassResolverConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

        public static ClassResolverConfigurationException onLoadingClass(String resolverClass, Throwable cause) {
            String message = "Error loading resolver class " + resolverClass;
            return new ClassResolverConfigurationException(message, cause);
        }

        public static ClassResolverConfigurationException onWrongArguments(
                Class<?> resolverClass, Throwable cause) {

            String message = String.format("class %s must either provide a default constructor, " +
                            "or a constructor taking one single argument of type %s",
                    resolverClass.getName(), List.class.getName());
            return new ClassResolverConfigurationException(message, cause);
        }

        public static ClassResolverConfigurationException onWrongConstructor(Class<?> resolverClass, List<String> args) {
            String message = String.format("class %s has no constructor taking a single argument " +
                            "of type %s, to accept configured parameters ['%s']",
                    resolverClass.getName(), List.class.getName(), Joiner.on("', '").join(args));
            return new ClassResolverConfigurationException(message);
        }

        public static ClassResolverConfigurationException onInstantiation(
                Constructor<?> constructor, List<String> args, Exception cause) {

            Class<?> declaringClass = constructor.getDeclaringClass();
            String message = String.format("class %s threw an exception in constructor %s('%s')",
                    declaringClass.getName(), declaringClass.getSimpleName(), Joiner.on("', '").join(args));
            return new ClassResolverConfigurationException(message, cause);
        }
    }
}
