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

let isLeaf = node => node.getFilteredChildren().length === 0;

let fold = (node, folded) => {
  if (!isLeaf(node)) {
    node._folded = folded;
    if (node.isFolded()) {
      node._currentChildren = [];
    }
    else {
      node._currentChildren = node.getFilteredChildren();
    }
    return true;
  }
  return false;
};

let resetFilteredChildrenOfAllNodes = root => {
  descendants(root, n => n.getOrigChildren()).forEach(n => {
    n._filteredChildren = n.getOrigChildren();
  });
};

let reapplyFilters = (root, filters) => {
  resetFilteredChildrenOfAllNodes(root);
  let recReapplyFilter = (node, filter) => {
    node._filteredChildren = node.getFilteredChildren().filter(filter);
    node.getFilteredChildren().forEach(c => recReapplyFilter(c, filter));
  };
  Array.from(filters.values()).forEach(filter => recReapplyFilter(root, filter));
  descendants(root, n => n.getFilteredChildren()).forEach(n => {
    if (!n.isFolded()) {
      n._currentChildren = n.getFilteredChildren();
    }
  });
};

let Node = class {
  constructor(projectData, parent) {
    this._projectData = projectData;
    this._parent = parent;
    this._origChildren = [];
    this._filteredChildren = this._origChildren;
    this._currentChildren = this._filteredChildren;
    this._folded = false;
    this._filters = new Map();
  }

  isPackage() {
    return this.getType() === nodeKinds.package
  }

  getName() {
    return this._projectData.name;
  }

  getFullName() {
    return this._projectData.fullname;
  }

  getType() {
    return this._projectData.type;
  }

  getParent() {
    return this._parent;
  }

  // FIXME: What is the meaning of 'orig' children? I guess it's 'all' children, before any filter is applied? Also why an abbreviation again? Does this stand for original? We could afford the 4 extra chars in the year 2017 ;-)
  getOrigChildren() {
    return this._origChildren;
  }

  getFilteredChildren() {
    return this._filteredChildren;
  }

  // FIXME: I don't see, why we need 3 sets of children? Shouldn't 'all' and 'filtered' be enough? Why is there 'current'? And why should it differ from 'filtered'?
  getCurrentChildren() {
    return this._currentChildren;
  }

  isRoot() {
    return !this._parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this._folded;
  }

  isLeaf() {
    return isLeaf(this);
  }

  isChildOf(d) {
    return descendants(d, n => n.getCurrentChildren()).indexOf(this) !== -1;
  }

  isFolded() {
    return this._folded;
  }

  changeFold() {
    return fold(this, !this._folded);
  }

  getFilters() {
    return this._filters;
  }

  getClass() {
    let foldableStyle = this.isLeaf() ? "notfoldable" : "foldable";
    return `node ${this.getType()} ${foldableStyle}`;
  }

  getVisibleDescendants() {
    return descendants(this, n => n.getCurrentChildren());
  }

  traverseTree() {
    if (this.isCurrentlyLeaf()) {
      return this.getName();
    }
    let subTree = this._currentChildren.reduce((sub, act) => sub + act.traverseTree() + ", ", "");
    return this.getName() + "(" + subTree + ")";
  }

  foldAllNodes(callback) {
    if (!isLeaf(this)) {
      this._currentChildren.forEach(d => d.foldAllNodes(callback));
      if (!this.isRoot()) {
        fold(this, true);
        callback(this);
      }
    }
  }

  // FIXME: Don't use cryptic abbreviations!!!!
  dfs(fun) {
    if (!isLeaf(this)) {
      this._currentChildren.forEach(c => c.dfs(fun));
      if (!this.isRoot()) {
        fun(this);
      }
    }
  }

  keyFunction() {
    return d => d.getFullName();
  }

  /**
   * filters the classes in the tree by the fullname (matching case);
   * empty packages are removed
   * @param filterString is a "small" regex: "*" stands for any keys (also nothing),
   * a space at the end makes the function filtering on endsWith
   * @param exclude
   */
  filterByName(filterString, exclude) {
    this._filters.set(NAME_Filter, createFilterFunction(filterString, exclude));
    reapplyFilters(this, this._filters);
  }

  filterByType(interfaces, classes, eliminatePkgs) {
    let classFilter =
        c => (c.getType() !== nodeKinds.package) &&
        boolFunc(c.getType() === nodeKinds.interface).implies(interfaces) &&
        boolFunc(c.getType().endsWith(nodeKinds.class)).implies(classes);
    let pkgFilter =
        c => (c.getType() === nodeKinds.package) &&
        boolFunc(eliminatePkgs).implies(descendants(c, n => n.getFilteredChildren()).reduce((acc, n) => acc || classFilter(n), false));
    this._filters.set(TYPE_FILTER, c => classFilter(c) || pkgFilter(c));
    reapplyFilters(this, this._filters);
  }

  resetFilterByType() {
    this._filters.delete(TYPE_FILTER);
    reapplyFilters(this, this._filters);
  }

  addChild(child) {
    this._origChildren.push(child);
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
    if (node.getType() === nodeKinds.package) {
      return node.getFilteredChildren().reduce((acc, c) => acc || filter(c), false);
    }
    else {
      let match = new RegExp(regexString).exec(node.getFullName());
      let res = match && match.length > 0;
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node.getFilteredChildren().reduce((acc, c) => acc || filter(c), false));
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