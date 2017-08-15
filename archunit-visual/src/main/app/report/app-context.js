"use strict";

/*
 * Some poor man's DI solution...
 */

const init = (getVisualizationStyles, getCalculateTextWidth) => {

  const getVisualizationFunctions = () => {
    return require('./visualization-functions').newInstance(getCalculateTextWidth());
  };

  const getGraphVisualizer = () => {
    return require('./graph-visualizer').newInstance(require('./dependencies-visualizer'))
  };

  const getNodeText = () => require('./node-text').init(getVisualizationStyles(), getCalculateTextWidth());

  const getJsonToRoot = () => {
    const jsonToDependencies = require('./dependencies.js').jsonToDependencies;

    return require('./tree').init(getNodeText(), getVisualizationFunctions(), getVisualizationStyles(), jsonToDependencies).jsonToRoot;
  };

  const getJsonToGraph = () => {
    return require('./graph').init(getJsonToRoot(), getGraphVisualizer()).jsonToGraph
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

  const getVisualizationStyles = () => overrides.visualizationStyles || require('./visualization-styles').fromEmbeddedStyleSheet();
  const getCalculateTextWidth = () => overrides.calculateTextWidth || require('./text-width-calculator');

  return init(getVisualizationStyles, getCalculateTextWidth);
};