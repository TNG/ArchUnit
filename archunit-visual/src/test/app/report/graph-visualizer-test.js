'use strict';

require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const calculateTextWidth = n => n.length * 6;

const visualizationStyles = require('./stubs').visualizationStylesStub();
const appContext = require('./main-files').get('app-context').newInstance({visualizationStyles, calculateTextWidth});
const treeVisualizer = appContext.getTreeVisualizer();
const visualizer = require('./main-files').get('graph-visualizer').newInstance(treeVisualizer, require('./main-files').get('dependencies-visualizer'));

describe("Visualizer", () => {
  it("visualizes the dependencies correctly", () => {
    const graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);
    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});

describe("Visual data of dependency", () => {
  it("calc their end positions correctly", () => {
    const graph = testObjects.testGraph2().graph;
    visualizer.visualizeGraph(graph);
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("calc their end positions correctly if having overlapping nodes and mutual dependencies", () => {
    const graph = testObjects.testGraphWithOverlappingNodesAndMutualDependencies().graph;
    visualizer.visualizeGraph(graph);
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node is dragged", () => {
    const graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);

    const toChange = "com.tngtech.test.testclass1";
    const node = graphWrapper.getNode(toChange);
    visualizer.drag(graphWrapper.graph, node, 10, -20, true);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node changes its radius on folding", () => {
    const graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);

    const toChange = "com.tngtech.main";
    const node = graphWrapper.getNode(toChange);
    node.changeFold();
    visualizer.update(graphWrapper.graph, node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});