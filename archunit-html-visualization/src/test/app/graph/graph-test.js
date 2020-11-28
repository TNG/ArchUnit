'use strict';

const {expect} = require('chai');
require('./testinfrastructure/graph-chai-extensions');
const createJsonFromClassNames = require('./testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const {createDependencies, createViolationsFromDependencies} = require('./testinfrastructure/test-json-creator');
const getGraphUi = require('./testinfrastructure/graph-creator').getGraphUi;

const circlePadding = 10;

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

      await graphUi.changeCircleGeometrySettings(10, 10 * circlePadding);

      expectSiblingNodesToHaveAtLeastPadding(graphUi.rootUi, 10 * circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      expectNodesToHaveAtLeastPaddingFromParent(graphUi.rootUi, 10 * circlePadding, 'com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');

      await graphUi.changeCircleGeometrySettings(10, circlePadding);

      expectSiblingNodesToHaveAtLeastPadding(graphUi.rootUi, circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      expectNodesToHaveAtLeastPaddingFromParent(graphUi.rootUi, circlePadding, 'com.tngtech.archunit', 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
    });

    it('increases and decreases the circle size if the font size is changed', async () => {
      const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');
      const graphUi = await getGraphUi(jsonRoot, []);
      await graphUi.clickNode('com.tngtech.archunit');

      graphUi.expectNodeSizeCloseTo(10 + circlePadding, 'com.tngtech.archunit.SomeClass1', 'com.tngtech.archunit.SomeClass2');

      await graphUi.changeCircleGeometrySettings(100, circlePadding);

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

  describe('layout after node dragging', () => {
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

    it('a node that is focused brings all of its dependent nodes into the foreground', async() => {
      const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.FocusClass', 'com.tngtech.pkg1.DependentClass', 'com.tngtech.pkg1.OtherClass', 'com.tngtech.pkg2.DependentClass', 'com.tngtech.pkg2.OtherClass');
      const jsonDependencies = createDependencies()
        .addMethodCall().from('com.tngtech.pkg1.FocusClass', 'startMethod()')
        .to('com.tngtech.pkg1.DependentClass', 'endMethod()')
        .addMethodCall().from('com.tngtech.pkg1.FocusClass', 'startMethod()')
        .to('com.tngtech.pkg2.DependentClass', 'endMethod()')
        .build();
      const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
      await graphUi.clickNode('com.tngtech');
      await graphUi.clickNode('com.tngtech.pkg1');
      await graphUi.clickNode('com.tngtech.pkg2');

      const otherNodeInPkg1 = graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg1.OtherClass');
      await otherNodeInPkg1.dragOverCompletely('com.tngtech.pkg1.DependentClass');
      const otherNodeInPkg2 = graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.OtherClass');
      await otherNodeInPkg2.dragOverCompletely('com.tngtech.pkg2.DependentClass');

      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg1.OtherClass').liesInFrontOf('com.tngtech.pkg1.DependentClass')).to.be.true;
      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.OtherClass').liesInFrontOf('com.tngtech.pkg2.DependentClass')).to.be.true;

      await graphUi.dragNode('com.tngtech.pkg1.FocusClass', 1, 1);

      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg1.DependentClass').liesInFrontOf('com.tngtech.pkg1.OtherClass')).to.be.true;
      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.DependentClass').liesInFrontOf('com.tngtech.pkg2.OtherClass')).to.be.true;
    });

    it('a node is not overlapped by dependencies from sibling nodes', async() => {
      const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.FocusClass1', 'com.tngtech.pkg1.FocusClass2', 'com.tngtech.pkg2.DependentClass1', 'com.tngtech.pkg2.DepClass2');
      const jsonDependencies = createDependencies()
      .addMethodCall().from('com.tngtech.pkg1.FocusClass1', 'startMethod()')
      .to('com.tngtech.pkg2.DependentClass1', 'endMethod()')
      .addMethodCall().from('com.tngtech.pkg1.FocusClass2', 'startMethod()')
      .to('com.tngtech.pkg2.DepClass2', 'endMethod()')
      .build();

      const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
      await graphUi.clickNode('com.tngtech');
      await graphUi.clickNode('com.tngtech.pkg1');
      await graphUi.clickNode('com.tngtech.pkg2');

      const otherNodeInPkg1 = graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.DependentClass1');
      await otherNodeInPkg1.dragOverCompletely('com.tngtech.pkg2.DepClass2');

      await graphUi.dragNode('com.tngtech.pkg1.FocusClass2', 1, 1);

      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.DepClass2').liesInFrontOf('com.tngtech.pkg2.DependentClass1')).to.be.true;

      await graphUi.dragNode('com.tngtech.pkg1.FocusClass1', 1, 1);

      expect(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.DependentClass1').liesInFrontOf('com.tngtech.pkg2.DepClass2')).to.be.true;
      expect(graphUi.getVisibleDependencyWithName('com.tngtech.pkg1.FocusClass2-com.tngtech.pkg2.DepClass2').isInFrontOf(graphUi.rootUi.getNodeWithFullName('com.tngtech.pkg2.DependentClass1')._svg)).to.be.false;
    });
  });

  it('can change the fold-states of the nodes to show all violations', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.SomeClass1', 'org.somepackage.NotShownClass');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg1.SomeClass1', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const violations = createViolationsFromDependencies(jsonDependencies)
    .dependencyWithIndex(0).violatesRuleWithName('rule1')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.selectViolation(violations[0]);

    graphUi.expectOnlyVisibleNodes('com.tngtech.pkg1');

    await graphUi.clickOnOpenSelectedViolations();

    graphUi.expectOnlyVisibleNodes('pkg2', 'SomeClass1', 'com.tngtech.pkg1');
  });

  it('can fold nodes with minimum depth that have no violations', async() => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg3.SomeOtherClass');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addFieldAccess().from('com.tngtech.pkg3.SomeOtherClass', 'startMethod()')
      .to('com.tngtech.pkg1.pkg2.SomeClass2', 'targetField')
      .build();
    const violations = createViolationsFromDependencies(jsonDependencies)
      .dependencyWithIndex(0).violatesRuleWithName('rule1')
      .dependencyWithIndex(1).violatesRuleWithName('rule2')
      .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.selectViolation(violations[0]);
    await graphUi.setValueForCheckBoxHideNodesWithoutViolationWhenRuleSelected(false);
    await graphUi.clickOnOpenSelectedViolations();

    await graphUi.clickNode('com.tngtech.pkg3');

    graphUi.expectOnlyVisibleNodes('pkg2', 'SomeClass1', 'pkg1', 'SomeOtherClass', 'pkg3', 'com.tngtech');

    await graphUi.clickOnFoldOtherNodes();

    graphUi.expectOnlyVisibleNodes('pkg2', 'SomeClass1', 'pkg1', 'pkg3', 'com.tngtech');
  });

  const getJsonRootDependenciesAndViolationsForThreeClasses = () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.pkg2.SomeClass3');
    const jsonDependencies = createDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.SomeClass1')
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass3')
      .to('com.tngtech.pkg1.SomeClass1')
      .build();
    const violations = createViolationsFromDependencies(jsonDependencies)
    .dependencyWithIndex(0).violatesRuleWithName('rule1')
    .dependencyWithIndex(1).violatesRuleWithName('rule2')
    .build();

    return {jsonRoot, jsonDependencies, violations};
  };

  it('can hide nodes that are not involved in violations and show them again', async() => {
    const {jsonRoot, jsonDependencies, violations} = getJsonRootDependenciesAndViolationsForThreeClasses();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.selectViolation(violations[0]);
    await graphUi.clickOnOpenSelectedViolations();
    await graphUi.setValueForCheckBoxHideNodesWithoutViolationWhenRuleSelected(true);
    await graphUi.clickNode('com.tngtech.pkg1.pkg2');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[0]);
    await graphUi.setValueForCheckBoxHideNodesWithoutViolationWhenRuleSelected(false);

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');
  });

  it('updates the nodes and dependencies, when the shown violation groups change and the option for hiding all nodes that are not involved in violations is enabled', async() => {
    const {jsonRoot, jsonDependencies, violations} = getJsonRootDependenciesAndViolationsForThreeClasses();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.setValueForCheckBoxHideNodesWithoutViolationWhenRuleSelected(true);
    await graphUi.selectViolation(violations[0]);
    await graphUi.clickOnOpenSelectedViolations();
    await graphUi.clickNode('com.tngtech.pkg1.pkg2');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1');

    await graphUi.selectViolation(violations[1]);
    await graphUi.clickOnOpenSelectedViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[0]);
    await graphUi.clickOnOpenSelectedViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');

    await graphUi.deselectViolation(violations[1]);
    await graphUi.clickOnOpenSelectedViolations();

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'SomeClass3', 'pkg2', 'com.tngtech.pkg1');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.pkg1.pkg2.SomeClass2-com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.pkg2.SomeClass3-com.tngtech.pkg1.SomeClass1');
  });
});
