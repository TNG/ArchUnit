package com.tngtech.archunit.integration;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.example.cycles.complexcycles.slice1.ClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycles.complexcycles.slice1.ClassOfMinimalCycleCallingSliceTwo;
import com.tngtech.archunit.example.cycles.complexcycles.slice1.SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree;
import com.tngtech.archunit.example.cycles.complexcycles.slice2.ClassOfMinimalCycleCallingSliceOne;
import com.tngtech.archunit.example.cycles.complexcycles.slice2.InstantiatedClassInSliceTwo;
import com.tngtech.archunit.example.cycles.complexcycles.slice2.SliceTwoInheritingFromSliceOne;
import com.tngtech.archunit.example.cycles.complexcycles.slice2.SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour;
import com.tngtech.archunit.example.cycles.complexcycles.slice3.ClassCallingConstructorInSliceFive;
import com.tngtech.archunit.example.cycles.complexcycles.slice3.InheritedClassInSliceThree;
import com.tngtech.archunit.example.cycles.complexcycles.slice4.ClassWithAccessedFieldCallingMethodInSliceOne;
import com.tngtech.archunit.example.cycles.complexcycles.slice5.InstantiatedClassInSliceFive;
import com.tngtech.archunit.example.cycles.constructorcycle.slice1.SliceOneCallingConstructorInSliceTwo;
import com.tngtech.archunit.example.cycles.constructorcycle.slice1.SomeClassWithCalledConstructor;
import com.tngtech.archunit.example.cycles.constructorcycle.slice2.SliceTwoCallingConstructorInSliceOne;
import com.tngtech.archunit.example.cycles.fieldaccesscycle.slice1.ClassInSliceOneWithAccessedField;
import com.tngtech.archunit.example.cycles.fieldaccesscycle.slice1.SliceOneAccessingFieldInSliceTwo;
import com.tngtech.archunit.example.cycles.fieldaccesscycle.slice2.SliceTwoAccessingFieldInSliceOne;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice1.ClassThatCallSliceThree;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice1.ClassThatInheritsFromSliceTwo;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice1.ClassThatIsInheritedFromSliceTwo;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice1.InterfaceInSliceOne;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice2.ClassThatInheritsFromSliceOne;
import com.tngtech.archunit.example.cycles.inheritancecycle.slice3.ClassThatImplementsInterfaceFromSliceOne;
import com.tngtech.archunit.example.cycles.membercycle.slice1.SliceOneWithFieldTypeInSliceTwo;
import com.tngtech.archunit.example.cycles.membercycle.slice2.SliceTwoWithMethodParameterTypeInSliceThree;
import com.tngtech.archunit.example.cycles.membercycle.slice3.SliceThreeWithMethodReturnTypeInSliceFour;
import com.tngtech.archunit.example.cycles.membercycle.slice4.SliceFourWithConstructorParameterInSliceOne;
import com.tngtech.archunit.example.cycles.simplecycle.slice1.SliceOneCallingMethodInSliceTwo;
import com.tngtech.archunit.example.cycles.simplecycle.slice1.SomeClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycles.simplecycle.slice2.SliceTwoCallingMethodOfSliceThree;
import com.tngtech.archunit.example.cycles.simplecycle.slice3.SliceThreeCallingMethodOfSliceOne;
import com.tngtech.archunit.example.cycles.simplescenario.administration.AdministrationService;
import com.tngtech.archunit.example.cycles.simplescenario.administration.Invoice;
import com.tngtech.archunit.example.cycles.simplescenario.importer.ImportService;
import com.tngtech.archunit.example.cycles.simplescenario.report.Report;
import com.tngtech.archunit.example.cycles.simplescenario.report.ReportService;
import com.tngtech.archunit.example.layers.AbstractController;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.example.layers.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.layers.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.layers.EvilCoreAccessor;
import com.tngtech.archunit.example.layers.MyController;
import com.tngtech.archunit.example.layers.MyService;
import com.tngtech.archunit.example.layers.OtherClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.layers.SecondBeanImplementingSomeBusinessInterface;
import com.tngtech.archunit.example.layers.SomeBusinessInterface;
import com.tngtech.archunit.example.layers.SomeCustomException;
import com.tngtech.archunit.example.layers.SomeMediator;
import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.layers.anticorruption.WithIllegalReturnType;
import com.tngtech.archunit.example.layers.anticorruption.WrappedResult;
import com.tngtech.archunit.example.layers.controller.ComplexControllerAnnotation;
import com.tngtech.archunit.example.layers.controller.SimpleControllerAnnotation;
import com.tngtech.archunit.example.layers.controller.SomeController;
import com.tngtech.archunit.example.layers.controller.SomeGuiController;
import com.tngtech.archunit.example.layers.controller.SomeUtility;
import com.tngtech.archunit.example.layers.controller.WronglyAnnotated;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneThreeController;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.layers.controller.three.UseCaseThreeController;
import com.tngtech.archunit.example.layers.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.layers.core.CoreSatellite;
import com.tngtech.archunit.example.layers.core.HighSecurity;
import com.tngtech.archunit.example.layers.core.VeryCentralCore;
import com.tngtech.archunit.example.layers.persistence.WrongSecurityCheck;
import com.tngtech.archunit.example.layers.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.layers.persistence.first.dao.EntityInWrongPackage;
import com.tngtech.archunit.example.layers.persistence.first.dao.jpa.SomeJpa;
import com.tngtech.archunit.example.layers.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.layers.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.layers.persistence.second.dao.jpa.OtherJpa;
import com.tngtech.archunit.example.layers.security.Secured;
import com.tngtech.archunit.example.layers.service.ComplexServiceAnnotation;
import com.tngtech.archunit.example.layers.service.ServiceHelper;
import com.tngtech.archunit.example.layers.service.ServiceInterface;
import com.tngtech.archunit.example.layers.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.example.layers.service.impl.ServiceImplementation;
import com.tngtech.archunit.example.layers.service.impl.SomeInterfacePlacedInTheWrongPackage;
import com.tngtech.archunit.example.layers.service.impl.WronglyNamedSvc;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartySubClassWithProblem;
import com.tngtech.archunit.example.layers.web.AnnotatedController;
import com.tngtech.archunit.example.layers.web.InheritedControllerImpl;
import com.tngtech.archunit.example.onionarchitecture.adapter.cli.AdministrationCLI;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductId;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductRepository;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ShoppingCartId;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ShoppingCartRepository;
import com.tngtech.archunit.example.onionarchitecture.adapter.rest.ShoppingController;
import com.tngtech.archunit.example.onionarchitecture.application.AdministrationPort;
import com.tngtech.archunit.example.onionarchitecture.domain.model.OrderItem;
import com.tngtech.archunit.example.onionarchitecture.domain.model.ShoppingCart;
import com.tngtech.archunit.example.onionarchitecture.domain.service.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture.domain.service.ProductName;
import com.tngtech.archunit.example.onionarchitecture.domain.service.ShoppingService;
import com.tngtech.archunit.example.plantuml.address.Address;
import com.tngtech.archunit.example.plantuml.catalog.ProductCatalog;
import com.tngtech.archunit.example.plantuml.customer.Customer;
import com.tngtech.archunit.example.plantuml.importer.ProductImport;
import com.tngtech.archunit.example.plantuml.order.Order;
import com.tngtech.archunit.example.plantuml.product.Product;
import com.tngtech.archunit.exampletest.ControllerRulesTest;
import com.tngtech.archunit.exampletest.SecurityTest;
import com.tngtech.archunit.testutils.CyclicErrorMatcher;
import com.tngtech.archunit.testutils.ExpectedClass;
import com.tngtech.archunit.testutils.ExpectedConstructor;
import com.tngtech.archunit.testutils.ExpectedField;
import com.tngtech.archunit.testutils.ExpectedMethod;
import com.tngtech.archunit.testutils.ExpectedTestFailures;
import com.tngtech.archunit.testutils.MessageAssertionChain;
import com.tngtech.archunit.testutils.ResultStoringExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.collect.Collections2.filter;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.example.layers.OtherClassViolatingSessionBeanRules.init;
import static com.tngtech.archunit.example.layers.SomeMediator.violateLayerRulesIndirectly;
import static com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController.doSomethingOne;
import static com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController.someString;
import static com.tngtech.archunit.example.layers.controller.three.UseCaseThreeController.doSomethingThree;
import static com.tngtech.archunit.example.layers.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.example.layers.core.VeryCentralCore.DO_CORE_STUFF_METHOD_NAME;
import static com.tngtech.archunit.example.layers.persistence.layerviolation.DaoCallingService.violateLayerRules;
import static com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules.dependentMethod;
import static com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules.illegalAccessToController;
import static com.tngtech.archunit.testutils.CyclicErrorMatcher.cycle;
import static com.tngtech.archunit.testutils.ExpectedAccess.callFromConstructor;
import static com.tngtech.archunit.testutils.ExpectedAccess.callFromMethod;
import static com.tngtech.archunit.testutils.ExpectedAccess.callFromStaticInitializer;
import static com.tngtech.archunit.testutils.ExpectedDependency.annotatedClass;
import static com.tngtech.archunit.testutils.ExpectedDependency.constructor;
import static com.tngtech.archunit.testutils.ExpectedDependency.field;
import static com.tngtech.archunit.testutils.ExpectedDependency.inheritanceFrom;
import static com.tngtech.archunit.testutils.ExpectedDependency.method;
import static com.tngtech.archunit.testutils.ExpectedLocation.javaClass;
import static com.tngtech.archunit.testutils.ExpectedNaming.simpleNameOf;
import static com.tngtech.archunit.testutils.ExpectedNaming.simpleNameOfAnonymousClassOf;
import static com.tngtech.archunit.testutils.ExpectedViolation.clazz;
import static com.tngtech.archunit.testutils.ExpectedViolation.javaPackageOf;
import static com.tngtech.archunit.testutils.SliceDependencyErrorMatcher.sliceDependency;
import static java.lang.System.lineSeparator;

