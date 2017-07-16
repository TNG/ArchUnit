'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const createDependencyBuilder = require('./main-files').get('dependency').buildDependency;
let buildDependency;

const CodeElement = {
  absent: {
    key: 0,
    title: ""
  },
  single: name => ({
    key: 1,
    title: name
  })
};

let buildDescription = () => ({
  withKinds: (inheritanceKind, accessKind) => ({
    withCodeElements: (startCodeUnit, targetElement) => ({
      inheritanceKind: inheritanceKind,
      accessKind: accessKind,
      startCodeUnit: startCodeUnit,
      targetElement: targetElement
    })
  })
});

describe("Dependency", () => {
  it("can be built by merging existing descriptions with different access groups", () => {
    let graphWrapper = testObjects.testGraph3();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.main.class1", to = "com.tngtech.interface1";

    let description1 = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    let description2 = buildDescription().withKinds("implements", "").withCodeElements(
        CodeElement.absent, CodeElement.absent);
    let act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    let exp = "startMethod(arg1, arg2) implements methodCall targetMethod()";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built by merging existing descriptions with same access groups but different access kinds", () => {
    let graphWrapper = testObjects.testGraph3();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    let description1 = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("field1"));
    let description2 = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("targetMethod()"));
    let description3 = buildDescription().withKinds("extends", "").withCodeElements(
        CodeElement.absent, CodeElement.absent);
    let act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    act = buildDependency(from, to).withMergedDescriptions(act.description, description3);
    let exp = "testclass1() extends several [...]";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a class", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    let description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("innertestclass1()"), CodeElement.single("field1"));
    let act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    let exp = "InnerTestClass1.innertestclass1() childrenAccess field1";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a package", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.test", to = "com.tngtech.class2";

    let description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("field1"));
    let act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1");
    let exp = "";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and target's parent is a class", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.main.class1", to = "com.tngtech.test.testclass1";

    let description = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    let act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    let exp = "startMethod(arg1, arg2) childrenAccess InnerTestClass1.targetMethod()";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and targets's parent is a package", () => {
    let graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    buildDependency = createDependencyBuilder(graphWrapper.graph.nodeMap);
    let from = "com.tngtech.main.class1", to = "com.tngtech.test";

    let description = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    let act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    let exp = "";
    expect(act.description.toString()).to.equal(exp);
  });
});
