'use strict';

const {expect} = require('chai');
require('./testinfrastructure/node-chai-extensions');
require('./testinfrastructure/dependencies-chai-extension');

const stubs = require('./testinfrastructure/stubs');
const {createTestDependencies, createTestGraph, testRoot} = require('./testinfrastructure/test-json-creator');
const AppContext = require('../../../main/app/graph/app-context');

const realInitGraph = require('../../../main/app/graph/graph').init;
// FIXME: Workaround -> removing foldAllNodes (which was ALWAYS true in production) broke half the tests. Obviously they should test real public API and not some cases that can never happen in production
const initGraph = function () {
  const initGraphArgs = arguments;
  return {
    create: (createGraphArgs) => {
      const graph = realInitGraph.apply(null, initGraphArgs).create(createGraphArgs);
      // FIXME: What is the public API we want to test? Tests were not realistic, since removing foldAllNodes broke 50% of all tests and restoring the behavior demands some weird touches to interna
      graph.root._callOnSelfThenEveryDescendant(node => node.unfold());
      graph.dependencies.recreateVisible();
      return graph;
    }
  }
};

const appContext = AppContext.newInstance({
  visualizationStyles: stubs.visualizationStylesStub(30),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub,
  RootView: stubs.NodeViewStub,
  DependencyView: stubs.DependencyViewStub,
  GraphView: stubs.GraphViewStub
});

const createResources = (graph, violations) => ({
  getResources: () => ({graph, violations})
});

