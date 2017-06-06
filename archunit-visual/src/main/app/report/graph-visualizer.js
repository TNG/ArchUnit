'use strict';

const treeVisualizer = require("./tree-visualizer.js").treeVisualizer;

const dependenciesVisualizer = require("./dependencies-visualizer.js").dependenciesVisualizer;

let visualizeGraph = graph => {
  treeVisualizer.visualizeTree(graph.root);
  dependenciesVisualizer.visualizeDependencies(graph.dependencies);
};

let drag = (graph, node, dx, dy, force) => {
  treeVisualizer.dragNode(node, dx, dy, force);
  dependenciesVisualizer.refreshVisualDataOf(node.projectData.fullname, graph.getVisibleDependencies());
};

let updateOnFolding = graph => {
  treeVisualizer.visualizeTree(graph.root);
  dependenciesVisualizer.refreshVisualDataOfDependencies(graph.getVisibleDependencies())
};

module.exports.visualizer = {
  setStyles: treeVisualizer.setStyles,
  setCirclePadding: treeVisualizer.setCirclePadding,
  visualizeGraph: visualizeGraph,
  drag: drag,
  updateOnFolding: updateOnFolding
};