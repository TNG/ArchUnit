'use strict';

const expect = require('chai').expect;
require('./chai/node-chai-extensions');
const Vector = require('./main-files').get('vectors').Vector;

const stubs = require('./stubs');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(10),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub
});
const circlePadding = appContext.getVisualizationStyles().getCirclePadding();
const Node = appContext.getNode();
const testJson = require('./test-json-creator');

const testRoot = require('./test-object-creator').tree;

const MAXIMUM_DELTA = 0.0001;

const getAbsolutePositionOfNode = node => node.getSelfAndPredecessors().reduce((acc, p) =>
  ({x: acc.x + p.visualData.relativePosition.x, y: acc.y + p.visualData.relativePosition.y}), {x: 0, y: 0});

describe('Root', () => {
  it('should have no parent', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit').build();
    const root = new Node(jsonRoot);
    expect(root.getParent()).to.equal(undefined);
  });

  it('should know that it is the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.isRoot()).to.equal(true);
  });

  it('should not fold or change its fold-state', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit').build();
    const root = new Node(jsonRoot);
    root.foldIfInnerNode();
    expect(root.isFolded()).to.equal(false);
    root._changeFoldIfInnerNodeAndRelayout();
    expect(root.isFolded()).to.equal(false);
  });

  it('should return the correct node by name', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .add(testJson.clazz('SomeClass2', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.getByName('com.tngtech.archunit.SomeClass1').getFullName()).to.equal('com.tngtech.archunit.SomeClass1');
    expect(root.getByName('com.tngtech.archunit.SomeClass2').getFullName()).to.equal('com.tngtech.archunit.SomeClass2');
  });
});

describe('Inner node', () => {
  it('can fold', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');

    innerNode.foldIfInnerNode();

    expect(innerNode.isFolded()).to.equal(true);
    expect(innerNode.isCurrentlyLeaf()).to.equal(true);
    expect(innerNode._originalChildren.map(node => node._view.isVisible)).to.not.include(true);
    expect(listenerStub.foldedNode()).to.equal(innerNode);
    expect(innerNode.getCurrentChildren()).to.containExactlyNodes([]);
  });

  it('can change the fold-state to folded', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');
    innerNode._changeFoldIfInnerNodeAndRelayout();

    expect(innerNode.isFolded()).to.equal(true);
    expect(innerNode.isCurrentlyLeaf()).to.equal(true);
    expect(innerNode._originalChildren.map(node => node._view.isVisible)).to.not.include(true);
    expect(listenerStub.foldedNode()).to.equal(innerNode);
    expect(innerNode.getCurrentChildren()).to.containExactlyNodes([]);
    return root.doNext(() => expect(listenerStub.onLayoutChangedWasCalled()).to.equal(true));
  });

  it('can change the fold-state to unfolded', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');
    innerNode._changeFoldIfInnerNodeAndRelayout();
    innerNode._changeFoldIfInnerNodeAndRelayout();

    const promises = [];
    expect(innerNode.isFolded()).to.equal(false);
    expect(innerNode.isCurrentlyLeaf()).to.equal(false);
    expect(innerNode.getCurrentChildren()).to.containExactlyNodes(['com.tngtech.archunit.test.SomeClass1', 'com.tngtech.archunit.test.SomeClass2']);
    promises.push(root.doNext(() => expect(innerNode._originalChildren.map(node => node._view.isVisible)).to.not.include(false)));
    expect(listenerStub.foldedNode()).to.equal(innerNode);
    promises.push(root.doNext(() => expect(listenerStub.onLayoutChangedWasCalled()).to.equal(true)));
    return Promise.all(promises);
  });
});

describe('Leaf', () => {
  it('should not fold or change its fold-state', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('Leaf', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    const leaf = root.getByName('com.tngtech.archunit.Leaf');
    leaf.foldIfInnerNode();
    expect(leaf.isFolded()).to.equal(false);
    leaf._changeFoldIfInnerNodeAndRelayout();
    expect(leaf.isFolded()).to.equal(false);
  });

  it('should know that it is a leaf', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('Leaf', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    const leaf = root.getByName('com.tngtech.archunit.Leaf');
    expect(leaf.isCurrentlyLeaf()).to.equal(true);
  });
});

