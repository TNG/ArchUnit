"use strict";

/*
 * Some poor man's DI solution...
 */
const init = (getNodeView, getDependencyView, getVisualizationStyles, getCalculateTextWidth) => {

  const getVisualizationFunctions = () => {
    return require('./visualization-functions').newInstance(getCalculateTextWidth());
  };

  const getNodeText = () => require('./node-text').init(getVisualizationStyles(), getCalculateTextWidth());

  const getJsonToRoot = () => require('./tree').init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles()).jsonToRoot;

  const getJsonToDependencies = () => require('./dependencies').init(getDependencyView()).jsonToDependencies;

  const getJsonToGraph = () => {
    return require('./graph').init(getJsonToRoot(), getJsonToDependencies()).jsonToGraph
  };

  return {
    getVisualizationFunctions,
    getVisualizationStyles,
    getJsonToRoot,
    getJsonToDependencies,
    getJsonToGraph
  }
};

const TRANSITION_DURATION = 300;

module.exports.newInstance = overrides => {
  overrides = overrides || {};

  const getNodeView = () => overrides.NodeView || require('./node-view').init(TRANSITION_DURATION).View;
  const getDependencyView = () => overrides.DependencyView || require('./dependency-view').init(TRANSITION_DURATION).View;
  const getVisualizationStyles = () => overrides.visualizationStyles || require('./visualization-styles').fromEmbeddedStyleSheet();
  const getCalculateTextWidth = () => overrides.calculateTextWidth || require('./text-width-calculator');

  return init(getNodeView, getDependencyView, getVisualizationStyles, getCalculateTextWidth);
};