class ExamplesIntegrationTest {

    @BeforeAll
    static void initExtension() {
        ResultStoringExtension.enable();
    }

    @AfterEach
    void tearDown() {
        ResultStoringExtension.reset();
    }

    @AfterAll
    static void disableExtension() {
        ResultStoringExtension.disable();
    }

    @TestFactory
    Stream<DynamicTest> CodingRulesTest() {
        ExpectedTestFailures expectFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.CodingRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.CodingRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.CodingRulesTest.class);

        expectFailures.ofRule("no classes should access standard streams");
        expectAccessToStandardStreams(expectFailures);
        expectFailures.times(2);

        expectFailures.ofRule("no classes should throw generic exceptions");
        expectThrownGenericExceptions(expectFailures);

        expectFailures.ofRule("no classes should use java.util.logging")
                .by(callFromStaticInitializer(ClassViolatingCodingRules.class)
                        .setting().field(ClassViolatingCodingRules.class, "log")
                        .inLine(9));

        expectFailures.ofRule("no classes should use JodaTime, because modern Java projects use the [java.time] API instead")
                .by(callFromMethod(ClassViolatingCodingRules.class, "jodaTimeIsBad")
                        .toMethod(org.joda.time.DateTime.class, "now")
                        .inLine(31)
                        .asDependency())
                .by(method(ClassViolatingCodingRules.class, "jodaTimeIsBad")
                        .withReturnType(org.joda.time.DateTime.class));

        expectFailures.ofRule("no classes should access standard streams and no classes should throw generic exceptions");
        expectAccessToStandardStreams(expectFailures);
        expectThrownGenericExceptions(expectFailures);

        expectFailures.ofRule("fields that have raw type java.util.logging.Logger should be private " +
                "and should be static and should be final, because we agreed on this convention")
                .by(ExpectedField.of(ClassViolatingCodingRules.class, "log").doesNotHaveModifier(JavaModifier.PRIVATE))
                .by(ExpectedField.of(ClassViolatingCodingRules.class, "log").doesNotHaveModifier(JavaModifier.FINAL));

