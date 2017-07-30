'use strict';

const createVisualizationStylesStub = () => {
  let circlePadding = 1;
  return {
    getCirclePadding: () => circlePadding,
    setCirclePadding: (padding) => circlePadding = padding
  };
};
module.exports.visualizationStylesStub = createVisualizationStylesStub;

module.exports.guiElementsStub = () => {
  const visualizationStylesStub = createVisualizationStylesStub();
  let textWidthCalculator = text => text.length;
  return {
    './text-width-calculator': (text) => textWidthCalculator(text),
    './visualization-styles': {
      fromEmbeddedStyleSheet: () => visualizationStylesStub
    },
    setCirclePadding: (padding) => visualizationStylesStub.setCirclePadding(padding),
    setCalculateTextWidth: calculator => textWidthCalculator = calculator
  };
};