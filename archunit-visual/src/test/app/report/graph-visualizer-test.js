'use strict';

require('./chai/tree-visualizer-chai-extensions');
require('./chai/dependencies-visualizer-chai-extensions');
const expect = require("chai").expect;

const testObjects = require("./test-object-creator.js");

const calculateTextWidth = n => n.length * 6;

// FIXME: Define these constants, that need to match production code, but can't be accessed from tests, in a central spot
const CIRCLE_TEXT_PADDING = 5;
// FIXME: Why can I set this to 0 and the test still passes??? --> because there is still no test that tests if a the text is at the correct place within the circle
const RELATIVE_TEXT_POSITION = 0.8;

const CIRCLE_PADDING = 10;

const guiElementsStub = require('./stubs').guiElementsStub();
guiElementsStub.setCirclePadding(CIRCLE_PADDING);
guiElementsStub.setCalculateTextWidth(calculateTextWidth);
const treeVisualizerFactory = require('./main-files').getRewired('tree-visualizer', guiElementsStub);
const visualizer = require('./main-files').getRewired('graph-visualizer', {
  './tree-visualizer': {
    newInstance: () => treeVisualizerFactory.newInstance()
  }
}).newInstance();

describe("Visualizer", () => {
  it("visualizes the tree and the dependencies correctly", () => {
    const graphWrapper = testObjects.testGraph2();
    visualizer.visualizeGraph(graphWrapper.graph);
    const checkLayout = node => {
      expect(node).to.haveTextWithinCircle(calculateTextWidth, CIRCLE_TEXT_PADDING, RELATIVE_TEXT_POSITION);
      expect(node).to.haveChildrenWithinCircle(CIRCLE_PADDING);
      expect(node.getOriginalChildren()).to.doNotOverlap(CIRCLE_PADDING);
      node.getOriginalChildren().forEach(c => checkLayout(c));
    };
    checkLayout(graphWrapper.graph.root);
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
    graphWrapper.graph.changeFoldStateOfNode(node);
    visualizer.update(graphWrapper.graph, node);

    expect(graphWrapper.graph.getVisibleDependencies()).to.haveCorrectEndPositions();
  });
});