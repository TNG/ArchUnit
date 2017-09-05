"use strict";

/*
 * Some poor man's DI solution...
 */

const init = (getNodeView, getDependencyView, getVisualizationStyles, getCalculateTextWidth) => {

  const getVisualizationFunctions = () => {
    return require('./visualization-functions').newInstance(getCalculateTextWidth());
  };

  const getNodeText = () => require('./node-text').init(getVisualizationStyles(), getCalculateTextWidth());

  const getJsonToRoot = () => {
    const jsonToDependencies = require('./dependencies').init(getDependencyView()).jsonToDependencies;

    return require('./tree').init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles(), jsonToDependencies).jsonToRoot;
  };

  const getJsonToGraph = () => {
    return require('./graph').init(getJsonToRoot()).jsonToGraph
  };

  return {
    getVisualizationFunctions,
    getVisualizationStyles,
    getJsonToRoot,
    getJsonToGraph
  }
};

module.exports.newInstance = overrides => {
  overrides = overrides || {};

  const TRANSITION_DURATION = 300;
  const getNodeView = () => overrides.NodeView || require('./node-view').init(TRANSITION_DURATION).View;
  const getDependencyView = () => overrides.DependencyView || require('./dependency-view').init(TRANSITION_DURATION).View;
  const getVisualizationStyles = () => overrides.visualizationStyles || require('./visualization-styles').fromEmbeddedStyleSheet();
  const getCalculateTextWidth = () => overrides.calculateTextWidth || require('./text-width-calculator');

  return init(getNodeView, getDependencyView, getVisualizationStyles, getCalculateTextWidth);
};