'use strict';

const {expect} = require('chai');
require('./testinfrastructure/node-chai-extensions');
require('./testinfrastructure/dependencies-chai-extension');
require('./testinfrastructure/graph-chai-extensions');
const createJsonFromClassNames = require('./testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const guiElementsMock = require('./testinfrastructure/gui-elements-mock');
const {createDependencies, createGraph} = require('./testinfrastructure/test-json-creator');
const AppContext = require('../../../main/app/graph/app-context');

const GraphUi = require('./testinfrastructure/graph-ui');
const svgMock = require('./testinfrastructure/svg-mock');
const realInitGraph = require('../../../main/app/graph/graph').init;

const getGraphUi = async (jsonRoot, jsonDependencies = [], violations = []) => {
  const appContext = appContextWith(createGraph(jsonRoot, jsonDependencies), violations);
  const graph = realInitGraph(appContext).create(svgMock.createSvgRoot(), svgMock.createEmptyElement());
  const graphUi = GraphUi.of(graph);

  await graphUi.waitForUpdateFinished();

  return graphUi;
};

const circlePadding = 10;

const appContextWith = (graph, violations) => AppContext.newInstance({
  guiElements: guiElementsMock,
  visualizationData: {
    jsonGraph: graph,
    jsonViolations: violations
  }
});

describe('Graph', () => {
  const expectSiblingNodesToHaveAtLeastPadding = (rootUi, padding, ...nodeFullNames) => expect(rootUi).to.haveSiblingNodesWithPaddingAtLeast(padding, nodeFullNames);
  const expectNodesToHaveAtLeastPaddingFromParent = (rootUi, padding, parentFullName, ...nodeFullNames) => expect(rootUi).to.haveNodesWithPaddingToParentAtLeast(padding, parentFullName, nodeFullNames);

  describe('initialization of the nodes', () => {
    it('sets the css classes so packages have a css-class "package", classes a css-class "class", interfaces a css-class "interface"', async()=> {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass', 'pkg1.SomeInterface');
      const graphUi = await getGraphUi(jsonRoot, []);
      await graphUi.clickNode('pkg1');

      graphUi.rootUi.getNodeWithFullName('pkg1').expectToHaveClasses(['package']);
      graphUi.rootUi.getNodeWithFullName('pkg1.SomeClass').expectToHaveClasses(['class']);
      graphUi.rootUi.getNodeWithFullName('pkg1.SomeInterface').expectToHaveClasses(['interface']);
    });

    it('nodes lie in front of their parent nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg1.SomeClass3', 'pkg2.SomeClass');
      const graphUi = await getGraphUi(jsonRoot, []);
      await graphUi.clickNode('pkg1');

      expect(graphUi.rootUi.getNodeWithFullName('pkg1.SomeClass1').isInForeground()).to.equal(false);

      expect(graphUi.rootUi.getNodeWithFullName('pkg1.SomeClass1').liesInFrontOf('pkg1')).to.equal(true);
      expect(graphUi.rootUi.getNodeWithFullName('pkg1.SomeClass2').liesInFrontOf('pkg1')).to.equal(true);
      expect(graphUi.rootUi.getNodeWithFullName('pkg1.SomeClass3').liesInFrontOf('pkg1')).to.equal(true);
      expectNodesToHaveAtLeastPaddingFromParent(graphUi.rootUi, circlePadding, 'pkg1', 'pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg1.SomeClass3');
    });

    it('sibling nodes have a minimum padding between each other', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass', 'pkg3.SomeClass', 'pkg4.SomeClass');
      const graphUi = await getGraphUi(jsonRoot, []);

      // click is not really needed but adds some maths to the test
      await graphUi.clickNode('pkg1');

      expectSiblingNodesToHaveAtLeastPadding(graphUi.rootUi, circlePadding, 'pkg1', 'pkg2', 'pkg3', 'pkg4');
    });
  });

  describe('layout nodes after setting changes', () => {
    it('increases and decreases the circle padding after change of padding settings', async () => {
      const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      const graphUi = await getGraphUi(jsonRoot, []);
      await graphUi.clickNode('com.tngtech.archunit');

      await graphUi.changeMenuSettings(10, 10 * circlePadding);

      expectSiblingNodesToHaveAtLeastPadding(graphUi.rootUi, 10 * circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      expectNodesToHaveAtLeastPaddingFromParent(graphUi.rootUi, 10 * circlePadding, 'com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');

      await graphUi.changeMenuSettings(10, circlePadding);

      expectSiblingNodesToHaveAtLeastPadding(graphUi.rootUi, circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      expectNodesToHaveAtLeastPaddingFromParent(graphUi.rootUi, circlePadding, 'com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    });

    it('increases and decreases the circle size if the font size is changed', async () => {
      const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      const graphUi = await getGraphUi(jsonRoot, []);
      await graphUi.clickNode('com.tngtech.archunit');

      graphUi.expectNodeSizeCloseTo(10 + circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');

      await graphUi.changeMenuSettings(100, circlePadding);

      // re-implement "production" logic (part of mocking also taken into account), to show how values are computed
      const lengthOfClassName = 'SomeClassN'.length;
      const circleTextPadding = 5;
      const scale = 10; // from mocking
      const expectedNodeSize = lengthOfClassName * 3 * scale / 2 + circleTextPadding;
      graphUi.expectNodeSizeCloseTo(expectedNodeSize, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    });
  });

  it('shows only the top level packages right after creation', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass$TmpInner$TmpInner2', 'some.package.SomeClass');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass')
      .to('some.package.SomeClass', 'targetMethod()')
      .build();

    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);

    graphUi.expectOnlyVisibleNodes('com.tngtech.archunit', 'some.package');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit-some.package');
  });

  it('shows children if I click on a node', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.pkg1.SomeClass', 'com.tngtech.archunit.pkg2.SomeClass');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.pkg1.SomeClass')
      .to('com.tngtech.archunit.pkg2.SomeClass', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);

    await graphUi.clickNode('com.tngtech.archunit');

    graphUi.expectOnlyVisibleNodes('pkg1', 'pkg2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.pkg1-com.tngtech.archunit.pkg2');
  });

  it('scrolls the graph to center if a node is dragged out of viewport', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass', 'org.SomeClass');
    const jsonDependencies = [];
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);

    await graphUi.dragNode('org', 1000, 1000);

    expect(graphUi._graph._view._svgContainerDivSelection.scrollLeft).to.be.above(909.9);
    expect(graphUi._graph._view._svgContainerDivSelection.scrollTop).to.be.above(909.9);
  });

  it('resizes the graph if a node is dragged out of window size', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass', 'org.SomeClass');
    const jsonDependencies = [];
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);

    await graphUi.dragNode('org', 1000, 1000);

    expect(graphUi._graph._view.svgElement.width).to.be.above(700);
    expect(graphUi._graph._view.svgElement.height).to.be.above(700);
  });

  it('can filter node by name containing', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2', 'com.tngtech.archunit.NotMatchingClass');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.NotMatchingClass', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.changeNodeFilter('*Some*');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('can filter node by control click', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.CtrlClickClass', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.CtrlClickClass', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass1', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.ctrlClickNode('com.tngtech.archunit.CtrlClickClass');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
    graphUi.expectNodeFilter('~com.tngtech.archunit.CtrlClickClass');
  });

  it('can filter node by name not containing', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2', 'com.tngtech.archunit.MatchingClass');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.MatchingClass', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.changeNodeFilter('~*Matching*');

    graphUi.expectOnlyVisibleNodes('com.tngtech.archunit', 'SomeClass1', 'SomeClass2');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('can filter nodes by type', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2', 'com.tngtech.archunit.SomeInterface');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
      .to('com.tngtech.archunit.SomeInterface', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.filterNodesByType({showInterfaces: false, showClasses: true});

    graphUi.expectOnlyVisibleNodes('com.tngtech.archunit', 'SomeClass1', 'SomeClass2');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('can filter dependencies by type', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .addInheritance().from('com.tngtech.archunit.SomeClass2')
      .to('com.tngtech.archunit.SomeClass1')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.filterDependenciesByType({
      INHERITANCE: false,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: true,
      FIELD_ACCESS: false
    });

    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('transforms the dependencies if a node is unfolded', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.pkgToUnfold.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.pkgToUnfold.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.pkgToUnfold-com.tngtech.archunit.SomeClass2');

    await graphUi.clickNode('com.tngtech.archunit.pkgToUnfold');

    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.pkgToUnfold.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('updates the positions of the dependencies if a node is dragged', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
      .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    const startGeometry = graphUi.getVisibleDependencyWithName('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2').start;

    await graphUi.dragNode('com.tngtech.archunit.SomeClass1', 10, 10);

    const endGeometry = graphUi.getVisibleDependencyWithName('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2').start;

    expect(startGeometry.startPoint.x).to.not.be.closeTo(endGeometry.startPoint.x, 0.1);
    expect(startGeometry.startPoint.y).to.not.be.closeTo(endGeometry.startPoint.y, 0.1);
    expect(startGeometry.endPoint.x).to.not.be.closeTo(endGeometry.endPoint.x, 0.1);
    expect(startGeometry.endPoint.y).to.not.be.closeTo(endGeometry.endPoint.y, 0.1);
  });

  it('can change the fold-states of the nodes to show all violations', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.SomeClass1', 'org.somepackage.NotShownClass');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg1.SomeClass1', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.selectViolation(violations[0]);

    graphUi.expectOnlyVisibleNodes('com.tngtech.pkg1');

    await graphUi.showAllViolations();

    graphUi.expectOnlyVisibleNodes('pkg2', 'SomeClass1', 'com.tngtech.pkg1');
  });

  it.skip('can fold nodes with minimum depth that have no violations', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg3.SomeOtherClass');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg3.SomeOtherClass', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    },
      {
        rule: 'rule2',
        violations: ['<com.tngtech.pkg3.SomeOtherClass.startMethod()> FIELD_ACCESS to <com.tngtech.pkg1.pkg2.SomeClass2.targetField>']
      }];
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    // TODO add correct interaction and assertion here
    await graphUi.selectViolation(violations[0]);

    // TODO remove old test style
    // return graph.root._updatePromise.then(() => {
    //   graph.dependencies.showViolations(violations[0]);
    //   graph.foldNodesWithMinimumDepthWithoutViolations();
    //
    //   return graph.root._updatePromise.then(() => {
    //     const expNodes = ['com.tngtech', 'com.tngtech.pkg1', 'com.tngtech.pkg1.pkg2',
    //       'com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass2',
    //       'com.tngtech.pkg3'];
    //     expect(graph.root.getSelfAndDescendants()).to.containExactlyNodes(expNodes);
    //     return graph.root._updatePromise;
    //   });
    // });
  });

  const getJsonRootDependenciesAndViolationsForThreeClasses = () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.pkg2.SomeClass3');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass3')
      .to('com.tngtech.pkg1.SomeClass1')
      .build();
    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }, {
      rule: 'rule2',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass3> INHERITANCE to <com.tngtech.pkg1.SomeClass1>']
    }];

    return {jsonRoot, jsonDependencies, violations};
  };

  it('can hide nodes that are not involved in violations and show them again', async() => {
    const {jsonRoot, jsonDependencies, violations} = getJsonRootDependenciesAndViolationsForThreeClasses();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.selectViolation(violations[0]);
    await graphUi.showAllViolations();
    await graphUi.hideNodesWithoutViolationsChanged(true);
    await graphUi.clickNode('com.tngtech.pkg1.pkg2');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[0]);
    await graphUi.hideNodesWithoutViolationsChanged(false);

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');
  });

  it('updates the nodes and dependencies, when the shown violation groups change and the option for hiding all nodes that are not involved in violations is enabled', async() => {
    const {jsonRoot, jsonDependencies, violations} = getJsonRootDependenciesAndViolationsForThreeClasses();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.hideNodesWithoutViolationsChanged(true);
    await graphUi.selectViolation(violations[0]);
    await graphUi.showAllViolations();
    await graphUi.clickNode('com.tngtech.pkg1.pkg2');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1');

    await graphUi.selectViolation(violations[1]);
    await graphUi.showAllViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[0]);
    await graphUi.showAllViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[1]);
    await graphUi.showAllViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');
  });
});
