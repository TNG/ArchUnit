'use strict';

const newInstance = (visualizationFunctions, visualizationStyles) => {
  /*
   * padding between the text in a circle and the rim of the circle
   */
  const CIRCLE_TEXT_PADDING = 5;
  const MIN_NODE_RADIUS = 40;

  const packSiblings = visualizationFunctions.packSiblings;
  const packEnclose = visualizationFunctions.packEnclose;
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

      const visualDataOfChildren = node.getCurrentChildren().map(c => c.visualData);
      visualDataOfChildren.forEach(c => c.r += visualizationStyles.getCirclePadding());
      packSiblings(visualDataOfChildren);
      const circle = packEnclose(visualDataOfChildren);
      visualDataOfChildren.forEach(c => c.r -= visualizationStyles.getCirclePadding());
      const childRadius = visualDataOfChildren.length === 1 ? visualDataOfChildren[0].r : 0;
      const minParentRadiusForOneChild = childRadius * 3;
      node.visualData.update(circle.x, circle.y, Math.max(circle.r, radiusOfAnyNode(node), minParentRadiusForOneChild));
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