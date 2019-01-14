'use strict';

const chai = require('chai');
const generalExtensions = require('../testinfrastructure/general-chai-extensions');
const {vectors} = require('../../../../main/app/graph/infrastructure/vectors');
const {testRoot} = require('../testinfrastructure/test-json-creator');
const stubs = require('../testinfrastructure/stubs');
const initDependency = require('../../../../main/app/graph/dependencies/dependency');
const AppContext = require('../../../../main/app/graph/app-context');

const expect = chai.expect;

chai.use(generalExtensions);

const MAXIMUM_DELTA = 0.001;
const CIRCLE_PADDING = 30;

const appContext = AppContext.newInstance({
  visualizationStyles: stubs.visualizationStylesStub(CIRCLE_PADDING),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub,
  RootView: stubs.NodeViewStub //FIXME: necessary??
});

const Root = appContext.getRoot();

const createRootWithTwoClasses = () => {
  const jsonRoot = testRoot.package('com.tngtech.archunit')
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('SomeClass2', 'class').build())
    .build();
  const root = new Root(jsonRoot, null, () => Promise.resolve());
  return {
    root: root,
    class1: root.getByName('com.tngtech.archunit.SomeClass1'),
    class2: root.getByName('com.tngtech.archunit.SomeClass2')
  };
};

const createRootWithTwoClassesInDifferentPackages = () => {
  const jsonRoot = testRoot.package('com.tngtech.archunit')
    .add(testRoot.package('pkg1')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .build())
    .add(testRoot.package('pkg2')
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build())
    .build();
  const root = new Root(jsonRoot, null, () => Promise.resolve());
  return {
    root: root,
    class1: root.getByName('com.tngtech.archunit.pkg1.SomeClass1'),
    class2: root.getByName('com.tngtech.archunit.pkg2.SomeClass2')
  };
};

const createRootWithTwoClassesAndOneInnerClass = () => {
  const jsonRoot = testRoot.package('com.tngtech.archunit')
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('SomeClass2', 'class')
      .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
      .build())
    .build();
  const root = new Root(jsonRoot, null, () => Promise.resolve());
  return {
    root: root,
    class1: root.getByName('com.tngtech.archunit.SomeClass1'),
    classWithInnerClass: root.getByName('com.tngtech.archunit.SomeClass2'),
    innerClass: root.getByName('com.tngtech.archunit.SomeClass2$SomeInnerClass')
  };
};

const dependencyParams = (originNode, targetNode, type) => ({
  originNode,
  targetNode,
  type,
  description: `Class <${originNode}> (verb to ${type}) class <${targetNode}>`
});

const shiftEnds = (createFromDependency) => {
  const testData = createRootWithTwoClassesInDifferentPackages();
  const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
  const testDependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));

  const testSetup = createFromDependency(testDependency);
  testSetup.originalDependency = testSetup.originalDependency || testDependency;
  testSetup.newOrigin = testSetup.newOrigin || testDependency.originNode.getParent();
  testSetup.newTarget = testSetup.newTarget || testDependency.targetNode.getParent();
  const shiftedDependency = dependencyCreator.shiftElementaryDependency(testSetup.originalDependency, testSetup.newOrigin, testSetup.newTarget);
  return {originalDependency: testSetup.originalDependency, shiftedDependency};
};

describe('ElementaryDependency', () => {
  it('knows its start and end node', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const dependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));
    expect(dependency.originNode).to.equal(testData.class1);
    expect(dependency.targetNode).to.equal(testData.class2);
  });

  it('can be shifted', () => {
    const {originalDependency, shiftedDependency} = shiftEnds(dependency => ({
      newOrigin: dependency.originNode.getParent(),
      newTarget: dependency.targetNode.getParent()
    }));

    expect(shiftedDependency.originNode).to.equal(originalDependency.originNode.getParent());
    expect(shiftedDependency.targetNode).to.equal(originalDependency.targetNode.getParent());
  });

  it('shifted dependencies retain violation property', () => {
    const shiftedWithViolation = shiftEnds(dependency => ({
      originalDependency: (() => {
        dependency.isViolation = true;
        return dependency;
      })()
    })).shiftedDependency;

    expect(shiftedWithViolation.isViolation).to.be.true;

    const shiftedWithoutViolation = shiftEnds(dependency => ({
      originalDependency: (() => {
        dependency.isViolation = false;
        return dependency;
      })()
    })).shiftedDependency;

    expect(shiftedWithoutViolation.isViolation).to.be.false;
  });

  it('transfers its violation-property if it is shifted to one of the end-nodes\' parents if one of them is a package', () => {
    const testData = createRootWithTwoClassesInDifferentPackages();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const dependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));
    dependency.isViolation = true;
    const actual = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.originNode.getParent(), dependency.targetNode);
    expect(actual.isViolation).to.equal(true);
  });

  it('can be shifted to one of the end-nodes\' parents if both are classes', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const dependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.innerClass, 'INHERITANCE'));
    const newTarget = dependency.targetNode.getParent();
    const actual = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.originNode, newTarget);
    expect(actual.isViolation).to.equal(false);
    expect(actual.originNode).to.equal(dependency.originNode);
    expect(actual.targetNode).to.equal(newTarget);
  });

  it('transfers its violation-property if it is shifted to one of the end-nodes\' parents if both are classes', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const dependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.innerClass, 'INHERITANCE'));
    dependency.isViolation = true;
    const actual = dependencyCreator.shiftElementaryDependency(dependency,
      dependency.originNode, dependency.targetNode.getParent());
    expect(actual.isViolation).to.equal(true);
  });
});

