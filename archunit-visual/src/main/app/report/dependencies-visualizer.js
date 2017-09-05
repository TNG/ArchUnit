'use strict';

const refreshVisualDataOf = (nodeFullName, dependencies) => {
  dependencies.filter(d => d.from.startsWith(nodeFullName) || d.to.startsWith(nodeFullName)).forEach(d => d.updateVisualData());
};

const refreshVisualDataOfDependencies = dependencies => {
  dependencies.forEach(d => d.updateVisualData());
};

const visualizeDependencies = dependencies => {
  refreshVisualDataOfDependencies(dependencies.getVisible());
  dependencies.addObserver(refreshVisualDataOfDependencies);
};

module.exports = {
  refreshVisualDataOf: refreshVisualDataOf,
  visualizeDependencies: visualizeDependencies,
  refreshVisualDataOfDependencies: refreshVisualDataOfDependencies
};