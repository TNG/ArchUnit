'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const initDependency = require('./main-files').get('dependency').init;

const ViewStub = {};
const DetailedViewStub = {};

const buildDescription = () => ({
  withTypes: (inheritanceType, accessType) => ({
    withCodeElements: (startCodeUnit, targetElement) => ({
      inheritanceType: inheritanceType,
      accessType: accessType,
      startCodeUnit: startCodeUnit,
      targetElement: targetElement,
      getInheritanceType: () => inheritanceType,
      getAccessType: () => accessType,
      hasDetailedDescription: () => true
    })
  })
});

describe("Dependency", () => {
  it("can be built by merging existing descriptions with different access groups", () => {
    const graphWrapper = testObjects.testGraph3();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const createDependency = dependencyCreator.createDependency;
    const from = "com.tngtech.main.class1", to = "com.tngtech.interface1";

    const dependency1 = createElementaryDependency(from, to).withDependencyDescription("methodCall", "startMethod(arg1, arg2)", "targetMethod()");
    const dependency2 = createElementaryDependency(from, to).withDependencyDescription("implements");
    const act = createDependency(from, to).byGroupingDependencies([dependency1, dependency2]);
    const exp = "implements methodCall";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built by merging existing descriptions with same access groups but different access types", () => {
    const graphWrapper = testObjects.testGraph3();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const createDependency = dependencyCreator.createDependency;
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const dependency1 = createElementaryDependency(from, to).withDependencyDescription("fieldAccess", "testclass1()", "field1");
    const dependency2 = createElementaryDependency(from, to).withDependencyDescription("methodCall", "testclass1()", "targetMethod()");
    const dependency3 = createElementaryDependency(from, to).withDependencyDescription("extends");
    let act = createDependency(from, to).byGroupingDependencies([dependency1, dependency2]);
    act = createDependency(from, to).byGroupingDependencies([act, dependency3]);
    const exp = "extends several";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createNewDependency = dependencyCreator.createNewDependency;
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const description = buildDescription().withTypes("", "fieldAccess").withCodeElements(
      "innertestclass1()", "field1");
    const act = createNewDependency(from, to).afterFoldingOneNode(description, from === "com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createNewDependency = dependencyCreator.createNewDependency;
    const from = "com.tngtech.test", to = "com.tngtech.class2";

    const description = buildDescription().withTypes("", "fieldAccess").withCodeElements(
      "testclass1()", "field1");
    const act = createNewDependency(from, to).afterFoldingOneNode(description, from === "com.tngtech.test.testclass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and target's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createNewDependency = dependencyCreator.createNewDependency;
    const from = "com.tngtech.main.class1", to = "com.tngtech.test.testclass1";

    const description = buildDescription().withTypes("", "methodCall").withCodeElements(
      "startMethod(arg1, arg2)", "targetMethod()");
    const act = createNewDependency(from, to).afterFoldingOneNode(description, to === "com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and targets's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, DetailedViewStub, graphWrapper.graph.root);
    const createNewDependency = dependencyCreator.createNewDependency;
    const from = "com.tngtech.main.class1", to = "com.tngtech.test";

    const description = buildDescription().withTypes("", "methodCall").withCodeElements(
      "startMethod(arg1, arg2)", "targetMethod()");
    const act = createNewDependency(from, to).afterFoldingOneNode(description, to === "com.tngtech.test.testclass1.InnerTestClass1");
    const exp = "";
    expect(act.description.toString()).to.equal(exp);
  });
});
