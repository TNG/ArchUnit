'use strict';

const expect = require('chai').expect;

const initDependency = require('./main-files').get('dependency').init;

const MAXIMUM_DELTA = 0.001;
const CIRCLE_PADDING = 30;
const stubs = require('./stubs');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(CIRCLE_PADDING),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub
});
const Root = appContext.getRoot();
const testJson = require('./test-json-creator');

const createTreeWithToClasses = () => {
  const jsonRoot = testJson.package('com.tngtech.archunit')
    .add(testJson.clazz('SomeClass1', 'class').build())
    .add(testJson.clazz('SomeClass2', 'class').build())
    .build();
  return {
    root: new Root(jsonRoot, null, () => Promise.resolve()),
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
    root: new Root(jsonRoot, null, () => Promise.resolve()),
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
    root: new Root(jsonRoot, null, () => Promise.resolve()),
    class1: 'com.tngtech.archunit.SomeClass1',
    classWithInnerClass: 'com.tngtech.archunit.SomeClass2',
    innerClass: 'com.tngtech.archunit.SomeClass2$SomeInnerClass'
  };
};

//FIXME: better use mock for the tree (only the getByName-Methode has to be mocked, and the nodes only need the methods
//isPackage() and getAbsoluteVisualData

describe('ElementaryDependency', () => {
  it('knows its start and end node', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('implements');
    expect(dependency.getStartNode()).to.equal(tree.root.getByName(tree.node1));
    expect(dependency.getEndNode()).to.equal(tree.root.getByName(tree.node2));
  });

  it('knows its types', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('methodCall');
    expect(dependency.getTypeNames()).to.equal('dependency methodCall');
  });

  it('creates correct InheritanceDescription', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2).withDependencyDescription('implements');
    const description = dependency.description;
    expect(description.hasTitle()).to.equal(false);
    expect(description.hasDetailedDescription()).to.equal(false);
  });

  it('creates correct AccessDescription', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess');
    const description = dependency.description;
    expect(description.hasTitle()).to.equal(true);
    expect(description.hasDetailedDescription()).to.equal(true);
  });

  it('creates correct string relative to itself', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess', 'startMethod()', 'targetMethod()');
    expect(dependency.toShortStringRelativeToPredecessors(tree.node1, tree.node2)).to.equal('startMethod()->targetMethod()');
  });

  it('creates correct string relative to the parent of the target node', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess', 'startMethod()', 'targetMethod()');
    const targetNode = dependency.getEndNode();
    expect(dependency.toShortStringRelativeToPredecessors(tree.node1, targetNode.getParent().getFullName())).to
      .equal(`startMethod()->${targetNode.getName()}.targetMethod()`);
  });

  it('creates correct string relative to the parents of both end nodes', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class1, tree.class2)
      .withDependencyDescription('implements');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getFullName(), dependency.getEndNode().getFullName());
    expect(act).to.equal(dependency);
  });

  it('can be shifted to one of the end-nodes\' parents if one of them is a package', () => {
    const tree = createTreeWithToClassesInDifferentPackages();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class1, tree.innerClass)
      .withDependencyDescription('implements');
    const act = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.getStartNode().getFullName(), dependency.getEndNode().getParent().getFullName());
    expect(act.getTypeNames()).to.equal('dependency childrenAccess');
    expect(act.description.hasDetailedDescription()).to.equal(false);
  });

  it('can be shifted to one of the end-nodes\' parents if both are classes and if the dependency has a detailed ' +
    'description: a correct child-access-description is created (with also a detailed description)', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const dependency = dependencyCreator.createElementaryDependency(tree.class1, tree.innerClass)
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType('methodCall')).to.equal('methodCall');
  });

  it('can merge access type with other access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType('fieldAccess')).to.equal('several');
  });

  it('can merge access type with empty access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeAccessTypeWithOtherAccessType()).to.equal('methodCall');
  });

  it('can merge inheritance type with other inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('methodCall').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('extends')).to.equal('extends');
  });
});

describe('InheritanceDescription', () => {
  it('can merge inheritance type with same inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('extends')).to.equal('extends');
  });

  it('can merge inheritance type with other inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('extends').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType('implements')).to.equal('several');
  });

  it('can merge inheritance type with empty inheritance type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('implements').description;
    expect(description.mergeInheritanceTypeWithOtherInheritanceType()).to.equal('implements');
  });

  it('can merge access type with other access type', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const description = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('implements').description;
    expect(description.mergeAccessTypeWithOtherAccessType('methodCall')).to.equal('methodCall');
  });
});

