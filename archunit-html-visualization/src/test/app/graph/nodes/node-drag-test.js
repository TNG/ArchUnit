'use strict';

const expect = require('chai').expect;

const rootCreator = require('../testinfrastructure/root-creator');
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;

const testWholeLayoutOn = require('../testinfrastructure/node-layout-test-infrastructure').testWholeLayoutOn;
const testLayoutOnRoot = require('../testinfrastructure/node-layout-test-infrastructure').testLayoutOnRoot;
const testGui = require('../testinfrastructure/node-gui-adapter').testGuiFromRoot;

const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;
const vectors = require('../../../../main/app/graph/infrastructure/vectors').vectors;

describe('Dragging a node', () => {
  describe('changes its position by the movement, if it is dragged', async () => {
    let root, _offsetPosition, nodePositionBefore;

    beforeEach(async () => {
      _offsetPosition = {x: 0, y: 0};
      root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', {
        onJumpedToPosition: offsetPosition => _offsetPosition = offsetPosition
      });

      nodePositionBefore = testGui(root).inspect.positionOf('my.company.SomeClass');
    });

    it('to the right', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: 20, dy: 0});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the bottom right', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: 20, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y + 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the bottom', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: 0, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x, y: nodePositionBefore.y + 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the bottom left', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: -20, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y + 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the left', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: -20, dy: 0});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the top left', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: -20, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y - 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the top', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: 0, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x, y: nodePositionBefore.y - 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });

    it('to the top right', async () => {
      await testGui(root).interact.dragNodeAndAwait('my.company.SomeClass', {dx: 20, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y - 20});
      testGui(root).test.that.node('my.company.SomeClass').is.atPosition(expectedPosition);
    });
  });

  it('changes the positions of its descendants by the movement', async () => {
    let _offsetPosition = {x: 0, y: 0};
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.someSubPkg.OtherClass', 'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass', {
        onJumpedToPosition: offsetPosition => _offsetPosition = offsetPosition
      });

    const somePkg = testGui(root).inspect.positionOf('my.company.somePkg');
    const someSubPkg = testGui(root).inspect.positionOf('my.company.somePkg.someSubPkg');
    const otherSubPkg = testGui(root).inspect.positionOf('my.company.somePkg.otherSubPkg');
    const someSubPkgSomeClass = testGui(root).inspect.positionOf('my.company.somePkg.someSubPkg.SomeClass');
    const someSubPkgSomeClassInnerClass = testGui(root).inspect.positionOf('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass');
    const someSubPkgOtherClass = testGui(root).inspect.positionOf('my.company.somePkg.someSubPkg.OtherClass');
    const otherSubPkgOtherClass = testGui(root).inspect.positionOf('my.company.somePkg.otherSubPkg.OtherClass');

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg', {dx: -50, dy: 70});

    const expectedPosition = nodePositionBefore => (vectors.add(_offsetPosition, {x: nodePositionBefore.x - 50, y: nodePositionBefore.y + 70}));

    testGui(root).test.that.node('my.company.somePkg').is.atPosition(expectedPosition(somePkg));
    testGui(root).test.that.node('my.company.somePkg.someSubPkg').is.atPosition(expectedPosition(someSubPkg));
    testGui(root).test.that.node('my.company.somePkg.otherSubPkg').is.atPosition(expectedPosition(otherSubPkg));
    testGui(root).test.that.node('my.company.somePkg.someSubPkg.SomeClass').is.atPosition(expectedPosition(someSubPkgSomeClass));
    testGui(root).test.that.node('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass').is.atPosition(expectedPosition(someSubPkgSomeClassInnerClass));
    testGui(root).test.that.node('my.company.somePkg.someSubPkg.OtherClass').is.atPosition(expectedPosition(someSubPkgOtherClass));
    testGui(root).test.that.node('my.company.somePkg.otherSubPkg.OtherClass').is.atPosition(expectedPosition(otherSubPkgOtherClass));
  });

  it('expands all predecessor nodes, if the node is dragged out of them', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.SomeClass');

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg.SomeClass', {dx: 1000, dy: -1000});

    testLayoutOnRoot(root).that.allNodes.areWithinTheirParentWithRespectToCirclePadding();
  });

  it('expands the root and notifies about it', async () => {
    let width, height;
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass',
      'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass', {
        onSizeExpanded: (halfWidth, halfHeight) => {
          width = 2 * halfWidth;
          height = 2 * halfHeight;
        }
      });

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg.someSubPkg.SomeClass', {dx: -1000, dy: 1000});

    testLayoutOnRoot(root).that.allNodes.areWithinDimensions(width, height);
  });

  //TODO: is calling the listener for the dragged node really necessary??
  it('calls the onNodeRimChanged-listener for the dragged node and the top most expanded nodes', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass');
    const nodeListenerMock = createListenerMock('onNodeRimChanged', 'onNodesFocused');
    root.addListener(nodeListenerMock.listener);

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg.someSubPkg', {dx: 1000, dy: -1000});

    nodeListenerMock.test.that.listenerFunction('onNodeRimChanged').was.called.with.nodes(
      'my.company.somePkg.someSubPkg', 'default');
  });

  it('leads to a correct layout (apart from padding between nodes)', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.someSubPkg.OtherClass', 'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass');

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg.someSubPkg.SomeClass', {dx: 1000, dy: -1000});

    testLayoutOnRoot(root)
      .that.allNodes.areWithinTheirParentWithRespectToCirclePadding()
      .and.that.allNodes.haveTheirLabelWithinNode()
      .and.that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode()
      .and.that.innerNodes.haveTheirLabelAtTheTop()
      .and.that.leaves.haveTheirLabelInTheMiddle();
  });

  describe('#overlapsWith()', () => {
    it('returns true for a node that is dragged over a sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.OtherClass');

      await testGui(root).interact.dragNodeOverOtherNodeAndAwait('my.company.SomeClass', 'my.company.OtherClass');

      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company.OtherClass'))).to.be.true;
      expect(root.getByName('my.company.OtherClass').overlapsWith(root.getByName('my.company.SomeClass'))).to.be.true;
    });

    it('returns true for the descendants of a node that is dragged over a sibling (as the dragged node is pushed into the foreground) and false again, when the other node is focussed', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');

      await testGui(root).interact.dragNodeOverOtherNodeAndAwait('my.company.somePkg', 'my.company.otherPkg');

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;
      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      await testGui(root).interact.dragNodeAndAwait('my.company.otherPkg', {dx: 1, dy: 1});

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.false;
      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.false;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.false;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.false;
    });

    it('returns true for a node that is dragged over another non-sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.OtherClass');

      await testGui(root).interact.dragNodeOverOtherNodeAndAwait('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.OtherClass');

      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg.OtherClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg.OtherClass').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;
    });

    it('returns true for the predecessors of a node that is dragged over another non-sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.OtherClass');

      await testGui(root).interact.dragNodeOverOtherNodeAndAwait('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg');

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;
      expect(root.getByName('my.company.somePkg').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg'))).to.be.true;
    });
  });

  //TODO: more test cases in graph-test
  describe('focuses the dragged node', () => {
    it('so that the dragged node is in foreground', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass$SomeInnerClass',
        'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass', 'my.company.secondPkg.FirstClass',
        'my.company.secondPkg.SecondClass', 'my.company.secondPkg.ThirdClass', 'my.company.thirdPkg.SomeClass');

      await testGui(root).interact.dragNodeAndAwait('my.company.firstPkg.FirstClass', {dx: -20, dy: 30});
      testGui(root).test.that.node('my.company.firstPkg.FirstClass').is.in.foreground();

      await testGui(root).interact.dragNodeAndAwait('my.company.firstPkg.SecondClass', {dx: -20, dy: 30});
      testGui(root).test.that.node('my.company.firstPkg.SecondClass').is.in.foreground();

      await testGui(root).interact.dragNodeAndAwait('my.company.secondPkg.FirstClass', {dx: -20, dy: 30});
      testGui(root).test.that.node('my.company.secondPkg.FirstClass').is.in.foreground();

      await testGui(root).interact.dragNodeAndAwait('my.company.secondPkg.SecondClass', {dx: -20, dy: 30});
      testGui(root).test.that.node('my.company.secondPkg.SecondClass').is.in.foreground();
    });

    describe('#liesInFrontOf()', () => {
      it('is true on the dragged node when called with any other node, that is not a descendant of the dragged node', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass',
          'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass', 'my.company.secondPkg.FirstClass');

        await testGui(root).interact.dragNodeAndAwait('my.company.firstPkg.FirstClass', {dx: -20, dy: 30});

        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.SecondClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.ThirdClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.secondPkg.FirstClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.secondPkg'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company'))).to.be.true;

        await testGui(root).interact.dragNodeAndAwait('my.company.secondPkg.FirstClass', {dx: -20, dy: 30});
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.FirstClass'))).to.be.true;
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.SecondClass'))).to.be.true;
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.ThirdClass'))).to.be.true;
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg'))).to.be.true;
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company.secondPkg'))).to.be.true;
        expect(root.getByName('my.company.secondPkg.FirstClass').liesInFrontOf(root.getByName('my.company'))).to.be.true;
      });

      it('is false on the dragged node when called with a descendant of the dragged node', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass',
          'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass');

        await testGui(root).interact.dragNodeAndAwait('my.company.firstPkg.FirstClass', {dx: -20, dy: 30});
        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.FirstClass'))).to.be.false;
        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.SecondClass'))).to.be.false;
        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.ThirdClass'))).to.be.false;
      });
    });
  });
});