'use strict';

import {InnerNode} from "./node";
import {VisualizationStyles} from "../visualization-styles";

class NodeText {
  private _node: InnerNode
  private _visualizationStyles: VisualizationStyles;

  constructor(node: InnerNode, visualizationStyles: VisualizationStyles) {
    this._node = node;
    this._visualizationStyles = visualizationStyles;
  }

  getY(): number {
    if (this._node.isCurrentlyLeaf()) {
      return 0;
    }
    else {
      const r = this._node.getRadius();
      return this._computeTextShift(r, this._node.getNameWidth());
    }
  }

  _computeTextShift(radius: number, width: number): number {
    const yCoordTopBorder = -1 * Math.sqrt(Math.pow(radius, 2) - Math.pow(width / 2, 2));
    const fontSize = this._visualizationStyles.getNodeFontSize();
    return yCoordTopBorder + fontSize;
  }
}

interface NodeTextFactory {
  getNodeText: (node: InnerNode) => NodeText
}

const init = (visualizationStyles: VisualizationStyles): NodeTextFactory => ({
  getNodeText: (node: InnerNode): NodeText => new NodeText(node, visualizationStyles)
});

export {NodeText, init};
