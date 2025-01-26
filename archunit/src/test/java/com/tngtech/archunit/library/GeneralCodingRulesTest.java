package com.tngtech.archunit.library;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.testclasses.packages.correct.customsuffix.ImplementationClassWithCorrectPackageCustomSuffix;
import com.tngtech.archunit.library.testclasses.packages.correct.defaultsuffix.ImplementationClassWithCorrectPackage;
import com.tngtech.archunit.library.testclasses.packages.correct.notest.ImplementationClassWithoutTestClass;
import com.tngtech.archunit.library.testclasses.packages.correct.onedirmatching.ImplementationClassWithOneTestPackageMatchingOutOfTwo;
import com.tngtech.archunit.library.testclasses.packages.incorrect.nodirmatching.ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackage;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.customsuffix.ImplementationClassWithWrongTestClassPackageCustomSuffix;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.customsuffix.subdir.ImplementationClassWithWrongTestClassPackageCustomSuffixTestingScenario;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.defaultsuffix.ImplementationClassWithWrongTestClassPackage;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.defaultsuffix.subdir.ImplementationClassWithWrongTestClassPackageTest;
import com.tngtech.archunit.library.testclasses.timeapi.incorrect.UsesJavaSqlDate;
import com.tngtech.archunit.library.testclasses.timeapi.incorrect.UsesJavaSqlTime;
import com.tngtech.archunit.library.testclasses.timeapi.incorrect.UsesJavaSqlTimestamp;
import com.tngtech.archunit.library.testclasses.timeapi.incorrect.UsesJavaUtilCalender;
import com.tngtech.archunit.library.testclasses.timeapi.incorrect.UsesJavaUtilDate;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.library.GeneralCodingRules.ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE;
import static com.tngtech.archunit.library.GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;
import static com.tngtech.archunit.library.GeneralCodingRules.OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;
import static com.tngtech.archunit.library.GeneralCodingRules.testClassesShouldResideInTheSamePackageAsImplementation;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;

public class GeneralCodingRulesTest {

