'use strict';

const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const initDependency = require('./main-files').get('dependency').init;

const ViewStub = class {

};

const stubs = require('./stubs');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(10),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub
});
const Node = appContext.getNode();
const testJson = require('./test-json-creator');

const createTreeWithToClasses = () => {
  const jsonRoot = testJson.package('com.tngtech.archunit')
    .add(testJson.clazz('SomeClass1', 'class').build())
    .add(testJson.clazz('SomeClass2', 'class').build())
    .build();
  return {
    root: new Node(jsonRoot),
    node1: 'com.tngtech.archunit.SomeClass1',
    node2: 'com.tngtech.archunit.SomeClass2'
  };
};

const createTreeWithToClassesInDifferentPackages = () => {
  const jsonRoot = testJson.package('com.tngtech.archunit')
    .add(testJson.package('pkg1')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .build())
    .add(testJson.package('pkg2')
      .add(testJson.clazz('SomeClass2', 'class').build())
      .build())
    .build();
  return {
    root: new Node(jsonRoot),
    class1: 'com.tngtech.archunit.pkg1.SomeClass1',
    class2: 'com.tngtech.archunit.pkg2.SomeClass2'
  };
};

const createTreeWithToClassesAndOneInnerClass = () => {
  const jsonRoot = testJson.package('com.tngtech.archunit')
    .add(testJson.clazz('SomeClass1', 'class').build())
    .add(testJson.clazz('SomeClass2', 'class')
      .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
      .build())
    .build();
  return {
    root: new Node(jsonRoot),
    class: 'com.tngtech.archunit.SomeClass1',
    innerClass: 'com.tngtech.archunit.SomeClass2$SomeInnerClass'
  };
};

describe('ElementaryDependency', () => {
  it('knows its start and end node', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('implements');
    expect(dependency.getStartNode()).to.equal(tree.root.getByName(tree.node1));
    expect(dependency.getEndNode()).to.equal(tree.root.getByName(tree.node2));
  });

  it('knows its types', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('methodCall');
    expect(dependency.getTypeNames()).to.equal('dependency methodCall');
  });

  it('creates correct InheritanceDescription', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('implements');
    const description = dependency.description;
    expect(description.hasTitle()).to.equal(false);
    expect(description.hasDetailedDescription()).to.equal(false);
  });

  it('creates correct AccessDescription', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess');
    const description = dependency.description;
    expect(description.hasTitle()).to.equal(true);
    expect(description.hasDetailedDescription()).to.equal(true);
  });

  it('creates correct string relative to itself', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess', 'startMethod()', 'targetMethod()');
    expect(dependency.toShortStringRelativeToPredecessors(tree.node1, tree.node2)).to.equal('startMethod()->targetMethod()');
  });

  it('creates correct string relative to the parent of the target node', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess', 'startMethod()', 'targetMethod()');
    const targetNode = dependency.getEndNode();
    expect(dependency.toShortStringRelativeToPredecessors(tree.node1, targetNode.getParent().getFullName())).to
      .equal(`startMethod()->${targetNode.getName()}.targetMethod()`);
  });

  it('creates correct string relative to the parents of both end nodes', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess', 'startMethod()', 'targetMethod()');
    const startNode = dependency.getStartNode();
    const targetNode = dependency.getEndNode();
    const exp = `${startNode.getName()}.startMethod()->${targetNode.getName()}.targetMethod()`;
    const act = dependency.toShortStringRelativeToPredecessors(startNode.getParent().getFullName(),
      targetNode.getParent().getFullName());
    expect(act).to.equal(exp);
  });

  it('can be shifted to one of the end-nodes: the same dependency should be returned', () => {
    const tree = createTreeWithToClassesInDifferentPackages();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class1, tree.class2)
      .withDependencyDescription('implements');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getFullName(), dependency.getEndNode().getFullName());
    expect(act).to.equal(dependency);
  });

  it('can be shifted to one of the end-nodes\' parents if one of them is a package', () => {
    const tree = createTreeWithToClassesInDifferentPackages();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class1, tree.class2)
      .withDependencyDescription('implements');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getParent().getFullName(), dependency.getEndNode().getFullName());
    expect(act.getTypeNames()).to.equal('dependency ');
    expect(act.description.hasDetailedDescription()).to.equal(false);
  });

  it('can be shifted to one of the end-nodes\' parents if both are classes and if the dependency has no detailed ' +
    'description: a correct child-access-description is created (with also no detailed description)', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class, tree.innerClass)
      .withDependencyDescription('implements');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getFullName(), dependency.getEndNode().getParent().getFullName());
    expect(act.getTypeNames()).to.equal('dependency childrenAccess');
    expect(act.description.hasDetailedDescription()).to.equal(false);
  });

  it('can be shifted to one of the end-nodes\' parents if both are classes and if the dependency has a detailed ' +
    'description: a correct child-access-description is created (with also a detailed description)', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class, tree.innerClass)
      .withDependencyDescription('fieldAccess', 'startCodeUnit()', 'targetField');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getFullName(), dependency.getEndNode().getParent().getFullName());
    expect(act.getTypeNames()).to.equal('dependency childrenAccess');
    expect(act.description.hasDetailedDescription()).to.equal(true);
  });
});

describe('AccessDescription', () => {
  it('can merge access type with same access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType('methodCall')).to.equal('methodCall');
  });

  it('can merge access type with other access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType('fieldAccess')).to.equal('several');
  });

  it('can merge access type with empty access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType()).to.equal('methodCall');
  });

  it('can merge inheritance type with other inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('extends')).to.equal('extends');
  });
});

describe('InheritanceDescription', () => {
  it('can merge inheritance type with same inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('extends')).to.equal('extends');
  });

  it('can merge inheritance type with other inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('implements')).to.equal('several');
  });

  it('can merge inheritance type with empty inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('implements').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType()).to.equal('implements');
  });

  it('can merge access type with other access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('implements').description;
    expect(description.mergeAccessTypeWithOtherAccessType('methodCall')).to.equal('methodCall');
  });
});

describe('GroupedDependency', () => {
  it('is not recreated when one already exists', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    expect(act).to.equal(groupedDependency);
  });

  it('has no detailed description and no types, if one of the end nodes is a package', () => {
    const tree = createTreeWithToClassesInDifferentPackages();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const groupedDependency = dependencyCreator.getUniqueDependency('com.tngtech.archunit.pkg1', tree.class2)
      .byGroupingDependencies([]);
    expect(groupedDependency.getTypeNames()).to.equal('dependency ');
    expect(groupedDependency.description.hasDetailedDescription()).to.equal(false);
  });

  it('is created correctly from one elementary dependency', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const elementaryDependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency fieldAccess');
  });

  it('is created correctly from two elementary dependencies with the same dependency group and kind', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall');
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency methodCall');
  });

  it('is created correctly from two elementary dependencies with the same dependency group but different kind', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall');
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('constructorCall');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency several');
  });

  it('is created correctly from two elementary dependencies with different dependency groups', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends');
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('constructorCall');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency extends constructorCall');
  });

  it('is created correctly from three elementary dependencies with two different dependency groups and three different kinds', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(ViewStub, tree.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends');
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('constructorCall');
    const elementaryDependency3 = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2, elementaryDependency3]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency extends several');
  });
});

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
