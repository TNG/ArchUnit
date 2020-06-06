'use strict';

const expect = require('chai').expect;

const rootCreator = require('../testinfrastructure/root-creator');
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;

const RootUi = require('./testinfrastructure/root-ui');

const vectors = require('../../../../main/app/graph/infrastructure/vectors').vectors;

describe('Dragging a node', () => {
  describe('changes its position by the movement, if it is dragged', async () => {
    let root, _offsetPosition, nodePositionBefore, rootUi;

    beforeEach(async () => {
      _offsetPosition = {x: 0, y: 0};
      root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', {
        onJumpedToPosition: offsetPosition => _offsetPosition = offsetPosition
      });

      rootUi = RootUi.of(root);

      nodePositionBefore = rootUi.getNodeWithFullName('my.company.SomeClass').absolutePosition;
    });

    it('to the right', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: 20, dy: 0});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y});

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the bottom right', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: 20, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y + 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the bottom', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: 0, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x, y: nodePositionBefore.y + 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the bottom left', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: -20, dy: 20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y + 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the left', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: -20, dy: 0});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the top left', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: -20, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x - 20, y: nodePositionBefore.y - 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the top', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: 0, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x, y: nodePositionBefore.y - 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });

    it('to the top right', async () => {
      await rootUi.getNodeWithFullName('my.company.SomeClass').drag({dx: 20, dy: -20});

      const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 20, y: nodePositionBefore.y - 20});
      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeAtPosition(expectedPosition);
    });
  });

  it('changes the positions of its descendants by the movement', async () => {
    let _offsetPosition = {x: 0, y: 0};
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.someSubPkg.OtherClass', 'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass', {
        onJumpedToPosition: offsetPosition => _offsetPosition = offsetPosition
      });

    const rootUi = RootUi.of(root);

    const somePkg = rootUi.getNodeWithFullName('my.company.somePkg').absolutePosition;
    const someSubPkg = rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg').absolutePosition;
    const otherSubPkg = rootUi.getNodeWithFullName('my.company.somePkg.otherSubPkg').absolutePosition;
    const someSubPkgSomeClass = rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass').absolutePosition;
    const someSubPkgSomeClassInnerClass = rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass').absolutePosition;
    const someSubPkgOtherClass = rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.OtherClass').absolutePosition;
    const otherSubPkgOtherClass = rootUi.getNodeWithFullName('my.company.somePkg.otherSubPkg.OtherClass').absolutePosition;

    await rootUi.getNodeWithFullName('my.company.somePkg').drag({dx: -50, dy: 70});

    const expectedPosition = nodePositionBefore => (vectors.add(_offsetPosition, {x: nodePositionBefore.x - 50, y: nodePositionBefore.y + 70}));

    rootUi.getNodeWithFullName('my.company.somePkg').expectToBeAtPosition(expectedPosition(somePkg));
    rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg').expectToBeAtPosition(expectedPosition(someSubPkg));
    rootUi.getNodeWithFullName('my.company.somePkg.otherSubPkg').expectToBeAtPosition(expectedPosition(otherSubPkg));
    rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass').expectToBeAtPosition(expectedPosition(someSubPkgSomeClass));
    rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass').expectToBeAtPosition(expectedPosition(someSubPkgSomeClassInnerClass));
    rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.OtherClass').expectToBeAtPosition(expectedPosition(someSubPkgOtherClass));
    rootUi.getNodeWithFullName('my.company.somePkg.otherSubPkg.OtherClass').expectToBeAtPosition(expectedPosition(otherSubPkgOtherClass));
  });

  it('expands all predecessor nodes, if the node is dragged out of them', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.SomeClass');

    const rootUi = RootUi.of(root);
    await rootUi.getNodeWithFullName('my.company.somePkg.SomeClass').drag({dx: 1000, dy: -1000});

    rootUi.allNodes.forEach(nodeUi => nodeUi.expectToBeWithin(nodeUi.parent));
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

    const rootUi = RootUi.of(root);

    await rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass').drag({dx: -1000, dy: 1000});

    rootUi.allNodes.forEach(nodeUi => nodeUi.expectToBeWithinRectangle(width, height));
  });

  it('calls the onNodeRimChanged-listener for the dragged node and the top most expanded nodes', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass');
    const nodeListenerMock = createListenerMock('onNodeRimChanged', 'onNodesFocused');
    root.addListener(nodeListenerMock.listener);

    await RootUi.of(root).getNodeWithFullName('my.company.somePkg.someSubPkg').drag({dx: 1000, dy: -1000});

    nodeListenerMock.test.that.listenerFunction('onNodeRimChanged').was.called.with.nodes(
      'my.company.somePkg.someSubPkg', 'default');
  });

  it('leads to a correct layout (apart from padding between nodes)', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.someSubPkg.SomeClass$SomeInnerClass',
      'my.company.somePkg.someSubPkg.OtherClass', 'my.company.somePkg.otherSubPkg.OtherClass', 'my.company.otherPkg.SomeClass');

    const rootUi = RootUi.of(root);
    await rootUi.getNodeWithFullName('my.company.somePkg.someSubPkg.SomeClass').drag({dx: 1000, dy: -1000});

    rootUi.allNodes.forEach(nodeUi => {
      nodeUi.expectToBeWithin(nodeUi.parent);
      nodeUi.expectToHaveLabelWithinCircle();
    });
    rootUi.nonLeafNodes.forEach(nodeUi => nodeUi.expectToHaveLabelAtTheTop());
    rootUi.nodesWithSingleChild.forEach(nodeUi => nodeUi.expectToHaveLabelAbove(nodeUi.childUis[0]));
    rootUi.leafNodes.forEach(nodeUi => nodeUi.expectToHaveLabelInTheMiddleOfCircle());
  });

  describe('#overlapsWith()', () => {
    it('returns true for a node that is dragged over a sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.OtherClass');

      await RootUi.of(root).getNodeWithFullName('my.company.SomeClass').dragOver('my.company.OtherClass');

      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company.OtherClass'))).to.be.true;
      expect(root.getByName('my.company.OtherClass').overlapsWith(root.getByName('my.company.SomeClass'))).to.be.true;
    });

    it('returns true for the descendants of a node that is dragged over a sibling (as the dragged node is pushed into the foreground) and false again, when the other node is focussed', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');

      const rootUi = RootUi.of(root);

      await rootUi.getNodeWithFullName('my.company.somePkg').dragOver('my.company.otherPkg');

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;
      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      await rootUi.getNodeWithFullName('my.company.otherPkg').drag({dx: 1, dy: 1});

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.false;
      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.false;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.false;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.false;
    });

    it('returns true for a node that is dragged over another non-sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.OtherClass');

      await RootUi.of(root).getNodeWithFullName('my.company.somePkg.SomeClass$SomeInnerClass').dragOver('my.company.otherPkg.OtherClass');

      expect(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.otherPkg.OtherClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg.OtherClass').overlapsWith(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;
    });

    it('returns true for the predecessors of a node that is dragged over another non-sibling', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.OtherClass');

      await RootUi.of(root).getNodeWithFullName('my.company.somePkg.SomeClass$SomeInnerClass').dragOver('my.company.otherPkg');

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;
      expect(root.getByName('my.company.somePkg').overlapsWith(root.getByName('my.company.otherPkg'))).to.be.true;

      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company.otherPkg').overlapsWith(root.getByName('my.company.somePkg'))).to.be.true;
    });
  });

  describe('focuses the dragged node', () => {
    it('so that the dragged node is in foreground', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass$SomeInnerClass',
        'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass', 'my.company.secondPkg.FirstClass',
        'my.company.secondPkg.SecondClass', 'my.company.secondPkg.ThirdClass', 'my.company.thirdPkg.SomeClass');

      const rootUi = RootUi.of(root);

      await rootUi.getNodeWithFullName('my.company.firstPkg.FirstClass').drag({dx: -20, dy: 30});
      rootUi.getNodeWithFullName('my.company.firstPkg.FirstClass').expectToBeInForeground();

      await rootUi.getNodeWithFullName('my.company.firstPkg.SecondClass').drag({dx: -20, dy: 30});
      rootUi.getNodeWithFullName('my.company.firstPkg.SecondClass').expectToBeInForeground();

      await rootUi.getNodeWithFullName('my.company.secondPkg.FirstClass').drag({dx: -20, dy: 30});
      rootUi.getNodeWithFullName('my.company.secondPkg.FirstClass').expectToBeInForeground();

      await rootUi.getNodeWithFullName('my.company.secondPkg.SecondClass').drag({dx: -20, dy: 30});
      rootUi.getNodeWithFullName('my.company.secondPkg.SecondClass').expectToBeInForeground();
    });

    describe('#liesInFrontOf()', () => {
      it('is true on the dragged node when called with any other node, that is not a descendant of the dragged node', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass',
          'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass', 'my.company.secondPkg.FirstClass');

        const rootUi = RootUi.of(root);

        await rootUi.getNodeWithFullName('my.company.firstPkg.FirstClass').drag({dx: -20, dy: 30});

        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.SecondClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg.ThirdClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.secondPkg.FirstClass'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.firstPkg'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company.secondPkg'))).to.be.true;
        expect(root.getByName('my.company.firstPkg.FirstClass').liesInFrontOf(root.getByName('my.company'))).to.be.true;

        await rootUi.getNodeWithFullName('my.company.secondPkg.FirstClass').drag({dx: -20, dy: 30});
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

        await RootUi.of(root).getNodeWithFullName('my.company.firstPkg.FirstClass').drag({dx: -20, dy: 30});

        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.FirstClass'))).to.be.false;
        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.SecondClass'))).to.be.false;
        expect(root.getByName('my.company.firstPkg').liesInFrontOf(root.getByName('my.company.firstPkg.ThirdClass'))).to.be.false;
      });
    });
  });
});