    @Test
    public void test_class_in_same_package_should_fail_when_test_class_reside_in_different_package_as_implementation() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithWrongTestClassPackage.class))
                .hasOnlyOneViolationWithStandardPattern(ImplementationClassWithWrongTestClassPackageTest.class,
                        "does not reside in same package as implementation class <" + ImplementationClassWithWrongTestClassPackage.class.getName()
                                + ">");
    }

    @Test
    public void test_class_in_same_package_should_fail_when_test_class_reside_in_different_package_as_implementation_with_custom_suffix() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation("TestingScenario"))
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithWrongTestClassPackageCustomSuffix.class))
                .hasOnlyOneViolationWithStandardPattern(ImplementationClassWithWrongTestClassPackageCustomSuffixTestingScenario.class,
                        "does not reside in same package as implementation class <"
                                + ImplementationClassWithWrongTestClassPackageCustomSuffix.class.getName() + ">");
    }

    @Test
    public void test_class_in_same_package_should_pass_when_test_class_and_implementation_class_reside_in_the_same_package() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithCorrectPackage.class))
                .hasNoViolation();
    }

    @Test
    public void test_class_in_same_package_should_pass_when_test_class_and_implementation_class_reside_in_the_same_package_with_custom_suffix() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation("TestingScenario"))
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithCorrectPackageCustomSuffix.class))
                .hasNoViolation();
    }

    @Test
    public void should_pass_when_test_class_is_missing_and_only_implementation_provided() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithoutTestClass.class))
                .hasNoViolation();
    }

    @Test
    public void should_pass_when_one_of_multiple_matching_test_classes_resides_in_the_implementation_package() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithOneTestPackageMatchingOutOfTwo.class))
                .hasNoViolation();
    }

    @Test
    public void should_not_pass_when_none_of_multiple_matching_test_classes_resides_in_implementation_package() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackage.class))
                .hasNumberOfViolations(2)
                .hasViolationWithStandardPattern(
                        com.tngtech.archunit.library.testclasses.packages.incorrect.nodirmatching.wrongdir1.ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackageTest.class,
                        "does not reside in same package as implementation class <"
                                + ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackage.class.getName() + ">"
                )
                .hasViolationWithStandardPattern(
                        com.tngtech.archunit.library.testclasses.packages.incorrect.nodirmatching.wrongdir2.ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackageTest.class,
                        "does not reside in same package as implementation class <"
                                + ImplementationClassWithMultipleTestsNotMatchingImplementationClassPackage.class.getName() + ">"
                );
    }

    @Test
    public void ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE_should_fail_on_assert_without_detail_message() {
        @SuppressWarnings("unused")
        class InvalidAssertions {
            void f(int x) {
                assert x > 0;
            }

            void f() {
                throw new AssertionError();
            }
        }
        assertThatRule(ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE)
                .checking(new ClassFileImporter().importClasses(InvalidAssertions.class))
                .hasNumberOfViolations(2)
                .hasViolationContaining("Method <%s.f(int)> calls constructor <%s.<init>()>",
                        InvalidAssertions.class.getName(), AssertionError.class.getName())
                .hasViolationContaining("Method <%s.f()> calls constructor <%s.<init>()>",
                        InvalidAssertions.class.getName(), AssertionError.class.getName());
    }

    @Test
    public void ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE_should_accept_assert_with_detail_message() {
        @SuppressWarnings("unused")
        class ValidAssertions {
            void f(int x) {
                assert x > 0 : "argument should be positive";
            }

            void f() {
                throw new AssertionError("f() should not be called");
            }
        }
        assertThatRule(ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE)
                .checking(new ClassFileImporter().importClasses(ValidAssertions.class))
                .hasNoViolation();
    }

    @Test
    public void DEPRECATED_API_SHOULD_NOT_BE_USED_should_fail_on_call_to_deprecated_method() {
        @SuppressWarnings("DeprecatedIsStillUsed")
        class ClassWithDeprecatedMembers {
            @Deprecated
            int target;

            @Deprecated
            ClassWithDeprecatedMembers() {
            }

            @Deprecated
            void target() {
            }
        }
        @SuppressWarnings("DeprecatedIsStillUsed")
        @Deprecated
        class DeprecatedClass {
            int target;

            void target() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            @DeprecatedAnnotation
            void origin() {
                ClassWithDeprecatedMembers instanceOfClassWithDeprecatedMembers = new ClassWithDeprecatedMembers();
                instanceOfClassWithDeprecatedMembers.target++;
                instanceOfClassWithDeprecatedMembers.target();
                DeprecatedClass instanceOfDeprecatedClass = new DeprecatedClass();
                instanceOfDeprecatedClass.target++;
                instanceOfDeprecatedClass.target();
                Class<?> deprecatedClass = DeprecatedClass.class;
            }
        }

        String innerClassConstructor = CONSTRUCTOR_NAME + "(" + GeneralCodingRulesTest.class.getName() + ")";
        String violatingMethod = "Method <" + Origin.class.getName() + ".origin()>";
        assertThatRule(DEPRECATED_API_SHOULD_NOT_BE_USED)
                .hasDescriptionContaining("no classes should access @Deprecated members or should depend on @Deprecated classes, because there should be a better alternative")
                .checking(new ClassFileImporter().importClasses(Origin.class, ClassWithDeprecatedMembers.class, DeprecatedClass.class))
                .hasNumberOfViolations(10)
                .hasViolationContaining("%s calls constructor <%s.%s>", violatingMethod, ClassWithDeprecatedMembers.class.getName(), innerClassConstructor)
                .hasViolationContaining("%s gets field <%s.target>", violatingMethod, ClassWithDeprecatedMembers.class.getName())
                .hasViolationContaining("%s sets field <%s.target>", violatingMethod, ClassWithDeprecatedMembers.class.getName())
                .hasViolationContaining("%s calls method <%s.target()>", violatingMethod, ClassWithDeprecatedMembers.class.getName())
                .hasViolationContaining("%s calls constructor <%s.%s>", violatingMethod, DeprecatedClass.class.getName(), innerClassConstructor)
                .hasViolationContaining("%s gets field <%s.target>", violatingMethod, DeprecatedClass.class.getName())
                .hasViolationContaining("%s sets field <%s.target>", violatingMethod, DeprecatedClass.class.getName())
                .hasViolationContaining("%s calls method <%s.target()>", violatingMethod, DeprecatedClass.class.getName())
                .hasViolationContaining("%s is annotated with <%s>", violatingMethod, DeprecatedAnnotation.class.getName())
                .hasViolationContaining("%s references class object <%s>", violatingMethod, DeprecatedClass.class.getName());
    }

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    private @interface DeprecatedAnnotation {
    }

    @Test
    public void OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED_should_fail_when_class_uses_java_util_date() {
        assertThatRule(OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED)
                .hasDescription("java.time API should be used, because legacy date/time APIs have been replaced since Java 8 (JSR 310)")
                .checking(new ClassFileImporter().importClasses(UsesJavaUtilDate.class))
                .hasViolationContaining("calls method <java.util.Date");
    }

    @Test
    public void OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED_should_fail_when_class_uses_java_sql_timestamp() {
        assertThatRule(OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED)
                .hasDescription("java.time API should be used, because legacy date/time APIs have been replaced since Java 8 (JSR 310)")
                .checking(new ClassFileImporter().importClasses(UsesJavaSqlTimestamp.class))
                .hasViolationContaining("calls constructor <java.sql.Timestamp");
    }

    @Test
    public void OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED_should_fail_when_class_uses_java_sql_time() {
        assertThatRule(OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED)
                .hasDescription("java.time API should be used, because legacy date/time APIs have been replaced since Java 8 (JSR 310)")
                .checking(new ClassFileImporter().importClasses(UsesJavaSqlTime.class))
                .hasViolationContaining("calls constructor <java.sql.Time");
    }

    @Test
    public void OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED_should_fail_when_class_uses_java_sql_date() {
        assertThatRule(OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED)
                .hasDescription("java.time API should be used, because legacy date/time APIs have been replaced since Java 8 (JSR 310)")
                .checking(new ClassFileImporter().importClasses(UsesJavaSqlDate.class))
                .hasViolationContaining("calls constructor <java.sql.Date");
    }

    @Test
    public void OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED_should_fail_when_class_uses_java_util_calender() {
        assertThatRule(OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED)
                .hasDescription("java.time API should be used, because legacy date/time APIs have been replaced since Java 8 (JSR 310)")
                .checking(new ClassFileImporter().importClasses(UsesJavaUtilCalender.class))
                .hasViolationContaining("calls method <java.util.Calendar");
    }
}
