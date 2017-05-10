'use strict';

require('./chai/dependency-visualdata-chai-extensions');
const expect = require("chai").expect;

const testTrees = require("./test-tree-creator.js");

const TEXT_WIDTH = n => n.length * 6;
const CIRCLE_TEXT_PADDING = 5;
const RELATIVE_TEXT_POSITION = 0.8;

const CIRCLE_PADDING = 10;
const packSiblings = require("../../../main/app/report/lib/d3.js").packSiblings;
const packEnclose = require("../../../main/app/report/lib/d3.js").packEnclose;

require("../../../main/app/report/tree-visualdata.js").setStyles(TEXT_WIDTH, CIRCLE_TEXT_PADDING, RELATIVE_TEXT_POSITION);
const visualizeTree = require("../../../main/app/report/tree-visualdata.js").visualizeTree;
const refreshVisualDataOf = require("../../../main/app/report/dependency-visualdata.js").refreshVisualDataOf;
const refreshVisualDataOfDependencies = require("../../../main/app/report/dependency-visualdata.js").refreshVisualDataOfDependencies;

describe("Visual data of dependency", () => {

  it("calc their end positions correctly", () => {
    let root = testTrees.testTreeWithDependencies2().root;
    visualizeTree(root, packSiblings, packEnclose, CIRCLE_PADDING, RELATIVE_TEXT_POSITION);
    refreshVisualDataOfDependencies(root.getVisibleEdges());
    expect(root.getVisibleEdges()).to.haveCorrectEndPositions();
  });

  it("calc their end positions correctly if having overlapping nodes and mutual dependencies", () => {
    let root = testTrees.testTreeWithOverlappingNodesAndMutualDependencies().root;
    visualizeTree(root, packSiblings, packEnclose, CIRCLE_PADDING, RELATIVE_TEXT_POSITION);
    refreshVisualDataOfDependencies(root.getVisibleEdges());
    expect(root.getVisibleEdges()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node changes its position", () => {
    let root = testTrees.testTreeWithDependencies2().root;
    visualizeTree(root, packSiblings, packEnclose, CIRCLE_PADDING, RELATIVE_TEXT_POSITION);
    refreshVisualDataOfDependencies(root.getVisibleEdges());

    let toChange = "com.tngtech.test.testclass1";
    let node = testTrees.getNode(root, toChange);
    node.visualData.x += 10;
    node.visualData.y -= 20;

    refreshVisualDataOf(toChange, root.getVisibleEdges());

    expect(root.getVisibleEdges()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node changes its radius", () => {
    let root = testTrees.testTreeWithDependencies2().root;
    visualizeTree(root, packSiblings, packEnclose, CIRCLE_PADDING, RELATIVE_TEXT_POSITION);
    refreshVisualDataOfDependencies(root.getVisibleEdges());

    let toChange = "com.tngtech.test.testclass1";
    testTrees.getNode(root, toChange).visualData.r -= 20;

    refreshVisualDataOf(toChange, root.getVisibleEdges());

    expect(root.getVisibleEdges()).to.haveCorrectEndPositions();
  });

  it("refreshes end positions of all dependencies correctly ", () => {
    let root = testTrees.testTreeWithDependencies2().root;
    visualizeTree(root, packSiblings, packEnclose, CIRCLE_PADDING, RELATIVE_TEXT_POSITION);
    refreshVisualDataOfDependencies(root.getVisibleEdges());

    root.getVisibleDescendants().forEach(n => {
      n.visualData.x += 20;
      n.visualData.y += 10;
      n.visualData.r -= 10;
    });

    refreshVisualDataOfDependencies(root.getVisibleEdges());
    expect(root.getVisibleEdges()).to.haveCorrectEndPositions();
  });
});