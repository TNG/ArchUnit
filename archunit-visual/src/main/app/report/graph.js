'use strict';

let jsonToRoot = require('./tree.js').jsonToRoot;
let jsonToDependencies = require('./dependencies.js').jsonToDependencies;

let createNodeMap = root => {
  let nodeMap = new Map();
  root.getVisibleDescendants().forEach(d => nodeMap.set(d.projectData.fullname, d));
  return nodeMap;
};

let refreshOnNodeFiltering = graph => {
  graph.dependencies.setNodeFilters(graph.root.filters);
};

let Graph = class {

  constructor(root, nodeMap, dependencies) {
    this.root = root;
    this.nodeMap = nodeMap;
    this.dependencies = dependencies;
  }

  getVisibleNodes() {
    return this.root.getVisibleDescendants();
  }

  getVisibleDependencies() {
    return this.dependencies.getVisible();
  }

  nodeKeyFunction() {
    return this.root.keyFunction();
  }

  dependencyKeyFunction() {
    return this.dependencies.keyFunction();
  }

  changeFoldStateOfNode(node) {
    if (node.changeFold()) {
      this.dependencies.changeFold(node.projectData.fullname, node.isFolded);
      return true;
    }
    return false;
  }

  foldAllNodes() {
    this.root.foldAllNodes();
    this.root.getAllDescendants().forEach(node => this.dependencies.changeFold(node.projectData.fullname, node.isFolded));
  }

  getDetailedDependenciesOf(from, to) {
    return this.dependencies.getDetailedDependenciesOf(from, to);
  }

  filterNodesByName(filterString) {
    return this.root.filterByName(filterString, () => refreshOnNodeFiltering(this));
  }

  resetFilterNodesByName() {
    this.root.resetFilterByName();
    refreshOnNodeFiltering(this);
  }

  filterNodesByType(interfaces, classes, eliminatePkgs) {
    this.root.filterByType(interfaces, classes, eliminatePkgs);
    refreshOnNodeFiltering(this);
  }

  resetFilterNodesByType() {
    this.root.resetFilterByType();
    refreshOnNodeFiltering(this);
  }

  filterDependenciesByKind() {
    return this.dependencies.filterByKind();
  }

  resetFilterDependenciesByKind() {
    this.dependencies.resetFilterByKind();
  }
};

let jsonToGraph = jsonRoot => {
  let root = jsonToRoot(jsonRoot);
  let nodeMap = createNodeMap(root);
  let deps = jsonToDependencies(jsonRoot, nodeMap);
  return new Graph(root, nodeMap, deps);
};

module.exports.jsonToGraph = jsonToGraph;