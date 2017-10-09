'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');
require('./chai/tree-visualizer-chai-extensions');

const testJson = require("./test-json-creator");
const testObjects = require("./test-object-creator.js");
const testTree = testObjects.tree;
const visualizationStyles = testObjects.visualizationStyles;
const calculateTextWidth = testObjects.calculateTextWidth;
const appContext = require('./main-files').get('app-context').newInstance({visualizationStyles, calculateTextWidth});
const jsonToRoot = appContext.getJsonToRoot();

describe("Node", () => {
  it("knows if it is root", () => {
    const tree = testObjects.testTree1();
    expect(tree.root.isRoot()).to.equal(true);
    expect(tree.getNode("com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is currently leaf", () => {
    const root = testObjects.testTree1().root;
    expect(root.isCurrentlyLeaf()).to.equal(false);
    root.changeFold();
    return root.updatePromise.then(() => expect(root.isCurrentlyLeaf()).to.equal(true));
  });

  it("can fold single node", () => {
    const root = testObjects.testTree1().root;
    root.changeFold();
    return root.updatePromise.then(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(["com.tngtech"]));
  });

  it("can unfold single node", () => {
    const root = testObjects.testTree1().root;
    const allNodes1 = testObjects.allNodes(root);
    root.changeFold();
    root.changeFold();
    return root.updatePromise.then(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(allNodes1));
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.test.subtest").changeFold();
    const toFold = tree.getNode("com.tngtech.test");
    const exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    toFold.changeFold();
    toFold.changeFold();
    return tree.root.updatePromise.then(() => expect(toFold.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("Adds CSS to make the mouse a pointer, if there are children to unfold", () => {
    const tree = testJson.package("com.tngtech")
      .add(testJson.clazz("Class1", "abstractclass").build())
      .build();

    const root = jsonToRoot(tree);

    expect(root.getClass()).to.contain(' foldable');
    expect(root.getClass()).not.to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).not.to.contain(' foldable');
  });
});

describe("Tree", () => {
  it("returns correct visible nodes", () => {
    const tree = testObjects.testTree1();
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.class2", "com.tngtech.class3"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    tree.getNode("com.tngtech.main").changeFold();
    return tree.root.updatePromise.then(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can inclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("main", false);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can exclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    const exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can reset filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    tree.getNode("com.tngtech.main").changeFold();
    return tree.root.updatePromise.then(() => {
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1"];
      tree.getNode("com.tngtech.main").changeFold();
      return tree.root.updatePromise.then(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        tree.root.filterByName("", false);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
          "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
          "com.tngtech.test.subtest.subtestclass1"];
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      });
    });
  });

  //FIXME: make test less ugly
  it("can filter, fold, reset filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    tree.getNode("com.tngtech.main").changeFold();
    return tree.root.updatePromise.then(() => {
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      tree.root.filterByName("", false);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
        "com.tngtech.test.subtest.subtestclass1"];
      tree.getNode("com.tngtech.main").changeFold();
      return tree.root.updatePromise.then(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
    });
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    //FIXME: at the end filtering should consider the promise in node by itsef
    tree.getNode("com.tngtech.main").changeFold();
    return tree.root.updatePromise.then(() => {
      tree.root.filterByName("subtest", true);
      let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1"];
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1"];
      tree.getNode("com.tngtech.main").changeFold();
      return tree.root.updatePromise.then(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        tree.root.filterByName("", false);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
          "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
          "com.tngtech.test.subtest.subtestclass1"];
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      });
    });
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main").changeFold()
    return tree.root.updatePromise.then(() => {
      tree.root.filterByName("subtest", true);
      let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1"];
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      tree.root.filterByName("", false);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
        "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
        "com.tngtech.test.subtest.subtestclass1"];
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
        "com.tngtech.test.subtest.subtestclass1"];
      tree.getNode("com.tngtech.main").changeFold()
      return tree.root.updatePromise.then(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      });
    });
  });

  it("can filter by type to hide interfaces", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1", "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter out everything by type except the root node", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, false);
    const exp = ["com.tngtech"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide classes and eliminate packages", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide interfaces and eliminate packages", function () {
    const root = testObjects.testTree3().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can reset the type-filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.filterByType(true, true);
    exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type and then filter by name", function () {
    const root = testObjects.testTree2().root;

    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.filterByName("test", true);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by name and then filter by type", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("test", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.filterByType(false, true);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  //FIXME: more tests, especially for different cases of node filter input
});


// FIXME: Define these constants, that need to match production code, but can't be accessed from tests, in a central spot
const CIRCLE_TEXT_PADDING = 5;
const CIRCLE_PADDING = testObjects.visualizationStyles.getCirclePadding();

// FIXME: These tests should really better communicate what they're actually testing, and what the preconditions are
describe("Layout of nodes", () => {
  it("draws text within node circles", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkText = node => {
      expect(node).to.haveTextWithinCircle(calculateTextWidth, CIRCLE_TEXT_PADDING, 0);
      node.getOriginalChildren().forEach(c => checkText(c));
    };
    checkText(graphWrapper.graph.root);
  });

  it("draws children within parent", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkLayout = node => {
      expect(node).to.haveChildrenWithinCircle(CIRCLE_PADDING);
      node.getOriginalChildren().forEach(c => checkLayout(c));
    };
    checkLayout(graphWrapper.graph.root);
  });

  it("draws children without overlap", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkLayout = node => {
      expect(node.getOriginalChildren()).to.doNotOverlap(CIRCLE_PADDING);
      node.getOriginalChildren().forEach(c => checkLayout(c));
    };
    checkLayout(graphWrapper.graph.root);
  });
});

