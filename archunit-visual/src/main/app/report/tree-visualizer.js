'use strict';

let textWidth;
let circleTextPadding;
let relativeTextPosition;
let circlePadding;
let packSiblings;
let packEnclose;

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
  let width = radiusOfLeafWithTitle(node.getName());
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

let adaptToFoldState = (node, root) => {
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
  return textWidth(title) / 2 + circleTextPadding;
};

let radiusOfAnyNode = (node, textPosition) => {
  let radius = radiusOfLeafWithTitle(node.getName());
  if (isOrigLeaf(node)) {
    return radius;
  }
  else {
    return radius / Math.sqrt(1 - textPosition * textPosition);
  }
};

let recVisualizeTree = (node) => {
  //isOrigLeaf(node)
  if (node.isCurrentlyLeaf()) {
    createVisualData(node, 0, 0, radiusOfAnyNode(node, relativeTextPosition));
  }
  else {
    //everywhere currentChildren instead of originally origChildren
    node.currentChildren.forEach(c => recVisualizeTree(c));

    let visualDataOfChildren = node.currentChildren.map(c => c.visualData);
    visualDataOfChildren.forEach(c => c.r += circlePadding / 2);
    packSiblings(visualDataOfChildren);
    let circle = packEnclose(visualDataOfChildren);
    visualDataOfChildren.forEach(c => c.r -= circlePadding / 2);
    let childRadius = visualDataOfChildren.length === 1 ? visualDataOfChildren[0].r : 0;
    createVisualData(node, circle.x, circle.y, Math.max(circle.r, radiusOfAnyNode(node, relativeTextPosition), childRadius / relativeTextPosition));
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

  if (!node.isCurrentlyLeaf()) {
    node.currentChildren.forEach(c => {
      c.visualData.x = node.visualData.x + c.visualData.dx;
      c.visualData.y = node.visualData.y + c.visualData.dy;
      c.visualData.dx = undefined;
      c.visualData.dy = undefined;
      calcPositionAndSetRadius(c);
    });
  }
};

let visualizeTree = (root) => {
  recVisualizeTree(root);
  calcPositionAndSetRadius(root);
  //root.addObserver(adaptToFoldState);
};

let createVisualData = (node, x, y, r) => {
  node.visualData = new VisualData(x, y, r, node.visualData);
};

let setStyles = (text_width_function, circle_text_padding, relative_text_position, circle_padding, pack_siblings, pack_enclose) => {
  textWidth = text_width_function;
  circleTextPadding = circle_text_padding;
  relativeTextPosition = relative_text_position;
  circlePadding = circle_padding;
  packSiblings = pack_siblings;
  packEnclose = pack_enclose;
};

let setCirclePadding = circle_padding => circlePadding = circle_padding;

module.exports.treeVisualizer = {
  setStyles: setStyles,
  setCirclePadding: setCirclePadding,
  visualizeTree: visualizeTree,
  dragNode: dragNode,
  adaptToFoldState: adaptToFoldState
};