"use strict";

/*
 * Some poor man's DI solution...
 */
const init = (getNodeView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getCalculateTextWidth) => {

  const getVisualizationFunctions = () => {
    return require('./visualization-functions').newInstance(getCalculateTextWidth());
  };

  const getNodeText = () => require('./node-text').init(getVisualizationStyles(), getCalculateTextWidth());

  const getJsonToRoot = () => require('./tree').init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles()).jsonToRoot;

  const getJsonToDependencies = () => require('./dependencies').init(getDependencyView(), getDetailedDependencyView()).jsonToDependencies;

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
const APPEAR_DURATION = 200;
const HIDE_DURATION = 300;

module.exports.newInstance = overrides => {
  overrides = overrides || {};

  const getNodeView = () => overrides.NodeView || require('./node-view').init(TRANSITION_DURATION).View;
  const getDependencyView = () => overrides.DependencyView || require('./dependency-view').init(TRANSITION_DURATION).View;
  const getDetailedDependencyView = () => overrides.DetailedDependencyView || require('./detailed-dependency-view').init(APPEAR_DURATION, HIDE_DURATION).View;
  const getGraphView = () => overrides.GraphView || require('./graph-view').init(TRANSITION_DURATION).View;
  const getVisualizationStyles = () => overrides.visualizationStyles || require('./visualization-styles').fromEmbeddedStyleSheet();
  const getCalculateTextWidth = () => overrides.calculateTextWidth || require('./text-width-calculator');

  return init(getNodeView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getCalculateTextWidth);
};