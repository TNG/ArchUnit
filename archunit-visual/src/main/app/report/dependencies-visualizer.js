'use strict';

const refreshVisualData = dependency => {
  dependency.visualData.recalc(dependency.mustShareNodes, dependency.getStartNode().getAbsoluteVisualData(), dependency.getEndNode().getAbsoluteVisualData());
};

const refreshVisualDataOf = (nodeFullName, dependencies) => {
  dependencies.filter(d => d.from.startsWith(nodeFullName) || d.to.startsWith(nodeFullName)).forEach(d => refreshVisualData(d));
};

const refreshVisualDataOfDependencies = dependencies => {
  dependencies.forEach(d => refreshVisualData(d));
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