describe('Inner node or leaf', () => {
  it('should know its parent', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.getByName('com.tngtech.archunit.SomeClass').getParent()).to.equal(root);
  });

  it('should know that is not the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.getByName('com.tngtech.archunit.SomeClass').isRoot()).to.equal(false);
  });

  it('can be dragged: changes its relative and absolute coordinates and the ones of its descendants,' +
    ' updates its view and calls the listener', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.visual.SomeClass');
    const dx = -5;
    const dy = 5;
    const expCoordinates = {x: dx, y: dy};

    nodeToDrag._drag(dx, dy);
    return root.doNext(() => {
      expect({
        x: nodeToDrag.visualData.relativePosition.x,
        y: nodeToDrag.visualData.relativePosition.y
      }).to.deep.equal(expCoordinates);
      nodeToDrag.getSelfAndDescendants().forEach(node =>
        expect({
          x: node.visualData.absolutePosition.x,
          y: node.visualData.absolutePosition.y
        }).to.deep.equal(getAbsolutePositionOfNode(node)));
      expect(nodeToDrag._view.hasJumpedToPosition).to.equal(true);
      expect(listenerStub.onDragWasCalled()).to.equal(true);
    });
  });

  it('can be dragged anywhere if it is a child of the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.SomeClass');
    const dx = -100;
    const dy = 100;
    const expCoordinates = {x: dx, y: dy};
    nodeToDrag._drag(dx, dy);
    return root.doNext(() =>
      expect({
        x: nodeToDrag.visualData.relativePosition.x,
        y: nodeToDrag.visualData.relativePosition.y
      }).to.deep.equal(expCoordinates));
  });

  it('is shifted to the rim of the parent if it dragged out of its parent and the parent is not the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    const nodeToDrag = root.getByName('com.tngtech.archunit.visual.SomeClass');

    nodeToDrag._drag(-50, 50);
    return root.doNext(() => {
      const expD = Math.trunc(Math.sqrt(Math.pow(nodeToDrag.getParent().getRadius() - nodeToDrag.getRadius(), 2) / 2));
      const expCoordinates = {x: -expD, y: expD};

      expect(nodeToDrag.visualData.relativePosition.x).to.closeTo(expCoordinates.x, MAXIMUM_DELTA);
      expect(nodeToDrag.visualData.relativePosition.y).to.closeTo(expCoordinates.y, MAXIMUM_DELTA);
    });
  });

  it('notifies its listeners, if it is dragged so that nodes (class and folded package) are overlapping', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToBeOverlapped')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.clazz('ClassToDrag', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.ClassToDrag');
    const nodeToBeOverlapped = root.getByName('com.tngtech.archunit.pkgToBeOverlapped');
    nodeToBeOverlapped._changeFoldIfInnerNodeAndRelayout();

    return root.doNext(() => {
      const dragVector = Vector.between(nodeToDrag.visualData.relativePosition, nodeToBeOverlapped.visualData.relativePosition);
      dragVector.norm(dragVector.length() - nodeToBeOverlapped.getRadius());

      nodeToDrag._drag(dragVector.x, dragVector.y);

      return root.doNext(() => {
        const exp = [{
          overlappedNode: 'com.tngtech.archunit.pkgToBeOverlapped',
          position: nodeToDrag.visualData.absolutePosition
        }];
        expect(listenerStub.overlappedNodesAndPosition()).to.deep.equal(exp);
      });
    });
  });

  it('does not notify its listeners, if it is dragged so that a class and an unfolded package are overlapping', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToBeOverlapped')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.clazz('ClassToDrag', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.ClassToDrag');
    const nodeToBeOverlapped = root.getByName('com.tngtech.archunit.pkgToBeOverlapped');

    return root.doNext(() => {
      const dragVector = Vector.between(nodeToDrag.visualData.relativePosition, nodeToBeOverlapped.visualData.relativePosition);
      dragVector.norm(3 * circlePadding);

      nodeToDrag._drag(dragVector.x, dragVector.y);

      return root.doNext(() => {
        expect(listenerStub.overlappedNodesAndPosition()).to.be.empty;
      });
    });
  });

  it('does not notify its listeners, if it is dragged so that a class and a folded package are overlapping and the package is in front of the class', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('ClassToDrag', 'class').build())
      .add(testJson.package('pkgToBeOverlapped')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.ClassToDrag');
    const nodeToBeOverlapped = root.getByName('com.tngtech.archunit.pkgToBeOverlapped');
    nodeToBeOverlapped._changeFoldIfInnerNodeAndRelayout();

    return root.doNext(() => {
      const dragVector = Vector.between(nodeToDrag.visualData.relativePosition, nodeToBeOverlapped.visualData.relativePosition);
      dragVector.norm(dragVector.length() - nodeToBeOverlapped.getRadius());

      nodeToDrag._drag(dragVector.x, dragVector.y);

      return root.doNext(() => {
        expect(listenerStub.overlappedNodesAndPosition()).to.be.empty;
      });
    });
  });

  it('notifies its listeners, if it is dragged so that three nodes are mutually overlapping', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .add(testJson.clazz('SomeClass2', 'class').build())
      .add(testJson.clazz('SomeClass3', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const node1 = root.getByName('com.tngtech.archunit.SomeClass1');
    const node2 = root.getByName('com.tngtech.archunit.SomeClass2');
    const node3 = root.getByName('com.tngtech.archunit.SomeClass3');

    return root.doNext(() => {
      const vectorToNode3 = Vector.between(node2.visualData.relativePosition, node3.visualData.relativePosition);
      vectorToNode3.norm(vectorToNode3.length() - node3.getRadius());
      node2._drag(vectorToNode3.x, vectorToNode3.y);

      return root.doNext(() => {
        const vectorToNode2 = Vector.between(node1.visualData.relativePosition, node2.visualData.relativePosition);
        vectorToNode2.norm(vectorToNode2.length() - node2.getRadius());
        const vectorToNode3 = Vector.between(node1.visualData.relativePosition, node3.visualData.relativePosition);
        vectorToNode3.norm(vectorToNode3.length() - node3.getRadius());
        const dragVector = vectorToNode2.scale(0.5).add(vectorToNode3.scale(0.5));

        node1._drag(dragVector.x, dragVector.y);

        return root.doNext(() => {
          const exp = [
            {
              overlappedNode: 'com.tngtech.archunit.SomeClass2',
              position: node3.visualData.absolutePosition
            },
            {
              overlappedNode: 'com.tngtech.archunit.SomeClass1',
              position: node2.visualData.absolutePosition
            },
            {
              overlappedNode: 'com.tngtech.archunit.SomeClass1',
              position: node3.visualData.absolutePosition
            },
            {
              overlappedNode: 'com.tngtech.archunit.SomeClass2',
              position: node3.visualData.absolutePosition
            }
          ];
          expect(listenerStub.overlappedNodesAndPosition()).to.deep.equal(exp);
        });
      });
    });
  });

  it('notifies its listeners, if it is dragged so that a child node of the dragged node is overlapping another node', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('ClassToBeOverlapped', 'class').build())
      .add(testJson.package('pkgToDrag')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();

    const nodeToDrag = root.getByName('com.tngtech.archunit.pkgToDrag');
    const nodeToOverlap = root.getByName('com.tngtech.archunit.pkgToDrag.SomeClass');
    const nodeToBeOverlapped = root.getByName('com.tngtech.archunit.ClassToBeOverlapped');

    return root.doNext(() => {
      const dragVector = Vector.between(nodeToOverlap.visualData.absolutePosition, nodeToBeOverlapped.visualData.absolutePosition);
      dragVector.norm(dragVector.length() - nodeToBeOverlapped.getRadius());

      nodeToDrag._drag(dragVector.x, dragVector.y);

      return root.doNext(() => {
        const exp = [
          {
            overlappedNode: 'com.tngtech.archunit.ClassToBeOverlapped',
            position: nodeToOverlap.visualData.absolutePosition
          }
        ];
        expect(listenerStub.overlappedNodesAndPosition()).to.deep.equal(exp);
      });
    });
  });
});

