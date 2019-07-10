'use strict';

const {expect} = require('chai');
require('./testinfrastructure/node-chai-extensions');
require('./testinfrastructure/dependencies-chai-extension');
require('./testinfrastructure/graph-chai-extensions');
const createJsonFromClassNames = require('./testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const guiElementsMock = require('./testinfrastructure/gui-elements-mock');
const {createDependencies, createGraph, nodeCreator} = require('./testinfrastructure/test-json-creator');
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

const circlePadding = 10;

const appContextWith = (graph, violations) => AppContext.newInstance({
  guiElements: guiElementsMock,
  visualizationData: {
    jsonGraph: graph,
    jsonViolations: violations
  }
});

describe.skip('Graph', () => {

  describe('creates a layout, so that', () => {
    //FIXME: this is not layout...layout is only positioning of the elements and so...better put it to smth like 'initialization of the nodes", "sets the css classes"
    //css classes like foldable must be checked in the fold tet methods then
    it('packages have a css-class "package", classes a css-class "class", interfaces a css-class "interface"', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass', 'pkg1.SomeInterface');
      const jsonDependencies = createMethodCallDependencies(jsonRoot);

      const graphGui = createTestGraphGui(jsonRoot, jsonDependencies);
      graphGui.clickNode('pkg1');
      await graphGui.isReady();

      graphGui.test.expectNodeToHaveCssClass('pkg1', 'package');
      graphGui.test.expectNodeToHaveCssClass('pkg1.SomeClass', 'class');
      graphGui.test.expectNodeToHaveCssClass('pkg1.SomeInterface', 'interface');
    });

    it('nodes have a padding to their parent nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg1.SomeClass3', 'pkg2.SomeClass');
      const jsonDependencies = createMethodCallDependencies(jsonRoot,
        {from: 'pkg1.SomeClass1', to: 'pkg2.SomeClass'}, {from: 'pkg1.SomeClass1', to: 'pkg1.SomeClass2'});

      const graphGui = createTestGraphGui(jsonRoot, jsonDependencies);
      graphGui.clickNode('pkg1');
      await graphGui.isReady();

      graphGui.test.expectNodesToHaveAtLeastPaddingFromParent(circlePadding, 'pkg1', 'pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg1.SomeClass3');
    });

    it('sibling nodes have a minimum padding between each other', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2',
        'pkg2.SomeClass', 'pkg3.SomeClass', 'pkg4.SomeClass');
      const jsonDependencies = createMethodCallDependencies(jsonRoot,
        {from: 'pkg1.SomeClass1', to: 'pkg2.SomeClass'}, {from: 'pkg1.SomeClass2', to: 'pkg1.SomeClass2'},
        {from: 'pkg1.SomeClass2', to: 'pkg1.SomeClass2'});

      const graphGui = createTestGraphGui(jsonRoot, jsonDependencies);
      await graphGui.isReady();

      graphGui.test.expectSiblingNodesToHaveAtLeastPadding(circlePadding, 'pkg1', 'pkg2', 'pkg3', 'pkg4');
    });

    it('', async () => {

    });
  });

  //TODO: alle möglichen Interaktionen des Users mit der GUI testen

  const jsonRootWithTwoClasses = nodeCreator.package('com.tngtech.archunit')
    .add(nodeCreator.package('pkg1')
      .add(nodeCreator.clazz('SomeClass', 'class').build())
      .build())
    .add(nodeCreator.package('pkg2')
      .add(nodeCreator.clazz('SomeClass', 'class').build())
      .build())
    .build();
  const jsonDependenciesOfTwoClass = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.pkg1.SomeClass', 'startMethod()')
    .to('com.tngtech.archunit.pkg2.SomeClass', 'targetMethod()')
    .build();
  const jsonGraphWithTwoClasses = createGraph(jsonRootWithTwoClasses, jsonDependenciesOfTwoClass);


  // FIXME -> Test infrastructure

  const createMethodCallDependencies = (jsonRoot, ...predicatesOrPartialNames) => {
    const createNodePredicate = predicateOrPartialName => typeof predicateOrPartialName === 'string'
      ? node => node.fullName.includes(predicateOrPartialName)
      : predicateOrPartialName;

    const predicates = predicatesOrPartialNames.map(({from, to}) => ({
      fromPredicate: createNodePredicate(from),
      toPredicate: createNodePredicate(to)
    }));

    const findMatchingEnds = (node, {fromPredicate, toPredicate}) => {
      const matchingFrom = [];
      const matchingTo = [];
      const findAllMatchingEnds = node => {
        if (fromPredicate(node)) {
          matchingFrom.push(node);
        }
        if (toPredicate(node)) {
          matchingTo.push(node)
        }
        node.children.forEach(findAllMatchingEnds)
      };
      findAllMatchingEnds(node);
      if (matchingFrom.length !== 1 || matchingTo.length !== 1) {
        throw new Error("from- and toPredicate must match exactly one node each")
      }
      return {matchingFrom: matchingFrom[0], matchingTo: matchingTo[0]};
    };
    const matchingEnds = predicates.map(fromAndToPredicates => findMatchingEnds(jsonRoot, fromAndToPredicates));

    const creator = createDependencies();
    matchingEnds.forEach(({matchingFrom, matchingTo}) => creator.addMethodCall().from(matchingFrom.fullName, 'startMethod()')
      .to(matchingTo.fullName, 'targetMethod()'));
    return creator.build();
  };

  //TODO: maybe do not create map from visual nesting, but from svg ids and check visual nesting in layout function...ask Peter what is better
  /*const createMapWithNodeFullNamesToSvgGroup = svgElement => {
    const svgGroupsWithAVisibleCircle = svgElement.getAllGroupsContainingAVisibleElementOfType('circle');
    svgGroupsWithAVisibleCircle.sort((g1, g2) => g2.getVisibleCircleRadius() - g1.getVisibleCircleRadius());
    const map = new Map();

    const splitByCircleLiesWithinNext = (svgGroupsToSplit, nextSvgGroup) => {
      const svgGroupsWithin = [];
      const svgGroupsNotWithin = [];
      const positionOfNext = nextSvgGroup.getVisibleSubElementOfType('circle').absolutePosition;
      const radiusOfNext = nextSvgGroup.getVisibleCircleRadius();

      const circleIsWithinNextCircle = (position, radius) => {
        const positionDiff = {
          x: position.x - positionOfNext.x,
          y: position.y - positionOfNext.y
        };
        const middlePointDistance = Math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y);
        return middlePointDistance + radius <= radiusOfNext;
      };

      svgGroupsToSplit.forEach(svgGroup => {
        const radius = svgGroup.getVisibleCircleRadius();
        const position = svgGroup.getVisibleSubElementOfType('circle').absolutePosition;
        if (circleIsWithinNextCircle(position, radius)) {
          svgGroupsWithin.push(svgGroup);
        } else {
          svgGroupsNotWithin.push(svgGroup);
        }
      });

      return {svgGroupsWithin, svgGroupsNotWithin};
    };

    const addNextSvgElementToMap = (fullNameSoFar, svgGroupsSortedByCircleRadius) => {
      if (svgGroupsSortedByCircleRadius.length === 0) {
        return;
      }
      const next = svgGroupsSortedByCircleRadius.shift();
      const split = splitByCircleLiesWithinNext(svgGroupsSortedByCircleRadius, next);

      const fullNameOfNext = fullNameSoFar + next.getVisibleText();
      map.set(fullNameOfNext, next);
      addNextSvgElementToMap(fullNameOfNext + (next._cssClasses.has('package') ? '.' : '$'), split.svgGroupsWithin);
      addNextSvgElementToMap(fullNameSoFar, split.svgGroupsNotWithin);
    };

    addNextSvgElementToMap('', svgGroupsWithAVisibleCircle);

    return map;
  };*/

  const createMapWithNodeFullNamesToSvgGroup = svgElement => {
    const svgGroupsWithAVisibleCircle = svgElement.getAllGroupsContainingAVisibleElementOfType('circle');
    return new Map(svgGroupsWithAVisibleCircle.map(svgGroup => [svgGroup.getAttribute('id'), svgGroup]));
  };

  const createTestGui = graph => {
    const testGui = {
      clickNode: fullNodeName => {
        testGui.inspect.getMapWithNodeFullNamesToSvgGroup().get(fullNodeName).getVisibleSubElementOfType('circle').click({
          ctrlKey: false,
          altKey: false
        })
      },
      test: {
        expectOnlyVisibleNodes: (...nodeFullNames) => expect(graph).to.haveOnlyVisibleNodes(nodeFullNames),
        expectOnlyVisibleDependencies: (...dependencyIds) => expect(graph).to.haveOnlyVisibleDependencies(dependencyIds),
        expectSiblingNodesToHaveAtLeastPadding: (padding, ...nodeFullNames) => expect(testGui.inspect.getMapWithNodeFullNamesToSvgGroup())
          .to.haveSiblingNodesWithPaddingAtLeast(padding, nodeFullNames),
        expectNodesToHaveAtLeastPaddingFromParent: (padding, parentFullName, ...nodeFullNames) => expect(testGui.inspect.getMapWithNodeFullNamesToSvgGroup())
          .to.haveNodesWithPaddingToParentAtLeast(padding, parentFullName, nodeFullNames),
        expectNodeToHaveCssClass: (nodeFullName, cssClass) => expect(testGui.inspect.getMapWithNodeFullNamesToSvgGroup().get(nodeFullName)).to.haveCssClass(cssClass)
      },
      inspect: {
        getMapWithNodeFullNamesToSvgGroup: () => createMapWithNodeFullNamesToSvgGroup(graph._view.svgElement),

      },
      isReady: () => graph._root._updatePromise
    };
    return testGui;
  };

  const createTestGraphGui = (jsonRoot, jsonDependencies) => createTestGui(realInitGraph(appContextWith(createGraph(jsonRoot, jsonDependencies))).create());


  it('shows only the top level packages right after creation', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass$TmpInner$TmpInner2', 'some.package.SomeClass');
    const jsonDependencies = createMethodCallDependencies(jsonRoot, {from: 'archunit.SomeClass', to: 'package.SomeClass'});

    const graphGui = createTestGraphGui(jsonRoot, jsonDependencies);
    await graphGui.isReady();

    graphGui.test.expectOnlyVisibleNodes('com.tngtech.archunit', 'some.package');
    graphGui.test.expectOnlyVisibleDependencies('com.tngtech.archunit-some.package');
  });

  it('shows children if I click on a node', () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.pkg1.SomeClass', 'com.tngtech.archunit.pkg2.SomeClass');
    const jsonDependencies = createMethodCallDependencies(jsonRoot, 'pkg1.SomeClass', 'pkg2.SomeClass');

    const graphGui = createTestGraphGui(jsonRoot, jsonDependencies);

    graphGui.clickNode('com.tngtech.archunit');

    graphGui.expectOnlyVisibleNodes('com.tngtech.archunit.pkg1', 'com.tngtech.archunit.pkg2');
    graphGui.expectOnlyVisibleDependencies({from: 'pkg1.SomeClass', to: 'pkg2.SomeClass'});
  });

  //TODO: Mock für Filter Menü
  //TODO: Jeder mögliche Filter-Usecase eher in Node
  //TODO: extra Layout-Test, siehe dazu auch TODOs in graph-chai-extensions

  it('initially folds all nodes', () => {
    const graph = realInitGraph(appContextWith(jsonGraphWithTwoClasses)).create(null);
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkg1', 'com.tngtech.archunit.pkg2'];
    const expDeps = ['com.tngtech.archunit.pkg1-com.tngtech.archunit.pkg2'];

    expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
    expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);
    return graph.root._updatePromise;
  });

  it('can filter node by name containing', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.clazz('SomeClass1', 'class').build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .add(nodeCreator.clazz('NotMatchingClass', 'class')
        .build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.NotMatchingClass', 'targetMethod()')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByName('*Some*');

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);
    });
  });

  it('can filter node by name not containing', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.clazz('SomeClass1', 'class').build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .add(nodeCreator.clazz('MatchingClass', 'class').build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.MatchingClass', 'targetMethod()')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByName('~*Matching*');

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);
    });
  });

  it('can filter nodes by type', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.clazz('SomeClass1', 'class').build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .add(nodeCreator.clazz('SomeInterface', 'interface').build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.SomeInterface', 'targetMethod()')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    const expNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2'];
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterNodesByType({showInterfaces: false, showClasses: true});

    return graph.root._updatePromise.then(() => {
      expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
      expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);
    });
  });

  it('can filter dependencies by type', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.clazz('SomeClass1', 'class').build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addInheritance().from('com.tngtech.archunit.SomeClass2')
      .to('com.tngtech.archunit.SomeClass1')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    const expDeps = ['com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2'];

    graph.filterDependenciesByType({
      INHERITANCE: false,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: true,
      FIELD_ACCESS: false
    });

    return graph.root._updatePromise.then(() => expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps));
  });

  it('transforms the dependencies if a node is folded', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.package('pkgToFold')
        .add(nodeCreator.clazz('SomeClass1', 'class').build())
        .build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.pkgToFold.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    const exp = ['com.tngtech.archunit.pkgToFold-com.tngtech.archunit.SomeClass2'];

    graph.root.getByName('com.tngtech.archunit.pkgToFold')._changeFoldIfInnerNodeAndRelayout();

    expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(exp);

    return graph.root._updatePromise;
  });

  it('updates the positions of the dependencies if a node is dragged', () => {
    const jsonRoot = nodeCreator.package('com.tngtech.archunit')
      .add(nodeCreator.clazz('SomeClass1', 'class').build())
      .add(nodeCreator.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const graph = initGraph(appContextWith(jsonGraph)).create();
    graph.root.getByName('com.tngtech.archunit.SomeClass1')._drag(10, 10);
    return graph.root._updatePromise.then(() => expect(graph.dependencies._getVisibleDependencies()[0]._view.hasJumpedToPosition).to.equal(true));
  });

  it('can change the fold-states of the nodes to show all violations', () => {
    const jsonRoot =
      nodeCreator.package('com.tngtech')
        .add(nodeCreator.package('pkg1')
          .add(nodeCreator.package('pkg2')
            .add(nodeCreator.clazz('SomeClass2', 'class').build())
            .build())
          .add(nodeCreator.clazz('SomeClass1', 'class').build())
          .build())
        .build();
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg1.SomeClass1', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];

    const graph = realInitGraph(appContextWith(jsonGraph, violations)).create(null);

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
      nodeCreator.package('com.tngtech')
        .add(nodeCreator.package('pkg1')
          .add(nodeCreator.package('pkg2')
            .add(nodeCreator.clazz('SomeClass2', 'class').build())
            .build())
          .add(nodeCreator.clazz('SomeClass1', 'class').build())
          .build())
        .add(nodeCreator.package('pkg3')
          .add(nodeCreator.clazz('SomeOtherClass', 'class').build())
          .build())
        .build();
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg3.SomeOtherClass', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const jsonGraph = createGraph(jsonRoot, jsonDependencies);

    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    },
      {
        rule: 'rule2',
        violations: ['<com.tngtech.pkg3.SomeOtherClass.startMethod()> FIELD_ACCESS to <com.tngtech.pkg1.pkg2.SomeClass2.targetField>']
      }];

    const graph = initGraph(appContextWith(jsonGraph, violations)).create(null, false);

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
    nodeCreator.package('com.tngtech')
      .add(nodeCreator.package('pkg1')
        .add(nodeCreator.package('pkg2')
          .add(nodeCreator.clazz('SomeClass2', 'class').build())
          .add(nodeCreator.clazz('SomeClass3', 'class').build())
          .build())
        .add(nodeCreator.clazz('SomeClass1', 'class').build())
        .build())
      .build();
  const jsonDependenciesForThreeClasses = createDependencies()
    .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
    .to('com.tngtech.pkg1.SomeClass1')
    .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass3')
    .to('com.tngtech.pkg1.SomeClass1')
    .build();
  const jsonGraphWithThreeClasses = createGraph(jsonRootWithThreeClasses, jsonDependenciesForThreeClasses);
  const violationsForThreeClasses = [{
    rule: 'rule1',
    violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
  },
    {
      rule: 'rule2',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass3> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];

  it('can hide nodes that are not involved in violations and show them again', () => {

    const graph = initGraph(appContextWith(jsonGraphWithThreeClasses, violationsForThreeClasses)).create(null, false);

    return graph.root._updatePromise.then(() => {
      graph.dependencies.showViolations(violationsForThreeClasses[0]);
      graph.onHideNodesWithoutViolationsChanged(true);

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
          'com.tngtech.pkg1.SomeClass1'];
        const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

        graph.onHideNodesWithoutViolationsChanged(false);

        return graph.root._updatePromise.then(() => {
          const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
            'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
          const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
            'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
          expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
          expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

          return graph.root._updatePromise;
        });
      });
    });
  });

  it('updates the nodes and dependencies, when the shown violation groups change and the option for hiding all ' +
    'nodes that are not involved in violations is enabled', () => {
    const graph = initGraph(appContextWith(jsonGraphWithThreeClasses, violationsForThreeClasses)).create(null, false);

    return graph.root._updatePromise.then(() => {
      graph.onHideNodesWithoutViolationsChanged(true);
      graph.showViolations(violationsForThreeClasses[0]);

      return graph.root._updatePromise.then(() => {
        const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
          'com.tngtech.pkg1.SomeClass1'];
        const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1'];
        expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
        expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

        graph.showViolations(violationsForThreeClasses[1]);

        return graph.root._updatePromise.then(() => {
          const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
            'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
          const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
            'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
          expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
          expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

          graph.hideViolations(violationsForThreeClasses[0]);

          return graph.root._updatePromise.then(() => {
            const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2',
              'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
            const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
            expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
            expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

            graph.hideViolations(violationsForThreeClasses[1]);

            return graph.root._updatePromise.then(() => {
              const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2', 'com.tngtech.pkg1.pkg2.SomeClass2',
                'com.tngtech.pkg1.pkg2.SomeClass3', 'com.tngtech.pkg1.SomeClass1'];
              const expDeps = ['com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1',
                'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1'];
              expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
              expect(graph.dependencies._getVisibleDependencies()).to.haveDependencies(expDeps);

              return graph.root._updatePromise;
            });
          });
        });
      });
    });
  });
});