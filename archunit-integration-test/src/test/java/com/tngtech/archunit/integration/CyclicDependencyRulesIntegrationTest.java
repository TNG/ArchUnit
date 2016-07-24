package com.tngtech.archunit.integration;

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
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatInheritsFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice1.ClassThatIsInheritedFromSliceTwo;
import com.tngtech.archunit.example.cycle.inheritancecycle.slice2.ClassThatInheritsFromSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SliceOneCallingMethodInSliceTwo;
import com.tngtech.archunit.example.cycle.simplecycle.slice1.SomeClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.simplecycle.slice2.SliceTwoCallingMethodOfSliceThree;
import com.tngtech.archunit.example.cycle.simplecycle.slice3.SliceThreeCallingMethodOfSliceOne;
import com.tngtech.archunit.example.cycle.simplescenario.administration.AdministrationService;
import com.tngtech.archunit.example.cycle.simplescenario.administration.Invoice;
import com.tngtech.archunit.example.cycle.simplescenario.importer.ImportService;
import com.tngtech.archunit.example.cycle.simplescenario.report.Report;
import com.tngtech.archunit.example.cycle.simplescenario.report.ReportService;
import com.tngtech.archunit.exampletest.CyclicDependencyRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.integration.CyclicErrorMatcher.cycle;
import static com.tngtech.archunit.junit.ExpectedViolation.from;

public class CyclicDependencyRulesIntegrationTest extends CyclicDependencyRulesTest {

    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void slices_should_not_contain_cyclic_dependencies_by_simple_method_calls() {
        expectViolationFromSimpleCycle(expectViolation);

        super.slices_should_not_contain_cyclic_dependencies_by_simple_method_calls();
    }

    static void expectViolationFromSimpleCycle(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..(simplecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of simplecycle")
                        .byAccess(from(SliceOneCallingMethodInSliceTwo.class, "callSliceTwo")
                                .toMethod(SliceTwoCallingMethodOfSliceThree.class, "doSomethingInSliceTwo")
                                .inLine(9))
                        .from("slice2 of simplecycle")
                        .byAccess(from(SliceTwoCallingMethodOfSliceThree.class, "callSliceThree")
                                .toMethod(SliceThreeCallingMethodOfSliceOne.class, "doSomethingInSliceThree")
                                .inLine(9))
                        .from("slice3 of simplecycle")
                        .byAccess(from(SliceThreeCallingMethodOfSliceOne.class, "callSliceOne")
                                .toMethod(SomeClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(9)));
    }

    @Test
    @Override
    public void slices_should_not_contain_cyclic_dependencies_by_simple_constructor_calls() {
        expectViolationFromConstructorCycle(expectViolation);

        super.slices_should_not_contain_cyclic_dependencies_by_simple_constructor_calls();
    }

    static void expectViolationFromConstructorCycle(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..(constructorcycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of constructorcycle")
                        .byAccess(from(SliceOneCallingConstructorInSliceTwo.class, "callSliceTwo")
                                .toConstructor(SliceTwoCallingConstructorInSliceOne.class)
                                .inLine(7))
                        .from("slice2 of constructorcycle")
                        .byAccess(from(SliceTwoCallingConstructorInSliceOne.class, "callSliceOne")
                                .toConstructor(SomeClassWithCalledConstructor.class)
                                .inLine(7)));
    }

    @Test
    @Override
    public void slices_should_not_contain_cyclic_dependencies_by_inheritance() {
        expectViolationFromInheritanceCycle(expectViolation);

        super.slices_should_not_contain_cyclic_dependencies_by_inheritance();
    }

    static void expectViolationFromInheritanceCycle(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..(inheritancecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .byAccess(from(ClassThatInheritsFromSliceTwo.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatInheritsFromSliceOne.class)
                                .inLine(5))
                        .from("slice2 of inheritancecycle")
                        .byAccess(from(ClassThatInheritsFromSliceOne.class, CONSTRUCTOR_NAME)
                                .toConstructor(ClassThatIsInheritedFromSliceTwo.class)
                                .inLine(5)));
    }

    @Test
    @Override
    public void slices_should_not_contain_cyclic_dependencies_by_field_access() {
        expectViolationFromFieldAccessCycle(expectViolation);

        super.slices_should_not_contain_cyclic_dependencies_by_field_access();
    }

    static void expectViolationFromFieldAccessCycle(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..(fieldaccesscycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of fieldaccesscycle")
                        .byAccess(from(SliceOneAccessingFieldInSliceTwo.class, "accessSliceTwo")
                                .setting().field(SliceTwoAccessingFieldInSliceOne.class, "accessedField")
                                .inLine(9))
                        .from("slice2 of fieldaccesscycle")
                        .byAccess(from(SliceTwoAccessingFieldInSliceOne.class, "accessSliceOne")
                                .setting().field(ClassInSliceOneWithAccessedField.class, "accessedField")
                                .inLine(10)));
    }

    @Test
    @Override
    public void simple_cyclic_scenario() {
        expectViolationFromSimpleCyclicScenario(expectViolation);

        super.simple_cyclic_scenario();
    }

    static void expectViolationFromSimpleCyclicScenario(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..simplescenario.(*)..' should be free of cycles")
                .by(cycle().from("administration")
                        .byAccess(from(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(ReportService.class, "getReport", String.class)
                                .inLine(12))
                        .byAccess(from(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(Report.class, "isEmpty")
                                .inLine(13))
                        .from("report")
                        .byAccess(from(ReportService.class, "getReport", String.class)
                                .toMethod(ImportService.class, "process", String.class)
                                .inLine(10))
                        .from("importer")
                        .byAccess(from(ImportService.class, "process", String.class)
                                .toMethod(AdministrationService.class, "createCustomerId", String.class)
                                .inLine(11)));
    }

    @Test
    @Override
    public void slices_should_not_contain_cyclic_dependencies() {
        expectViolationFromComplexCyclicScenario(expectViolation);

        super.slices_should_not_contain_cyclic_dependencies();
    }

    static void expectViolationFromComplexCyclicScenario(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("slices matching '..(complexcycles).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of complexcycles")
                        .byAccess(from(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .byAccess(from(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .byAccess(from(SliceTwoInheritingFromSliceOne.class, CONSTRUCTOR_NAME)
                                .toConstructor(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class)
                                .inLine(5))
                        .byAccess(from(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .toMethod(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .inLine(9)))

                .by(cycle().from("slice1 of complexcycles")
                        .byAccess(from(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .byAccess(from(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .byAccess(from(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, CONSTRUCTOR_NAME)
                                .toConstructor(InheritedClassInSliceThree.class)
                                .inLine(6))
                        .from("slice3 of complexcycles")
                        .byAccess(from(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .byAccess(from(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .byAccess(from(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)))

                .by(cycle().from("slice1 of complexcycles")
                        .byAccess(from(ClassOfMinimalCircleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCircleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .byAccess(from(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .byAccess(from(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .toConstructor(ClassWithAccessedFieldCallingMethodInSliceOne.class)
                                .inLine(8))
                        .byAccess(from(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .setting().field(ClassWithAccessedFieldCallingMethodInSliceOne.class, "accessedField")
                                .inLine(8))
                        .from("slice4 of complexcycles")
                        .byAccess(from(ClassWithAccessedFieldCallingMethodInSliceOne.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(10)))

                .by(cycle().from("slice1 of complexcycles")
                        .byAccess(from(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceThree")
                                .toMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .inLine(14))
                        .from("slice3 of complexcycles")
                        .byAccess(from(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .byAccess(from(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .byAccess(from(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)));
    }
}
