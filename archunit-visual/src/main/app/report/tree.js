'use strict';

const nodeKinds = require('./node-kinds.json');
const boolFunc = require('./booleanutils').booleanFunctions;

const TYPE_FILTER = "typefilter";
const NAME_Filter = "namefilter";

let ProjectData = class {
  constructor(name, fullname, type) {
    this.name = name;
    this.fullname = fullname;
    this.type = type;
  }
};

let descendants = (node, childrenSelector) => {
  let recDescendants = (res, node, childrenSelector) => {
    res.push(node);
    let arr = childrenSelector(node);
    arr.forEach(n => recDescendants(res, n, childrenSelector));
  };
  let res = [];
  recDescendants(res, node, childrenSelector);
  return res;
};

let isLeaf = node => node.filteredChildren.length === 0;

let fold = (node, folded) => {
  if (!isLeaf(node)) {
    node.isFolded = folded;
    if (node.isFolded) {
      node.currentChildren = [];
    }
    else {
      node.currentChildren = node.filteredChildren;
    }
    return true;
  }
  return false;
};

let resetFilteredChildrenOfAllNodes = root => {
  descendants(root, n => n.origChildren).forEach(n => {
    n.filteredChildren = n.origChildren;
  });
};

let reapplyFilters = (root, filters) => {
  resetFilteredChildrenOfAllNodes(root);
  let recReapplyFilter = (node, filter) => {
    node.filteredChildren = node.filteredChildren.filter(filter);
    node.filteredChildren.forEach(c => recReapplyFilter(c, filter));
  };
  Array.from(filters.values()).forEach(filter => recReapplyFilter(root, filter));
  descendants(root, n => n.filteredChildren).forEach(n => {
    if (!n.isFolded) {
      n.currentChildren = n.filteredChildren;
    }
  });
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

  isPackage() {
    return this.getType() === nodeKinds.package
  }

  getName() {
    return this.projectData.name;
  }

  getFullName() {
    return this.projectData.fullname;
  }

  getType() {
    return this.projectData.type;
  }

  isRoot() {
    return !this.parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this.isFolded;
  }

  isLeaf() {
    return isLeaf(this);
  }

  isChildOf(d) {
    return descendants(d, n => n.currentChildren).indexOf(this) !== -1;
  }

  changeFold() {
    return fold(this, !this.isFolded);
  }

  getClass() {
    let foldableStyle = this.isLeaf() ? "notfoldable" : "foldable";
    return `node ${this.getType()} ${foldableStyle}`;
  }

  getVisibleDescendants() {
    return descendants(this, n => n.currentChildren);
  }

  traverseTree() {
    if (this.isCurrentlyLeaf()) return this.projectData.name;
    let subTree = this.currentChildren.reduce((sub, act) => sub + act.traverseTree() + ", ", "");
    return this.projectData.name + "(" + subTree + ")";
  }

  foldAllNodes(callback) {
    if (!isLeaf(this)) {
      this.currentChildren.forEach(d => d.foldAllNodes(callback));
      if (!this.isRoot()) {
        fold(this, true);
        callback(this);
      }
    }
  }

  // FIXME: Don't use cryptic abbreviations!!!!
  dfs(fun) {
    if (!isLeaf(this)) {
      this.currentChildren.forEach(c => c.dfs(fun));
      if (!this.isRoot()) {
        fun(this);
      }
    }
  }

  keyFunction() {
    return d => d.projectData.fullname;
  }

  /**
   * filters the classes in the tree by the fullname (matching case);
   * empty packages are removed
   * @param filterString is a "small" regex: "*" stands for any keys (also nothing),
   * a space at the end makes the function filtering on endsWith
   * @param exclude
   */
  filterByName(filterString, exclude) {
    this.filters.set(NAME_Filter, createFilterFunction(filterString, exclude));
    reapplyFilters(this, this.filters);
  }

  filterByType(interfaces, classes, eliminatePkgs) {
    let classFilter =
        c => (c.projectData.type !== nodeKinds.package) &&
        boolFunc(c.projectData.type === nodeKinds.interface).implies(interfaces) &&
        boolFunc(c.projectData.type.endsWith(nodeKinds.class)).implies(classes);
    let pkgFilter =
        c => (c.projectData.type === nodeKinds.package) &&
        boolFunc(eliminatePkgs).implies(descendants(c, n => n.filteredChildren).reduce((acc, n) => acc || classFilter(n), false));
    this.filters.set(TYPE_FILTER, c => classFilter(c) || pkgFilter(c));
    reapplyFilters(this, this.filters);
  }

  resetFilterByType() {
    this.filters.delete(TYPE_FILTER);
    reapplyFilters(this, this.filters);
  }

  addChild(child) {
    this.origChildren.push(child);
    this.currentChildren = this.origChildren;
  }
};

let createFilterFunction = (filterString, exclude) => {
  filterString = leftTrim(filterString);
  let endsWith = filterString.endsWith(" ");
  filterString = filterString.trim();
  let regexString = escapeRegExp(filterString).replace(/\*/g, ".*");
  if (endsWith) {
    regexString = "(" + regexString + ")$";
  }

  let filter = node => {
    if (node.projectData.type === nodeKinds.package) {
      return node.filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      let match = new RegExp(regexString).exec(node.projectData.fullname);
      let res = match && match.length > 0;
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node.filteredChildren.reduce((acc, c) => acc || filter(c), false));
    }
  };
  return filter;
};

let leftTrim = str => {
  return str.replace(/^\s+/g, '');
};

let escapeRegExp = str => {
  return str.replace(/[-[\]/{}()+?.\\^$|]/g, '\\$&');
};

let parseJsonProjectData = jsonElement => {
  return new ProjectData(jsonElement.name, jsonElement.fullname, jsonElement.type);
};

let parseJsonNode = (parent, jsonNode) => {
  let node = new Node(parseJsonProjectData(jsonNode), parent);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => node.addChild(parseJsonNode(node, c)));
  }
  return node;
};

let jsonToRoot = jsonRoot => {
  return parseJsonNode(null, jsonRoot);
};

module.exports.jsonToRoot = jsonToRoot;