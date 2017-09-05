'use strict';

const refreshVisualDataOfDependencies = dependencies => {
  dependencies.forEach(d => d.updateVisualData());
};

const visualizeDependencies = dependencies => {
  refreshVisualDataOfDependencies(dependencies.getVisible());
  dependencies.addObserver(refreshVisualDataOfDependencies);
};

module.exports = {
  visualizeDependencies: visualizeDependencies,
  refreshVisualDataOfDependencies: refreshVisualDataOfDependencies
};