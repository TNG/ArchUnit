package com.tngtech.archunit.library.freeze;

import java.util.regex.Pattern;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;

import static com.tngtech.archunit.base.ReflectionUtils.newInstanceOf;

class ViolationLineMatcherFactory {
    private static final String FREEZE_LINE_MATCHER_PROPERTY = "freeze.lineMatcher";
    private static final FuzzyLineNumberMatcher DEFAULT_MATCHER = new FuzzyLineNumberMatcher();

    static ViolationLineMatcher create() {
        return ArchConfiguration.get().containsProperty(FREEZE_LINE_MATCHER_PROPERTY)
                ? createInstance(ArchConfiguration.get().getProperty(FREEZE_LINE_MATCHER_PROPERTY))
                : DEFAULT_MATCHER;
    }

    @MayResolveTypesViaReflection(reason = "This is not part of the import process")
    private static ViolationLineMatcher createInstance(String lineMatcherClassName) {
        try {
            return (ViolationLineMatcher) newInstanceOf(Class.forName(lineMatcherClassName));
        } catch (Exception e) {
            String message = String.format("Could not instantiate %s of configured type '%s=%s'",
                    ViolationLineMatcher.class.getSimpleName(), FREEZE_LINE_MATCHER_PROPERTY, lineMatcherClassName);
            throw new StoreInitializationFailedException(message, e);
        }
    }

    private static class FuzzyLineNumberMatcher implements ViolationLineMatcher {
        private static final Pattern LINE_WITHOUT_LINE_NUMBER_PATTERN = Pattern.compile("(.*\\(.*):\\d+\\)$");

        @Override
        public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
            return ignoreLineNumber(lineFromFirstViolation).equals(ignoreLineNumber(lineFromSecondViolation));
        }

        private String ignoreLineNumber(String violation) {
            return LINE_WITHOUT_LINE_NUMBER_PATTERN.matcher(violation).replaceAll("$1");
        }
    }
}
