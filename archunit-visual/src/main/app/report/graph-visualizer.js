'use strict';

module.exports.newInstance = (dependenciesVisualizer) => {
  return {
    visualizeGraph: graph => {
      dependenciesVisualizer.visualizeDependencies(graph.root._dependencies);
    },
    drag: (graph, node) => {
      dependenciesVisualizer.refreshVisualDataOf(node.getFullName(), graph.getVisibleDependencies());
    },
    update: graph => {
      dependenciesVisualizer.refreshVisualDataOfDependencies(graph.getVisibleDependencies())
    }
  }
};