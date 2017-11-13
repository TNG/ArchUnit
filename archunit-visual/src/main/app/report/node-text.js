'use strict';

//FIXME: create test
const init = (visualizationStyles, calculateTextWidth) => {

  const computeTextShift = (radius, width) => {
    const yCoordTopBorder = -1 * Math.sqrt(Math.pow(radius, 2) - Math.pow(width / 2, 2));
    const fontSize = visualizationStyles.getNodeFontSize();
    return yCoordTopBorder + fontSize
  };

  return class {
    constructor(node) {
      this._node = node;
    }

    getY() {
      if (this._node.isRoot()) {
        const fontSize = visualizationStyles.getNodeFontSize();
        return (-1 * this._node.getRadius()) + fontSize;
      }
      else if (this._node.isCurrentlyLeaf()) {
        return 0;
      }
      else {
        const r = this._node.getRadius();
        return computeTextShift(r, calculateTextWidth(this._node.getName(), this._node.getClass()));
      }
    }
  };
};

module.exports.init = init;