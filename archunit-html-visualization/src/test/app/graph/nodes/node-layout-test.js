'use strict';

const chai = require('chai');
const expect = chai.expect;
const checkOnRoot = require('../testinfrastructure/node-layout-test-infrastructure').checkOnRoot;
require('../testinfrastructure/node-layout-chai-extensions');

const createJsonFromClassNames = require('../testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;

const rootCreator = require('../testinfrastructure/root-creator');
const visualizationStyles = rootCreator.appContext.getVisualizationStyles();

beforeEach(() => {
  visualizationStyles.resetCirclePadding();
  visualizationStyles.resetNodeFontSize();
});

describe('Node layout', () => {
  describe('positions the nodes and sets the radii, so that', () => {
    it('they are within their parent and have a padding to it', async () => {
      const jsonRoot = createJsonFromClassNames('com.pkg1.SomeClass1$SomeInnerClass', 'com.pkg1.SomeClass2$SomeInnerClass1',
        'com.pkg1.SomeClass2$SomeInnerClass2', 'com.pkg2.SomeClass');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.allNodes.areWithinTheirParentWithRespectToPadding(visualizationStyles.getCirclePadding());
    });

    it('they have a padding to their sibling nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1$SomeInnerClass1', 'pkg1.SomeClass1$SomeInnerClass2',
        'pkg1.SomeClass1$SomeInnerClass3', 'pkg1.SomeClass2', 'pkg1.SomeClass3', 'pkg2.SomeClass');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.allNodes.havePaddingToTheirSiblings(visualizationStyles.getCirclePadding());
    });
  });

  describe('is adapted to changed layout setting:', () => {
    it('considers a changed circle padding', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);

      const newCirclePadding = 20;
      visualizationStyles.setCirclePadding(20);

      root.relayoutCompletely();
      await root._updatePromise;

      checkOnRoot(root).that.allNodes.areWithinTheirParentWithRespectToPadding(newCirclePadding);
      checkOnRoot(root).that.allNodes.havePaddingToTheirSiblings(newCirclePadding);
    });

    it('considers a changed node font size', async () => {
      const jsonRoot = createJsonFromClassNames('somePkgWithVeryLongName.Class');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);

      const newFontSize = 30;
      visualizationStyles.setNodeFontSize(30);

      root.relayoutCompletely();
      await root._updatePromise;

      checkOnRoot(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(newFontSize);
    });

    it('considers a new circle padding, if it is changed during a relayout', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
      const root = rootCreator.createRootFromJson(jsonRoot);
      root.relayoutCompletely();

      await new Promise(resolve => setTimeout(resolve, 3));

      const newCirclePadding = 20;
      visualizationStyles.setCirclePadding(newCirclePadding);
      root.relayoutCompletely();
      await root._updatePromise;

      checkOnRoot(root).that.allNodes.areWithinTheirParentWithRespectToPadding(newCirclePadding);
      checkOnRoot(root).that.allNodes.havePaddingToTheirSiblings(newCirclePadding);
    });

    it('considers a new node fontsize, if it is changed during a relayout', async () => {
      const jsonRoot = createJsonFromClassNames('somePkgWithVeryLongName.Class');
      const root = rootCreator.createRootFromJson(jsonRoot);
      root.relayoutCompletely();

      await new Promise(resolve => setTimeout(resolve, 3));

      const newFontSize = 30;
      visualizationStyles.setNodeFontSize(newFontSize);
      root.relayoutCompletely();
      await root._updatePromise;

      checkOnRoot(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(newFontSize);
    });
  });

  describe('positions the labels of the nodes and calculates the radii, so that', () => {
    it('the labels are within the nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClassWithAVeryLongName', 'pkg1.SomeClass', 'somePkgWithAVeryLongName.SomeClass');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.allNodes.haveTheirLabelWithinNode();
    });

    it('the labels of the leaves are in the middle', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass', 'pkg3');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.leaves.haveTheirLabelInTheMiddle();
    });

    it('the labels of the inner nodes are at the top', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.innerNodes.haveTheirLabelAtTheTop();
    });

    it('the labels of the inner nodes with only one child are above the child circle', async () => {
      const jsonRoot = createJsonFromClassNames('somePkgWithVeryLongName.Class');
      const root = await rootCreator.createRootWithLayoutFromJson(jsonRoot);
      checkOnRoot(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(visualizationStyles.getNodeFontSize());
    });
  });

  it('notifies its listeners about the changed layout', async () => {
    const jsonRoot = createJsonFromClassNames('com.pkg.SomeClass');
    const root = rootCreator.createRootFromJson(jsonRoot);
    let onLayoutChangedWasCalled = false;
    root.addListener({onLayoutChanged: () => onLayoutChangedWasCalled = true});
    root.relayoutCompletely();
    await root._updatePromise;
    expect(onLayoutChangedWasCalled).to.be.true;
  });

  it('is only computed once, if it is called several times immediately', async () => {
    const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
    const root = rootCreator.createRootFromJson(jsonRoot);
    let numberOfOnLayoutChangedCalls = 0;
    root.addListener({onLayoutChanged: () => numberOfOnLayoutChangedCalls++});
    root.relayoutCompletely();
    root.relayoutCompletely();
    root.relayoutCompletely();
    root.relayoutCompletely();
    await root._updatePromise;
    expect(numberOfOnLayoutChangedCalls).to.equal(1);
  });

  it('is computed again when calling enforceCompleteRelayout()', async () => {
    const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
    const root = rootCreator.createRootFromJson(jsonRoot);
    let numberOfOnLayoutChangedCalls = 0;
    root.addListener({onLayoutChanged: () => numberOfOnLayoutChangedCalls++});
    root.relayoutCompletely();
    root.relayoutCompletely();
    root.enforceCompleteRelayout();
    root.enforceCompleteRelayout();
    await root._updatePromise;
    expect(numberOfOnLayoutChangedCalls).to.equal(3);
  });
});