'use strict';

const jsonToDependencies = require('./dependencies.js').jsonToDependencies;

const nodeKinds = require('./node-kinds.json');
const boolFunc = require('./booleanutils').booleanFunctions;

const TYPE_FILTER = "typefilter";
const NAME_Filter = "namefilter";

const NodeDescription = class {
  constructor(name, fullName, type) {
    this.name = name;
    this.fullName = fullName;
    this.type = type;
  }
};

const descendants = (node, childrenSelector) => {
  const recDescendants = (res, node, childrenSelector) => {
    res.push(node);
    const arr = childrenSelector(node);
    arr.forEach(n => recDescendants(res, n, childrenSelector));
  };
  const res = [];
  recDescendants(res, node, childrenSelector);
  return res;
};

const isLeaf = node => node._filteredChildren.length === 0;
const fold = (node, folded) => {
  if (!isLeaf(node)) {
    node._folded = folded;
    return true;
  }
  return false;
};

const resetFilteredChildrenOfAllNodes = root => {
  descendants(root, n => n.getOriginalChildren()).forEach(n => {
    n._filteredChildren = n.getOriginalChildren();
  });
};

const reapplyFilters = (root, filters) => {
  resetFilteredChildrenOfAllNodes(root);
  const recReapplyFilter = (node, filter) => {
    node._filteredChildren = node._filteredChildren.filter(filter);
    node._filteredChildren.forEach(c => recReapplyFilter(c, filter));
  };
  Array.from(filters.values()).forEach(filter => recReapplyFilter(root, filter));
};

const getRoot = node => {
  let root = node;
  while (root._parent) {
    root = root._parent;
  }
  return root;
};

const getDependencies = node => {
  return getRoot(node)._dependencies;
};

const Node = class {
  constructor(jsonNode) {
    this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);

    const jsonChildren = jsonNode.hasOwnProperty("children") ? jsonNode.children : [];
    this._originalChildren = jsonChildren.map(jsonChild => new Node(jsonChild));
    this._originalChildren.forEach(c => c._parent = this);

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
    const wasFolded = fold(this, true);
    if (wasFolded) {
      getDependencies(this).changeFold(this.getFullName(), this.isFolded());
    }
    return wasFolded;
  }

  isLeaf() {
    return isLeaf(this);
  }

  changeFold() {
    const wasFolded = fold(this, !this._folded);
    if (wasFolded) {
      getDependencies(this).changeFold(this.getFullName(), this.isFolded());
    }
    return wasFolded;
  }

  getFilters() {
    return this._filters;
  }

  getClass() {
    const foldableStyle = isLeaf(this) ? "not-foldable" : "foldable";
    return `node ${this.getType()} ${foldableStyle}`;
  }

  getVisibleDescendants() {
    return descendants(this, n => n.getCurrentChildren());
  }

  getVisibleDependencies() {
    return getDependencies(this).getVisible();
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

  callOnEveryNode(fun) {
    this.getCurrentChildren().forEach(c => c.callOnEveryNode(fun));
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

    getDependencies(this).setNodeFilters(getRoot(this).getFilters());
  }

  filterByType(interfaces, classes, eliminatePkgs) {
    const classFilter =
      c => (c.getType() !== nodeKinds.package) &&
      boolFunc(c.getType() === nodeKinds.interface).implies(interfaces) &&
      boolFunc(c.getType().endsWith(nodeKinds.class)).implies(classes);
    const pkgFilter =
      c => (c.getType() === nodeKinds.package) &&
      boolFunc(eliminatePkgs).implies(descendants(c, n => n._filteredChildren).reduce((acc, n) => acc || classFilter(n), false));
    this._filters.set(TYPE_FILTER, c => classFilter(c) || pkgFilter(c));
    reapplyFilters(this, this._filters);

    getDependencies(this).setNodeFilters(getRoot(this).getFilters());
  }

  resetFilterByType() {
    this._filters.delete(TYPE_FILTER);
    reapplyFilters(this, this._filters);

    getDependencies(this).setNodeFilters(getRoot(this).getFilters());
  }
};

const createFilterFunction = (filterString, exclude) => {
  filterString = leftTrim(filterString);
  const endsWith = filterString.endsWith(" ");
  filterString = filterString.trim();
  let regexString = escapeRegExp(filterString).replace(/\*/g, ".*");
  if (endsWith) {
    regexString = "(" + regexString + ")$";
  }

  const filter = node => {
    if (node.getType() === nodeKinds.package) {
      return node._filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      const match = new RegExp(regexString).exec(node.getFullName());
      let res = match && match.length > 0;
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node._filteredChildren.reduce((acc, c) => acc || filter(c), false));
    }
  };
  return filter;
};

const leftTrim = str => {
  return str.replace(/^\s+/g, '');
};

const escapeRegExp = str => {
  return str.replace(/[-[\]/{}()+?.\\^$|]/g, '\\$&');
};

const jsonToRoot = jsonRoot => {
  const root = new Node(jsonRoot);

  const map = new Map();
  root.callOnEveryNode(n => map.set(n.getFullName(), n));
  root.getByName = name => map.get(name);

  root._dependencies = jsonToDependencies(jsonRoot, root);
  root.getDetailedDependenciesOf = (from, to) => root._dependencies.getDetailedDependenciesOf(from, to);
  root.filterDependenciesByKind = () => root._dependencies.filterByKind();
  root.resetFilterDependenciesByKind = () => root._dependencies.resetFilterByKind();

  return root;
};

module.exports.jsonToRoot = jsonToRoot;