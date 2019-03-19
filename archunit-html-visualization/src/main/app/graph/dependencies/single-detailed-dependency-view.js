'use strict';

const View = class {
  constructor(svgText, detailedDependencyDescription, offsetY) {
    this._svgElement = svgText.addTSpan(detailedDependencyDescription);
    this._svgElement.offsetY = offsetY;
  }

  get textWidth() {
    return this._svgElement.textWidth;
  }

  set positionX(x) {
    this._svgElement.positionX = x;
  }

  remove() {
    this._svgElement.detachFromParent();
  }
};

module.exports = View;