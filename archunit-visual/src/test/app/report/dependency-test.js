'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const createDependencyBuilder = require('./main-files').get('dependency').buildDependency;

const buildDescription = () => ({
  withKinds: (inheritanceKind, accessKind) => ({
    withCodeElements: (startCodeUnit, targetElement) => ({
      inheritanceKind: inheritanceKind,
      accessKind: accessKind,
      startCodeUnit: startCodeUnit,
      targetElement: targetElement,
      getInheritanceKind: () => inheritanceKind,
      getAccessKind: () => accessKind
    })
  })
});

describe("Dependency", () => {
  it("can be built by merging existing descriptions with different access groups", () => {
    const graphWrapper = testObjects.testGraph3();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.interface1";

    const description1 = buildDescription().withKinds("", "methodCall").withCodeElements("startMethod(arg1, arg2)", "targetMethod()");
    const description2 = buildDescription().withKinds("implements", "").withCodeElements();
    const act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    const exp = "implements methodCall";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built by merging existing descriptions with same access groups but different access kinds", () => {
    const graphWrapper = testObjects.testGraph3();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const description1 = buildDescription().withKinds("", "fieldAccess").withCodeElements(
      "testclass1()", "field1");
    const description2 = buildDescription().withKinds("", "methodCall").withCodeElements(
      "testclass1()", "targetMethod()");
    const description3 = buildDescription().withKinds("extends", "").withCodeElements();
    let act = buildDependency(from, to).withMergedDescriptions(description1, description2);
    act = buildDependency(from, to).withMergedDescriptions(act.description, description3);
    const exp = "extends several";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
      "innertestclass1()", "field1");
    const act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.test", to = "com.tngtech.class2";

    const description = buildDescription().withKinds("", "fieldAccess").withCodeElements(
      "testclass1()", "field1");
    const act = buildDependency(from, to).withExistingDescription(description).whenStartIsFolded("com.tngtech.test.testclass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and target's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.test.testclass1";

    const description = buildDescription().withKinds("", "methodCall").withCodeElements(
      "startMethod(arg1, arg2)", "targetMethod()");
    const act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and targets's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const buildDependency = createDependencyBuilder(graphWrapper.graph.root);
    const from = "com.tngtech.main.class1", to = "com.tngtech.test";

    const description = buildDescription().withKinds("", "methodCall").withCodeElements(
      "startMethod(arg1, arg2)", "targetMethod()");
    const act = buildDependency(from, to).withExistingDescription(description).whenTargetIsFolded("com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });
});
