'use strict';

require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const TEXT_WIDTH = n => n.length * 6;
const CIRCLE_TEXT_PADDING = 5;
const RELATIVE_TEXT_POSITION = 0.8;
const CIRCLE_PADDING = 10;

const packSiblings = require("../../../main/app/report/lib/d3.js").packSiblings;
const packEnclose = require("../../../main/app/report/lib/d3.js").packEnclose;

const visualizer = require("../../../main/app/report/graph-visualizer.js").visualizer;
visualizer.setStyles(TEXT_WIDTH, CIRCLE_TEXT_PADDING, RELATIVE_TEXT_POSITION, CIRCLE_PADDING, packSiblings, packEnclose);

describe("Visual data of dependency", () => {

  it("calc their end positions correctly", () => {
    let graph = testObjects.testGraph2().graph;
    visualizer.visualizeGraph(graph);
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("calc their end positions correctly if having overlapping nodes and mutual dependencies", () => {
    let graph = testObjects.testGraphWithOverlappingNodesAndMutualDependencies().graph;
    visualizer.visualizeGraph(graph);
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node is dragged", () => {
    let graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);

    let toChange = "com.tngtech.test.testclass1";
    let node = graphWrapper.getNode(toChange);
    visualizer.drag(graphWrapper.graph, node, 10, -20, true);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node changes its radius on folding", () => {
    let graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);

    let toChange = "com.tngtech.main";
    let node = graphWrapper.getNode(toChange);
    graphWrapper.graph.changeFoldStateOfNode(node);
    visualizer.updateOnFolding(graphWrapper.graph, node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});