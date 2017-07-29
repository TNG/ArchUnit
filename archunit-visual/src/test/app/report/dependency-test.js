'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const createDependencyBuilder = require('./main-files').get('dependency').buildDependency;

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

const buildDescription = () => ({
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
    const graphWrapper = testObjects.testGraph3();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.interface1";

    const description1 = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    const description2 = buildDescription().withKinds("implements", "").withCodeElements(
        CodeElement.absent, CodeElement.absent);
    const act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    const exp = "startMethod(arg1, arg2) implements methodCall targetMethod()";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built by merging existing descriptions with same access groups but different access kinds", () => {
    const graphWrapper = testObjects.testGraph3();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const description1 = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("field1"));
    const description2 = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("targetMethod()"));
    const description3 = buildDescription().withKinds("extends", "").withCodeElements(
        CodeElement.absent, CodeElement.absent);
    let act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    act = buildDependency(from, to).withMergedDescriptions(act.description, description3);
    const exp = "testclass1() extends several [...]";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("innertestclass1()"), CodeElement.single("field1"));
    const act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "InnerTestClass1.innertestclass1() childrenAccess field1";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test", to = "com.tngtech.class2";

    const description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
        CodeElement.single("testclass1()"), CodeElement.single("field1"));
    const act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and target's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.test.testclass1";

    const description = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    const act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "startMethod(arg1, arg2) childrenAccess InnerTestClass1.targetMethod()";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and targets's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.test";

    const description = buildDescription().withKinds("", "methodCall").withCodeElements(
        CodeElement.single("startMethod(arg1, arg2)"), CodeElement.single("targetMethod()"));
    const act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });
});
