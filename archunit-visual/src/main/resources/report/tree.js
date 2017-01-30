'use strict';

let ProjectData = class {
  constructor(name, fullname, type) {
    this.name = name;
    this.fullname = fullname;
    this.type = type;
  }
};

let spaceFromPointToNodeBorder = (x, y, nodeVisualData) => {
  let diff = Math.sqrt(Math.pow(y - nodeVisualData.y, 2) + Math.pow(x - nodeVisualData.x, 2));
  return nodeVisualData.r - diff;
};

let VisualData = class {
  constructor(x, y, r) {
    this.x = x;
    this.y = y;
    this.origR = r;
    this.r = this.origR;
    //this.dragPro = new DragProtocol(this.x, this.y);
  }

  move(dx, dy, parent, callback, addToProtocol) {
    let newX = this.x + dx;
    let newY = this.y + dy;
    if (parent.isFolded || spaceFromPointToNodeBorder(newX, newY, parent.visualData) >= this.r) {
      this.x = newX;
      this.y = newY;
      //if (addToProtocol) this.dragPro.drag(this.x, this.y);
      callback();
    }
  }
};

/**
 * Node functions
 */

let recdescendants = (res, node) => {
  res.push(node);
  node.currentChildren.forEach(d => recdescendants(res, d));
};

let descendants = node => {
  let res = [];
  recdescendants(res, node);
  return res;
};

let isLeaf = d => d.origChildren.length === 0;

let isOnlyChild = d => d.parent && d.parent.origChildren.length === 1;

/**
 * corrects the position of the given node if it is not within its parent
 * @param d
 */
let checkAndCorrectPosition = d => {
  let space = spaceFromPointToNodeBorder(d.visualData.x, d.visualData.y, d.parent.visualData);
  if (space < d.visualData.r) {
    let dr = d.visualData.r - space + 0.5;
    let alpha = Math.atan2(d.visualData.y - d.parent.visualData.y, d.visualData.x - d.parent.visualData.x);
    let dy = Math.abs(Math.sin(alpha) * dr);
    let dx = Math.abs(Math.cos(alpha) * dr);
    dy = Math.sign(d.parent.visualData.y - d.visualData.y) * dy;
    dx = Math.sign(d.parent.visualData.x - d.visualData.x) * dx;
    d.drag(dx, dy);
  }
};

/**
 * gets the minimum radius of all nodes on the same level as the given node
 * @param d
 * @returns {*|formatRounded|number}
 */
let getFoldedR = d => {
  let foldedR = d.visualData.r;
  if (!d.isRoot()) {
    d.parent.currentChildren.forEach(e => foldedR = e.visualData.r < foldedR ? e.visualData.r : foldedR);
  }
  return foldedR;
};

let setIsFolded = (d, isFolded) => {
  d.isFolded = isFolded;
  if (d.isFolded) {
    d.visualData.r = getFoldedR(d);
    d.currentChildren = [];
  }
  else {
    d.visualData.r = d.visualData.origR;
    d.currentChildren = d.origChildren;
    if (!d.isRoot()) {
      checkAndCorrectPosition(d);
    }
  }
};

let fold = (d, folded) => {
  if (!isLeaf(d)) {
    setIsFolded(d, folded);
    //d.deps.changeFold(d.projectData.fullname, d.isFolded);
  }
};

let Node = class {

  constructor(projectData, parent, deps) {
    this.projectData = projectData;
    this.parent = parent;
    this.origChildren = [];
    this.currentChildren = this.origChildren;
    this.isFolded = false;
    this.deps = deps;
  }

  initVisual(x, y, r) {
    this.visualData = new VisualData(x, y, isOnlyChild(this) ? r / 2 : r);
  }

  /**
   * moves this node by the given values
   * @param dx
   * @param dy
   * @param force defines whether the node should be moved even if the new position is invalid
   */
  drag(dx, dy) {
    this.visualData.move(dx, dy, this.parent, () => this.origChildren.forEach(d => d.drag(dx, dy)), true);
    //this.deps.recalcEndCoordinatesOf(this.projectData.fullname);
  }

  isRoot() {
    return !this.parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this.isFolded;
  }

  isChildOf(d) {
    return descendants(d).indexOf(this) !== -1;
  }

  changeFold() {
    fold(this, !this.isFolded);
  }

  getClass() {
    return this.projectData.type;
  }

  getVisibleDescendants() {
    let res = [];
    recdescendants(res, this);
    return res;
  }

  traverseTree() {
    if (this.isCurrentlyLeaf()) return this.projectData.name;
    let subTree = this.currentChildren.reduce((sub, act) => sub + act.traverseTree() + ", ", "");
    return this.projectData.name + "(" + subTree + ")";
  }

  foldAllExceptRoot() {
    if (!isLeaf(this)) {
      this.currentChildren.forEach(d => d.foldAllExceptRoot());
      if (!this.isRoot()) fold(this, true);
    }
  }

  /**
   * identifies the given node
   * @param d
   */
  keyFunction() {
    return d => d.projectData.fullname;
  }

  initNodeMap() {
    this.nodeMap = new Map();
    descendants(this).forEach(d => this.nodeMap.set(d.projectData.fullname, d));
    //this.deps.setNodeMap(this.nodeMap);
  }
};

let addChild = (d, child) => {
  d.origChildren.push(child);
  d.currentChildren = d.origChildren;
};

let jsonToProjectData = jsonEl => {
  return new ProjectData(jsonEl.name, jsonEl.fullname, jsonEl.type);
};

let jsonToNode = (parent, jsonNode, dependencies) => {
  let node = new Node(jsonToProjectData(jsonNode), parent, dependencies);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => addChild(node, jsonToNode(node, c, dependencies)));
  }
  return node;
};

let jsonToRoot = (jsonRoot, dependencies) => {
  let root = jsonToNode(null, jsonRoot, dependencies);
  root.initNodeMap();
  return root;
};

module.exports.jsonToRoot = jsonToRoot;