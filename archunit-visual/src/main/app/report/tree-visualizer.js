'use strict';

let TEXT_WIDTH;
let CIRCLE_TEXT_PADDING;
let RELATIVE_TEXT_POSITION;

let isOrigLeaf = node => node.origChildren.length === 0;
//TODO: filteredChildren, falls nach dem Filtern das Layout neu bestimmt werden soll (sodass zum Beispiel die wenigen übrigen Klassen größer werden

let spaceFromPointToNodeBorder = (x, y, nodeVisualData) => {
  let spaceBetweenPoints = Math.sqrt(Math.pow(y - nodeVisualData.y, 2) + Math.pow(x - nodeVisualData.x, 2));
  return nodeVisualData.r - spaceBetweenPoints;
};

let getFoldedRadius = node => {
  let foldedRadius = node.visualData.r;
  if (!node.isRoot()) {
    node.parent.origChildren.forEach(e => foldedRadius = e.visualData.r < foldedRadius ? e.visualData.r : foldedRadius);
  }
  let width = radiusOfLeafWithTitle(node.projectData.name);
  return Math.max(foldedRadius, width);
};

let dragNodeBackIntoItsParent = node => {
  let space = spaceFromPointToNodeBorder(node.visualData.x, node.visualData.y, node.parent.visualData);
  if (space < node.visualData.r) {
    let dr = node.visualData.r - space;
    let alpha = Math.atan2(node.visualData.y - node.parent.visualData.y, node.visualData.x - node.parent.visualData.x);
    let dy = Math.abs(Math.sin(alpha) * dr);
    let dx = Math.abs(Math.cos(alpha) * dr);
    dy = Math.sign(node.parent.visualData.y - node.visualData.y) * dy;
    dx = Math.sign(node.parent.visualData.x - node.visualData.x) * dx;
    dragNode(node, dx, dy, true);
  }
};

let dragNode = (node, dx, dy, force) => {
  node.visualData.move(dx, dy, node.parent, () => node.origChildren.forEach(d => dragNode(d, dx, dy, true)), true, force);
};

let adaptToFoldState = node => {
  if (node.isFolded) {
    node.visualData.r = getFoldedRadius(node);
  }
  else {
    node.visualData.r = node.visualData.origRadius;
    if (!node.isRoot() && !node.parent.isRoot()) {
      dragNodeBackIntoItsParent(node);
    }
  }
};

let VisualData = class {
  constructor(x, y, r, oldVisualData) {
    this.x = x;
    this.y = y;
    this.origRadius = r;
    this.r = this.origRadius;
    this.visible = oldVisualData ? oldVisualData.visible : false;
    //this.dragPro = new DragProtocol(this.x, this.y);
  }

  move(dx, dy, parent, callback, addToProtocol, force) {
    let newX = this.x + dx;
    let newY = this.y + dy;
    let space = spaceFromPointToNodeBorder(newX, newY, parent.visualData);
    if (force || parent.isRoot() || parent.isFolded || space >= this.r) {
      this.x = newX;
      this.y = newY;
      //if (addToProtocol) this.dragPro.drag(this.x, this.y);
      callback();
    }
  }
};

let radiusOfLeafWithTitle = title => {
  return TEXT_WIDTH(title) / 2 + CIRCLE_TEXT_PADDING;
};

let radiusOfAnyNode = (node, TEXT_POSITION) => {
  let radius = radiusOfLeafWithTitle(node.projectData.name);
  if (isOrigLeaf(node)) {
    return radius;
  }
  else {
    return radius / Math.sqrt(1 - TEXT_POSITION * TEXT_POSITION);
  }
};

let recVisualizeTree = (node, packSiblings, packEnclose, circpadding) => {
  if (isOrigLeaf(node)) {
    createVisualData(node, 0, 0, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION));
  }
  else {
    node.origChildren.forEach(c => recVisualizeTree(c, packSiblings, packEnclose, circpadding, RELATIVE_TEXT_POSITION));
    let visualDataOfChildren = node.origChildren.map(c => c.visualData);
    visualDataOfChildren.forEach(c => c.r += circpadding / 2);
    packSiblings(visualDataOfChildren);
    let circ = packEnclose(visualDataOfChildren);
    visualDataOfChildren.forEach(c => c.r -= circpadding / 2);
    let childradius = visualDataOfChildren.length === 1 ? visualDataOfChildren[0].r : 0;
    createVisualData(node, circ.x, circ.y, Math.max(circ.r, radiusOfAnyNode(node, RELATIVE_TEXT_POSITION), childradius / RELATIVE_TEXT_POSITION));
    visualDataOfChildren.forEach(c => {
      c.dx = c.x - node.visualData.x;
      c.dy = c.y - node.visualData.y;
    });
  }
};

let calcPositionAndSetRadius = node => {
  if (node.isRoot()) {
    node.visualData.x = node.visualData.r;
    node.visualData.y = node.visualData.r;
  }
  if (!isOrigLeaf(node)) {
    node.origChildren.forEach(c => {
      c.visualData.x = node.visualData.x + c.visualData.dx;
      c.visualData.y = node.visualData.y + c.visualData.dy;
      c.visualData.dx = undefined;
      c.visualData.dy = undefined;
      calcPositionAndSetRadius(c);
    });
  }

  if (node.isFolded) {
    node.visualData.r = getFoldedRadius(node);
  }
};

let visualizeTree = (root, packSiblings, packEnclose, circpadding) => {
  recVisualizeTree(root, packSiblings, packEnclose, circpadding);
  calcPositionAndSetRadius(root);
  root.addObserver(adaptToFoldState);
};

let createVisualData = (node, x, y, r) => {
  node.visualData = new VisualData(x, y, r, node.visualData);
};

let setStyles = (textWidthFunction, circleTextPadding, relativeTextPosition) => {
  TEXT_WIDTH = textWidthFunction;
  CIRCLE_TEXT_PADDING = circleTextPadding;
  RELATIVE_TEXT_POSITION = relativeTextPosition;
};

module.exports.setStyles = setStyles;
module.exports.visualizeTree = visualizeTree;
module.exports.dragNode = dragNode;