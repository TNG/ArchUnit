"use strict";

/*
 * Some poor man's DI solution...
 */

const visualizationFunctions = require('./visualization-functions');
const nodeText = require('./node-text');
const node = require('./node');
const dependencies = require('./dependencies');
const textWidthCalculator = require('./text-width-calculator');
const visualizationStyles = require('./visualization-styles');
const nodeView = require('./node-view');
const detailedDependencyView = require('./detailed-dependency-view');
const dependencyView = require('./dependency-view');
const graphView = require('./graph-view');

const init = (getNodeView, getDependencyView, getGraphView, getVisualizationStyles) => {

  const getVisualizationFunctions = () => visualizationFunctions.newInstance();

  const getNodeText = () => nodeText.init(getVisualizationStyles());

  const getRoot = () => node.init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles());

  const getDependencies = () => dependencies.init(getDependencyView());

  return {
    getVisualizationStyles,
    getRoot,
    getDependencies,
    getGraphView,
  }
};

const TRANSITION_DURATION = 1000;

module.exports = {
  newInstance: overrides => {
    overrides = overrides || {};

    const getCalculateTextWidth = () => overrides.calculateTextWidth || textWidthCalculator;
    const getVisualizationStyles = () => overrides.visualizationStyles || visualizationStyles.fromEmbeddedStyleSheet();
    const getNodeView = () => overrides.NodeView || nodeView.init(TRANSITION_DURATION);
    const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(TRANSITION_DURATION, getCalculateTextWidth(), getVisualizationStyles());
    const getDependencyView = () => overrides.DependencyView || dependencyView.init(getDetailedDependencyView(), TRANSITION_DURATION);
    const getGraphView = () => overrides.GraphView || graphView.init(TRANSITION_DURATION);

    return init(getNodeView, getDependencyView, getGraphView, getVisualizationStyles);
  }
};