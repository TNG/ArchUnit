'use strict';

const NODE_TEXT_STYLE_SELECTOR = '.node text';
const CIRCLE_STYLE_SELECTOR = '.circle';

const LINE_STYLE_PREFIX = 'line.';

const DEPENDENCY_TEXT_STYLE_SELECTOR = "text.access";

const rgbToHex = (rgbString, defaultHex) => {
  if (!rgbString) {
    return defaultHex;
  }
  let numbers = rgbString.split(",");
  numbers = numbers.map(n => parseInt(n.replace(/[rgb()\s]/g, "")));
  const numbersAsHex = numbers.map(n => {
    const hex = n.toString(16);
    return hex.length === 1 ? "0" + hex : hex;
  });
  return numbersAsHex.reduce((acc, n) => acc + n, "#");
};

module.exports.from = function (styleSheet) {
  const unique = (elements) => {
    if (elements.length === 0) {
      return null;
    }
    if (elements.length !== 1) {
      throw new Error('Expecting exactly one element in ' + elements);
    }
    return elements[0];
  };
  const cssRules = Array.from(styleSheet.cssRules);
  const nodeTextRule = unique(cssRules.filter(rule => rule.selectorText === NODE_TEXT_STYLE_SELECTOR));
  const circleRule = unique(cssRules.filter(rule => rule.selectorText === CIRCLE_STYLE_SELECTOR));
  const dependencyTextRule = unique(cssRules.filter(rule => rule.selectorText === DEPENDENCY_TEXT_STYLE_SELECTOR));

  return {
    getNodeFontSize: () => parseInt(nodeTextRule.style.getPropertyValue('font-size'), 10),

    setNodeFontSize: (sizeInPixels) => {
      nodeTextRule.style.setProperty('font-size', `${sizeInPixels}px`);
    },

    getCirclePadding: () => parseInt(circleRule.style.getPropertyValue('padding'), 10),

    setCirclePadding: (paddingInPixels) => {
      circleRule.style.setProperty('padding', `${paddingInPixels}px`)
    },

    getDependencyTitleFontSize: () => parseInt(dependencyTextRule.style.getPropertyValue('font-size'), 10),

    getLineStyle: (kind, title) => {
      const rule = unique(cssRules.filter(rule => rule.selectorText === LINE_STYLE_PREFIX + kind));
      return {
        title: title,
        styles: [{
          name: "stroke",
          value: rgbToHex(rule.style.getPropertyValue('stroke'), "#000000")
        }, {
          name: "stroke-dasharray",
          value: rule.style.getPropertyValue('stroke-dasharray')
        }]
      };
    }
  };
};