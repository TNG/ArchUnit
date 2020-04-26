'use strict';

const defaultCirclePadding = 5;
const defaultNodeFontSize = 10;

let _circlePadding = defaultCirclePadding;
let _nodeFontSize = defaultNodeFontSize;

const createVisualizationStylesMock = () => {
  return {
    getCirclePadding: () => _circlePadding,
    setCirclePadding: padding => _circlePadding = padding,
    getNodeFontSize: () => _nodeFontSize,
    setNodeFontSize: fontSize => _nodeFontSize = fontSize,
    getDependencyTitleFontSize: () => 10,

    // helper methods for tests not present in production
    resetCirclePadding: () => _circlePadding = defaultCirclePadding,
    resetNodeFontSize: () => _nodeFontSize = defaultNodeFontSize,
    getDefaultFontSize: () => defaultNodeFontSize,
  }
};

module.exports.createVisualizationStylesMock = createVisualizationStylesMock;
