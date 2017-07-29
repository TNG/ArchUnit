'use strict';

module.exports.newInstance = config => {
  const treeVisualizer = require("./tree-visualizer.js").newInstance(config);
  const dependenciesVisualizer = require("./dependencies-visualizer.js");

  return {
    visualizeGraph: graph => {
      treeVisualizer.visualizeTree(graph.root);
      dependenciesVisualizer.visualizeDependencies(graph.root._dependencies);
    },
    drag: (graph, node, dx, dy, force) => {
      treeVisualizer.dragNode(node, dx, dy, force);
      dependenciesVisualizer.refreshVisualDataOf(node.getFullName(), graph.getVisibleDependencies());
    },
    update: graph => {
      treeVisualizer.visualizeTree(graph.root);
      dependenciesVisualizer.refreshVisualDataOfDependencies(graph.getVisibleDependencies())
    }
  }
};