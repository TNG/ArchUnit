'use strict';

require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

describe("Visualizer", () => {
  it("visualizes the dependencies correctly", () => {
    const graphWrapper = testObjects.testGraph2();
    graphWrapper.graph.dependencies.updateVisualData();
    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});

describe("Visual data of dependency", () => {
  it("calc their end positions correctly", () => {
    const graph = testObjects.testGraph2().graph;
    graph.dependencies.updateVisualData();
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("calc their end positions correctly if having overlapping nodes and mutual dependencies", () => {
    const graph = testObjects.testGraphWithOverlappingNodesAndMutualDependencies().graph;
    graph.dependencies.updateVisualData();
    expect(graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node is dragged", () => {
    const graphWrapper = testObjects.testGraph2();
    graphWrapper.graph.root.callOnSelfThenEveryDescendant(node => node._view = {
      updatePosition: () => {
      }
    });
    graphWrapper.graph.dependencies.updateVisualData();

    const toChange = "com.tngtech.test.testclass1";
    const node = graphWrapper.getNode(toChange);
    node.drag(10, -20);
    graphWrapper.graph.dependencies.updateVisualDataOfDependenciesOfNode(node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });

  it("refreshes its end positions correctly if a node changes its radius on folding", () => {
    const graphWrapper = testObjects.testGraph2();
    graphWrapper.graph.dependencies.updateVisualData();

    const toChange = "com.tngtech.main";
    const node = graphWrapper.getNode(toChange);
    node.changeFold();
    graphWrapper.graph.root.relayout();
    graphWrapper.graph.getVisibleDependencies().forEach(d => d.updateVisualData());

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});