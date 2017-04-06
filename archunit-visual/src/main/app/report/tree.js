'use strict';

let textwidth;
let CIRCLETEXTPADDING;

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
    let space = spaceFromPointToNodeBorder(newX, newY, parent.visualData);
    if (parent.isRoot() || parent.isFolded || space >= this.r) {
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

let isLeaf = d => d.filteredChildren.length === 0;

//let isOnlyChild = d => d.parent && d.parent.origChildren.length === 1;

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
  let width = radiusofleaf(d.projectData.name);
  return Math.max(foldedR, width);
};

let setIsFolded = (d, isFolded) => {
  d.isFolded = isFolded;
  if (d.isFolded) {
    d.visualData.r = getFoldedR(d);
    d.currentChildren = [];
  }
  else {
    d.currentChildren = d.filteredChildren;
    changeRadius(d, d.visualData.origR);
  }
};

let fold = (d, folded) => {
  if (!isLeaf(d)) {
    setIsFolded(d, folded);
    d.deps.changeFold(d.projectData.fullname, d.isFolded);
    return true;
  }
  return false;
};

let reapplyFilters = (n, filters) => {
  n.filteredChildren = Array.from(filters.values()).reduce((children, filter) => children.filter(filter), n.origChildren);
  n.filteredChildren.forEach(c => reapplyFilters(c, filters));
  if (!n.isFolded) {
    n.currentChildren = n.filteredChildren;
  }
};

let changeRadius = (n, newR) => {
  n.visualData.r = newR;
  if (!n.isRoot() && !n.parent.isRoot()) {
    checkAndCorrectPosition(n);
  }
  n.deps.recalcEndCoordinatesOf(n.projectData.fullname);
};

let radiusofleaf = name => {
  return textwidth(name) / 2 + CIRCLETEXTPADDING;
};

let radiusofanynode = (n, TEXTPOSITION) => {
  let radius = radiusofleaf(n.projectData.name);
  if (n.isOrigLeaf()) {
    return radius;
  }
  else {
    return radius / Math.sqrt(1 - TEXTPOSITION * TEXTPOSITION);
  }
};

let reclayout = (n, packSiblings, packEnclose, circpadding, textposition) => {
  if (n.isOrigLeaf()) {
    n.initVisual(0, 0, radiusofanynode(n, textposition));
  }
  else {
    n.origChildren.forEach(c => reclayout(c, packSiblings, packEnclose, circpadding, textposition));
    let children = n.origChildren.map(c => c.visualData);
    children.forEach(c => c.r += circpadding / 2);
    packSiblings(children);
    let circ = packEnclose(children);
    children.forEach(c => c.r -= circpadding / 2);
    let childradius = children.length === 1 ? children[0].r : 0;
    n.initVisual(circ.x, circ.y, Math.max(circ.r, radiusofanynode(n, textposition), childradius / textposition));
    children.forEach(c => {
      c.dx = c.x - n.visualData.x;
      c.dy = c.y - n.visualData.y;
    });
  }
};

let calcPosAndSetRadius = n => {
  if (n.isRoot()) {
    n.visualData.x = n.visualData.r;
    n.visualData.y = n.visualData.r;
  }
  if (!n.isOrigLeaf()) {
    n.origChildren.forEach(c => {
      c.visualData.x = n.visualData.x + c.visualData.dx;
      c.visualData.y = n.visualData.y + c.visualData.dy;
      c.visualData.dx = undefined;
      c.visualData.dy = undefined;
      calcPosAndSetRadius(c);
    });
  }

  if (n.isFolded) {
    n.visualData.r = getFoldedR(n);
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
    this.filters = new Map();
  }

  layout(packSiblings, packEnclose, circpadding, textposition) {
    reclayout(this, packSiblings, packEnclose, circpadding, textposition);
    calcPosAndSetRadius(this);
  }

  initVisual(x, y, r) {
    this.visualData = new VisualData(x, y, r);
  }

  /**
   * moves this node by the given values
   * @param dx
   * @param dy
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

  isOrigLeaf() {
    return this.origChildren.length === 0;
    //TODO: filteredChildren, falls nach dem Filtern das Layout neu bestimmt werden soll (sodass zum Beispiel die
    //wenigen übrigen Klassen größer werden
  }

  isChildOf(d) {
    return descendants(d, true).indexOf(this) !== -1;
  }

  changeFold() {
    return fold(this, !this.isFolded);
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
   */
  keyFunction() {
    return d => d.projectData.fullname;
  }

  setDepsForAll(deps) {
    descendants(this, false).forEach(d => d.deps = deps);
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
  filterByName(str, filterByFullname, filterClassesAndEliminatePkgs, filterPackages, filterClasses, inclusive, matchCase) {
    let filter = filterFunction(str, filterByFullname, filterClassesAndEliminatePkgs, filterPackages, filterClasses,
        inclusive, matchCase);
    this.filters.set("namefilter", filter);
    reapplyFilters(this, this.filters);
    this.deps.setNodeFilters(this.filters);
  }

  resetFilterByName() {
    this.filters.delete("namefilter");
    reapplyFilters(this, this.filters);
    this.deps.setNodeFilters(this.filters);
  }

  filterByType(interfaces, classes, eliminatePkgs) {
    let classfilter =
        c => (c.projectData.type !== "package")
        && (c.projectData.type !== "interface" || interfaces)
        && (!c.projectData.type.endsWith("class") || classes);
    let pkgfilter =
        c => (c.projectData.type === "package")
        && (!eliminatePkgs || descendants(c, false).reduce((acc, n) => acc || classfilter(n), false));
    this.filters.set("typefilter", c => classfilter(c) || pkgfilter(c));
    reapplyFilters(this, this.filters);
    this.deps.setNodeFilters(this.filters);
  }

  resetFilterByType() {
    this.filters.delete("typefilter");
    reapplyFilters(this, this.filters);
    this.deps.setNodeFilters(this.filters);
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

let jsonToRoot = (jsonRoot, textwidthfunction, circletextpadding) => {
  textwidth = textwidthfunction;
  CIRCLETEXTPADDING = circletextpadding;
  let root = jsonToNode(null, jsonRoot);
  initNodeMap(root);
  return root;
};

module.exports.jsonToRoot = jsonToRoot;