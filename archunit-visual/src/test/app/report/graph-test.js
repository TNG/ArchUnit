'use strict';

const expect = require('chai').expect;
require('./chai/node-chai-extensions');
require('./chai/dependencies-chai-extension');

const stubs = require('./stubs');

const testJson = require('./test-json-creator');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(30),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub, //FIXME: really necessary??
  DependencyView: stubs.DependencyViewStub,
  GraphView: stubs.GraphViewStub
});
const Graph = appContext.getGraph();

describe('Graph', () => {
  const jsonRootWithTwoClasses = testJson.package('com.tngtech.archunit')
    .add(testJson.package('pkg1')
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.archunit.pkg2.SomeClass', 'startMethod()', 'targetMethod()')
        .build())
      .build())
    .add(testJson.package('pkg2')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build())
    .build();

  it('creates a correct tree-structure with dependencies and a correct layout', () => {
    const graph = new Graph(jsonRootWithTwoClasses);

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkg1',
      'com.tngtech.archunit.pkg1.SomeClass', 'com.tngtech.archunit.pkg2',
      'com.tngtech.archunit.pkg2.SomeClass'];
    const expDeps = ['com.tngtech.archunit.pkg1.SomeClass->com.tngtech.archunit.pkg2.SomeClass(methodCall)'];

    const actNodes = graph.root.getSelfAndDescendants();
    const actDeps = graph.dependencies.getVisible();
    expect(actNodes).to.containExactlyNodes(expNodes);
    expect(actDeps).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise;
  });

  it('can fold all nodes', () => {
    const graph = new Graph(jsonRootWithTwoClasses);

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkg1', 'com.tngtech.archunit.pkg2'];
    const expDeps = ['com.tngtech.archunit.pkg1->com.tngtech.archunit.pkg2()'];

    graph.foldAllNodes();

    expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
    expect(graph.dependencies.getVisible()).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise;
  });

  it('can filter node by name containing', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.archunit.SomeClass2', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.archunit.NotMatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('NotMatchingClass', 'class')
        .build())
      .build();
    const graph = new Graph(jsonRoot);

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1->com.tngtech.archunit.SomeClass2(methodCall)'];

    graph.filterNodesByNameContaining('Some', false);

    expect(graph.dependencies.getVisible()).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise.then(() => expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes));
  });

  it('can filter node by name not containing', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.archunit.SomeClass2', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.archunit.MatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('MatchingClass', 'class')
        .build())
      .build();
    const graph = new Graph(jsonRoot);

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1->com.tngtech.archunit.SomeClass2(methodCall)'];

    graph.filterNodesByNameNotContaining('Matching', true);

    expect(graph.dependencies.getVisible()).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise.then(() => expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes));
  });

  it('can filter nodes by type', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.archunit.SomeClass2', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.archunit.SomeInterface', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeInterface', 'interface')
        .build())
      .build();
    const graph = new Graph(jsonRoot);

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1->com.tngtech.archunit.SomeClass2(methodCall)'];

    graph.filterNodesByType({showInterfaces: false, showClasses: true});

    expect(graph.dependencies.getVisible()).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise.then(() => expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes));
  });

  it('can filter dependencies by type', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.archunit.SomeClass2', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .extending('com.tngtech.archunit.SomeClass1')
        .build())
      .build();
    const graph = new Graph(jsonRoot);

    const expDeps = ['com.tngtech.archunit.SomeClass1->com.tngtech.archunit.SomeClass2(methodCall)'];

    graph.filterDependenciesByType({
      showImplementing: false,
      showExtending: false,
      showConstructorCall: false,
      showMethodCall: true,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDepsBetweenChildAndParent: true
    });

    expect(graph.dependencies.getVisible()).to.haveDependencyStrings(expDeps);
  });
});