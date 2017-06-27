'use strict';

const NODE_TEXT_STYLE_SELECTOR = '.node text';
const CIRCLE_STYLE_SELECTOR = '.circle';

const LINE_STYLE_PREFIX = 'line.';
const DEFAULT_KIND = 'access';
const BORDER_PREFIX = 'dashed ';
const BORDER_COLOR = 'black';

const CONSTRUCTOR_CALL_STYLE_SELECTOR = 'line.constructorCall';
const METHOD_CALL_STYLE_SELECTOR = 'line.methodCall';
const FIELD_ACCESS_STYLE_SELECTOR = 'line.fieldAccess';
const EXTENDS_STYLE_SELECTOR = 'line.extends';
const IMPLEMENTS_STYLE_SELECTOR = 'line.implements';
const IMPLEMENTS_ANONYMOUS_STYLE_SELECTOR = 'line.implementsAnonymous';
const INNERCLASS_ACCESS_STYLE_SELECTOR = 'line.childrenAccess';

const rgbToHex = (rgbString, defaultHex) => {
  if (!rgbString) {
    return defaultHex;
  }
  let numbers = rgbString.split(",");
  numbers = numbers.map(n => parseInt(n.replace(/[rgb\(\)\s]/g, "")));
  let numbersAsHex = numbers.map(n => {
    let hex = n.toString(16);
    return hex.length == 1 ? "0" + hex : hex;
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

  return {
    getNodeFontSize: () => parseInt(nodeTextRule.style.getPropertyValue('font-size'), 10),

    setNodeFontSize: (sizeInPixels) => {
      nodeTextRule.style.setProperty('font-size', `${sizeInPixels}px`);
    },

    getCirclePadding: () => parseInt(circleRule.style.getPropertyValue('padding'), 10),

    getLineStyle: (kind, title) => {
      let rule = unique(cssRules.filter(rule => rule.selectorText === LINE_STYLE_PREFIX + kind));
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