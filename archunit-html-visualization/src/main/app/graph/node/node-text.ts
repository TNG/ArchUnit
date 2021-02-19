'use strict';

import {InnerNode, Node} from "./node";
import {VisualizationStyles} from "../visualization-styles";

const init = (visualizationStyles: VisualizationStyles) => {

  const computeTextShift = (radius: number, width: number) => {
    const yCoordTopBorder = -1 * Math.sqrt(Math.pow(radius, 2) - Math.pow(width / 2, 2));
    const fontSize = visualizationStyles.getNodeFontSize();
    return yCoordTopBorder + fontSize;
  };

  return class {
    private _node: InnerNode

    constructor(node: InnerNode) {
      this._node = node;
    }

    getY() {
      if (this._node.isCurrentlyLeaf()) {
        return 0;
      }
      else {
        const r = this._node.getRadius();
        return computeTextShift(r, this._node.getNameWidth());
      }
    }
  };
};

module.exports = {init};
