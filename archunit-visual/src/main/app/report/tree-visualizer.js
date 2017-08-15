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

  const recVisualizeTree = (node) => {
    if (node.isCurrentlyLeaf()) {
      node.visualData.update(0, 0, radiusOfAnyNode(node));
    }
    else {
      node.getCurrentChildren().forEach(c => recVisualizeTree(c));

      if (node.getCurrentChildren().length === 1) {
        const onlyChild = node.getCurrentChildren()[0];
        node.visualData.update(onlyChild.getX(), onlyChild.getY(), 3 * onlyChild.getRadius());
      } else {
        const childCircles = node.getCurrentChildren().map(c => c.visualData);
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        node.visualData.update(circle.x, circle.y, circle.r);
      }
    }
  };

  const visualizeTree = (root) => {
    recVisualizeTree(root);

    root.visualData.update(root.getRadius(), root.getRadius());
    root.getDescendants().forEach(d => d.visualData.update(d.getParent().getX() + d.getX(), d.getParent().getY() + d.getY()));
  };

  return {
    visualizeTree: visualizeTree
  }
};

module.exports.newInstance = newInstance;