describe('Node layout', () => {
  const jsonRoot = testJson.package('com.tngtech.archunit')
    .add(testJson.clazz('SomeClass1', 'class').build())
    .add(testJson.clazz('SomeClass2', 'class').build())
    .add(testJson.package('visual')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .add(testJson.clazz('SomeClass2', 'class').build())
      .add(testJson.clazz('SomeClass3', 'class').build())
      .build())
    .build();

  it("should set a node's absolute position correctly", () => {
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        const absolutePosition = getAbsolutePositionOfNode(node);
        expect(node.visualData.absolutePosition.x).to.closeTo(absolutePosition.x, MAXIMUM_DELTA);
        expect(node.visualData.absolutePosition.y).to.closeTo(absolutePosition.y, MAXIMUM_DELTA);
      });
    });
  });

  it('should make all nodes fixed after having done the layout', () => {
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        expect(node.visualData.absolutePosition.fx).to.not.be.undefined;
        expect(node.visualData.absolutePosition.fy).to.not.be.undefined;
        expect(node.visualData.absolutePosition.isFixed()).to.be.true;
      });
    });
  });

  it('should put every child node within its parent node considering the padding', () => {
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          expect(node).to.locatedWithinWithPadding(node.getParent(), circlePadding);
        }
      });
    });
  });

  it('does the relayout only once, when it is called several times after each other', () => {
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const movedNodes = [];
    stubs.saveMovedNodesTo(movedNodes);
    root.relayoutCompletely();
    root.relayoutCompletely();
    root.relayoutCompletely();
    return root._updatePromise.then(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(movedNodes);
    });
  });

  it('should update the node-views on relayouting and call the listener', () => {
    let onRadiusChangedWasCalled = false;
    const root = new Node(jsonRoot, null, () => onRadiusChangedWasCalled = true);
    root.getLinks = () => [];
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    root.relayoutCompletely();
    return root.doNext(() => {
      expect(listenerStub.onLayoutChangedWasCalled()).to.equal(true);
      expect(onRadiusChangedWasCalled).to.equal(true);
      root.callOnEveryDescendantThenSelf(node => {
        expect(node._view.hasMovedToPosition).to.equal(true);
        expect(node._view.hasMovedToRadius).to.equal(true);
      });
    });
  });

  it('should not make two siblings overlap', () => {
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          node.getParent().getOriginalChildren().filter(child => child != node).forEach(sibling =>
            expect(node).to.notOverlapWith(sibling, 2 * circlePadding));
        }
      });
    });
  });

  it('should put the text at the correct position in the circle: for leaves in the middle, for inner nodes at the top ' +
    'and for the root at the very top; furthermore the text must be within the circle (except for the root)', () => {
    const nodeFontsize = appContext.getVisualizationStyles().getNodeFontSize();
    const calculateTextWith = stubs.calculateTextWidthStub;
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    root.relayoutCompletely();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        if (node.isRoot()) {
          expect(node._view.textOffset).to.closeTo(-node.getRadius() + nodeFontsize, MAXIMUM_DELTA);
        }
        else if (node.isCurrentlyLeaf()) {
          expect(node._view.textOffset).to.equal(0);
          expect(calculateTextWith(node.getName()) / 2).to.be.at.most(node.getRadius());
        }
        else {
          const halfTextWith = calculateTextWith(node.getName()) / 2;
          const offset = node._view.textOffset;
          expect(Math.sqrt(halfTextWith * halfTextWith + offset * offset)).to.be.at.most(node.getRadius());
        }
      });
    });
  });
});

