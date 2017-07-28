'use strict';

require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const visualizer = require('./main-files').get('graph-visualizer').newInstance({});

// FIXME: This is no test of dependency-visualizer, but graph-visualizer, and it uses the deprecated global test objects pattern
//        Also: The test doesn't say, what its preconditions are, nor, what it really asserts (I'd have to look into haveCorrectEndPositions(),
//        because without that, the test has the same semantics as 'expect(visualizer).to.doItRight()', doesn't really say much...)
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
    graphWrapper.graph.changeFoldStateOfNode(node);
    visualizer.update(graphWrapper.graph, node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});