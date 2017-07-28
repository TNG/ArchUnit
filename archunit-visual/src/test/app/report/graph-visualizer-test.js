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

// FIXME: Why can I pass an empty config, and still assertions of CIRCLE_PADDING pass?? --> the assertion does not pass, but the test is ignored, I don't no why...
const visualizer = require('./main-files').get('graph-visualizer').newInstance({
  calculateTextWidth: calculateTextWidth,
  visualizationStyles: {
    getCirclePadding: () => CIRCLE_PADDING
  }
});

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