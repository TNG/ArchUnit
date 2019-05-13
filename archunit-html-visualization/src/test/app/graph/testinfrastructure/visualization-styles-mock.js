'use strict';

const defaultCirclePadding = 5;
const defaultNodeFontSize = 10;

let _circlePadding = defaultCirclePadding;
let _nodeFontSize = defaultNodeFontSize;

const createVisualizationStylesMock = () => {
  return {
    getCirclePadding: () => _circlePadding,
    setCirclePadding: padding => _circlePadding = padding,
    resetCirclePadding: () => _circlePadding = defaultCirclePadding,
    getNodeFontSize: () => _nodeFontSize,
    setNodeFontSize: fontSize => _nodeFontSize = fontSize,
    resetNodeFontSize: () => _nodeFontSize = defaultNodeFontSize,
    getDependencyTitleFontSize: () => 10
  }
};

module.exports.createVisualizationStylesMock = createVisualizationStylesMock;