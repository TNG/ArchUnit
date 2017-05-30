'use strict';

require('./chai/tree-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const TEXT_WIDTH = n => n.length * 6;
const CIRCLE_TEXT_PADDING = 5;
const RELATIVE_TEXT_POSITION = 0.8;

const CIRCLE_PADDING = 10;
const packSiblings = require("../../../main/app/report/lib/d3.js").packSiblings;
const packEnclose = require("../../../main/app/report/lib/d3.js").packEnclose;

const treeVisualizer = require("../../../main/app/report/tree-visualizer.js").treeVisualizer;
treeVisualizer.setStyles(TEXT_WIDTH, CIRCLE_TEXT_PADDING, RELATIVE_TEXT_POSITION, CIRCLE_PADDING, packSiblings, packEnclose);

let radiusOfLeaf = leaf => TEXT_WIDTH(leaf.projectData.name) / 2 + CIRCLE_TEXT_PADDING;

let moveToMiddleOfParent = (node, parent) =>
    treeVisualizer.dragNode(node, parent.visualData.x - node.visualData.x, parent.visualData.y - node.visualData.y, false);

let calcDeltaToRightUpperCornerOfParent = (node, parent) => {
  let delta = (parent.visualData.r - node.visualData.r - 0.5) / Math.sqrt(2);
  return delta;
};

describe("Visual data of node", () => {
  it("adapts radius on folding to minimum radius on the same level", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toFold = tree.getNode("com.tngtech.main");
    let expRadius = Math.min.apply(Math, [toFold.visualData.r, tree.getNode("com.tngtech.class2").visualData.r,
      tree.getNode("com.tngtech.class3").visualData.r, tree.getNode("com.tngtech.test").visualData.r]);
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);

    expRadius = Math.max(radiusOfLeaf(toFold), expRadius);
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("reset radius on unfolding", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toFold = tree.getNode("com.tngtech.main");
    let expRadius = toFold.visualData.r;
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("can be dragged", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toDrag = tree.getNode("com.tngtech.class2");
    let dx = 1, dy = -3;
    let newX = toDrag.visualData.x + dx, newY = toDrag.visualData.y + dy;
    treeVisualizer.dragNode(toDrag, dx, dy, false);
    expect(toDrag.visualData.x).to.equal(newX);
    expect(toDrag.visualData.y).to.equal(newY);
  });

  it("drags also its children if it is dragged", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let allNodes2 = testObjects.allNodes(tree.root);
    let toDrag = tree.getNode("com.tngtech.test");
    let dx = 1, dy = -3;
    let exp = new Map();
    allNodes2.forEach(n => {
      let node = tree.getNode(n);
      let xy = [node.visualData.x, node.visualData.y];
      if (node.isChildOf(toDrag)) {
        xy[0] += dx;
        xy[1] += dy;
      }
      exp.set(n, xy);
    });
    treeVisualizer.dragNode(toDrag, dx, dy, false);
    expect(tree.root.getVisibleDescendants()).to.haveExactlyPositions(exp);
  });

  it("cannot be dragged out of its parent", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toDrag = tree.getNode("com.tngtech.test.subtest.subtestclass1");
    let parent = tree.getNode("com.tngtech.test.subtest");
    let dx = toDrag.visualData.x + parent.visualData.r, dy = 5;
    let newX = toDrag.visualData.x, newY = toDrag.visualData.y;
    treeVisualizer.dragNode(toDrag, dx, dy, false);
    expect(toDrag.visualData.x).to.equal(newX);
    expect(toDrag.visualData.y).to.equal(newY);
  });

  it("is dragged automatically back into its parent on unfolding, so that it is completely within its parent", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toDrag = tree.getNode("com.tngtech.test.subtest");
    let parent = tree.getNode("com.tngtech.test");

    let newDelta = (parent.visualData.r - toDrag.visualData.r) / Math.sqrt(2);

    moveToMiddleOfParent(toDrag, parent);

    let newX = toDrag.visualData.x + newDelta, newY = toDrag.visualData.y - newDelta;

    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);
    let delta = calcDeltaToRightUpperCornerOfParent(toDrag, parent);
    treeVisualizer.dragNode(toDrag, delta, -delta, false);
    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);

    expect(toDrag.visualData.x).to.be.within(newX - 2, newX + 2);
    expect(toDrag.visualData.y).to.be.within(newY - 2, newY + 2);
  });

  it("is not dragged automatically back into its parent on unfolding if its parent is the root", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);

    let toDrag = tree.getNode("com.tngtech.test");
    let parent = tree.getNode("com.tngtech");

    moveToMiddleOfParent(toDrag, parent);

    let delta = calcDeltaToRightUpperCornerOfParent(toDrag, parent);
    let newX = toDrag.visualData.x + delta, newY = toDrag.visualData.y - delta;

    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);
    treeVisualizer.dragNode(toDrag, delta, -delta, false);
    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);

    expect(toDrag.visualData.x).to.be.within(newX - 2, newX + 2);
    expect(toDrag.visualData.y).to.be.within(newY - 2, newY + 2);
  });
});

describe("Tree", () => {
  it("does the initial layout correct", () => {
    let tree = testObjects.testTree2();
    treeVisualizer.visualizeTree(tree.root);
    let checkLayout = node => {
      expect(node).to.haveTextWithinCircle(TEXT_WIDTH, CIRCLE_TEXT_PADDING, RELATIVE_TEXT_POSITION);
      expect(node).to.haveChildrenWithinCircle(CIRCLE_PADDING);
      expect(node.origChildren).to.doNotOverlap(CIRCLE_PADDING);
      node.origChildren.forEach(c => checkLayout(c));
    };
    checkLayout(tree.root);
  });
});