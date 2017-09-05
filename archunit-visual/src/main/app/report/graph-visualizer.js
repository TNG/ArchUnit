'use strict';

module.exports.newInstance = (dependenciesVisualizer) => {
  return {
    visualizeGraph: graph => {
      dependenciesVisualizer.visualizeDependencies(graph.root._dependencies);
    },
    update: graph => {
      dependenciesVisualizer.refreshVisualDataOfDependencies(graph.getVisibleDependencies())
    }
  }
};