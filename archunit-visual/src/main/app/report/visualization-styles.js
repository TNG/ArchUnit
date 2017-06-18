'use strict';

const NODE_TEXT_STYLE_SELECTOR = '.node text';
const CIRCLE_STYLE_SELECTOR = '.circle';

module.exports.from = function (styleSheet) {
  const unique = (elements) => {
    if (elements.length !== 1) {
      throw new Error('Expecting exactly one element in ' + elements);
    }
    return elements[0];
  };
  const cssRules = Array.from(styleSheet.cssRules);
  const nodeTextRule = unique(cssRules.filter(rule => rule.selectorText === NODE_TEXT_STYLE_SELECTOR));
  const circleRule = unique(cssRules.filter(rule => rule.selectorText === CIRCLE_STYLE_SELECTOR));

  return {
    getNodeFontSize: () => {
      return parseInt(nodeTextRule.style.getPropertyValue('font-size'), 10);
    },

    setNodeFontSize: (sizeInPixels) => {
      nodeTextRule.style.setProperty('font-size', `${sizeInPixels}px`);
    },

    getCirclePadding: () => {
      return parseInt(circleRule.style.getPropertyValue('padding'), 10);
    },

    setCirclePadding: (paddingInPixels) => {
      circleRule.style.setProperty('padding', `${paddingInPixels}px`)
    }
  }
};