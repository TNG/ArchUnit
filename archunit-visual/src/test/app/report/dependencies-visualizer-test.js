'use strict';

require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const visualizer = require("../../../main/app/report/graph-visualizer.js").visualizer;

// FIXME: This is no test of dependency-visualizer, but graph-visualizer, and it uses the deprecated global test objects pattern
//        Also: The test doesn't say, what its preconditions are, nor, what it really asserts (I'd have to look into haveCorrectEndPositions(),
//        because without that, the test has the same semantics as 'expect(visualizer).to.doItRight()', doesn't really say much...)
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
    visualizer.update(graphWrapper.graph, node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});