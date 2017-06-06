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

let fold = (node, folded) => {
  if (!isLeaf(node)) {
    node.isFolded = folded;
    if (node.isFolded) {
      node.currentChildren = [];
    }
    else {
      node.currentChildren = node.filteredChildren;
    }

    //node.observers.forEach(f => f(node));
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
  let recReapplyFilter = (node, filter) => { //filters
    node.filteredChildren = node.filteredChildren.filter(filter);//Array.from(filters.values()).reduce((children, filter) => children.filter(filter), node.origChildren);
    node.filteredChildren.forEach(c => recReapplyFilter(c, filter));
    /*if (!node.isFolded) {
      node.currentChildren = node.filteredChildren;
     }*/
  };
  Array.from(filters.values()).forEach(filter => recReapplyFilter(root, filter));
  descendants(root, n => n.filteredChildren).forEach(n => {
    if (!n.isFolded) {
      n.currentChildren = n.filteredChildren;
    }
  });
  //recReapplyFilters(root, filters);
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
    //this.observers = parent ? parent.observers : [];
  }

  /*addObserver(observerFunction) {
    this.observers.push(observerFunction);
   }*/

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
    return this.projectData.type + (!this.isLeaf() && this.projectData.type !== nodeKinds.package ? " foldable" : " foldunable");
  }

  getVisibleDescendants() {
    return descendants(this, n => n.currentChildren);
  }

  getAllDescendants() {
    return descendants(this, n => n.origChildren);
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
   * the root package is ignored while filtering
   */
  filterByName(filterString, callback) {
    let applyFilter = filter => {
      this.filters.set(NAME_Filter, filter);
      reapplyFilters(this, this.filters);
      callback();
    };
    return filterBuilder(filterString, applyFilter);
  }

  resetFilterByName() {
    this.filters.delete(NAME_Filter);
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
        return descendants(node, n => n.filteredChildren).reduce((acc, d) => acc || (d.projectData.type !== nodeKinds.package &&
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
  return parseJsonNode(null, jsonRoot);
};

module.exports.jsonToRoot = jsonToRoot;