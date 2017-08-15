'use strict';

const createVisualizationStylesStub = () => {
  let circlePadding = 1;
  let nodeFontSize = 10;
  return {
    getCirclePadding: () => circlePadding,
    setCirclePadding: padding => circlePadding = padding,
    getNodeFontSize: () => nodeFontSize,
    setNodeFontSize: fontSize => nodeFontSize = fontSize
  };
};
module.exports.visualizationStylesStub = createVisualizationStylesStub;