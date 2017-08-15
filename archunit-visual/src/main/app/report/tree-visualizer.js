'use strict';

const newInstance = (visualizationFunctions, visualizationStyles) => {
  /*
   * padding between the text in a circle and the rim of the circle
   */
  const CIRCLE_TEXT_PADDING = 5;
  const MIN_NODE_RADIUS = 40;

  const packCirclesAndReturnEnclosingCircle = visualizationFunctions.packCirclesAndReturnEnclosingCircle;
  const calculateTextWidth = visualizationFunctions.calculateTextWidth;

  const isOriginalLeaf = node => node.getOriginalChildren().length === 0;

  const radiusOfLeafWithTitle = title => {
    return calculateTextWidth(title) / 2 + CIRCLE_TEXT_PADDING;
  };

  const radiusOfAnyNode = node => {
    const radius = radiusOfLeafWithTitle(node.getName());
    return isOriginalLeaf(node) ? radius : Math.max(radius, MIN_NODE_RADIUS);
  };

  const visualizeTree = (root) => {
    root.visualData.update(root.getRadius(), root.getRadius());
    root.getDescendants().forEach(d => d.visualData.update(d.getParent().getX() + d.getX(), d.getParent().getY() + d.getY()));
  };

  return {visualizeTree, radiusOfAnyNode, packCirclesAndReturnEnclosingCircle, visualizationStyles}
};

module.exports.newInstance = newInstance;