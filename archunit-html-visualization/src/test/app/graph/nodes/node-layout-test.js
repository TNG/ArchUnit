'use strict';

const expect = require('chai').expect;

const rootCreator = require('../testinfrastructure/root-creator');
const visualizationStyles = rootCreator.getVisualizationStyles();

const RootUi = require('./testinfrastructure/root-ui');

const testLayoutOn = require('../testinfrastructure/node-layout-test-infrastructure').testLayoutOnRoot;

afterEach(() => {
  visualizationStyles.resetCirclePadding();
  visualizationStyles.resetNodeFontSize();
});

describe('Node layout', () => {
  describe('when a node is sized and positioned', () => {
    it('is within its parent', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'com.pkg1.SomeClass1$SomeInnerClass',
        'com.pkg1.SomeClass2$SomeInnerClass1',
        'com.pkg1.SomeClass2$SomeInnerClass2',
        'com.pkg2.SomeClass');

      testLayoutOn(root).that.allNodes.areWithinTheirParentWithRespectToCirclePadding();
    });

    it('does not overlap with its siblings', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'pkg1.SomeClass1$SomeInnerClass1',
        'pkg1.SomeClass1$SomeInnerClass2',
        'pkg1.SomeClass1$SomeInnerClass3',
        'pkg1.SomeClass2',
        'pkg1.SomeClass3',
        'pkg2.SomeClass');

      testLayoutOn(root).that.allNodes.havePaddingToTheirSiblings();
    });
  });

  describe('change layout settings', () => {
    it('correctly re-layouts with respect to circle padding', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');

      visualizationStyles.setCirclePadding(20);

      root.relayoutCompletely();
      await root._updatePromise;

      const rootUi = RootUi.of(root);

      // FIXME: Migrate to test-ui structure instead of implicit assertions
      rootUi.allNodes().forEach(nodeUi => nodeUi.expectToBeWithin(nodeUi.parent));

      testLayoutOn(root).that.allNodes.areWithinTheirParentWithRespectToCirclePadding();
      testLayoutOn(root).that.allNodes.havePaddingToTheirSiblings();
    });

    it('considers a changed node font size', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('somePkgWithVeryLongName.Class');

      const newFontSize = 30;
      visualizationStyles.setNodeFontSize(30);

      root.relayoutCompletely();
      await root._updatePromise;

      testLayoutOn(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(newFontSize);
    });

    it('considers a new circle padding, if it is changed during a relayout', async () => {
      const root = rootCreator.createRootFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
      root.relayoutCompletely();

      await new Promise(resolve => setTimeout(resolve, 3));

      const newCirclePadding = 20;
      visualizationStyles.setCirclePadding(newCirclePadding);
      root.relayoutCompletely();
      await root._updatePromise;

      testLayoutOn(root).that.allNodes.areWithinTheirParentWithRespectToCirclePadding();
      testLayoutOn(root).that.allNodes.havePaddingToTheirSiblings();
    });

    it('considers a new node fontsize, if it is changed during a relayout', async () => {
      const root = rootCreator.createRootFromClassNames('somePkgWithVeryLongName.Class');
      root.relayoutCompletely();

      await new Promise(resolve => setTimeout(resolve, 3));

      const newFontSize = 30;
      visualizationStyles.setNodeFontSize(newFontSize);
      root.relayoutCompletely();
      await root._updatePromise;

      testLayoutOn(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(newFontSize);
    });
  });

  describe('positions the labels of the nodes and calculates the radii, so that', () => {
    it('the labels are within the nodes', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('pkg1.SomeClassWithAVeryLongName', 'pkg1.SomeClass',
        'somePkgWithAVeryLongName.SomeClass');
      testLayoutOn(root).that.allNodes.haveTheirLabelWithinNode();
    });

    it('the labels of the leaves are in the middle', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass', 'pkg3');
      testLayoutOn(root).that.leaves.haveTheirLabelInTheMiddle();
    });

    it('the labels of the inner nodes are at the top', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass');
      testLayoutOn(root).that.innerNodes.haveTheirLabelAtTheTop();
    });

    it('the labels of the inner nodes with only one child are above the child circle', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('somePkgWithVeryLongName.Class');
      testLayoutOn(root).that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode(visualizationStyles.getNodeFontSize());
    });
  });

  it('changes the root size and notifies about it', async () => {
    let width, height;
    const root = await rootCreator.createRootFromClassNamesAndLayout('com.pkg.SomeClass', {
      onSizeChanged: (halfWidth, halfHeight) => {
        width = 2 * halfWidth;
        height = 2 * halfHeight;
      }
    });
    testLayoutOn(root).that.allNodes.areWithinDimensions(width, height);
  });

  it('notifies its listeners about the changed layout', async () => {
    const root = rootCreator.createRootFromClassNames('com.pkg.SomeClass');
    let onLayoutChangedWasCalled = false;
    root.addListener({onLayoutChanged: () => onLayoutChangedWasCalled = true});
    root.relayoutCompletely();
    await root._updatePromise;
    expect(onLayoutChangedWasCalled).to.be.true;
  });

  it('is only computed once, if it is called several times immediately after each other', async () => {
    const root = rootCreator.createRootFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
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
    const root = rootCreator.createRootFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2', 'pkg2.SomeClass');
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