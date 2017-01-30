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
    if (parent.isRoot() || parent.isFolded || spaceFromPointToNodeBorder(newX, newY, parent.visualData) >= this.r) {
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

let recdescendants = (res, node, onlyVisible) => {
  res.push(node);
  let arr = onlyVisible ? node.currentChildren : node.origChildren;
  arr.forEach(d => recdescendants(res, d, onlyVisible));
};

let descendants = (node, onlyVisible) => {
  let res = [];
  recdescendants(res, node, onlyVisible);
  return res;
};

let recpredecessors = (res, node) => {
  if (!node.isRoot()) {
    res.push(node.parent);
    recpredecessors(res, node.parent);
  }
};

let predecessors = node => {
  let res = [];
  recpredecessors(res, node);
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
    d.parent.origChildren.forEach(e => foldedR = e.visualData.r < foldedR ? e.visualData.r : foldedR);
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
    d.currentChildren = d.filteredChildren;
    if (!d.isRoot()) {
      checkAndCorrectPosition(d);
    }
  }
};

let fold = (d, folded) => {
  if (!isLeaf(d)) {
    setIsFolded(d, folded);
    d.deps.changeFold(d.projectData.fullname, d.isFolded);
  }
};

let filterAll = (n, filterFun) => {
  if (!isLeaf(n)) {
    n.filteredChildren = n.origChildren.filter(filterFun);
    n.filteredChildren.forEach(c => filterAll(c, filterFun));
    if (!n.isFolded) {
      n.currentChildren = n.filteredChildren;
    }
  }
};

let resetFilter = n => {
  n.filteredChildren = n.origChildren;
  n.filteredChildren.forEach(c => resetFilter(c));
  if (!n.isFolded) {
    n.currentChildren = n.filteredChildren;
  }
};

let Node = class {

  constructor(projectData, parent) {
    this.projectData = projectData;
    this.parent = parent;
    this.origChildren = [];
    this.filteredChildren = this.origChildren;
    this.currentChildren = this.filteredChildren;
    this.isFolded = false;
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
    this.deps.recalcEndCoordinatesOf(this.projectData.fullname);
  }

  isRoot() {
    return !this.parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this.isFolded;
  }

  isChildOf(d) {
    return descendants(d, true).indexOf(this) !== -1;
  }

  changeFold() {
    fold(this, !this.isFolded);
  }

  getClass() {
    return this.projectData.type;
  }

  getVisibleDescendants() {
    return descendants(this, true);
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

  setDepsForAll(deps) {
    descendants(this, true).forEach(d => d.deps = deps);
  }

  getVisibleEdges() {
    return this.deps.getVisible();
  }

  /**
   * filters the children of this node and invokes this method recursively for all children;
   * the root package is ignored while filtering
   *
   * @param str string that should be filtered by
   * @param filterByFullname true, if the fullname of a package/class should contain the string to be filtered by
   * @param filterClassesAndEliminatePkgs true, if only classes should be filtered and packages without matching classes
   * should be eliminated; if this param is true, filterPackages and filterClasses is ignored
   * @param filterPackages true, if packages should be filtered
   * @param filterClasses true, if classes should be filtered
   * @param inclusive true, if packages resp. classes not matching the filter should be eliminated, otherwise true
   */
  filterAll(str, filterByFullname, filterClassesAndEliminatePkgs, filterPackages, filterClasses, inclusive, matchCase) {
    let filterFun = filterFunction(str, filterByFullname, filterClassesAndEliminatePkgs, filterPackages, filterClasses,
        inclusive, matchCase);
    filterAll(this, filterFun);
    this.deps.filter(filterFun);
  }

  resetFilter() {
    resetFilter(this);
    this.deps.resetFilter();
  }
};

let isElementMatching = (c, str, filterByFullName, inclusive, matchCase) => {
  let toFilter = filterByFullName ? c.projectData.fullname : c.projectData.name;
  let res;
  if (matchCase) {
    res = toFilter.includes(str);
  }
  else {
    res = toFilter.toLowerCase().includes(str.toLowerCase());
  }
  return inclusive ? res : !res;
};

let filterFunction = (str, filterByFullName, filterClassesAndEliminatePkgs, filterPackages, filterClasses,
                      inclusive, matchCase) => {
  return c => {
    if (filterClassesAndEliminatePkgs) {
      if (c.projectData.type === "package") {
        return descendants(c, false).reduce((acc, n) => acc || (n.projectData.type !== "package" &&
        filterFunction(str, filterByFullName, filterClassesAndEliminatePkgs, filterPackages, filterClasses,
            inclusive, matchCase)(n)), false);
      }
      else {
        return isElementMatching(c, str, filterByFullName, inclusive, matchCase);
      }
    }
    else {
      if (!filterPackages && !filterClasses) {
        return true;
      }
      if (filterClasses && !filterPackages && c.projectData.type === "package") {
        return true;
      }
      if (filterPackages && c.projectData.type !== "package") {
        return (filterClasses ? isElementMatching(c, str, filterByFullName, inclusive, matchCase) : true)
            && predecessors(c).reduce((acc, n) => acc && (n.isRoot() || filterFunction(str, filterByFullName,
                filterClassesAndEliminatePkgs, filterPackages, filterClasses, inclusive, matchCase)(n)), true);
      }
      return isElementMatching(c, str, filterByFullName, inclusive, matchCase);
    }
  }
};

let initNodeMap = root => {
  root.nodeMap = new Map();
  descendants(root, true).forEach(d => root.nodeMap.set(d.projectData.fullname, d));
};

let addChild = (d, child) => {
  d.origChildren.push(child);
  d.currentChildren = d.origChildren;
};

let jsonToProjectData = jsonEl => {
  return new ProjectData(jsonEl.name, jsonEl.fullname, jsonEl.type);
};

let jsonToNode = (parent, jsonNode) => {
  let node = new Node(jsonToProjectData(jsonNode), parent);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => addChild(node, jsonToNode(node, c)));
  }
  return node;
};

let jsonToRoot = (jsonRoot) => {
  let root = jsonToNode(null, jsonRoot);
  initNodeMap(root);
  return root;
};

module.exports.jsonToRoot = jsonToRoot;