describe('GroupedDependency', () => {
  it('is not recreated when one with the same start and end node already exists: the description is updated', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);

    const elementaryDependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('implements');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([elementaryDependency]);
    expect(act).to.equal(groupedDependency);
    expect(act.getTypeNames()).to.equal('dependency implements');
  });

  it('has no detailed description and no types, if one of the end nodes is a package', () => {
    const tree = createTreeWithToClassesInDifferentPackages();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const groupedDependency = dependencyCreator.getUniqueDependency('com.tngtech.archunit.pkg1', tree.class2)
      .byGroupingDependencies([]);
    expect(groupedDependency.getTypeNames()).to.equal('dependency ');
    expect(groupedDependency.description.hasDetailedDescription()).to.equal(false);
  });

  it('is created correctly from one elementary dependency', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const elementaryDependency = dependencyCreator.createElementaryDependency(tree.node1, tree.node2)
      .withDependencyDescription('fieldAccess');
    const act = dependencyCreator.getUniqueDependency(tree.node1, tree.node2)
      .byGroupingDependencies([elementaryDependency]);
    expect(act.hasDetailedDescription()).to.equal(true);
    expect(act.getTypeNames()).to.equal('dependency fieldAccess');
  });

  it('is created correctly from two elementary dependencies with the same dependency group and kind', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
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

  const setNodeVisualDataTo = (node, x, y, r) => {
    node.nodeCircle.relativePosition.x = x;
    node.nodeCircle.absoluteCircle.x = node.getParent() ? node.getParent().nodeCircle.absoluteCircle.x + x : x;
    node.nodeCircle.relativePosition.y = y;
    node.nodeCircle.absoluteCircle.y = node.getParent() ? node.getParent().nodeCircle.absoluteCircle.y + y : y;
    node.nodeCircle.absoluteCircle.r = r;
  };

  it('calculates the correct coordinates for its end points, if the dependency points to the upper left corner', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 45, 40, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 33.287, y: 30.6296};
    const expEndPoint = {x: 27.809, y: 26.247};

    //FIXME: maybe create chai-extension deepCloseTo
    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the upper side', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 20, 60, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 20, y: 45};
    const expEndPoint = {x: 20, y: 30};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the upper right corner', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 40, 10);
    setNodeVisualDataTo(startNode, 45, 20, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 33.287, y: 29.370};
    const expEndPoint = {x: 27.809, y: 33.753};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the right side', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 60, 20, 10);
    setNodeVisualDataTo(startNode, 20, 20, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 35, y: 20};
    const expEndPoint = {x: 50, y: 20};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower right corner', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 45, 40, 15);
    setNodeVisualDataTo(startNode, 20, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 27.809, y: 26.247};
    const expEndPoint = {x: 33.287, y: 30.6296};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower side', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 60, 15);
    setNodeVisualDataTo(startNode, 20, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 20, y: 30};
    const expEndPoint = {x: 20, y: 45};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower left corner', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 45, 20, 15);
    setNodeVisualDataTo(startNode, 20, 40, 10);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 27.809, y: 33.753};
    const expEndPoint = {x: 33.287, y: 29.370};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the left side', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 20, 15);
    setNodeVisualDataTo(startNode, 60, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 50, y: 20};
    const expEndPoint = {x: 35, y: 20};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the end node is within the start node', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.classWithInnerClass);
    const endNode = tree.root.getByName(tree.innerClass);

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.classWithInnerClass, tree.innerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 16.718, y: 27.812};
    const expEndPoint = {x: 22.519, y: 31.680};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the start node is within the end node', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.innerClass);
    const endNode = tree.root.getByName(tree.classWithInnerClass);

    setNodeVisualDataTo(endNode, 50, 50, 40);
    setNodeVisualDataTo(startNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.innerClass, tree.classWithInnerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 22.519, y: 31.680};
    const expEndPoint = {x: 16.718, y: 27.812};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the end node is exactly in the middle of the start node: ' +
    'then the dependency points to the lower left corner', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.classWithInnerClass);
    const endNode = tree.root.getByName(tree.innerClass);

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, 0, 0, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.classWithInnerClass, tree.innerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 78.284, y: 78.284};
    const expEndPoint = {x: 60.607, y: 60.607};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points if it must "share" the end nodes with another dependency', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.node1);
    const endNode = tree.root.getByName(tree.node2);

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 45, 40, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.visualData.mustShareNodes = true;
    dependency.jumpToPosition();

    const expStartPoint = {x: 30.056, y: 38.701};
    const expEndPoint = {x: 21.104, y: 29.939};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points if it must "share" the end nodes with another dependency ' +
    'and the end node is within the start node', () => {
    const tree = createTreeWithToClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);
    const startNode = tree.root.getByName(tree.classWithInnerClass);
    const endNode = tree.root.getByName(tree.innerClass);

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(tree.classWithInnerClass, tree.innerClass).byGroupingDependencies([]);
    dependency.visualData.mustShareNodes = true;
    dependency.jumpToPosition();

    const expStartPoint = {x: 23.093, y: 20.402};
    const expEndPoint = {x: 29.231, y: 26.154};

    expect(dependency.visualData.startPoint.x).to.closeTo(expStartPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.startPoint.y).to.closeTo(expStartPoint.y, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.x).to.closeTo(expEndPoint.x, MAXIMUM_DELTA);
    expect(dependency.visualData.endPoint.y).to.closeTo(expEndPoint.y, MAXIMUM_DELTA);
  });

  it('updates its view after jumping to its position: does not show the view if the dependency is hidden', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.hide();
    dependency.jumpToPosition();

    expect(dependency._view.hasJumpedToPosition).to.equal(true);
    expect(dependency._view.isVisible).to.equal(false);
  });

  it('updates its view after moving to its position: does not show the view if the dependency is hidden', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency.hide();
    const promise = dependency.moveToPosition();

    expect(dependency._view.hasMovedToPosition).to.equal(true);
    return promise.then(() => expect(dependency._view.isVisible).to.equal(false));
  });

  it('shows the view on jumping to position if the dependency is visible', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency._isVisible = true;
    dependency.jumpToPosition();

    expect(dependency._view.isVisible).to.equal(true);
  });

  it('shows the view on moving to position if the dependency is visible', () => {
    const tree = createTreeWithToClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, tree.root);

    const dependency = dependencyCreator.getUniqueDependency(tree.node1, tree.node2).byGroupingDependencies([]);
    dependency._isVisible = true;
    const promise = dependency.moveToPosition();

    return promise.then(() => expect(dependency._view.isVisible).to.equal(true));
  });
});