describe('GroupedDependency', () => {
  it('is not recreated when one with the same start and end node already exists: the type and the ' +
    'violation-property are updated', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    expect(groupedDependency.isViolation).to.equal(false);

    const elementaryDependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));
    elementaryDependency.isViolation = true;
    const actual = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([elementaryDependency]);
    expect(actual).to.equal(groupedDependency);
    expect(actual.isViolation).to.equal(true);
  });

  it('determines the correct container node of both end nodes, if one is included in the other one', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(testData.classWithInnerClass, testData.classWithInnerClass).byGroupingDependencies([]);

    expect(groupedDependency.containerNode).to.equal(testData.classWithInnerClass);
  });

  it('determines the correct container node of both end nodes, if the have a common predecessor distinct from them', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);

    expect(groupedDependency.containerNode).to.equal(testData.root);
  });

  it('has no detailed description and no types, if one of the end nodes is a package', () => {
    const testData = createRootWithTwoClassesInDifferentPackages();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const groupedDependency = dependencyCreator.getUniqueDependency(testData.root.getByName('com.tngtech.archunit.pkg1'), testData.class2)
      .byGroupingDependencies([]);
    expect(groupedDependency.hasDetailedDescription()).to.equal(false);
  });

  it('is created correctly from one elementary dependency', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const elementaryDependency = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'FIELD_ACCESS'));
    const actual = dependencyCreator.getUniqueDependency(testData.class1, testData.class2)
      .byGroupingDependencies([elementaryDependency]);
    expect(actual.hasDetailedDescription()).to.equal(true);
  });

  it('is created correctly from two elementary dependencies', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'METHOD_CALL'));
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));
    const actual = dependencyCreator.getUniqueDependency(testData.class1, testData.class2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2]);
    expect(actual.hasDetailedDescription()).to.equal(true);
  });

  it('is created correctly from three elementary dependencies', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const elementaryDependency1 = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'CONSTRUCTOR_CALL'));
    const elementaryDependency2 = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'METHOD_CALL'));
    const elementaryDependency3 = dependencyCreator.createElementaryDependency(dependencyParams(testData.class1, testData.class2, 'INHERITANCE'));
    const actual = dependencyCreator.getUniqueDependency(testData.class1, testData.class2)
      .byGroupingDependencies([elementaryDependency1, elementaryDependency2, elementaryDependency3]);
    expect(actual.hasDetailedDescription()).to.equal(true);
  });

  const setNodeVisualDataTo = (node, x, y, r) => {
    node.nodeShape.relativePosition.x = x;
    node.nodeShape.absoluteCircle.x = node.getParent() ? node.getParent().nodeShape.absoluteShape.position.x + x : x;
    node.nodeShape.relativePosition.y = y;
    node.nodeShape.absoluteCircle.y = node.getParent() ? node.getParent().nodeShape.absoluteShape.position.y + y : y;
    node.nodeShape.absoluteCircle.r = r;
  };

  it('calculates the correct coordinates for its end points, if the dependency points to the upper left corner', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 45, 40, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 33.287, y: 30.6296};
    const expEndPoint = {x: 27.809, y: 26.247};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the upper side', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 20, 60, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 20, y: 45};
    const expEndPoint = {x: 20, y: 30};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the upper right corner', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 40, 10);
    setNodeVisualDataTo(startNode, 45, 20, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 33.287, y: 29.370};
    const expEndPoint = {x: 27.809, y: 33.753};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the right side', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 60, 20, 10);
    setNodeVisualDataTo(startNode, 20, 20, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 35, y: 20};
    const expEndPoint = {x: 50, y: 20};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower right corner', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 45, 40, 15);
    setNodeVisualDataTo(startNode, 20, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 27.809, y: 26.247};
    const expEndPoint = {x: 33.287, y: 30.6296};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower side', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 60, 15);
    setNodeVisualDataTo(startNode, 20, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 20, y: 30};
    const expEndPoint = {x: 20, y: 45};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the lower left corner', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 45, 20, 15);
    setNodeVisualDataTo(startNode, 20, 40, 10);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 27.809, y: 33.753};
    const expEndPoint = {x: 33.287, y: 29.370};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the dependency points to the left side', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 20, 15);
    setNodeVisualDataTo(startNode, 60, 20, 10);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 50, y: 20};
    const expEndPoint = {x: 35, y: 20};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the end node is within the start node', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.classWithInnerClass;
    const endNode = testData.innerClass;

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.classWithInnerClass, testData.innerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 16.718, y: 27.812};
    const expEndPoint = {x: 22.519, y: 31.680};

    expect(vectors.add(dependency.visualData.startPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the start node is within the end node', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.innerClass;
    const endNode = testData.classWithInnerClass;

    setNodeVisualDataTo(endNode, 50, 50, 40);
    setNodeVisualDataTo(startNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.innerClass, testData.classWithInnerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 22.519, y: 31.680};
    const expEndPoint = {x: 16.718, y: 27.812};

    expect(vectors.add(dependency.visualData.startPoint, endNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, endNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points, if the end node is exactly in the middle of the start node: ' +
    'then the dependency points to the lower left corner', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.classWithInnerClass;
    const endNode = testData.innerClass;

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, 0, 0, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.classWithInnerClass, testData.innerClass).byGroupingDependencies([]);
    dependency.jumpToPosition();

    const expStartPoint = {x: 78.284, y: 78.284};
    const expEndPoint = {x: 60.607, y: 60.607};

    expect(vectors.add(dependency.visualData.startPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points if it must "share" the end nodes with another dependency', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.class1;
    const endNode = testData.class2;

    setNodeVisualDataTo(endNode, 20, 20, 10);
    setNodeVisualDataTo(startNode, 45, 40, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.visualData.mustShareNodes = true;
    dependency.jumpToPosition();

    const expStartPoint = {x: 30.056, y: 38.701};
    const expEndPoint = {x: 21.104, y: 29.939};

    expect(vectors.add(dependency.visualData.startPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, testData.root.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('calculates the correct coordinates for its end points if it must "share" the end nodes with another dependency ' +
    'and the end node is within the start node', () => {
    const testData = createRootWithTwoClassesAndOneInnerClass();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);
    const startNode = testData.classWithInnerClass;
    const endNode = testData.innerClass;

    setNodeVisualDataTo(startNode, 50, 50, 40);
    setNodeVisualDataTo(endNode, -15, -10, 15);

    const dependency = dependencyCreator.getUniqueDependency(testData.classWithInnerClass, testData.innerClass).byGroupingDependencies([]);
    dependency.visualData.mustShareNodes = true;
    dependency.jumpToPosition();

    const expStartPoint = {x: 23.093, y: 20.402};
    const expEndPoint = {x: 29.231, y: 26.154};

    expect(vectors.add(dependency.visualData.startPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expStartPoint, MAXIMUM_DELTA);
    expect(vectors.add(dependency.visualData.endPoint, startNode.nodeShape.absoluteShape.position)).to.deep.closeTo(expEndPoint, MAXIMUM_DELTA);
  });

  it('updates its view after jumping to its position: does not show the view if the dependency is hidden', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.hide();
    dependency.jumpToPosition();

    expect(dependency._view.hasJumpedToPosition).to.equal(true);
    expect(dependency._view.isVisible).to.equal(false);
  });

  it('updates its view after moving to its position: does not show the view if the dependency is hidden', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency.hide();
    const promise = dependency.moveToPosition();

    expect(dependency._view.hasMovedToPosition).to.equal(true);
    return promise.then(() => expect(dependency._view.isVisible).to.equal(false));
  });

  it('shows the view on jumping to position if the dependency is visible', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency._isVisible = true;
    dependency.jumpToPosition();

    expect(dependency._view.isVisible).to.equal(true);
  });

  it('shows the view on moving to position if the dependency is visible', () => {
    const testData = createRootWithTwoClasses();
    const dependencyCreator = initDependency(stubs.DependencyViewStub, testData.root);

    const dependency = dependencyCreator.getUniqueDependency(testData.class1, testData.class2).byGroupingDependencies([]);
    dependency._isVisible = true;
    const promise = dependency.moveToPosition();

    return promise.then(() => expect(dependency._view.isVisible).to.equal(true));
  });
});