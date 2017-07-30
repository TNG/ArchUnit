'use strict';

/*
 * padding between the text in a circle and the rim of the circle
 */
const CIRCLE_TEXT_PADDING = 5;
/*
 * defines after which proportion of the circle the text is positioned; only affects nodes
 */
const RELATIVE_TEXT_POSITION = 0.8;

const packSiblings = require('d3').packSiblings;
const packEnclose = require('d3').packEnclose;

const calculateTextWidth = require('./text-width-calculator');
const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();

const isOriginalLeaf = node => node.getOriginalChildren().length === 0;

const spaceFromPointToNodeBorder = (x, y, nodeVisualData) => {
  const spaceBetweenPoints = Math.sqrt(Math.pow(y - nodeVisualData.y, 2) + Math.pow(x - nodeVisualData.x, 2));
  return nodeVisualData.r - spaceBetweenPoints;
};

const getFoldedRadius = node => {
  let foldedRadius = node.visualData.r;
  if (!node.isRoot()) {
    node.getParent().getOriginalChildren().forEach(e => foldedRadius = e.visualData.r < foldedRadius ? e.visualData.r : foldedRadius);
  }
  const width = radiusOfLeafWithTitle(node.getName());
  return Math.max(foldedRadius, width);
};

const dragNodeBackIntoItsParent = node => {
  const space = spaceFromPointToNodeBorder(node.visualData.x, node.visualData.y, node.getParent().visualData);
  if (space < node.visualData.r) {
    const dr = node.visualData.r - space;
    const alpha = Math.atan2(node.visualData.y - node.getParent().visualData.y, node.visualData.x - node.getParent().visualData.x);
    let dy = Math.abs(Math.sin(alpha) * dr);
    let dx = Math.abs(Math.cos(alpha) * dr);
    dy = Math.sign(node.getParent().visualData.y - node.visualData.y) * dy;
    dx = Math.sign(node.getParent().visualData.x - node.visualData.x) * dx;
    dragNode(node, dx, dy, true);
  }
};

const dragNode = (node, dx, dy, force) => {
  node.visualData.move(dx, dy, node.getParent(), () => node.getOriginalChildren().forEach(d => dragNode(d, dx, dy, true)), true, force);
};

const adaptToFoldState = (node) => {
  if (node.isFolded()) {
    node.visualData.r = getFoldedRadius(node);
  }
  else {
    node.visualData.r = node.visualData.originalRadius;
    if (!node.isRoot() && !node.getParent().isRoot()) {
      dragNodeBackIntoItsParent(node);
    }
  }
};

const VisualData = class {
  constructor(x, y, r, oldVisualData) {
    this.x = x;
    this.y = y;
    this.originalRadius = r;
    this.r = this.originalRadius;
    this.visible = oldVisualData ? oldVisualData.visible : false;
  }

  move(dx, dy, parent, callback, addToProtocol, force) {
    const newX = this.x + dx;
    const newY = this.y + dy;
    const space = spaceFromPointToNodeBorder(newX, newY, parent.visualData);
    if (force || parent.isRoot() || parent.isFolded() || space >= this.r) {
      this.x = newX;
      this.y = newY;
      callback();
    }
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
    createVisualData(node, 0, 0, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION));
  }
  else {
    node.getCurrentChildren().forEach(c => recVisualizeTree(c));

    const visualDataOfChildren = node.getCurrentChildren().map(c => c.visualData);
    visualDataOfChildren.forEach(c => c.r += visualizationStyles.getCirclePadding() / 2);
    packSiblings(visualDataOfChildren);
    const circle = packEnclose(visualDataOfChildren);
    visualDataOfChildren.forEach(c => c.r -= visualizationStyles.getCirclePadding() / 2);
    const childRadius = visualDataOfChildren.length === 1 ? visualDataOfChildren[0].r : 0;
    createVisualData(node, circle.x, circle.y, Math.max(circle.r, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION), childRadius / RELATIVE_TEXT_POSITION));
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

const createVisualData = (node, x, y, r) => {
  node.visualData = new VisualData(x, y, r, node.visualData);
};

module.exports.newInstance = () => {
  return {
    visualizeTree: visualizeTree,
    dragNode: dragNode,
    adaptToFoldState: adaptToFoldState
  }
};