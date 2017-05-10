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

let descendants = (node, onlyVisible) => {
  let recDescendants = (res, node, onlyVisible) => {
    res.push(node);
    let arr = onlyVisible ? node.currentChildren : node.filteredChildren; // node.origChildren;
    arr.forEach(n => recDescendants(res, n, onlyVisible));
  };
  let res = [];
  recDescendants(res, node, onlyVisible);
  return res;
};

let predecessors = node => {
  let recpredecessors = (res, node) => {
    if (!node.isRoot()) {
      res.push(node.parent);
      recpredecessors(res, node.parent);
    }
  };
  let res = [];
  recpredecessors(res, node);
  return res;
};

let isLeaf = node => node.filteredChildren.length === 0;

let fold = (node, folded, callback) => {
  if (!isLeaf(node)) {
    node.isFolded = folded;
    if (node.isFolded) {
      node.currentChildren = [];
    }
    else {
      node.currentChildren = node.filteredChildren;
    }
    node.deps.changeFold(node.projectData.fullname, node.isFolded);
    callback(node);
    return true;
  }
  return false;
};

let reapplyFilters = (root, filters, callback) => {
  let recReapplyFilters = (node, filters) => {
    node.filteredChildren = Array.from(filters.values()).reduce((children, filter) => children.filter(filter), node.origChildren);
    node.filteredChildren.forEach(c => recReapplyFilters(c, filters));
    if (!node.isFolded) {
      node.currentChildren = node.filteredChildren;
    }
  };
  recReapplyFilters(root, filters);
  root.deps.setNodeFilters(root.filters);
  callback(root.getVisibleEdges());
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
    return descendants(d, true).indexOf(this) !== -1;
  }

  //FIXME: besser ohne Callback, stattdessen einfach nach dem Folden aufrufen??
  changeFold(callback) {
    return fold(this, !this.isFolded, callback);
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

  foldAllExceptRoot(callback) {
    if (!isLeaf(this)) {
      this.currentChildren.forEach(d => d.foldAllExceptRoot(callback));
      if (!this.isRoot()) fold(this, true, callback);
    }
  }

  keyFunction() {
    return d => d.projectData.fullname;
  }

  setDependencies(deps) {
    descendants(this, false).forEach(d => d.deps = deps);
  }

  getVisibleEdges() {
    return this.deps.getVisible();
  }

  /**
   * the root package is ignored while filtering
   */
  filterByName(filterString, callback) {
    let applyFilter = filter => {
      this.filters.set(NAME_Filter, filter);
      reapplyFilters(this, this.filters, callback);
    };
    return filterBuilder(filterString, applyFilter);
  }

  resetFilterByName(callback) {
    this.filters.delete(NAME_Filter);
    reapplyFilters(this, this.filters, callback);
  }

  //FIXME: if classes are excluded, then included again and interfaces are shown instead, it fails
  filterByType(interfaces, classes, eliminatePkgs, callback) {
    let classFilter =
        c => (c.projectData.type !== nodeKinds.package) &&
        boolFunc(c.projectData.type === nodeKinds.interface).implies(interfaces) &&
        boolFunc(c.projectData.type.endsWith(nodeKinds.class)).implies(classes);
    let pkgFilter =
        c => (c.projectData.type === nodeKinds.package) &&
        boolFunc(eliminatePkgs).implies(descendants(c, false).reduce((acc, n) => acc || classFilter(n), false));
    this.filters.set(TYPE_FILTER, c => classFilter(c) || pkgFilter(c));
    reapplyFilters(this, this.filters, callback);
  }

  resetFilterByType(callback) {
    this.filters.delete(TYPE_FILTER);
    reapplyFilters(this, this.filters, callback);
  }
};

let filterBuilder = (filterString, applyFilter) => {
  let filterSettings = {};
  filterSettings.filterString = filterString;
  let matchCase = {
    matchCase: matchCase => {
      filterSettings.caseConverter = str => matchCase ? str : str.toLowerCase();
      applyFilter(filterFunction(filterSettings));
    }
  };
  let howToFilter = {
    inclusive: () => {
      filterSettings.includeOrExclude = res => res;
      return matchCase;
    },
    exclusive: () => {
      filterSettings.includeOrExclude = res => !res;
      return matchCase;
    }
  };
  let whatToFilter = {
    filterClassesAndEliminatePkgs: () => {
      filterSettings.filterClassesAndEliminatePkgs = true;
      return howToFilter;
    },
    filterPkgsOrClasses: (filterPackages, filterClasses) => {
      filterSettings.filterPackages = filterPackages;
      filterSettings.filterClasses = filterClasses;
      return howToFilter;
    }
  };
  return {
    by: () => ({
      fullname: () => {
        filterSettings.propertyFunc = n => n.fullname;
        return whatToFilter;
      },
      simplename: () => {
        filterSettings.propertyFunc = n => n.name;
        return whatToFilter;
      }
    })
  };
};

let isElementMatching = (node, filterSettings) => {
  let toFilter = filterSettings.propertyFunc(node.projectData);
  let res = filterSettings.caseConverter(toFilter).includes(filterSettings.caseConverter(filterSettings.filterString));
  return filterSettings.includeOrExclude(res);
};

let filterFunction = (filterSettings) => {
  return node => {
    if (filterSettings.filterClassesAndEliminatePkgs) {
      if (node.projectData.type === nodeKinds.package) {
        return descendants(node, false).reduce((acc, d) => acc || (d.projectData.type !== nodeKinds.package &&
        filterFunction(filterSettings)(d)), false);
      }
      else {
        return isElementMatching(node, filterSettings);
      }
    }
    else {
      if (!filterSettings.filterPackages && !filterSettings.filterClasses) {
        return true;
      }
      if (filterSettings.filterClasses && !filterSettings.filterPackages && node.projectData.type === nodeKinds.package) {
        return true;
      }
      if (filterSettings.filterPackages && node.projectData.type !== nodeKinds.package) {
        return (filterSettings.filterClasses ? isElementMatching(node, filterSettings) : true)
            && predecessors(node).reduce((acc, p) => acc && (p.isRoot() || filterFunction(filterSettings)(p)), true);
      }
      return isElementMatching(node, filterSettings);
    }
  }
};

let createNodeMap = root => {
  root.nodeMap = new Map();
  descendants(root, true).forEach(d => root.nodeMap.set(d.projectData.fullname, d));
};

let addChild = (node, child) => {
  node.origChildren.push(child);
  node.currentChildren = node.origChildren;
};

let parseJsonProjectData = jsonElement => {
  return new ProjectData(jsonElement.name, jsonElement.fullname, jsonElement.type);
};

let parseJsonNode = (parent, jsonNode) => {
  let node = new Node(parseJsonProjectData(jsonNode), parent);
  if (jsonNode.hasOwnProperty("children")) {
    jsonNode.children.forEach(c => addChild(node, parseJsonNode(node, c)));
  }
  return node;
};

let jsonToRoot = jsonRoot => {
  let root = parseJsonNode(null, jsonRoot);
  createNodeMap(root);
  return root;
};

module.exports.jsonToRoot = jsonToRoot;