'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const initDependency = require('./main-files').get('dependency').init;

const ViewStub = class{

};

describe("Dependency", () => {
  it("can be built by merging existing descriptions with different access groups", () => {
    const graphWrapper = testObjects.testGraph3();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const getUniqueDependency = dependencyCreator.getUniqueDependency;
    const from = "com.tngtech.main.class1", to = "com.tngtech.interface1";

    const dependency1 = createElementaryDependency(from, to).withDependencyDescription("methodCall", "startMethod(arg1, arg2)", "targetMethod()");
    const dependency2 = createElementaryDependency(from, to).withDependencyDescription("implements");
    const act = getUniqueDependency(from, to).byGroupingDependencies([dependency1, dependency2]);
    const exp = "implements methodCall";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built by merging existing descriptions with same access groups but different access types", () => {
    const graphWrapper = testObjects.testGraph3();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const getUniqueDependency = dependencyCreator.getUniqueDependency;
    const from = "com.tngtech.test.testclass1", to = "com.tngtech.class2";

    const dependency1 = createElementaryDependency(from, to).withDependencyDescription("fieldAccess", "testclass1()", "field1");
    const dependency2 = createElementaryDependency(from, to).withDependencyDescription("methodCall", "testclass1()", "targetMethod()");
    const act = getUniqueDependency(from, to).byGroupingDependencies([dependency1, dependency2]);
    const exp = "several";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const shiftElementaryDependency = dependencyCreator.shiftElementaryDependency;
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const dep = createElementaryDependency("com.tngtech.test.testclass1.InnerTestClass1", "com.tngtech.class2")
      .withDependencyDescription('fieldAccess', "innertestclass1()", "field1");
    const act = shiftElementaryDependency(dep, "com.tngtech.test.testclass1", dep.to);
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when start is folded and start's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const shiftElementaryDependency = dependencyCreator.shiftElementaryDependency;
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const dep = createElementaryDependency("com.tngtech.test.testclass1", "com.tngtech.class2")
      .withDependencyDescription('fieldAccess', "testclass1()", "field1");

    const act = shiftElementaryDependency(dep, "com.tngtech.test", dep.to);
    expect(act.description.toString()).to.equal('');
  });

  it("can be built with existing description when target is folded and target's parent is a class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const shiftElementaryDependency = dependencyCreator.shiftElementaryDependency;
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const dep = createElementaryDependency("com.tngtech.main.class1", 'com.tngtech.test.testclass1.InnerTestClass1')
      .withDependencyDescription('methodCall', 'startMethod(arg1, arg2)', 'targetMethod()');

    const act = shiftElementaryDependency(dep, dep.from, 'com.tngtech.test.testclass1');
    const exp = "childrenAccess";
    expect(act.description.toString()).to.equal(exp);
  });

  it("can be built with existing description when target is folded and targets's parent is a package", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();
    const dependencyCreator = initDependency(ViewStub, graphWrapper.graph.root);
    const shiftElementaryDependency = dependencyCreator.shiftElementaryDependency;
    const createElementaryDependency = dependencyCreator.createElementaryDependency;
    const dep = createElementaryDependency('com.tngtech.main.class1', 'com.tngtech.test.testclass1.InnerTestClass1')
      .withDependencyDescription('methodCall', 'startMethod(arg1, arg2)', 'targetMethod()');

    const act = shiftElementaryDependency(dep, dep.from, 'com.tngtech.test');
    expect(act.description.toString()).to.equal('');
  });
});
