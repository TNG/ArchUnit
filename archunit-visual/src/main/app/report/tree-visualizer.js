'use strict';

const newInstance = (visualizationFunctions, visualizationStyles) => {
  /*
   * padding between the text in a circle and the rim of the circle
   */
  const CIRCLE_TEXT_PADDING = 5;
  /*
   * defines after which proportion of the circle the text is positioned; only affects nodes
   */
  const RELATIVE_TEXT_POSITION = 0.8;

  const packSiblings = visualizationFunctions.packSiblings;
  const packEnclose = visualizationFunctions.packEnclose;
  const calculateTextWidth = visualizationFunctions.calculateTextWidth;

  const isOriginalLeaf = node => node.getOriginalChildren().length === 0;

  const getFoldedRadius = node => {
    let foldedRadius = node.visualData.r;
    if (!node.isRoot()) {
      node.getParent().getOriginalChildren().forEach(e => foldedRadius = e.visualData.r < foldedRadius ? e.visualData.r : foldedRadius);
    }
    const width = radiusOfLeafWithTitle(node.getName());
    return Math.max(foldedRadius, width);
  };

  const adaptToFoldState = (node) => {
    if (node.isFolded()) {
      node.visualData.r = getFoldedRadius(node);
    }
  };

  const radiusOfLeafWithTitle = title => {
    return calculateTextWidth(title) / 2 + CIRCLE_TEXT_PADDING;
  };

  const radiusOfAnyNode = (node, textPosition) => {
    const radius = radiusOfLeafWithTitle(node.getName());
    if (isOriginalLeaf(node)) {
      return radius;
    }
    else {
      return radius / Math.sqrt(1 - textPosition * textPosition);
    }
  };

  const recVisualizeTree = (node) => {
    if (node.isCurrentlyLeaf()) {
      updateVisualData(node, 0, 0, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION));
    }
    else {
      node.getCurrentChildren().forEach(c => recVisualizeTree(c));

      const visualDataOfChildren = node.getCurrentChildren().map(c => c.visualData);
      visualDataOfChildren.forEach(c => c.r += visualizationStyles.getCirclePadding() / 2);
      packSiblings(visualDataOfChildren);
      const circle = packEnclose(visualDataOfChildren);
      visualDataOfChildren.forEach(c => c.r -= visualizationStyles.getCirclePadding() / 2);
      const childRadius = visualDataOfChildren.length === 1 ? visualDataOfChildren[0].r : 0;
      updateVisualData(node, circle.x, circle.y, Math.max(circle.r, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION), childRadius / RELATIVE_TEXT_POSITION));
      visualDataOfChildren.forEach(c => {
        c.dx = c.x - node.visualData.x;
        c.dy = c.y - node.visualData.y;
      });
    }
  };

  const calcPositionAndSetRadius = node => {
    if (node.isRoot()) {
      node.visualData.x = node.visualData.r;
      node.visualData.y = node.visualData.r;
    }

    if (!node.isCurrentlyLeaf()) {
      node.getCurrentChildren().forEach(c => {
        c.visualData.x = node.visualData.x + c.visualData.dx;
        c.visualData.y = node.visualData.y + c.visualData.dy;
        c.visualData.dx = undefined;
        c.visualData.dy = undefined;
        calcPositionAndSetRadius(c);
      });
    }
  };

  const visualizeTree = (root) => {
    recVisualizeTree(root);
    calcPositionAndSetRadius(root);
  };

  const updateVisualData = (node, x, y, r) => {
    node.visualData.x = x;
    node.visualData.y = y;
    node.visualData.r = r;
  };

  return {
    visualizeTree: visualizeTree,
    adaptToFoldState: adaptToFoldState
  }
};

module.exports.newInstance = newInstance;