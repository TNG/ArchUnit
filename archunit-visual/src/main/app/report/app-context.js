"use strict";

/*
 * Some poor man's DI solution...
 */
const init = (getNodeView, getDependencyView, getGraphView, getVisualizationStyles, getCalculateTextWidth) => {

  const getVisualizationFunctions = () => {
    return require('./visualization-functions').newInstance(getCalculateTextWidth());
  };

  const getNodeText = () => require('./node-text').init(getVisualizationStyles(), getCalculateTextWidth());

  const getJsonToRoot = () => require('./tree').init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles()).jsonToRoot;

  const getJsonToDependencies = () => require('./dependencies').init(getDependencyView()).jsonToDependencies;

  const getJsonToGraph = () => {
    return require('./graph').init(getJsonToRoot(), getJsonToDependencies(), getGraphView()).jsonToGraph;
  };

  return {
    getVisualizationFunctions,
    getVisualizationStyles,
    getJsonToRoot,
    getJsonToDependencies,
    getGraphView,
    getJsonToGraph
  }
};

const TRANSITION_DURATION = 300;

module.exports.newInstance = overrides => {
  overrides = overrides || {};

  const getCalculateTextWidth = () => overrides.calculateTextWidth || require('./text-width-calculator');
  const getVisualizationStyles = () => overrides.visualizationStyles || require('./visualization-styles').fromEmbeddedStyleSheet();
  const getNodeView = () => overrides.NodeView || require('./node-view').init(TRANSITION_DURATION).View;
  const getDetailedDependencyView = () => overrides.DetailedDependencyView || require('./detailed-dependency-view').init(TRANSITION_DURATION, getCalculateTextWidth(), getVisualizationStyles()).View;
  const getDependencyView = () => overrides.DependencyView || require('./dependency-view').init(getDetailedDependencyView(), TRANSITION_DURATION).View;
  const getGraphView = () => overrides.GraphView || require('./graph-view').init(TRANSITION_DURATION).View;

  return init(getNodeView, getDependencyView, getGraphView, getVisualizationStyles, getCalculateTextWidth);
};