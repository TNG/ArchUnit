'use strict';

const guiElements = require('./gui-elements');

module.exports = (text, cssClassOfText) => {
  const textSvg = guiElements.textSizeComputationSvg();
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