'use strict';

let ProjectData = class {
  constructor(name, fullname, type) {
    this.name = name;
    this.fullname = fullname;
    this.type = type;
  }
};

let VisualData = class {
  constructor(x, y, r) {
    this.x = x;
    this.y = y;
    this.origR = r;
    this.r = this.origR;
    //this.dragPro = new DragProtocol(this.x, this.y);
  }

  move(dx, dy, parentVisualData, callback, addToProtocol) {
    let newX = this.x + dx;
    let newY = this.y + dy;



    this.x += dx;
    this.y += dy;
    //if (addToProtocol) //this.dragPro.drag(this.x, this.y);
    callback();
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

  drag(dx, dy) {
    this.visualData.move(dx, dy, this.parent.visualData, () => this.origChildren.forEach(d => d.drag(dx, dy)), true);
    //this.deps.recalcEndCoordinatesOf(this.projectData.fullname);
  }

  isRoot() {
    return !this.parent;
  }

  isCurrentLeaf() {
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
    if (this.isCurrentLeaf()) return this.projectData.name;
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