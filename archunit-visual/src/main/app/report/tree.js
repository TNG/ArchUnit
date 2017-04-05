'use strict';

let textwidth;

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

let isLeaf = d => d.filteredChildren.length === 0; //!d.filteredChildren ||

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
  let w = textwidth(d.projectData.name);
  console.log(d.projectData.fullname + ": " + foldedR + "/" + w);
  return Math.max(foldedR, w);
};

let setIsFolded = (d, isFolded) => {
  d.isFolded = isFolded;
  if (d.isFolded) {
    d.visualData.r = getFoldedR(d);
    d.currentChildren = [];
  }
  else {
    d.currentChildren = d.filteredChildren;
    d.changeRadius(d.visualData.origR, false);
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

let Node = class {

  constructor(projectData, parent) {
    this.projectData = projectData;
    this.parent = parent;
    this.origChildren = []; //null
    this.filteredChildren = this.origChildren;
    this.currentChildren = this.filteredChildren;
    this.isFolded = false;
    this.filters = new Map();
  }

  //Neuversuch:
  layout(packSiblings, packEnclose, radius) {
    this.reclayout(packSiblings, packEnclose, radius);
    this.calcPos();
  }

  reclayout(packSiblings, packEnclose, radius) {
    if (this.isOrigLeaf()) {
      this.initVisual(0, 0, radius(this));
    }
    else {
      this.origChildren.forEach(c => c.reclayout(packSiblings, packEnclose, radius));
      let children = this.origChildren.map(c => c.visualData);
      packSiblings(children);
      let circ = packEnclose(children);
      let childradius = children.length === 1 ? children[0].r : 0;
      this.initVisual(circ.x, circ.y, Math.max(circ.r + 10, radius(this), childradius / 0.8));
      children.forEach(n => {
        n.dx = n.x - this.visualData.x;
        n.dy = n.y - this.visualData.y;
      });
    }
  }

  calcPos() {
    console.log(this.projectData.fullname + "->" + this.visualData.x + "/" + this.visualData.y);
    if (this.isRoot()) {
      this.visualData.x = this.visualData.r;
      this.visualData.y = this.visualData.r;
    }
    if (!this.isOrigLeaf()) {
      this.origChildren.forEach(n => {
        n.visualData.x = this.visualData.x + n.visualData.dx;
        n.visualData.y = this.visualData.y + n.visualData.dy;
        n.calcPos();
      });
    }
  }

  setTextWidthFunction(textWidthFunction) {
    textwidth = textWidthFunction;
  }

  ///Ende Neuversuch


  initVisual(x, y, r) {
    this.visualData = new VisualData(x, y, r); //TODO: statt r ursprünglich: isOnlyChild(this) ? r / 2 : r
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

  changeRadius(newR, permamently) {
    this.visualData.r = newR;
    if (permamently) {
      this.visualData.origR = newR;
    }
    if (!this.isRoot() && !this.parent.isRoot()) {
      checkAndCorrectPosition(this);
    }
    /*
     if (!this.isRoot() && !this.parent.isRoot()
     && this.visualData.r >= MINPROPORTIONOFCHILDANDPARENTRADIUS * this.parent.visualData.r) {
     this.parent.changeRadius(this.visualData.r / MINPROPORTIONOFCHILDANDPARENTRADIUS, true);

     //console.log(new Error().stack);
     }
     */
    this.deps.recalcEndCoordinatesOf(this.projectData.fullname);
  }

  isRoot() {
    return !this.parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this.isFolded;
  }

  isOrigLeaf() {
    return this.origChildren.length === 0; //!this.origChildren ||
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
   * @param d
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

let jsonToRoot = (jsonRoot) => {
  let root = jsonToNode(null, jsonRoot);
  initNodeMap(root);
  return root;
};

module.exports.jsonToRoot = jsonToRoot;