describe('Graph', () => {
  const jsonRootWithTwoClasses = testRoot.package('com.tngtech.archunit')
    .add(testRoot.package('pkg1')
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build())
    .add(testRoot.package('pkg2')
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build())
    .build();
  const jsonDependenciesOfTwoClass = createTestDependencies()
    .addMethodCall().from('com.tngtech.archunit.pkg1.SomeClass', 'startMethod()')
    .to('com.tngtech.archunit.pkg2.SomeClass', 'targetMethod()')
    .build();
  const jsonGraphWithTwoClasses = createTestGraph(jsonRootWithTwoClasses, jsonDependenciesOfTwoClass);

  it('creates a correct tree-structure with dependencies and a correct layout', () => {
    const graph = initGraph(appContext, createResources(jsonGraphWithTwoClasses)).create();

    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkg1',
      'com.tngtech.archunit.pkg1.SomeClass', 'com.tngtech.archunit.pkg2',
      'com.tngtech.archunit.pkg2.SomeClass'];
    const expDeps = ['com.tngtech.archunit.pkg1.SomeClass-com.tngtech.archunit.pkg2.SomeClass'];

    const actNodes = graph.root.getSelfAndDescendants();
    const actDeps = graph.dependencies._getVisibleDependencies();
    expect(actNodes).to.containExactlyNodes(expNodes);
    expect(actDeps).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise;
  });

  it('initially folds all nodes', () => {
    const graph = realInitGraph(appContext, createResources(jsonGraphWithTwoClasses)).create(null);
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkg1', 'com.tngtech.archunit.pkg2'];
    const expDeps = ['com.tngtech.archunit.pkg1-com.tngtech.archunit.pkg2'];

    expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
    expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);
    return graph.root._updatePromise;
  });

  it('can filter node by name containing', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .add(testRoot.clazz('NotMatchingClass', 'class')
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.NotMatchingClass', 'targetMethod()')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByName('*Some*');

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);
    });
  });

  it('can filter node by name not containing', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .add(testRoot.clazz('MatchingClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.MatchingClass', 'targetMethod()')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByName('~*Matching*');

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);
    });
  });

  it('can filter nodes by type', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.SomeInterface', 'targetMethod()')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByType({showInterfaces: false, showClasses: true});

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);
    });
  });

  it('can filter dependencies by type', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addInheritance().from('com.tngtech.archunit.SomeClass2')
      .to('com.tngtech.archunit.SomeClass1')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterDependenciesByType({
      INHERITANCE: false,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: true,
      FIELD_ACCESS: false
    });

    return graph.root._updatePromise.then(() => expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps));
  });

  it('transforms the dependencies if a node is folded', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.pkgToFold.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    const exp = ['com.tngtech.archunit.pkgToFold-com.tngtech.archunit.SomeClass2'];

    graph.root.getByName('com.tngtech.archunit.pkgToFold')._changeFoldIfInnerNodeAndRelayout();

    expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(exp);

    return graph.root._updatePromise;
  });

  it('updates the positions of the dependencies if a node is dragged', () => {
    const jsonRoot = testRoot.package('com.tngtech.archunit')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContext, createResources(jsonGraph)).create();
    graph.root.getByName('com.tngtech.archunit.SomeClass1')._drag(10, 10);
    return graph.root._updatePromise.then(() => expect(graph.dependencies._getVisibleDependencies()[0]._view.hasJumpedToPosition).to.equal(true));
  });

  it('can change the fold-states of the nodes to show all violations', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.package('pkg1')
          .add(testRoot.package('pkg2')
            .add(testRoot.clazz('SomeClass2', 'class').build())
            .build())
          .add(testRoot.clazz('SomeClass1', 'class').build())
          .build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg1.SomeClass1', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];

    const graph = realInitGraph(appContext, createResources(jsonGraph, violations)).create(null);

    return graph.root._updatePromise.then(() => {
      graph.dependencies.showViolations(violations[0]);
      graph.unfoldNodesToShowAllViolations();

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.SomeClass1'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        return graph.root._updatePromise;
      });
    });
  });

  it('can fold nodes with minimum depth that have no violations', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.package('pkg1')
          .add(testRoot.package('pkg2')
            .add(testRoot.clazz('SomeClass2', 'class').build())
            .build())
          .add(testRoot.clazz('SomeClass1', 'class').build())
          .build())
        .add(testRoot.package('pkg3')
          .add(testRoot.clazz('SomeOtherClass', 'class').build())
          .build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg3.SomeOtherClass', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const jsonGraph = createTestGraph(jsonRoot, jsonDependencies);

    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    },
      {
        rule: 'rule2',
        violations: ['<com.tngtech.pkg3.SomeOtherClass.startMethod()> FIELD_ACCESS to <com.tngtech.pkg1.pkg2.SomeClass2.targetField>']
      }];

    const graph = initGraph(appContext, createResources(jsonGraph, violations)).create(null, false);

    return graph.root._updatePromise.then(() => {
      graph.dependencies.showViolations(violations[0]);
      graph.foldNodesWithMinimumDepthWithoutViolations();

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2',
          'com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass2',
          'com.tngtech.pkg3'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        return graph.root._updatePromise;
      });
    });
  });

  const jsonRootWithThreeClasses =
    testRoot.package('com.tngtech')
      .add(testRoot.package('pkg1')
        .add(testRoot.package('pkg2')
          .add(testRoot.clazz('SomeClass2', 'class').build())
          .add(testRoot.clazz('SomeClass3', 'class').build())
          .build())
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .build())
      .build();
  const jsonDependenciesForThreeClasses = createTestDependencies()
    .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
    .to('com.tngtech.pkg1.SomeClass1')
    .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass3')
    .to('com.tngtech.pkg1.SomeClass1')
    .build();
  const jsonGraphWithThreeClasses = createTestGraph(jsonRootWithThreeClasses, jsonDependenciesForThreeClasses);
  const violationsForThreeClasses = [{
    rule: 'rule1',
    violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
  },
    {
      rule: 'rule2',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass3> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];

  it('can hide nodes that are not involved in violations and show them again', () => {

    const graph = initGraph(appContext, createResources(jsonGraphWithThreeClasses, violationsForThreeClasses)).create(null, false);

    return graph.root._updatePromise.then(() => {
      graph.dependencies.showViolations(violationsForThreeClasses[0]);
      graph.onHideNodesWithoutViolationsChanged(true);

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
          'com.tngtech.pkg1.SomeClass1'];
        const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

        graph.onHideNodesWithoutViolationsChanged(false);

        return graph.root._updatePromise.then(() => {
          const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
            'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
          const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
            'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
          expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
          expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

          return graph.root._updatePromise;
        });
      });
    });
  });

  it('updates the nodes and dependencies, when the shown violation groups change and the option for hiding all ' +
    'nodes that are not involved in violations is enabled', () => {
    const graph = initGraph(appContext, createResources(jsonGraphWithThreeClasses, violationsForThreeClasses)).create(null, false);

    return graph.root._updatePromise.then(() => {
      graph.onHideNodesWithoutViolationsChanged(true);
      graph.showViolations(violationsForThreeClasses[0]);

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
          'com.tngtech.pkg1.SomeClass1'];
        const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

        graph.showViolations(violationsForThreeClasses[1]);

        return graph.root._updatePromise.then(() => {
          const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
            'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
          const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
            'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
          expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
          expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

          graph.hideViolations(violationsForThreeClasses[0]);

          return graph.root._updatePromise.then(() => {
            const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2',
              'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
            const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
            expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
            expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

            graph.hideViolations(violationsForThreeClasses[1]);

            return graph.root._updatePromise.then(() => {
              const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
                'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
              const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
                'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
              expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
              expect(graph.dependencies._getVisibleDependencies()).to.haveDependencyStrings(expDeps);

              return graph.root._updatePromise;
            });
          });
        });
      });
    });
  });
});