        return expectFailures.toDynamicTests();
    }

    private static void expectAccessToStandardStreams(ExpectedTestFailures expectFailures) {
        expectFailures
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .getting().field(System.class, "out")
                        .inLine(12))
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .getting().field(System.class, "err")
                        .inLine(13))
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .toMethod(SomeCustomException.class, "printStackTrace")
                        .inLine(14))
                .by(callFromMethod(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .getting().field(System.class, "out")
                        .inLine(25));
    }

    private static void expectThrownGenericExceptions(ExpectedTestFailures expectFailures) {
        expectFailures
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Throwable.class)
                        .inLine(22))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(24))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(RuntimeException.class, String.class, Throwable.class)
                        .inLine(26))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(26));
    }

    @TestFactory
    Stream<DynamicTest> ControllerRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        ControllerRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.ControllerRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.ControllerRulesTest.class)

                .ofRule(String.format("classes that reside in a package '..controller..' should "
                                + "only call methods that are declared in a package '..controller..' or are annotated with @%s",
                        Secured.class.getSimpleName()))
                .by(callFromMethod(SomeController.class, "doSthController")
                        .toMethod(ServiceViolatingDaoRules.class, "doSthService")
                        .inLine(11))
                .by(callFromMethod(SomeEnum.class, "values")
                        .toMethod(SomeEnum[].class, "clone")
                        .inLine(3))

                .ofRule(String.format("classes that reside in a package '..controller..' should "
                                + "only call constructors that are declared in a package '..controller..' or are annotated with @%s",
                        Secured.class.getSimpleName()))
                .by(callFromMethod(SomeGuiController.class, "callServiceLayer")
                        .toConstructor(ServiceHelper.class)
                        .inLine(7))
                .by(callFromConstructor(UseCaseTwoController.class)
                        .toConstructor(AbstractController.class)
                        .inLine(6))

                .ofRule(String.format("classes that reside in a package '..controller..' should "
                                + "only call code units that are declared in a package '..controller..' or are annotated with @%s",
                        Secured.class.getSimpleName()))
                .by(callFromMethod(SomeController.class, "doSthController")
                        .toMethod(ServiceViolatingDaoRules.class, "doSthService")
                        .inLine(11))
                .by(callFromMethod(SomeGuiController.class, "callServiceLayer")
                        .toConstructor(ServiceHelper.class)
                        .inLine(7))
                .by(callFromConstructor(UseCaseTwoController.class)
                        .toConstructor(AbstractController.class)
                        .inLine(6))
                .by(callFromMethod(SomeEnum.class, "values")
                        .toMethod(SomeEnum[].class, "clone")
                        .inLine(3))

                .ofRule(String.format("classes that reside in a package '..controller..' should "
                                + "only access fields that are declared in a package '..controller..' or are annotated with @%s",
                        Secured.class.getSimpleName()))
                .by(callFromMethod(SomeGuiController.class, "callServiceLayer")
                        .getting().field(ServiceHelper.class, "insecure")
                        .inLine(10))

                .ofRule(String.format("classes that reside in a package '..controller..' should "
                                + "only access members that are declared in a package '..controller..' or are annotated with @%s",
                        Secured.class.getSimpleName()))
                .by(callFromMethod(SomeController.class, "doSthController")
                        .toMethod(ServiceViolatingDaoRules.class, "doSthService")
                        .inLine(11))
                .by(callFromMethod(SomeGuiController.class, "callServiceLayer")
                        .toConstructor(ServiceHelper.class)
                        .inLine(7))
                .by(callFromMethod(SomeGuiController.class, "callServiceLayer")
                        .getting().field(ServiceHelper.class, "insecure")
                        .inLine(10))
                .by(callFromConstructor(UseCaseTwoController.class)
                        .toConstructor(AbstractController.class)
                        .inLine(6))
                .by(callFromMethod(SomeEnum.class, "values")
                        .toMethod(SomeEnum[].class, "clone")
                        .inLine(3))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> CyclicDependencyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.CyclicDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.CyclicDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.CyclicDependencyRulesTest.class)

                .ofRule("slices matching '..(simplecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of simplecycle")
                        .by(callFromMethod(SliceOneCallingMethodInSliceTwo.class, "callSliceTwo")
                                .toMethod(SliceTwoCallingMethodOfSliceThree.class, "doSomethingInSliceTwo")
                                .inLine(9))
                        .by(field(SliceOneCallingMethodInSliceTwo.class, "classInSliceTwo")
                                .ofType(SliceTwoCallingMethodOfSliceThree.class))
                        .from("slice2 of simplecycle")
                        .by(callFromMethod(SliceTwoCallingMethodOfSliceThree.class, "callSliceThree")
                                .toMethod(SliceThreeCallingMethodOfSliceOne.class, "doSomethingInSliceThree")
                                .inLine(9))
                        .by(field(SliceTwoCallingMethodOfSliceThree.class, "classInSliceThree")
                                .ofType(SliceThreeCallingMethodOfSliceOne.class))
                        .from("slice3 of simplecycle")
                        .by(callFromMethod(SliceThreeCallingMethodOfSliceOne.class, "callSliceOne")
                                .toMethod(SomeClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(9))
                        .by(field(SliceThreeCallingMethodOfSliceOne.class, "someClassInSliceOne")
                                .ofType(SomeClassBeingCalledInSliceOne.class)))

                .ofRule("slices matching '..(constructorcycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of constructorcycle")
                        .by(callFromMethod(SliceOneCallingConstructorInSliceTwo.class, "callSliceTwo")
                                .toConstructor(SliceTwoCallingConstructorInSliceOne.class)
                                .inLine(7))
                        .from("slice2 of constructorcycle")
                        .by(callFromMethod(SliceTwoCallingConstructorInSliceOne.class, "callSliceOne")
                                .toConstructor(SomeClassWithCalledConstructor.class)
                                .inLine(7)))

                .ofRule("slices matching '..(inheritancecycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceTwo.class)
                                .extending(ClassThatInheritsFromSliceOne.class))
                        .by(callFromConstructor(ClassThatInheritsFromSliceTwo.class)
                                .toConstructor(ClassThatInheritsFromSliceOne.class)
                                .inLine(5))
                        .from("slice2 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatInheritsFromSliceOne.class)
                                .extending(ClassThatIsInheritedFromSliceTwo.class))
                        .by(callFromConstructor(ClassThatInheritsFromSliceOne.class)
                                .toConstructor(ClassThatIsInheritedFromSliceTwo.class)
                                .inLine(5)))
                .by(cycle()
                        .from("slice1 of inheritancecycle")
                        .by(callFromConstructor(ClassThatCallSliceThree.class)
                                .toConstructor(ClassThatImplementsInterfaceFromSliceOne.class)
                                .inLine(7))
                        .from("slice3 of inheritancecycle")
                        .by(inheritanceFrom(ClassThatImplementsInterfaceFromSliceOne.class)
                                .implementing(InterfaceInSliceOne.class)))

                .ofRule("slices matching '..(fieldaccesscycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of fieldaccesscycle")
                        .by(field(SliceOneAccessingFieldInSliceTwo.class, "classInSliceTwo")
                                .ofType(SliceTwoAccessingFieldInSliceOne.class))
                        .by(callFromMethod(SliceOneAccessingFieldInSliceTwo.class, "accessSliceTwo")
                                .setting().field(SliceTwoAccessingFieldInSliceOne.class, "accessedField")
                                .inLine(9))
                        .from("slice2 of fieldaccesscycle")
                        .by(field(SliceTwoAccessingFieldInSliceOne.class, "classInSliceOne")
                                .ofType(ClassInSliceOneWithAccessedField.class))
                        .by(callFromMethod(SliceTwoAccessingFieldInSliceOne.class, "accessSliceOne")
                                .setting().field(ClassInSliceOneWithAccessedField.class, "accessedField")
                                .inLine(10)))

                .ofRule("slices matching '..(membercycle).(*)..' should be free of cycles")
                .by(cycle()
                        .from("slice1 of membercycle")
                        .by(field(SliceOneWithFieldTypeInSliceTwo.class, "classInSliceTwo")
                                .ofType(SliceTwoWithMethodParameterTypeInSliceThree.class))
                        .from("slice2 of membercycle")
                        .by(method(SliceTwoWithMethodParameterTypeInSliceThree.class, "methodWithParameterInSliceThree")
                                .withParameter(SliceThreeWithMethodReturnTypeInSliceFour.class))
                        .from("slice3 of membercycle")
                        .by(method(SliceThreeWithMethodReturnTypeInSliceFour.class, "methodWithReturnTypeInSliceFour")
                                .withReturnType(SliceFourWithConstructorParameterInSliceOne.class))
                        .from("slice4 of membercycle")
                        .by(constructor(SliceFourWithConstructorParameterInSliceOne.class)
                                .withParameter(SliceOneWithFieldTypeInSliceTwo.class)))

                .ofRule("slices matching '..simplescenario.(*)..' should be free of cycles")
                .by(cycle().from("administration")
                        .by(callFromMethod(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(ReportService.class, "getReport", String.class)
                                .inLine(12))
                        .by(field(AdministrationService.class, "reportService")
                                .ofType(ReportService.class))
                        .by(callFromMethod(AdministrationService.class, "saveNewInvoice", Invoice.class)
                                .toMethod(Report.class, "isEmpty")
                                .inLine(13))
                        .from("report")
                        .by(callFromMethod(ReportService.class, "getReport", String.class)
                                .toMethod(ImportService.class, "process", String.class)
                                .inLine(10))
                        .by(field(ReportService.class, "importService")
                                .ofType(ImportService.class))
                        .from("importer")
                        .by(callFromMethod(ImportService.class, "process", String.class)
                                .toMethod(AdministrationService.class, "createCustomerId", String.class)
                                .inLine(11))
                        .by(field(ImportService.class, "administrationService")
                                .ofType(AdministrationService.class)))

                .ofRule("slices matching '..(complexcycles).(*)..' should be free of cycles")
                .by(cycleFromComplexSlice1To2())
                .by(cycleFromComplexSlice1To2To3To5())
                .by(cycle().from("slice1 of complexcycles")
                        .by(callFromMethod(ClassOfMinimalCycleCallingSliceTwo.class, "callSliceTwo")
                                .toMethod(ClassOfMinimalCycleCallingSliceOne.class, "callSliceOne")
                                .inLine(9))
                        .by(callFromMethod(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                                .toConstructor(InstantiatedClassInSliceTwo.class)
                                .inLine(10))
                        .from("slice2 of complexcycles")
                        .by(callFromMethod(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .toConstructor(ClassWithAccessedFieldCallingMethodInSliceOne.class)
                                .inLine(8))
                        .by(callFromMethod(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class, "accessSliceFour")
                                .setting().field(ClassWithAccessedFieldCallingMethodInSliceOne.class, "accessedField")
                                .inLine(8))
                        .from("slice4 of complexcycles")
                        .by(callFromMethod(ClassWithAccessedFieldCallingMethodInSliceOne.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(10))
                        .by(field(ClassWithAccessedFieldCallingMethodInSliceOne.class, "classInSliceOne")
                                .ofType(ClassBeingCalledInSliceOne.class)))
                .by(cycle().from("slice1 of complexcycles")
                        .by(callFromMethod(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceThree")
                                .toMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .inLine(14))
                        .by(field(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "classInSliceThree")
                                .ofType(ClassCallingConstructorInSliceFive.class))
                        .from("slice3 of complexcycles")
                        .by(callFromMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                                .toConstructor(InstantiatedClassInSliceFive.class)
                                .inLine(7))
                        .from("slice5 of complexcycles")
                        .by(callFromMethod(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toConstructor(ClassBeingCalledInSliceOne.class)
                                .inLine(7))
                        .by(callFromMethod(InstantiatedClassInSliceFive.class, "callSliceOne")
                                .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                                .inLine(7)))

                .ofRule("Slices of complex scenario ignoring some violations should be free of cycles")
                .by(cycleFromComplexSlice1To2())
                .by(cycleFromComplexSlice1To2To3To5())

                .ofRule("slices assigned from complex slice one or two should be free of cycles")
                .by(cycleFromComplexSlice1To2("Complex-Cycle[One]", "Complex-Cycle[Two]"))

                .toDynamicTests();
    }

    private static CyclicErrorMatcher cycleFromComplexSlice1To2() {
        return cycleFromComplexSlice1To2("slice1 of complexcycles", "slice2 of complexcycles");
    }

    private static CyclicErrorMatcher cycleFromComplexSlice1To2(String sliceOneDescription, String sliceTwoDescription) {
        return cycle()
                .from(sliceOneDescription)
                .by(callFromMethod(ClassOfMinimalCycleCallingSliceTwo.class, "callSliceTwo")
                        .toMethod(ClassOfMinimalCycleCallingSliceOne.class, "callSliceOne")
                        .inLine(9))
                .by(field(ClassOfMinimalCycleCallingSliceTwo.class, "classInSliceTwo")
                        .ofType(ClassOfMinimalCycleCallingSliceOne.class))
                .by(callFromMethod(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                        .toConstructor(InstantiatedClassInSliceTwo.class)
                        .inLine(10))
                .from(sliceTwoDescription)
                .by(inheritanceFrom(SliceTwoInheritingFromSliceOne.class)
                        .extending(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class))
                .by(field(ClassOfMinimalCycleCallingSliceOne.class, "classInSliceOne")
                        .ofType(ClassOfMinimalCycleCallingSliceTwo.class))
                .by(callFromConstructor(SliceTwoInheritingFromSliceOne.class)
                        .toConstructor(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class).inLine(5))
                .by(callFromMethod(ClassOfMinimalCycleCallingSliceOne.class, "callSliceOne")
                        .toMethod(ClassOfMinimalCycleCallingSliceTwo.class, "callSliceTwo")
                        .inLine(9));
    }

    private static CyclicErrorMatcher cycleFromComplexSlice1To2To3To5() {
        return cycle().from("slice1 of complexcycles")
                .by(callFromMethod(ClassOfMinimalCycleCallingSliceTwo.class, "callSliceTwo")
                        .toMethod(ClassOfMinimalCycleCallingSliceOne.class, "callSliceOne")
                        .inLine(9))
                .by(callFromMethod(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, "callSliceTwo")
                        .toConstructor(InstantiatedClassInSliceTwo.class)
                        .inLine(10))
                .from("slice2 of complexcycles")
                .by(inheritanceFrom(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class)
                        .extending(InheritedClassInSliceThree.class))
                .by(callFromConstructor(SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour.class)
                        .toConstructor(InheritedClassInSliceThree.class)
                        .inLine(6))
                .from("slice3 of complexcycles")
                .by(callFromMethod(ClassCallingConstructorInSliceFive.class, "callSliceFive")
                        .toConstructor(InstantiatedClassInSliceFive.class)
                        .inLine(7))
                .from("slice5 of complexcycles")
                .by(callFromMethod(InstantiatedClassInSliceFive.class, "callSliceOne")
                        .toConstructor(ClassBeingCalledInSliceOne.class)
                        .inLine(7))
                .by(callFromMethod(InstantiatedClassInSliceFive.class, "callSliceOne")
                        .toMethod(ClassBeingCalledInSliceOne.class, "doSomethingInSliceOne")
                        .inLine(7));
    }

    @TestFactory
    Stream<DynamicTest> DaoRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.DaoRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.DaoRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.DaoRulesTest.class)

                .ofRule("DAOs should reside in a package '..dao..'")
                .by(javaPackageOf(InWrongPackageDao.class).notMatching("..dao.."))

                .ofRule("Entities should reside in a package '..domain..'")
                .by(javaPackageOf(EntityInWrongPackage.class).notMatching("..domain.."))

                .ofRule("Only DAOs may use the " + EntityManager.class.getSimpleName())
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(26))
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(ServiceViolatingDaoRules.MyEntityManager.class, "persist", Object.class)
                        .inLine(27))

                .ofRule("no methods that are declared in classes that have name matching '.*Dao' "
                        + String.format("should declare throwable of type %s", SQLException.class.getName()))
                .by(ExpectedMethod.of(OtherDao.class, "testConnection").throwsException(SQLException.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> FrozenRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.FrozenRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.FrozenRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.FrozenRulesTest.class)

                .ofRule("no classes should depend on classes that reside in a package '..service..'")
                .by(callFromMethod(SomeController.class, "doSthController").
                        toMethod(ServiceViolatingDaoRules.class, "doSthService")
                        .inLine(11)
                        .asDependency())
                .by(callFromMethod(SomeController.class, "doSthWithSecuredService").
                        toMethod(ServiceViolatingLayerRules.class, "properlySecured")
                        .inLine(15)
                        .asDependency())

                .ofRule("no classes should depend on classes that are assignable to javax.persistence.EntityManager")
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager").
                        toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(26)
                        .asDependency())
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager").
                        toMethod(ServiceViolatingDaoRules.MyEntityManager.class, "persist", Object.class)
                        .inLine(27)
                        .asDependency())

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> InterfaceRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.InterfaceRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.InterfaceRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.InterfaceRulesTest.class)

                .ofRule("no classes that are interfaces should have name matching '.*Interface'")
                .by(clazz(SomeBusinessInterface.class).havingNameMatching(".*Interface"))

                .ofRule("no classes that are interfaces should have simple name containing 'Interface'")
                .by(clazz(SomeBusinessInterface.class).havingSimpleNameContaining("Interface"))
                .by(clazz(SomeInterfacePlacedInTheWrongPackage.class).havingSimpleNameContaining("Interface"))

                .ofRule("no classes that reside in a package '..impl..' should be interfaces")
                .by(clazz(SomeInterfacePlacedInTheWrongPackage.class).beingAnInterface())

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> LayerDependencyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.LayerDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.LayerDependencyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.LayerDependencyRulesTest.class)

                .ofRule("no classes that reside in a package '..service..' " +
                        "should access classes that reside in a package '..controller..'")
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, someString)
                        .inLine(25))
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(26))
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(27))

                .ofRule("no classes that reside in a package '..persistence..' should " +
                        "access classes that reside in a package '..service..'")
                .by(callFromMethod(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14))

                .ofRule("classes that reside in a package '..service..' should " +
                        "only be accessed by any package ['..controller..', '..service..']")
                .by(callFromMethod(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14))
                .by(callFromMethod(SomeMediator.class, violateLayerRulesIndirectly)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(15))

                .ofRule("classes that reside in a package '..service..' should "
                        + "only access classes that reside in any package ['..service..', '..persistence..', 'java..']")
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, UseCaseOneTwoController.someString)
                        .inLine(25))
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(26))
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, UseCaseTwoController.doSomethingTwo)
                        .inLine(27))

                .ofRule("no classes that reside in a package '..service..' " +
                        "should depend on classes that reside in a package '..controller..'")
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, someString)
                        .inLine(25).asDependency())
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(26).asDependency())
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(27).asDependency())
                .by(method(ServiceViolatingLayerRules.class, dependentMethod).withParameter(UseCaseTwoController.class))
                .by(method(ServiceViolatingLayerRules.class, dependentMethod).withReturnType(SomeGuiController.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(ComplexControllerAnnotation.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SimpleControllerAnnotation.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SomeEnum.class))
                .by(method(ComplexServiceAnnotation.class, "controllerAnnotation").withReturnType(ComplexControllerAnnotation.class))
                .by(method(ComplexServiceAnnotation.class, "controllerEnum").withReturnType(SomeEnum.class))

                .ofRule("no classes that reside in a package '..persistence..' should " +
                        "depend on classes that reside in a package '..service..'")
                .by(callFromMethod(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14).asDependency())
                .by(field(DaoCallingService.class, "service").ofType(ServiceViolatingLayerRules.class))
                .by(inheritanceFrom(DaoCallingService.class).implementing(ServiceInterface.class))

                .ofRule("classes that reside in a package '..service..' should " +
                        "only have dependent classes that reside in any package ['..controller..', '..service..']")
                .by(callFromMethod(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(14).asDependency())
                .by(callFromMethod(SomeMediator.class, violateLayerRulesIndirectly)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(15).asDependency())
                .by(inheritanceFrom(DaoCallingService.class).implementing(ServiceInterface.class))
                .by(constructor(SomeMediator.class).withParameter(ServiceViolatingLayerRules.class))
                .by(field(SomeMediator.class, "service").ofType(ServiceViolatingLayerRules.class))
                .by(field(DaoCallingService.class, "service").ofType(ServiceViolatingLayerRules.class))

                .ofRule("classes that reside in a package '..service..' should "
                        + "only depend on classes that reside in any package ['..service..', '..persistence..', 'java..', 'javax..']")
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, someString)
                        .inLine(25).asDependency())
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(26).asDependency())
                .by(callFromMethod(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(27).asDependency())
                .by(method(ServiceViolatingLayerRules.class, dependentMethod)
                        .withParameter(UseCaseTwoController.class))
                .by(method(ServiceViolatingLayerRules.class, dependentMethod)
                        .withReturnType(SomeGuiController.class))
                .by(field(ServiceHelper.class, "properlySecured")
                        .withAnnotationType(Secured.class))
                .by(method(ServiceViolatingLayerRules.class, "properlySecured")
                        .withAnnotationType(Secured.class))
                .by(constructor(ServiceHelper.class)
                        .withAnnotationType(Secured.class))
                .by(annotatedClass(ServiceViolatingDaoRules.class).annotatedWith(MyService.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).annotatedWith(MyService.class))
                .by(annotatedClass(ServiceImplementation.class).annotatedWith(MyService.class))
                .by(annotatedClass(WronglyNamedSvc.class).annotatedWith(MyService.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(ComplexControllerAnnotation.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SimpleControllerAnnotation.class))
                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SomeEnum.class))
                .by(method(ComplexServiceAnnotation.class, "controllerAnnotation").withReturnType(ComplexControllerAnnotation.class))
                .by(method(ComplexServiceAnnotation.class, "controllerEnum").withReturnType(SomeEnum.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> LayeredArchitectureTest() {
        BiConsumer<String, ExpectedTestFailures> addExpectedCommonFailure =
                (memberName, expectedTestFailures) ->
                        expectedTestFailures
                                .ofRule(memberName,
                                        "Layered architecture consisting of" + lineSeparator() +
                                                "layer 'Controllers' ('com.tngtech.archunit.example.layers.controller..')" + lineSeparator() +
                                                "layer 'Services' ('com.tngtech.archunit.example.layers.service..')" + lineSeparator() +
                                                "layer 'Persistence' ('com.tngtech.archunit.example.layers.persistence..')" + lineSeparator() +
                                                "where layer 'Controllers' may not be accessed by any layer" + lineSeparator() +
                                                "where layer 'Services' may only be accessed by layers ['Controllers']" + lineSeparator() +
                                                "where layer 'Persistence' may only be accessed by layers ['Services']")

                                .by(inheritanceFrom(DaoCallingService.class)
                                        .implementing(ServiceInterface.class))

                                .by(field(DaoCallingService.class, "service").ofType(ServiceViolatingLayerRules.class))

                                .by(callFromMethod(DaoCallingService.class, "violateLayerRules")
                                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                                        .inLine(14)
                                        .asDependency())

                                .by(callFromMethod(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .toConstructor(UseCaseTwoController.class)
                                        .inLine(26)
                                        .asDependency())

                                .by(callFromMethod(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .toMethod(UseCaseTwoController.class, "doSomethingTwo")
                                        .inLine(27)
                                        .asDependency())

                                .by(callFromMethod(ServiceViolatingLayerRules.class, "illegalAccessToController")
                                        .getting().field(UseCaseOneTwoController.class, "someString")
                                        .inLine(25)
                                        .asDependency())

                                .by(method(ServiceViolatingLayerRules.class, dependentMethod).withParameter(UseCaseTwoController.class))

                                .by(method(ServiceViolatingLayerRules.class, dependentMethod).withReturnType(SomeGuiController.class))

                                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(ComplexControllerAnnotation.class))
                                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SimpleControllerAnnotation.class))
                                .by(annotatedClass(ServiceViolatingLayerRules.class).withAnnotationParameterType(SomeEnum.class))
                                .by(method(ComplexServiceAnnotation.class, "controllerAnnotation").withReturnType(ComplexControllerAnnotation.class))
                                .by(method(ComplexServiceAnnotation.class, "controllerEnum").withReturnType(SomeEnum.class));

        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.LayeredArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit4.LayeredArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit5.LayeredArchitectureTest.class);

        addExpectedCommonFailure.accept("layer_dependencies_are_respected", expectedTestFailures);
        expectedTestFailures
                .by(callFromMethod(SomeMediator.class, "violateLayerRulesIndirectly")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(15)
                        .asDependency())
                .by(constructor(SomeMediator.class).withParameter(ServiceViolatingLayerRules.class))
                .by(field(SomeMediator.class, "service").ofType(ServiceViolatingLayerRules.class));

        addExpectedCommonFailure.accept("layer_dependencies_are_respected_with_exception", expectedTestFailures);

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> OnionArchitectureTest() {
        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.OnionArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit4.OnionArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit5.OnionArchitectureTest.class)

                .ofRule("Onion architecture consisting of" + lineSeparator() +
                        "domain models ('..domain.model..')" + lineSeparator() +
                        "domain services ('..domain.service..')" + lineSeparator() +
                        "application services ('..application..')" + lineSeparator() +
                        "adapter 'cli' ('..adapter.cli..')" + lineSeparator() +
                        "adapter 'persistence' ('..adapter.persistence..')" + lineSeparator() +
                        "adapter 'rest' ('..adapter.rest..')")

                .by(constructor(OrderItem.class).withParameter(OrderQuantity.class))
                .by(constructor(com.tngtech.archunit.example.onionarchitecture.domain.model.Product.class).withParameter(ProductId.class))
                .by(constructor(com.tngtech.archunit.example.onionarchitecture.domain.model.Product.class).withParameter(ProductName.class))
                .by(constructor(ShoppingCart.class).withParameter(ShoppingCartId.class))
                .by(constructor(ShoppingService.class).withParameter(ProductRepository.class))
                .by(constructor(ShoppingService.class).withParameter(ShoppingCartRepository.class))

                .by(field(OrderItem.class, "quantity").ofType(OrderQuantity.class))
                .by(field(com.tngtech.archunit.example.onionarchitecture.domain.model.Product.class, "id").ofType(ProductId.class))
                .by(field(com.tngtech.archunit.example.onionarchitecture.domain.model.Product.class, "name").ofType(ProductName.class))
                .by(field(ShoppingCart.class, "id").ofType(ShoppingCartId.class))
                .by(field(ShoppingService.class, "productRepository").ofType(ProductRepository.class))
                .by(field(ShoppingService.class, "shoppingCartRepository").ofType(ShoppingCartRepository.class))

                .by(callFromMethod(AdministrationCLI.class, "handle", String[].class, AdministrationPort.class)
                        .toMethod(ProductRepository.class, "getTotalCount")
                        .inLine(17).asDependency())
                .by(callFromMethod(ShoppingController.class, "addToShoppingCart", UUID.class, UUID.class, int.class)
                        .toConstructor(ProductId.class, UUID.class)
                        .inLine(20).asDependency())
                .by(callFromMethod(ShoppingController.class, "addToShoppingCart", UUID.class, UUID.class, int.class)
                        .toConstructor(ShoppingCartId.class, UUID.class)
                        .inLine(20).asDependency())
                .by(method(ShoppingService.class, "addToShoppingCart").withParameter(ProductId.class))
                .by(method(ShoppingService.class, "addToShoppingCart").withParameter(ShoppingCartId.class))
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ShoppingCartRepository.class, "read", ShoppingCartId.class)
                        .inLine(21).asDependency())
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ProductRepository.class, "read", ProductId.class)
                        .inLine(22).asDependency())
                .by(callFromMethod(ShoppingService.class, "addToShoppingCart", ShoppingCartId.class, ProductId.class, OrderQuantity.class)
                        .toMethod(ShoppingCartRepository.class, "save", ShoppingCart.class)
                        .inLine(25).asDependency());

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> MethodsTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.MethodsTest.class,
                        com.tngtech.archunit.exampletest.junit4.MethodsTest.class,
                        com.tngtech.archunit.exampletest.junit5.MethodsTest.class)

                .ofRule("methods that are declared in classes that reside in a package '..anticorruption..' and are public "
                        + String.format("should have raw return type %s, ", WrappedResult.class.getName())
                        + "because we do not want to couple the client code directly to the return types of the encapsulated module")
                .by(ExpectedMethod.of(WithIllegalReturnType.class, "directlyReturnInternalType").toNotHaveRawReturnType(WrappedResult.class))
                .by(ExpectedMethod.of(WithIllegalReturnType.class, "otherIllegalMethod", String.class).toNotHaveRawReturnType(WrappedResult.class))

                .ofRule("no code units that are declared in classes that reside in a package '..persistence..' "
                        + "should be annotated with @" + Secured.class.getSimpleName())
                .by(ExpectedConstructor.of(SomeJpa.class).beingAnnotatedWith(Secured.class))
                .by(ExpectedMethod.of(OtherJpa.class, "getEntityManager").beingAnnotatedWith(Secured.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> NamingConventionTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.NamingConventionTest.class,
                        com.tngtech.archunit.exampletest.junit4.NamingConventionTest.class,
                        com.tngtech.archunit.exampletest.junit5.NamingConventionTest.class)

                .ofRule(String.format(
                        "classes that reside in a package '..service..' "
                                + "and are annotated with @%s "
                                + "should have simple name starting with 'Service'",
                        MyService.class.getSimpleName()))
                .by(simpleNameOf(WronglyNamedSvc.class).notStartingWith("Service"))

                .ofRule("classes that reside in a package '..controller..' should have simple name not containing 'Gui'")
                .by(simpleNameOf(SomeGuiController.class).containing("Gui"))

                .ofRule(String.format(
                        "classes that reside in a package '..controller..' "
                                + "or are annotated with @%s "
                                + "or are assignable to %s "
                                + "should have simple name ending with 'Controller'",
                        MyController.class.getSimpleName(), AbstractController.class.getName()))
                .by(simpleNameOf(InheritedControllerImpl.class).notEndingWith("Controller"))
                .by(simpleNameOf(ComplexControllerAnnotation.class).notEndingWith("Controller"))
                .by(simpleNameOf(SimpleControllerAnnotation.class).notEndingWith("Controller"))
                .by(simpleNameOf(SomeUtility.class).notEndingWith("Controller"))
                .by(simpleNameOf(WronglyAnnotated.class).notEndingWith("Controller"))
                .by(simpleNameOf(SomeEnum.class).notEndingWith("Controller"))
                .by(simpleNameOfAnonymousClassOf(UseCaseOneThreeController.class).notEndingWith("Controller"))

                .ofRule("classes that have simple name containing 'Controller' should reside in a package '..controller..'")
                .by(javaClass(AbstractController.class).notResidingIn("..controller.."))
                .by(javaClass(AnnotatedController.class).notResidingIn("..controller.."))
                .by(javaClass(InheritedControllerImpl.class).notResidingIn("..controller.."))
                .by(javaClass(MyController.class).notResidingIn("..controller.."))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> PlantUmlArchitectureTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.PlantUmlArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit4.PlantUmlArchitectureTest.class,
                        com.tngtech.archunit.exampletest.junit5.PlantUmlArchitectureTest.class)

                .ofRule("classes should adhere to PlantUML diagram <shopping_example.puml>"
                        + " while ignoring dependencies outside of packages ['..catalog']")
                .by(field(Address.class, "productCatalog")
                        .ofType(ProductCatalog.class))

                .ofRule("classes should adhere to PlantUML diagram <shopping_example.puml>"
                        + " while ignoring dependencies not contained in the diagram")
                .by(field(Address.class, "productCatalog")
                        .ofType(ProductCatalog.class))
                .by(field(Product.class, "customer")
                        .ofType(Customer.class))
                .by(method(Product.class, "getOrder")
                        .withReturnType(Order.class))
                .by(method(Customer.class, "addOrder")
                        .withParameter(Order.class))
                .by(callFromMethod(ProductCatalog.class, "gonnaDoSomethingIllegalWithOrder")
                        .toConstructor(Order.class).inLine(12).asDependency())
                .by(callFromMethod(ProductCatalog.class, "gonnaDoSomethingIllegalWithOrder")
                        .toMethod(Order.class, "addProducts", Set.class).inLine(16).asDependency())
                .by(callFromMethod(ProductImport.class, "getCustomer")
                        .toConstructor(Customer.class).inLine(14).asDependency())
                .by(method(ProductImport.class, "getCustomer")
                        .withReturnType(Customer.class))
                .by(method(Order.class, "report")
                        .withParameter(Address.class))

                .ofRule(String.format("classes should adhere to PlantUML diagram <shopping_example.puml>,"
                                + " ignoring dependencies with origin equivalent to %s,"
                                + " ignoring dependencies with target that is part of JDK,"
                                + " ignoring dependencies from %s to %s",
                        ProductCatalog.class.getName(), Product.class.getName(), Order.class.getName()))
                .by(field(Address.class, "productCatalog")
                        .ofType(ProductCatalog.class))
                .by(field(Product.class, "customer")
                        .ofType(Customer.class))
                .by(method(Customer.class, "addOrder")
                        .withParameter(Order.class))
                .by(callFromMethod(ProductImport.class, "getCustomer")
                        .toConstructor(Customer.class).inLine(14).asDependency())
                .by(method(ProductImport.class, "getCustomer")
                        .withReturnType(Customer.class))
                .by(method(Order.class, "report")
                        .withParameter(Address.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> RestrictNumberOfClassesWithACertainPropertyTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.RestrictNumberOfClassesWithACertainPropertyTest.class,
                        com.tngtech.archunit.exampletest.junit4.RestrictNumberOfClassesWithACertainPropertyTest.class,
                        com.tngtech.archunit.exampletest.junit5.RestrictNumberOfClassesWithACertainPropertyTest.class)

                .ofRule(String.format(
                        "classes that implement %s should contain number of elements less than or equal to '1', "
                                + "because from now on new classes should implement %s",
                        SomeBusinessInterface.class.getName(), SomeOtherBusinessInterface.class.getName()))
                .by(classesContaining(ClassViolatingSessionBeanRules.class, SecondBeanImplementingSomeBusinessInterface.class))

                .toDynamicTests();

    }

    private static MessageAssertionChain.Link classesContaining(final Class<?>... classes) {
        final String expectedLine = String.format("there is/are %d element(s) in classes %s", classes.length, namesOf(classes));
        return new MessageAssertionChain.Link() {
            @Override
            public Result filterMatching(List<String> lines) {
                List<String> rest = new ArrayList<>();
                for (String line : lines) {
                    if (!line.equals(expectedLine)) {
                        rest.add(line);
                    }
                }
                boolean matches = (rest.size() == lines.size() - 1);
                return new Result(matches, rest, String.format("No line matched '%s'", expectedLine));
            }

            @Override
            public String getDescription() {
                return "classes containing " + namesOf(classes);
            }
        };
    }

    // TODO: This can at the moment not really be covered by JUnit support, but probably should be...
    @TestFactory
    Stream<DynamicTest> SecurityTest() {
        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(SecurityTest.class);

        Consumer<String> addExpectedFailure = ruleText -> expectedTestFailures.ofRule(ruleText)
                .by(callFromMethod(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "getInstance", String.class)
                        .inLine(19))
                .by(callFromMethod(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "generateCertificate", InputStream.class)
                        .inLine(19));

        addExpectedFailure.accept("classes that reside in a package 'java.security..' "
                + "should only be accessed by any package ['..example.layers.security..', 'java.security..'], "
                + "because we want to have one isolated cross-cutting concern 'security'");

        addExpectedFailure.accept("classes that reside in a package 'java.security.cert..' "
                + "should only be accessed by any package ['..example.layers.security..', 'java..', '..sun..', 'javax..', 'apple.security..']");

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> SessionBeanRulesTest() {
        MessageAssertionChain.Link someBusinessInterfaceIsImplementedByTwoBeans =
                new MessageAssertionChain.Link() {
                    @Override
                    public Result filterMatching(List<String> lines) {
                        Collection<String> interesting = filter(lines, containsPattern(" is implemented by "));
                        if (interesting.size() != 1) {
                            return new Result(false, lines);
                        }
                        String[] parts = interesting.iterator().next().split(" is implemented by ");
                        if (parts.length != 2) {
                            return new Result(false, lines);
                        }

                        if (partsMatchExpectedViolation(parts)) {
                            List<String> resultLines = new ArrayList<>(lines);
                            resultLines.removeAll(interesting);
                            return new Result(true, resultLines);
                        } else {
                            return new Result(false, lines);
                        }
                    }

                    private boolean partsMatchExpectedViolation(String[] parts) {
                        ImmutableSet<String> violations = ImmutableSet.copyOf(parts[1].split(", "));
                        return parts[0].equals(SomeBusinessInterface.class.getSimpleName()) &&
                                violations.equals(ImmutableSet.of(
                                        ClassViolatingSessionBeanRules.class.getSimpleName(),
                                        SecondBeanImplementingSomeBusinessInterface.class.getSimpleName()));
                    }

                    @Override
                    public String getDescription() {
                        String violatingImplementations = Joiner.on(", ").join(
                                ClassViolatingSessionBeanRules.class.getSimpleName(),
                                SecondBeanImplementingSomeBusinessInterface.class.getSimpleName());

                        return String.format("Message contains: %s is implemented by {%s}",
                                SomeBusinessInterface.class.getSimpleName(), violatingImplementations);
                    }
                };

        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.SessionBeanRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.SessionBeanRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.SessionBeanRulesTest.class)

                .ofRule("No Stateless Session Bean should have state")
                .by(callFromMethod(ClassViolatingSessionBeanRules.class, "setState", String.class)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(25))
                .by(callFromMethod(OtherClassViolatingSessionBeanRules.class, init)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(13))

                .ofRule("classes that are business interfaces should have a unique implementation")
                .by(someBusinessInterfaceIsImplementedByTwoBeans)

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> SingleClassTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.SingleClassTest.class,
                        com.tngtech.archunit.exampletest.junit4.SingleClassTest.class,
                        com.tngtech.archunit.exampletest.junit5.SingleClassTest.class)

                .ofRule(String.format("the class %s should only be accessed by classes that implement %s",
                        VeryCentralCore.class.getName(), CoreSatellite.class.getName()))
                .by(callFromMethod(EvilCoreAccessor.class, "iShouldNotAccessCore")
                        .toConstructor(VeryCentralCore.class)
                        .inLine(8))
                .by(callFromMethod(EvilCoreAccessor.class, "iShouldNotAccessCore")
                        .toMethod(VeryCentralCore.class, DO_CORE_STUFF_METHOD_NAME)
                        .inLine(8))

                .ofRule(String.format("no class %s should access classes that reside outside of packages ['..core..', 'java..']",
                        VeryCentralCore.class.getName()))
                .by(callFromMethod(VeryCentralCore.class, "coreDoingIllegalStuff")
                        .toConstructor(AnnotatedController.class)
                        .inLine(15))

                .ofRule(String.format("classes that are annotated with @%s should be %s",
                        HighSecurity.class.getSimpleName(), VeryCentralCore.class.getName()))
                .by(ExpectedClass.javaClass(WronglyAnnotated.class).notBeing(VeryCentralCore.class))

                .ofRule(String.format("classes that implement %s should not be %s",
                        SomeOtherBusinessInterface.class.getName(), VeryCentralCore.class.getName()))
                .by(ExpectedClass.javaClass(VeryCentralCore.class).being(VeryCentralCore.class))

                .toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> SlicesIsolationTest() {
        BiConsumer<String, ExpectedTestFailures> addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice =
                (memberName, expectedTestFailures) ->
                        expectedTestFailures
                                .ofRule(memberName, "Controllers should not depend on each other")
                                .by(sliceDependency()
                                        .described("Controller one depends on Controller three")
                                        .by(callFromMethod(UseCaseOneThreeController.class, doSomethingOne)
                                                .toConstructor(UseCaseThreeController.class)
                                                .inLine(13))
                                        .by(callFromMethod(UseCaseOneThreeController.class, doSomethingOne)
                                                .toMethod(UseCaseThreeController.class, doSomethingThree)
                                                .inLine(13)))
                                .by(sliceDependency()
                                        .described("Controller two depends on Controller one")
                                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                                .toConstructor(UseCaseOneTwoController.class)
                                                .inLine(10))
                                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                                .inLine(10)));

        ExpectedTestFailures expectedTestFailures = ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.SlicesIsolationTest.class,
                        com.tngtech.archunit.exampletest.junit4.SlicesIsolationTest.class,
                        com.tngtech.archunit.exampletest.junit5.SlicesIsolationTest.class);

        // controllers_should_only_use_their_own_slice
        addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice
                .accept("controllers_should_only_use_their_own_slice", expectedTestFailures);
        expectedTestFailures.by(sliceDependency()
                .described("Controller one depends on Controller two")
                .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(10))
                .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(10)))
                .by(sliceDependency()
                        .described("Controller three depends on Controller one")
                        .by(callFromMethod(UseCaseThreeController.class, doSomethingThree)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(callFromMethod(UseCaseThreeController.class, doSomethingThree)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));

        // controllers_should_only_use_their_own_slice_with_custom_ignore
        addExpectedCommonFailureFor_controllers_should_only_use_their_own_slice
                .accept("controllers_should_only_use_their_own_slice_with_custom_ignore", expectedTestFailures);

        // specific_controllers_should_only_use_their_own_slice
        expectedTestFailures
                .ofRule("Controllers one and two should not depend on each other")
                .by(sliceDependency()
                        .described("Controller one depends on Controller two")
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toConstructor(UseCaseTwoController.class)
                                .inLine(10))
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toMethod(UseCaseTwoController.class, doSomethingTwo)
                                .inLine(10)))
                .by(sliceDependency()
                        .described("Controller two depends on Controller one")
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(10))
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(10)));

        return expectedTestFailures.toDynamicTests();
    }

    @TestFactory
    Stream<DynamicTest> ThirdPartyRulesTest() {
        return ExpectedTestFailures
                .forTests(
                        com.tngtech.archunit.exampletest.ThirdPartyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit4.ThirdPartyRulesTest.class,
                        com.tngtech.archunit.exampletest.junit5.ThirdPartyRulesTest.class)

                .ofRule(String.format("classes should not instantiate %s and its subclasses, but instead use %s",
                        ThirdPartyClassWithProblem.class.getSimpleName(),
                        ThirdPartyClassWorkaroundFactory.class.getSimpleName()))
                .by(callFromMethod(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartyClass")
                        .toConstructor(ThirdPartyClassWithProblem.class)
                        .inLine(9))
                .by(callFromMethod(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartySubClass")
                        .toConstructor(ThirdPartySubClassWithProblem.class)
                        .inLine(17))

                .toDynamicTests();
    }
}
