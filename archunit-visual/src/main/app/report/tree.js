'use strict';

const nodeKinds = require('./node-kinds.json');
const boolFunc = require('./booleanutils').booleanFunctions;

const TYPE_FILTER = "typefilter";
const NAME_Filter = "namefilter";

let NodeDescription = class {
  constructor(name, fullName, type) {
    this.name = name;
    this.fullName = fullName;
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

let isLeaf = node => node._filteredChildren.length === 0;
let fold = (node, folded) => {
  if (!isLeaf(node)) {
    node._folded = folded;
    return true;
  }
  return false;
};

let resetFilteredChildrenOfAllNodes = root => {
  descendants(root, n => n.getOriginalChildren()).forEach(n => {
    n._filteredChildren = n.getOriginalChildren();
  });
};

let reapplyFilters = (root, filters) => {
  resetFilteredChildrenOfAllNodes(root);
  let recReapplyFilter = (node, filter) => {
    node._filteredChildren = node._filteredChildren.filter(filter);
    node._filteredChildren.forEach(c => recReapplyFilter(c, filter));
  };
  Array.from(filters.values()).forEach(filter => recReapplyFilter(root, filter));
};

let Node = class {
  constructor(description, parent) {
    this._description = description;
    this._parent = parent;
    this._originalChildren = [];
    this._filteredChildren = this._originalChildren;
    this._folded = false;
    this._filters = new Map();
  }

  isPackage() {
    return this.getType() === nodeKinds.package
  }

  getName() {
    return this._description.name;
  }

  getFullName() {
    return this._description.fullName;
  }

  getType() {
    return this._description.type;
  }

  getParent() {
    return this._parent;
  }

  getOriginalChildren() {
    return this._originalChildren;
  }

  getCurrentChildren() {
    return this._folded ? [] : this._filteredChildren;
  }

  isRoot() {
    return !this._parent;
  }

  isCurrentlyLeaf() {
    return isLeaf(this) || this._folded;
  }

  isChildOf(d) {
    return descendants(d, n => n.getCurrentChildren()).indexOf(this) !== -1;
  }

  isFolded() {
    return this._folded;
  }

  fold() {
    return fold(this, true);
  }

  isLeaf() {
    return isLeaf(this);
  }

  changeFold() {
    return fold(this, !this._folded);
  }

  getFilters() {
    return this._filters;
  }

  getClass() {
    let foldableStyle = isLeaf(this) ? "not-foldable" : "foldable";
    return `node ${this.getType()} ${foldableStyle}`;
  }

  getVisibleDescendants() {
    return descendants(this, n => n.getCurrentChildren());
  }

  foldAllNodes(callback) {
    if (!isLeaf(this)) {
      this.getCurrentChildren().forEach(d => d.foldAllNodes(callback));
      if (!this.isRoot()) {
        fold(this, true);
        callback(this);
      }
    }
  }

  recursiveCall(fun) {
    this.getCurrentChildren().forEach(c => c.recursiveCall(fun));
    fun(this);
  }

  /**
   * filters the classes in the tree by the fullName (matching case);
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
        boolFunc(eliminatePkgs).implies(descendants(c, n => n._filteredChildren).reduce((acc, n) => acc || classFilter(n), false));
    this._filters.set(TYPE_FILTER, c => classFilter(c) || pkgFilter(c));
    reapplyFilters(this, this._filters);
  }

  resetFilterByType() {
    this._filters.delete(TYPE_FILTER);
    reapplyFilters(this, this._filters);
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
      return node._filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      let match = new RegExp(regexString).exec(node.getFullName());
      let res = match && match.length > 0;
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node._filteredChildren.reduce((acc, c) => acc || filter(c), false));
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

let parseNodeDescriptionFromJson = jsonElement => {
  return new NodeDescription(jsonElement.name, jsonElement.fullName, jsonElement.type);
};

let parseJsonNode = (parent, jsonNode) => {
  let node = new Node(parseNodeDescriptionFromJson(jsonNode), parent);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => node._originalChildren.push(parseJsonNode(node, c)));
  }
  return node;
};

let jsonToRoot = jsonRoot => {
  return parseJsonNode(null, jsonRoot);
};

module.exports.jsonToRoot = jsonToRoot;