describe("Dragging nodes", () => {
  it("can be dragged", () => {
    const tree = testTree('root.SomeClass1', 'root.SomeClass2');
    tree._callOnSelfThenEveryDescendant(node => node._view = {
      updatePosition: () => {
      }
    });

    const toDrag = tree.getByName('root.SomeClass1');
    const dx = 1;
    const dy = -3;
    const expected = {x: toDrag.getX() + dx, y: toDrag.getY() + dy};

    toDrag._drag(dx, dy);

    return tree.updatePromise.then(() => expect({
      x: toDrag.getX(),
      y: toDrag.getY()
    }).to.deep.equal(expected));
  });

  const expectedCoordsAfterDrag = (root, toDrag, dx, dy) => {
    const result = new Map(root.getSelfAndDescendants().map(node => [node, node.getAbsoluteCoords()]));
    toDrag.getSelfAndDescendants().forEach(node => {
      const coords = result.get(node);
      coords.x += dx;
      coords.y += dy;
    });
    return result;
  };

  it("drags along its children, if it is dragged", () => {
    const root = testTree(
      'root.SomeClass',
      'root.sub.SubClass1',
      'root.sub.SubClass2');
    root._callOnSelfThenEveryDescendant(node => node._view = {
      updatePosition: () => {
      }
    });

    const toDrag = root.getByName('root.sub');
    const dx = -5, dy = 4;

    const expectedCoordsByNode = expectedCoordsAfterDrag(root, toDrag, dx, dy);

    toDrag._drag(dx, dy);

    return root.updatePromise.then(() => {
      root.getSelfAndDescendants().forEach(node => {
        expect(node.getAbsoluteCoords()).to.deep.equal(expectedCoordsByNode.get(node))
      });
    });
  });

  it("can't be dragged out of its parent", () => {
    const tree = testTree(
      'root.other',
      'root.parent.SomeClass1',
      'root.parent.SomeClass2');
    tree._callOnSelfThenEveryDescendant(node => node._view = {
      updatePosition: () => {
      }
    });

    const toDrag = tree.getByName('root.parent.SomeClass1');
    const parent = toDrag.getParent();
    expect(parent).not.to.be.root();

    const deltaOutsideOfParent = 2 * parent.getRadius() + 1;

    toDrag._drag(deltaOutsideOfParent, 0);
    expect(toDrag).to.be.locatedWithin(parent);

    toDrag._drag(0, deltaOutsideOfParent);
    expect(toDrag).to.be.locatedWithin(parent);
  });

  it("can be dragged anywhere, if parent is root", () => {
    const tree = testTree(
      'root.SomeClass1',
      'root.SomeClass2');
    tree._callOnSelfThenEveryDescendant(node => node._view = {
      updatePosition: () => {
      }
    });

    const toDrag = tree.getByName('root.SomeClass1');
    const parent = toDrag.getParent();
    expect(parent).to.be.root();

    const oldX = toDrag.getX();
    const oldY = toDrag.getY();
    const deltaOutsideOfParent = 2 * parent.getRadius() + 1;

    toDrag._drag(deltaOutsideOfParent, 0);

    return tree.updatePromise.then(() => {
      expect(toDrag.getX()).to.equal(oldX + deltaOutsideOfParent);

      toDrag._drag(0, deltaOutsideOfParent);
      return tree.updatePromise.then(() => {
        expect(toDrag.getY()).to.equal(oldY + deltaOutsideOfParent);
      });
    });
  });
});