describe('Node', () => {
  it('creates the correct tree-structure from json-input', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('test')
        .add(testJson.clazz('SomeTestClass', 'class')
          .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const exp = ['com.tngtech.archunit(package)', 'com.tngtech.archunit.SomeClass(class)',
      'com.tngtech.archunit.SomeInterface(interface)', 'com.tngtech.archunit.visual(package)',
      'com.tngtech.archunit.visual.SomeClass(class)', 'com.tngtech.archunit.test(package)',
      'com.tngtech.archunit.test.SomeTestClass(class)', 'com.tngtech.archunit.test.SomeTestClass$SomeInnerClass(class)'];
    const act = root.getSelfAndDescendants().map(node => `${node.getFullName()}(${node._description.type})`);
    expect(act).to.deep.equal(exp);
  });

  it('Adds CSS to make the mouse a pointer, if there are children to unfold', () => {
    const jsonRoot = testJson.package("com.tngtech")
      .add(testJson.clazz("Class1", "abstractclass").build())
      .build();

    const root = new Node(jsonRoot);

    expect(root.getClass()).to.contain(' foldable');
    expect(root.getClass()).not.to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).not.to.contain(' foldable');
  });

  it('can hide interfaces: hides packages with only interfaces, changes CSS-class of classes with only inner ' +
    'interfaces, does not hide interfaces with an inner class', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeInterfaceWithInnerClass', 'interface')
        .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
        .build())
      .add(testJson.package('interfaces')
        .add(testJson.clazz('SomeInterface', 'interface').build())
        .build())
      .add(testJson.package('classes')
        .add(testJson.clazz('SomeClassWithInnerInterface', 'class')
          .havingInnerClass(testJson.clazz('SomeInnerInterface', 'interface').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.SomeClass',
      'com.tngtech.archunit.SomeInterfaceWithInnerClass',
      'com.tngtech.archunit.SomeInterfaceWithInnerClass$SomeInnerClass', 'com.tngtech.archunit.classes',
      'com.tngtech.archunit.classes.SomeClassWithInnerInterface'];
    const expHiddenNodes = ['com.tngtech.archunit.SomeInterface', 'com.tngtech.archunit.interfaces',
      'com.tngtech.archunit.classes.SomeClassWithInnerInterface$SomeInnerInterface'].map(nodeFullName => root.getByName(nodeFullName));
    const nodeWithChangedCssClass = root.getByName('com.tngtech.archunit.classes.SomeClassWithInnerInterface');

    root.filterByType(false, true);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(nodeWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
      expect(nodeWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
    });
  });

  it('can hide classes: hides packages with only classes, changes CSS-class of interfaces with only inner ' +
    'classes, does not hide classes with an inner interface', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerInterface', 'class')
        .havingInnerClass(testJson.clazz('SomeInnerInterface', 'interface').build())
        .build())
      .add(testJson.package('classes')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('interfaces')
        .add(testJson.clazz('SomeInterfaceWithInnerClass', 'interface')
          .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.SomeInterface', 'com.tngtech.archunit.SomeClassWithInnerInterface',
      'com.tngtech.archunit.SomeClassWithInnerInterface$SomeInnerInterface', 'com.tngtech.archunit.interfaces',
      'com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass'];
    const expHiddenNodes = ['com.tngtech.archunit.SomeClass', 'com.tngtech.archunit.classes',
      'com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass$SomeInnerClass'].map(nodeFullName => root.getByName(nodeFullName));
    const nodeWithChangedCssClass = root.getByName('com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass');

    root.filterByType(true, false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(nodeWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
      expect(nodeWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
    });
  });

  it('can hide classes and interfaces, so that only the root remains', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerInterface', 'class')
        .havingInnerClass(testJson.clazz('SomeInnerInterface', 'interface').build())
        .build())
      .add(testJson.package('classes')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('interfaces')
        .add(testJson.clazz('SomeInterfaceWithInnerClass', 'interface')
          .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const expHiddenNodes = ['com.tngtech.archunit.SomeClass', 'com.tngtech.archunit.SomeInterface',
      'com.tngtech.archunit.SomeClassWithInnerInterface', 'com.tngtech.archunit.classes',
      'com.tngtech.archunit.interfaces'].map(nodeFullName => root.getByName(nodeFullName));

    const visibleNodes = ['com.tngtech.archunit'];

    root.filterByType(false, false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(root._view.cssClass).to.not.contain(' foldable');
      expect(root._view.cssClass).to.contain(' not-foldable');
    });
  });

  it('can hide classes and show again: sets visibilities correctly and ' +
    'resets the CSS-class of interfaces with only inner classes', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerInterface', 'class')
        .havingInnerClass(testJson.clazz('SomeInnerInterface', 'interface').build())
        .build())
      .add(testJson.package('classes')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('interfaces')
        .add(testJson.clazz('SomeInterfaceWithInnerClass', 'interface')
          .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.SomeClass', 'com.tngtech.archunit.classes', 'com.tngtech.archunit.classes.SomeClass',
      'com.tngtech.archunit.SomeInterface', 'com.tngtech.archunit.SomeClassWithInnerInterface',
      'com.tngtech.archunit.SomeClassWithInnerInterface$SomeInnerInterface', 'com.tngtech.archunit.interfaces',
      'com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass',
      'com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass$SomeInnerClass'];
    const nodeWithChangedCssClass = root.getByName('com.tngtech.archunit.interfaces.SomeInterfaceWithInnerClass');

    root.filterByType(true, false);
    root.filterByType(true, true);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(nodeWithChangedCssClass._view.cssClass).to.contain(' foldable');
      expect(nodeWithChangedCssClass._view.cssClass).to.not.contain(' not-foldable');
    });
  });

  it('can filter nodes by name using a simple string: hides packages with no matching classes, does not hide not ' +
    'matching class with matching inner classes', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('XMatchingClass', 'class').build())
      .add(testJson.clazz('XMatchingInterface', 'interface').build())
      .add(testJson.clazz('NotMatchingClassWithMatchingInnerChild', 'class')
        .havingInnerClass(testJson.clazz('XMatchingClass', 'class').build())
        .build())
      .add(testJson.package('pkgWithNoMatchingClass')
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .add(testJson.package('XMatchingPkg')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('pkgWithMatchingClass')
        .add(testJson.clazz('ClassXMatching', 'class').build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.XMatchingClass', 'com.tngtech.archunit.XMatchingInterface',
      'com.tngtech.archunit.NotMatchingClassWithMatchingInnerChild',
      'com.tngtech.archunit.NotMatchingClassWithMatchingInnerChild$XMatchingClass',
      'com.tngtech.archunit.XMatchingPkg', 'com.tngtech.archunit.XMatchingPkg.SomeClass',
      'com.tngtech.archunit.pkgWithMatchingClass', 'com.tngtech.archunit.pkgWithMatchingClass.ClassXMatching'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgWithNoMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.NotMatchingClass'].map(nodeFullName => root.getByName(nodeFullName));

    root.filterByName('XMatching', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can filter nodes by name using a string with a space at its end: changes the CSS-class of a matching node ' +
    'with no matching children (this situation is only possible at filtering on matching end)', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('MatchingClassXX', 'class').build())
      .add(testJson.clazz('MatchingInterfaceXX', 'interface').build())
      .add(testJson.clazz('NotMatchingXXClass', 'class').build())
      .add(testJson.package('matchingPkgWithNoMatchingClassXX')
        .add(testJson.clazz('NotMatchingClassXx', 'class').build())
        .build())
      .add(testJson.package('pkgWithMatchingClass')
        .add(testJson.clazz('MatchingClassWithNotMatchingChildXX', 'class')
          .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.MatchingClassXX',
      'com.tngtech.archunit.MatchingInterfaceXX', 'com.tngtech.archunit.matchingPkgWithNoMatchingClassXX',
      'com.tngtech.archunit.pkgWithMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX'];
    const expHiddenNodes = ['com.tngtech.archunit.NotMatchingXXClass',
      'com.tngtech.archunit.matchingPkgWithNoMatchingClassXX.NotMatchingClassXx',
      'com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX$NotMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.NotMatchingClass'].map(nodeFullName => root.getByName(nodeFullName));
    const pkgWithChangedCssClass = root.getByName('com.tngtech.archunit.matchingPkgWithNoMatchingClassXX');
    const classWithChangedCssClass = root.getByName('com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX');

    root.filterByName('XX ', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(pkgWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
      expect(pkgWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
      expect(classWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
      expect(classWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
    });
  });

  it('can filter nodes by name using a string with a star in it', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('XMatchingClassY', 'class').build())
      .add(testJson.clazz('XNotMatchingClassWithMatchingChild', 'class')
        .havingInnerClass(testJson.clazz('MatchingYClass', 'class').build())
        .build())
      .add(testJson.package('notMatchingXPkgWithMatchingClass')
        .add(testJson.clazz('YMatchingClass', 'class').build())
        .build())
      .add(testJson.package('XMatchingYPkg')
        .add(testJson.clazz('MatchingClass', 'class').build())
        .build())
      .add(testJson.package('pkgWithNoMatchingYClass')
        .add(testJson.clazz('NotMatchingXClass1', 'class').build())
        .add(testJson.clazz('NotMatchingClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.XMatchingClassY', 'com.tngtech.archunit.XNotMatchingClassWithMatchingChild',
      'com.tngtech.archunit.XNotMatchingClassWithMatchingChild$MatchingYClass',
      'com.tngtech.archunit.notMatchingXPkgWithMatchingClass',
      'com.tngtech.archunit.notMatchingXPkgWithMatchingClass.YMatchingClass',
      'com.tngtech.archunit.XMatchingYPkg', 'com.tngtech.archunit.XMatchingYPkg.MatchingClass'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgWithNoMatchingYClass'].map(nodeFullName => root.getByName(nodeFullName));

    root.filterByName('X*Y', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('should filter out a node not matching a part with wildcard', () => {
    const root = testRoot(
      'my.company.first.SomeClass',
      'my.company.first.OtherClass',
      'my.company.second.SomeClass',
      'my.company.second.OtherClass');

    root.filterByName('my.*.first', false);
    return root.doNext(() => expect(root).to.containOnlyClasses('my.company.first.SomeClass', 'my.company.first.OtherClass'))
      .then(() => {
        root.filterByName('company*.Some', false);
        return root.doNext(() => expect(root).to.containOnlyClasses('my.company.first.SomeClass', 'my.company.second.SomeClass'))
          .then(() => {
            root.filterByName('company*.Some', true);
            return root.doNext(() => expect(root).to.containOnlyClasses('my.company.first.OtherClass', 'my.company.second.OtherClass'))
              .then(() => {
                root.filterByName('company*.Some ', false);
                return root.doNext(() => expect(root).to.containNoClasses());
              });
          });
      });
  });

  it('can filter nodes by name and exclude the matching nodes: changes CSS-class of not matching class with ' +
    'matching inner class (can occur in this scenario)', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('XMatchingClass', 'class').build())
      .add(testJson.clazz('XMatchingInterface', 'interface').build())
      .add(testJson.clazz('NotMatchingClassWithMatchingChild', 'class')
        .havingInnerClass(testJson.clazz('XMatchingClass', 'class').build())
        .build())
      .add(testJson.package('notMatchingPkgWithOnlyMatchingClasses')
        .add(testJson.clazz('XMatchingClass', 'class').build())
        .build())
      .add(testJson.package('XMatchingPkg')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('pkgWithMatchingClass')
        .add(testJson.clazz('XMatchingClass', 'class').build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.NotMatchingClassWithMatchingChild',
      'com.tngtech.archunit.notMatchingPkgWithOnlyMatchingClasses', 'com.tngtech.archunit.pkgWithMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.NotMatchingClass'];
    const expHiddenNodes = ['com.tngtech.archunit.XMatchingClass', 'com.tngtech.archunit.XMatchingInterface',
      'com.tngtech.archunit.NotMatchingClassWithMatchingChild$XMatchingClass',
      'com.tngtech.archunit.XMatchingPkg', 'com.tngtech.archunit.pkgWithMatchingClass.XMatchingClass']
      .map(nodeFullName => root.getByName(nodeFullName));
    const classWithChangedCssClass =
      root.getByName('com.tngtech.archunit.NotMatchingClassWithMatchingChild');

    root.filterByName('XMatching', true);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(classWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
      expect(classWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
    });
  });

  it('can filter nodes by name using a string with a space at the end and exclude the matching nodes: does not hide ' +
    'matching nodes with a not matching child', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('MatchingClassXX', 'class').build())
      .add(testJson.clazz('MatchingClassWithNotMatchingChildXX', 'class')
        .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .add(testJson.package('MatchingPkgWithNoMatchingChildXX')
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.MatchingClassWithNotMatchingChildXX',
      'com.tngtech.archunit.MatchingClassWithNotMatchingChildXX$NotMatchingClass',
      'com.tngtech.archunit.MatchingPkgWithNoMatchingChildXX',
      'com.tngtech.archunit.MatchingPkgWithNoMatchingChildXX.NotMatchingClass'];
    const expHiddenNodes = ['com.tngtech.archunit.MatchingClassXX'].map(nodeFullName => root.getByName(nodeFullName));

    root.filterByName('XX ', true);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can reset the node-filter by name: the CSS-class of a node, that was matching the filter but has no child ' +
    'matching the filter, is reset correctly', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('MatchingClassXX', 'class').build())
      .add(testJson.clazz('MatchingInterfaceXX', 'interface').build())
      .add(testJson.clazz('NotMatchingXXClass', 'class').build())
      .add(testJson.package('matchingPkgWithNoMatchingClassXX')
        .add(testJson.clazz('NotMatchingClassXx', 'class').build())
        .build())
      .add(testJson.package('pkgWithMatchingClass')
        .add(testJson.clazz('MatchingClassWithNotMatchingChildXX', 'class')
          .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.MatchingClassXX',
      'com.tngtech.archunit.MatchingInterfaceXX', 'com.tngtech.archunit.NotMatchingXXClass',
      'com.tngtech.archunit.matchingPkgWithNoMatchingClassXX',
      'com.tngtech.archunit.matchingPkgWithNoMatchingClassXX.NotMatchingClassXx',
      'com.tngtech.archunit.pkgWithMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX',
      'com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX$NotMatchingClass',
      'com.tngtech.archunit.pkgWithMatchingClass.NotMatchingClass'];
    const pkgWithChangedCssClass = root.getByName('com.tngtech.archunit.matchingPkgWithNoMatchingClassXX');
    const classWithChangedCssClass = root.getByName('com.tngtech.archunit.pkgWithMatchingClass.MatchingClassWithNotMatchingChildXX');

    root.filterByName('XX ', false);
    root.filterByName('', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(pkgWithChangedCssClass._view.cssClass).to.contain(' foldable');
      expect(pkgWithChangedCssClass._view.cssClass).to.not.contain(' not-foldable');
      expect(classWithChangedCssClass._view.cssClass).to.contain(' foldable');
      expect(classWithChangedCssClass._view.cssClass).to.not.contain(' not-foldable');
    });
  });

  it('can change the node filter by name (without resetting it before): shows nodes matching the new filter but not ' +
    'the old one again', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('XMatchingClass', 'class').build())
      .add(testJson.clazz('YMatchingInterface', 'interface').build())
      .add(testJson.clazz('NotMatchingClassWithMatchingChild', 'class')
        .havingInnerClass(testJson.clazz('YMatchingClass', 'class').build())
        .build())
      .add(testJson.package('pkgWithNoMatchingClasses')
        .add(testJson.clazz('XMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.YMatchingInterface',
      'com.tngtech.archunit.NotMatchingClassWithMatchingChild',
      'com.tngtech.archunit.NotMatchingClassWithMatchingChild$YMatchingClass'];
    const expHiddenNodes = [
      'com.tngtech.archunit.XMatchingClass', 'com.tngtech.archunit.pkgWithNoMatchingClasses']
      .map(nodeFullName => root.getByName(nodeFullName));

    root.filterByName('XMatching', false);
    root.filterByName('YMatching', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can filter by name (ending with a space) and filter by type (hiding classes): hides packages (not matching) ' +
    'without children matching both filters and does not hide not matching nodes with a matching child, and changes ' +
    'CSS-class of a node loosing its children only because of both filters (that means every child is matching exactly ' +
    'one filter)', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgWithoutChildrenMatchingBothFilters')
        .add(testJson.clazz('NameMatchingClassX', 'class').build())
        .add(testJson.clazz('NotNameMatchingInterface', 'interface').build())
        .build())
      .add(testJson.clazz('NameMatchingInterfaceX', 'interface').build())
      .add(testJson.clazz('NameMatchingClassX', 'class').build())
      .add(testJson.clazz('NotNameMatchingInterface', 'interface').build())
      .add(testJson.package('nameMatchingPkgX')
        .add(testJson.clazz('NotMatchingClass', 'class')).build())
      .add(testJson.package('pkgWithChildMatchingBothFilters')
        .add(testJson.clazz('NameMatchingInterfaceX', 'interface').build())
        .add(testJson.clazz('NotNameMatchingClassWithChildMatchingBothFilters', 'class')
          .havingInnerClass(testJson.clazz('NameMatchingInterfaceX', 'interface').build())
          .build())
        .build())
      .add(testJson.clazz('NameMatchingInterfaceWithNoMatchingChildX', 'interface')
        .havingInnerClass(testJson.clazz('NotNameMatchingInterface', 'interface').build())
        .havingInnerClass(testJson.clazz('NameMatchingClassX', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    root.filterByName('X ', false);
    root.filterByType(true, false);

    const visibleNodes = ['com.tngtech.archunit',
      'com.tngtech.archunit.NameMatchingInterfaceX',
      'com.tngtech.archunit.pkgWithChildMatchingBothFilters',
      'com.tngtech.archunit.pkgWithChildMatchingBothFilters.NameMatchingInterfaceX',
      'com.tngtech.archunit.pkgWithChildMatchingBothFilters.NotNameMatchingClassWithChildMatchingBothFilters',
      'com.tngtech.archunit.pkgWithChildMatchingBothFilters.NotNameMatchingClassWithChildMatchingBothFilters$NameMatchingInterfaceX',
      'com.tngtech.archunit.NameMatchingInterfaceWithNoMatchingChildX'];
    const expHiddenNodes = [
      'com.tngtech.archunit.pkgWithoutChildrenMatchingBothFilters', 'com.tngtech.archunit.NameMatchingClassX',
      'com.tngtech.archunit.NotNameMatchingInterface', 'com.tngtech.archunit.nameMatchingPkgX',
      'com.tngtech.archunit.NameMatchingInterfaceWithNoMatchingChildX$NotNameMatchingInterface',
      'com.tngtech.archunit.NameMatchingInterfaceWithNoMatchingChildX$NameMatchingClassX']
      .map(nodeFullName => root.getByName(nodeFullName));
    const interfaceWithChangedCssClass = root.getByName('com.tngtech.archunit.NameMatchingInterfaceWithNoMatchingChildX');

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(interfaceWithChangedCssClass._view.cssClass).to.contain(' not-foldable');
      expect(interfaceWithChangedCssClass._view.cssClass).to.not.contain(' foldable');
    });
  });

  it('can filter by name and by type and then reset the name-filter: resets CSS-class of node with a child matching ' +
    'the type-filter but not the name-filter', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('NameMatchingClassX', 'class').build())
      .add(testJson.clazz('NameMatchingInterfaceX', 'interface').build())
      .add(testJson.clazz('NotNameMatchingInterface', 'interface').build())
      .add(testJson.clazz('NameMatchingInterfaceWithChildOnlyMatchingNameFilterX', 'interface')
        .havingInnerClass(testJson.clazz('NotNameMatchingInterface', 'interface').build())
        .build())
      .add(testJson.package('pkgWithNoNameMatchingChild')
        .add(testJson.clazz('NotNameMatchingInterface', 'interface').build())
        .build())
      .build();
    const root = new Node(jsonRoot);

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.NameMatchingInterfaceX',
      'com.tngtech.archunit.NotNameMatchingInterface', 'com.tngtech.archunit.pkgWithNoNameMatchingChild',
      'com.tngtech.archunit.pkgWithNoNameMatchingChild.NotNameMatchingInterface',
      'com.tngtech.archunit.NameMatchingInterfaceWithChildOnlyMatchingNameFilterX',
      'com.tngtech.archunit.NameMatchingInterfaceWithChildOnlyMatchingNameFilterX$NotNameMatchingInterface'];
    const expHiddenNodes = ['com.tngtech.archunit.NameMatchingClassX'].map(nodeFullName => root.getByName(nodeFullName));
    const nodeWithChangedCssClass = root.getByName('com.tngtech.archunit.NameMatchingInterfaceWithChildOnlyMatchingNameFilterX');

    root.filterByName('X ', false);
    root.filterByType(true, false);
    root.filterByName('', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
      expect(nodeWithChangedCssClass._view.cssClass).to.contain(' foldable');
      expect(nodeWithChangedCssClass._view.cssClass).to.not.contain(' not-foldable');
    });
  });

  it('can fold and then filter by name: the not matching folded node with matching children (but which are hidden ' +
    'through folding) should not be hidden', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingXClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFold');

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkgToFold'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgToFold.MatchingXClass'].map(nodeFullName => root.getByName(nodeFullName));

    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    root.filterByName('X', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can filter by name, fold and unfold a node in this order: the filter should be still applied after unfolding ' +
    '(especially on the hidden nodes)', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .add(testJson.clazz('MatchingXClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFold');

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkgToFold',
      'com.tngtech.archunit.pkgToFold.MatchingXClass'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgToFold.NotMatchingClass'].map(nodeFullName => root.getByName(nodeFullName));

    root.filterByName('X', false);
    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    pkgToFold._changeFoldIfInnerNodeAndRelayout();

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can fold, filter by name and reset the filter in this order: filtering should not influence the fold-state of the ' +
    'folded node', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFoldX')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFoldX');

    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    root.filterByName('X', true);
    root.filterByName('', false);

    return root.doNext(() => {
      expect(pkgToFold.isFolded()).to.equal(true);
      expect(pkgToFold.isCurrentlyLeaf()).to.equal(true);
      expect(pkgToFold._originalChildren.map(node => node._view.isVisible)).to.not.include(true);
      expect(pkgToFold.getCurrentChildren()).to.containExactlyNodes([]);
    });
  });

  it('can filter by name, fold and reset the filter in this order: the fold-state should not be changed by resetting the filter', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class').build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFold');

    root.filterByName('X', true);
    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    root.filterByName('', false);

    return root.doNext(() => {
      expect(pkgToFold.isFolded()).to.equal(true);
      expect(pkgToFold.isCurrentlyLeaf()).to.equal(true);
      expect(pkgToFold._originalChildren.map(node => node._view.isVisible)).to.not.include(true);
      expect(pkgToFold.getCurrentChildren()).to.containExactlyNodes([]);
    });
  });

  it('can fold, filter by name and unfold: then the filter should be applied on the hidden children of the folded ' +
    'node', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .add(testJson.clazz('MatchingXClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFold');

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkgToFold',
      'com.tngtech.archunit.pkgToFold.MatchingXClass'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgToFold.NotMatchingClass'].map(nodeFullName => root.getByName(nodeFullName));

    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    root.filterByName('X', false);
    pkgToFold._changeFoldIfInnerNodeAndRelayout();

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });

  it('can unfold and filter by name: then the node, which would have been shown by unfolding but does not pass ' +
    'the filter, is hidden', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .add(testJson.clazz('MatchingXClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.getLinks = () => [];
    const pkgToFold = root.getByName('com.tngtech.archunit.pkgToFold');
    pkgToFold._changeFoldIfInnerNodeAndRelayout();

    const visibleNodes = ['com.tngtech.archunit', 'com.tngtech.archunit.pkgToFold',
      'com.tngtech.archunit.pkgToFold.MatchingXClass'];
    const expHiddenNodes = ['com.tngtech.archunit.pkgToFold.NotMatchingClass'].map(nodeFullName => root.getByName(nodeFullName));

    pkgToFold._changeFoldIfInnerNodeAndRelayout();
    root.filterByName('X', false);

    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containExactlyNodes(visibleNodes);
      expect(root.getSelfAndDescendants().map(node => node._view.isVisible)).to.not.include(false);
      expect(expHiddenNodes.map(node => node._view.isVisible)).to.not.include(true);
    });
  });
});