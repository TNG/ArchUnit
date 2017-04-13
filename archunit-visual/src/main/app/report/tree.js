'use strict';

let textWidth;
let circleTextPadding;

let ProjectData = class {
  constructor(name, fullname, type) {
    this.name = name;
    this.fullname = fullname;
    this.type = type;
  }
};

let spaceFromPointToNodeBorder = (x, y, nodeVisualData) => {
  let spaceBetweenPoints = Math.sqrt(Math.pow(y - nodeVisualData.y, 2) + Math.pow(x - nodeVisualData.x, 2));
  return nodeVisualData.r - spaceBetweenPoints;
};

let VisualData = class {
  constructor(x, y, r) {
    this.x = x;
    this.y = y;
    this.origRadius = r;
    this.r = this.origRadius;
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

let recdescendants = (res, node, onlyVisible) => {
  res.push(node);
  let arr = onlyVisible ? node.currentChildren : node.origChildren;
  arr.forEach(n => recdescendants(res, n, onlyVisible));
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

let isLeaf = node => node.filteredChildren.length === 0;


let isOrigLeaf = node => node.origChildren.length === 0;
//TODO: filteredChildren, falls nach dem Filtern das Layout neu bestimmt werden soll (sodass zum Beispiel die
// wenigen übrigen Klassen größer werden

let dragNodeBackIntoItsParent = node => {
  let space = spaceFromPointToNodeBorder(node.visualData.x, node.visualData.y, node.parent.visualData);
  if (space < node.visualData.r) {
    let dr = node.visualData.r - space;
    let alpha = Math.atan2(node.visualData.y - node.parent.visualData.y, node.visualData.x - node.parent.visualData.x);
    let dy = Math.abs(Math.sin(alpha) * dr);
    let dx = Math.abs(Math.cos(alpha) * dr);
    dy = Math.sign(node.parent.visualData.y - node.visualData.y) * dy;
    dx = Math.sign(node.parent.visualData.x - node.visualData.x) * dx;
    node.drag(dx, dy, true);
  }
};

let getFoldedRadius = node => {
  let foldedRadius = node.visualData.r;
  if (!node.isRoot()) {
    node.parent.origChildren.forEach(e => foldedRadius = e.visualData.r < foldedRadius ? e.visualData.r : foldedRadius);
  }
  let width = radiusOfLeafWithTitle(node.projectData.name);
  return Math.max(foldedRadius, width);
};

let adaptRadiusAndPositionToFoldState = (node) => {
  if (node.isFolded) {
    node.visualData.r = getFoldedRadius(node);
    node.currentChildren = [];
  }
  else {
    node.currentChildren = node.filteredChildren;
    node.visualData.r = node.visualData.origRadius;
    if (!node.isRoot() && !node.parent.isRoot()) {
      dragNodeBackIntoItsParent(node);
    }
    node.deps.recalcEndCoordinatesOf(node.projectData.fullname);
  }
};

let fold = (node, folded) => {
  if (!isLeaf(node)) {
    node.isFolded = folded;
    adaptRadiusAndPositionToFoldState(node);
    node.deps.changeFold(node.projectData.fullname, node.isFolded);
    return true;
  }
  return false;
};

let recreapplyFilters = (node, filters) => {
  node.filteredChildren = Array.from(filters.values()).reduce((children, filter) => children.filter(filter), node.origChildren);
  node.filteredChildren.forEach(c => recreapplyFilters(c, filters));
  if (!node.isFolded) {
    node.currentChildren = node.filteredChildren;
  }
};

let reapplyFilters = (root, filters) => {
  recreapplyFilters(root, filters);
  root.deps.setNodeFilters(root.filters);
};

let radiusOfLeafWithTitle = title => {
  return textWidth(title) / 2 + circleTextPadding;
};

let radiusOfAnyNode = (node, TEXTPOSITION) => {
  let radius = radiusOfLeafWithTitle(node.projectData.name);
  if (isOrigLeaf(node)) {
    return radius;
  }
  else {
    return radius / Math.sqrt(1 - TEXTPOSITION * TEXTPOSITION);
  }
};

let reclayout = (node, packSiblings, packEnclose, circpadding, textposition) => {
  if (isOrigLeaf(node)) {
    node.initVisual(0, 0, radiusOfAnyNode(node, textposition));
  }
  else {
    node.origChildren.forEach(c => reclayout(c, packSiblings, packEnclose, circpadding, textposition));
    let children = node.origChildren.map(c => c.visualData);
    children.forEach(c => c.r += circpadding / 2);
    packSiblings(children);
    let circ = packEnclose(children);
    children.forEach(c => c.r -= circpadding / 2);
    let childradius = children.length === 1 ? children[0].r : 0;
    node.initVisual(circ.x, circ.y, Math.max(circ.r, radiusOfAnyNode(node, textposition), childradius / textposition));
    children.forEach(c => {
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
    calcPositionAndSetRadius(this);
  }

  initVisual(x, y, r) {
    this.visualData = new VisualData(x, y, r);
  }

  /**
   * moves this node by the given values
   * @param dx
   * @param dy
   */
  drag(dx, dy, force) {
    this.visualData.move(dx, dy, this.parent, () => this.origChildren.forEach(d => d.drag(dx, dy, true)), true, force);
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
  }

  resetFilterByName() {
    this.filters.delete("namefilter");
    reapplyFilters(this, this.filters);
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
  }

  resetFilterByType() {
    this.filters.delete("typefilter");
    reapplyFilters(this, this.filters);
  }
};

let isElementMatching = (node, str, filterByFullName, inclusive, matchCase) => {
  let toFilter = filterByFullName ? node.projectData.fullname : node.projectData.name;
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
  return node => {
    if (filterClassesAndEliminatePkgs) {
      if (node.projectData.type === "package") {
        return descendants(node, false).reduce((acc, d) => acc || (d.projectData.type !== "package" &&
        filterFunction(str, filterByFullName, filterClassesAndEliminatePkgs, filterPackages, filterClasses,
            inclusive, matchCase)(d)), false);
      }
      else {
        return isElementMatching(node, str, filterByFullName, inclusive, matchCase);
      }
    }
    else {
      if (!filterPackages && !filterClasses) {
        return true;
      }
      if (filterClasses && !filterPackages && node.projectData.type === "package") {
        return true;
      }
      if (filterPackages && node.projectData.type !== "package") {
        return (filterClasses ? isElementMatching(node, str, filterByFullName, inclusive, matchCase) : true)
            && predecessors(node).reduce((acc, p) => acc && (p.isRoot() || filterFunction(str, filterByFullName,
                filterClassesAndEliminatePkgs, filterPackages, filterClasses, inclusive, matchCase)(p)), true);
      }
      return isElementMatching(node, str, filterByFullName, inclusive, matchCase);
    }
  }
};

let initNodeMap = root => {
  root.nodeMap = new Map();
  descendants(root, true).forEach(d => root.nodeMap.set(d.projectData.fullname, d));
};

let addChild = (node, child) => {
  node.origChildren.push(child);
  node.currentChildren = node.origChildren;
};

let jsonToProjectData = jsonElement => {
  return new ProjectData(jsonElement.name, jsonElement.fullname, jsonElement.type);
};

let jsonToNode = (parent, jsonNode) => {
  let node = new Node(jsonToProjectData(jsonNode), parent);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => addChild(node, jsonToNode(node, c)));
  }
  return node;
};

let jsonToRoot = (jsonRoot, textwidthfunction, circletextpadding) => {
  textWidth = textwidthfunction;
  circleTextPadding = circletextpadding;
  let root = jsonToNode(null, jsonRoot);
  initNodeMap(root);
  return root;
};

module.exports.jsonToRoot = jsonToRoot;