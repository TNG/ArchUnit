'use strict';

const {getTextSizeComputationSvg} = require('./gui-elements');

const calculateTextWidth = (text, cssClassOfText) => {
  const textSvg = getTextSizeComputationSvg();
  const textElement = textSvg.select('text');
  if (cssClassOfText) {
    textElement.attr('class', cssClassOfText)
  }
  textElement.text(text);
  const width = textElement.node().getComputedTextLength();
  if (cssClassOfText) {
    textElement.classed(cssClassOfText, false)
  }
  return width;
};

module.exports = calculateTextWidth;