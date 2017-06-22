'use strict';

require('./chai/dependencies-chai-extension');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

let depsOfTree2WithTestFolded = [
  "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
  "com.tngtech.test->com.tngtech.class2()",
  "com.tngtech.test->com.tngtech.main.class1()",
  "com.tngtech.test->com.tngtech.interface1()",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

let depsOfTree2WithMainFolded = [
  "com.tngtech.main->com.tngtech.interface1()",
  "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
  "com.tngtech.test.testclass1->com.tngtech.main()",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])",
  "com.tngtech.class2->com.tngtech.main()",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

describe("Dependencies", () => {

  it("are created correctly", () => {
    let graphWrapper = testObjects.testGraph1();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("are initially uniqued correctly", () => {
    let graphWrapper = testObjects.testGraph2();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("know if they must share their at least one of their end nodes", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    let hasEndNodes = (from, to) => d => (d.from === from || d.to === from) && (d.from === to || d.to === to);
    let filter = d => hasEndNodes("com.tngtech.test.subtest.subtestclass1", "com.tngtech.interface1")(d)
    || hasEndNodes("com.tngtech.class2", "com.tngtech.class2.InnerClass2")(d);
    graphWrapper.graph.getVisibleDependencies().filter(filter).forEach(d => expect(d.mustShareNodes).to.equal(true));
    graphWrapper.graph.getVisibleDependencies().filter(d => !filter(d)).forEach(d => expect(d.mustShareNodes).to.equal(false));
  });

  it("transform if origin is folded and origin is a package", () => {
    let graphWrapper = testObjects.testGraph2();
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(depsOfTree2WithTestFolded);
  });

  it("transform if target is folded and target is a package", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(depsOfTree2WithMainFolded);
  });

  it("transform if origin and target are folded and both are packages", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));

    let exp = [
      "com.tngtech.main->com.tngtech.interface1()",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main()",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);
  });

  it("transform if class with inner class is folded", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test.testclass1"));

    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) methodCall targetMethod())",
      "com.tngtech.main.class1->com.tngtech.test.testclass1(startMethod(arg1, arg2) childrenAccess InnerTestClass1.targetMethod())",
      "com.tngtech.test.testclass1->com.tngtech.class2([...] several field1)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(startMethod(arg) constructorCall testclass1())",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)",
      "com.tngtech.class2.InnerClass2->com.tngtech.class2(startCodeUnit() fieldAccess targetField)",
      "com.tngtech.class2->com.tngtech.class2.InnerClass2(startCodeUnit() fieldAccess innerTargetField)",
      "com.tngtech.interface1->com.tngtech.test.subtest.subtestclass1(startMethod() methodCall targetMethod())"
    ];

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);
  });

  it("transform reverse on unfold of a single package", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("transform reverse on unfold of several packages", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("transform reverse on unfold of a single package, when another package is folded", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(depsOfTree2WithMainFolded);
  });

  it("are uniqued and grouped correctly with complicated dependency structure", () => {
    let graphWrapper = testObjects.testGraph3();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.main.class3->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.main"));
    exp = [
      "com.tngtech.main->com.tngtech.interface1()",
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() extends several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main()",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])",
      "com.tngtech.class2->com.tngtech.main()",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);
  });

  it("does the filtering of classes (no dependencies of eliminated nodes) and resets them correctly", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByName("subtest", true);
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.filterNodesByName("", false);
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the following correctly (in this order): fold, filter, reset filter and unfold", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    graphWrapper.graph.filterNodesByName("subtest", true);
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.filterNodesByName("", false);
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(depsOfTree2WithTestFolded);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the following correctly (in this order): fold, filter, unfold and reset filter", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));

    graphWrapper.graph.filterNodesByName("subtest", true);
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.filterNodesByName("", false);
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the following correctly (in this order): filter, fold, unfold and reset the filter", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByName("subtest", true);
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));

    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.filterNodesByName("", false);
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the following correctly (in this order): filter, fold, reset the filter and unfold", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByName("subtest", true);
    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.filterNodesByName("", false);
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(depsOfTree2WithTestFolded);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));

    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the filtering by type (hiding interfaces) correctly (no dependencies of eliminated nodes) " +
      "and resets them correctly", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByType({showInterfaces: false, showClasses: true, showEmptyPackages: true});
    let exp = [
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])",
      "com.tngtech.class2->com.tngtech.main.class1(extends)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.resetFilterNodesByType();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the filtering by type (hiding classes) with eliminating packages correctly " +
      "(no dependencies of eliminated nodes) and resets them correctly", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByType({showInterfaces: true, showClasses: false, showEmptyPackages: false});
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies([]);

    graphWrapper.graph.resetFilterNodesByType();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the filtering by kind (only show inheritance) correctly and resets it", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterDependenciesByKind()
        .showImplementing(true)
        .showExtending(true)
        .showConstructorCall(false)
        .showMethodCall(false)
        .showFieldAccess(false)
        .showAnonymousImplementing(false)
        .showDepsBetweenChildAndParent(true);
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(implements)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.resetFilterDependenciesByKind();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("dies the filtering by kind (do not show inheritance) correcty and resets it", () => {
    let graphWrapper = testObjects.testGraph3();
    graphWrapper.graph.filterDependenciesByKind()
        .showImplementing(false)
        .showExtending(false)
        .showConstructorCall(true)
        .showMethodCall(true)
        .showFieldAccess(true)
        .showAnonymousImplementing(true)
        .showDepsBetweenChildAndParent(true);
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) methodCall targetMethod())",
      "com.tngtech.main.class3->com.tngtech.interface1(startMethod(arg1, arg2) methodCall targetMethod())",
      "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
      "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])"
    ];
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(exp);

    graphWrapper.graph.resetFilterDependenciesByKind();
    expect(graphWrapper.graph.getVisibleDependencies()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("lists correctly the detailed dependencies of class", () => {
    let graphWrapper = testObjects.testGraph2();

    let exp = [
      "testclass1()->field1",
      "testclass1()->targetMethod()"
    ];
    let act = graphWrapper.graph.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
        .map(d => d.description);
    expect(act).to.containExactlyDependencies(exp);
  });

  it("lists correctly the detailed dependencies of class with inner classes depending on the fold-state of the class", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();

    let act = graphWrapper.graph.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
        .map(d => d.description);
    let exp = [
      "testclass1()->field1"
    ];
    expect(act).to.containExactlyDependencies(exp);

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test.testclass1"));
    act = graphWrapper.graph.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
        .map(d => d.description);
    exp = [
      "testclass1()->field1",
      "InnerTestClass1.innertestclass1()->field1"
    ];
    expect(act).to.containExactlyDependencies(exp);
  });

  it("lists correctly the detailed dependencies of folded package", () => {
    let graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.changeFoldStateOfNode(graphWrapper.getNode("com.tngtech.test"));

    let exp = [
      "testclass1.testclass1()->field1",
      "testclass1.testclass1()->targetMethod()",
      "subtest.subtestclass1.startMethod1()->targetMethod()",
    ];
    let act = graphWrapper.graph.getDetailedDependenciesOf("com.tngtech.test", "com.tngtech.class2").map(d => d.description);
    expect(act).to.containExactlyDependencies(exp);
  });
});