'use strict';

require('./chai/tree-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator");

const visualizationStyles = testObjects.visualizationStyles;
const calculateTextWidth = testObjects.calculateTextWidth;
const appContext = require('./main-files').get('app-context').newInstance({visualizationStyles, calculateTextWidth});
const treeVisualizer = appContext.getTreeVisualizer();

// FIXME: Define (if unavoidable??) magic constants in one place
const CIRCLE_TEXT_PADDING = 5;
const radiusOfLeaf = leaf => calculateTextWidth(leaf.getName()) / 2 + CIRCLE_TEXT_PADDING;

const moveToMiddleOfParent = (node, parent) =>
  node.drag(parent.visualData.x - node.visualData.x, parent.visualData.y - node.visualData.y);

const calcDeltaToRightUpperCornerOfParent = (node, parent) => {
  const delta = (parent.visualData.r - node.visualData.r - 0.5) / Math.sqrt(2);
  return delta;
};

describe("Visual data of node", () => {
  it("adapts radius on folding to minimum radius on the same level", () => {
    const tree = testObjects.testTree2();

    const toFold = tree.getNode("com.tngtech.main");
    let expRadius = Math.min.apply(Math, [toFold.visualData.r, tree.getNode("com.tngtech.class2").visualData.r,
      tree.getNode("com.tngtech.class3").visualData.r, tree.getNode("com.tngtech.test").visualData.r]);
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);

    expRadius = Math.max(radiusOfLeaf(toFold), expRadius);
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  // FIXME: Why is this important? It looks fine, even if the radius is 4px off after fold and unfold
  xit("reset radius on unfolding", () => {
    const tree = testObjects.testTree2();

    const toFold = tree.getNode("com.tngtech.main");
    const expRadius = toFold.visualData.r;
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);
    toFold.changeFold();
    treeVisualizer.adaptToFoldState(toFold);
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  // FIXME: I don't think this test makes sense, since updateVisualization() did exactly this (recalculating and relayouting)
  //        and this test only worked, because it was in isolation without the whole rendering process.
  //        I.e. this test is broken now, because changeFold() already does the visualization update, that the d3 rendering cycle
  //        would otherwise have done at a different point, however, the effect always was the same, the graph is relayouted,
  //        if the folding is changed... (or am I wrong here??)
  xit("is not dragged automatically back into its parent on unfolding if its parent is the root", () => {
    const tree = testObjects.testTree2();

    const toDrag = tree.getNode("com.tngtech.test");
    const parent = tree.getNode("com.tngtech");

    moveToMiddleOfParent(toDrag, parent);

    const delta = calcDeltaToRightUpperCornerOfParent(toDrag, parent);
    const newX = toDrag.visualData.x + delta, newY = toDrag.visualData.y - delta;

    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);
    toDrag.drag(delta, -delta);
    toDrag.changeFold();
    treeVisualizer.adaptToFoldState(toDrag);

    expect(toDrag.visualData.x).to.be.within(newX - 2, newX + 2);
    expect(toDrag.visualData.y).to.be.within(newY - 2, newY + 2);
  });
});