package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassOfMinimalCircleCallingSliceTwo;
import com.tngtech.archunit.example.cycle.complexcycles.slice1.SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.ClassOfMinimalCircleCallingSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.InstantiatedClassInSliceTwo;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.SliceTwoInheritingFromSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour;
import com.tngtech.archunit.example.cycle.complexcycles.slice3.ClassCallingConstructorInSliceFive;
import com.tngtech.archunit.example.cycle.complexcycles.slice3.InheritedClassInSliceThree;
import com.tngtech.archunit.example.cycle.complexcycles.slice4.ClassWithAccessedFieldCallingMethodInSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice5.InstantiatedClassInSliceFive;
import com.tngtech.archunit.example.cycle.constructorcycle.slice1.SliceOneCallingConstructorInSliceTwo;
import com.tngtech.archunit.example.cycle.constructorcycle.slice1.SomeClassWithCalledConstructor;
import com.tngtech.archunit.example.cycle.constructorcycle.slice2.SliceTwoCallingConstructorInSliceOne;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice1.ClassInSliceOneWithAccessedField;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice1.SliceOneAccessingFieldInSliceTwo;
import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice2.SliceTwoAccessingFieldInSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatCallSliceThree;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatInheritsFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatIsInheritedFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.InterfaceInSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice2.ClassThatInheritsFromSliceOne;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice3.ClassThatImplementsInterfaceFromSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SliceOneCallingMethodInSliceTwo;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SomeClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice2.SliceTwoCallingMethodOfSliceThree;
import com.tngtech.archunit.example.cycle.simplecycle.slice3.SliceThreeCallingMethodOfSliceOne;
import com.tngtech.archunit.example.cycle.simplescenario.administration.AdministrationService;
import com.tngtech.archunit.example.cycle.simplescenario.administration.Invoice;
import com.tngtech.archunit.example.cycle.simplescenario.importer.ImportService;
import com.tngtech.archunit.example.cycle.simplescenario.report.Report;
import com.tngtech.archunit.example.cycle.simplescenario.report.ReportService;
import com.tngtech.archunit.exampletest.junit.CyclicDependencyRulesTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.integration.junit.CyclicErrorMatcher.cycle;
import static com.tngtech.archunit.junit.ExpectedAccess.accessFrom;
import static com.tngtech.archunit.junit.ExpectedInheritance.inheritanceFrom;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.cycle")
public class CyclicDependencyRulesIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromSimpleCycle")
    public static final ArchRule NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES =
            CyclicDependencyRulesTest.NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromConstructorCycle")
    public static final ArchRule NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES =
            CyclicDependencyRulesTest.NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromInheritanceCycle")
    public static final ArchRule NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES =
            CyclicDependencyRulesTest.NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromFieldAccessCycle")
    public static final ArchRule NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES =
            CyclicDependencyRulesTest.NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromSimpleCyclicScenario")
    public static final ArchRule NO_CYCLES_IN_SIMPLE_SCENARIO =
            CyclicDependencyRulesTest.NO_CYCLES_IN_SIMPLE_SCENARIO;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromComplexCyclicScenario")
    public static final ArchRule NO_CYCLES_IN_COMPLEX_SCENARIO =
            CyclicDependencyRulesTest.NO_CYCLES_IN_COMPLEX_SCENARIO;

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromSimpleCycle(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..(simplecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of simplecycle")
                        .by(accessFrom(SliceOneCallingMethodInSliceTwo.class, "callSliceTwo")
                                .toMethod(SliceTwoCallingMethodOfSliceThree.class, "doSomethingInSliceTwo")
                                .inLine(9))
                        .from("slice2 of simplecycle")
                        .by(accessFrom(SliceTwoCallingMethodOfSliceThree.class, "callSliceThree")
                                .toMethod(SliceThreeCallingMethodOfSliceOne.class, "doSomethingInSliceThree")
                                .inLine(9))
                        .from("slice3 of simplecycle")
                        .by(accessFrom(SliceThreeCallingMethodOfSliceOne.class, "callSliceOne")
                                .toMethod(SomeClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(9)));
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromConstructorCycle(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..(constructorcycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of constructorcycle")
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwo.class, "callSliceTwo")
                                .toConstructor(SliceTwoCallingConstructorInSliceOne.class)
                                .inLine(7))
                        .from("slice2 of constructorcycle")
                        .by(accessFrom(SliceTwoCallingConstructorInSliceOne.class, "callSliceOne")
                                .toConstructor(SomeClassWithCalledConstructor.class)
                                .inLine(7)));
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromInheritanceCycle(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..(inheritancecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceTwo.class)
                                .extending(ClassThatInheritsFromSliceOne.class))
                        .by(accessFrom(ClassThatInheritsFromSliceTwo.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatInheritsFromSliceOne.class)
                                .inLine(5))
                        .from("slice2 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceOne.class)
                                .extending(ClassThatIsInheritedFromSliceTwo.class))
                        .by(accessFrom(ClassThatInheritsFromSliceOne.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatIsInheritedFromSliceTwo.class)
                                .inLine(5)))

                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(accessFrom(ClassThatCallSliceThree.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatImplementsInterfaceFromSliceOne.class)
                                .inLine(7))
                        .from("slice3 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatImplementsInterfaceFromSliceOne.class)
                                .implementing(InterfaceInSliceOne.class)));
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromFieldAccessCycle(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..(fieldaccesscycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of fieldaccesscycle")
                        .by(accessFrom(SliceOneAccessingFieldInSliceTwo.class, "accessSliceTwo")
                                .setting().field(SliceTwoAccessingFieldInSliceOne.class, "accessedField")
                                .inLine(9))
                        .from("slice2 of fieldaccesscycle")
                        .by(accessFrom(SliceTwoAccessingFieldInSliceOne.class, "accessSliceOne")
                                .setting().field(ClassInSliceOneWithAccessedField.class, "accessedField")
                                .inLine(10)));
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromSimpleCyclicScenario(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..simplescenario.(*)..' should be free of cycles")
                .by(cycle().from("administration")
                        .by(accessFrom(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(ReportService.class, "getReport", String.class)
                                .inLine(12))
                        .by(accessFrom(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(Report.class, "isEmpty")
                                .inLine(13))
                        .from("report")
                        .by(accessFrom(ReportService.class, "getReport", String.class)
                                .toMethod(ImportService.class, "process", String.class)
                                .inLine(10))
                        .from("importer")
                        .by(accessFrom(ImportService.class, "process", String.class)
                                .toMethod(AdministrationService.class, "createCustomerId", String.class)
                                .inLine(11)));
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationFromComplexCyclicScenario(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("slices matching '..(complexcycles).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of complexcycles")
                        .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .by(inheritanceFrom(SliceTwoInheritingFromSliceOne.class)
                                .extending(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class))
                        .by(accessFrom(SliceTwoInheritingFromSliceOne.class, CONSTRUCTOR_NAME)
                                .toConstructor(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class)
                                .inLine(5))
                        .by(accessFrom(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .toMethod(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .inLine(9)))

                .by(cycle().from("slice1 of complexcycles")
                        .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .by(inheritanceFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class)
                                .extending(InheritedClassInSliceThree.class))
                        .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, CONSTRUCTOR_NAME)
                                .toConstructor(InheritedClassInSliceThree.class)
                                .inLine(6))
                        .from("slice3 of complexcycles")
                        .by(accessFrom(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)))

                .by(cycle().from("slice1 of complexcycles")
                        .by(accessFrom(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .toConstructor(ClassWithAccessedFieldCallingMethodInSliceOne.class)
                                .inLine(8))
                        .by(accessFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .setting().field(ClassWithAccessedFieldCallingMethodInSliceOne.class, "accessedField")
                                .inLine(8))
                        .from("slice4 of complexcycles")
                        .by(accessFrom(ClassWithAccessedFieldCallingMethodInSliceOne.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(10)))

                .by(cycle().from("slice1 of complexcycles")
                        .by(accessFrom(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceThree")
                                .toMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .inLine(14))
                        .from("slice3 of complexcycles")
                        .by(accessFrom(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .by(accessFrom(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)));
    }
}
