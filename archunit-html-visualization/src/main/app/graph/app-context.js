"use strict";

/*
 * Some poor man's DI solution...
 */

const visualizationFunctions = require('./visualization-functions');
const nodeText = require('./nodes/node-text');
const node = require('./nodes/node');
const dependencies = require('./dependencies/dependencies');
const textWidthCalculator = require('./infrastructure/text-width-calculator');
const visualizationStyles = require('./visualization-styles');
const nodeView = require('./nodes/node-view');
const rootView = require('./nodes/root-view');
const detailedDependencyView = require('./dependencies/detailed-dependency-view');
const dependencyView = require('./dependencies/dependency-view');
const graphView = require('./graph-view');
const visualizationData = require('./infrastructure/visualization-data');

const init = (getNodeView, getRootView, getDependencyView, getGraphView, getVisualizationStyles, getVisualizationData) => {

  const getVisualizationFunctions = () => visualizationFunctions.newInstance();

  const getNodeText = () => nodeText.init(getVisualizationStyles());

  const getRoot = () => node.init(getNodeView(), getRootView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles());

  const getDependencies = () => dependencies.init(getDependencyView());

  return {
    getVisualizationStyles,
    getRoot,
    getDependencies,
    getGraphView,
    getVisualizationData
  }
};

const TRANSITION_DURATION = 1000;

module.exports = {
  newInstance: overrides => {
    overrides = overrides || {};

    const getCalculateTextWidth = () => overrides.calculateTextWidth || textWidthCalculator;
    const getVisualizationStyles = () => overrides.visualizationStyles || visualizationStyles.fromEmbeddedStyleSheet();
    const getNodeView = () => overrides.NodeView || nodeView.init(TRANSITION_DURATION);
    const getRootView = () => overrides.RootView || rootView.init(TRANSITION_DURATION);
    const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(TRANSITION_DURATION, getCalculateTextWidth(), getVisualizationStyles());
    const getDependencyView = () => overrides.DependencyView || dependencyView.init(getDetailedDependencyView(), TRANSITION_DURATION);
    const getGraphView = () => overrides.GraphView || graphView.init(TRANSITION_DURATION);
    const getVisualizationData = () => overrides.visualizationData || visualizationData;

    return init(getNodeView, getRootView, getDependencyView, getGraphView, getVisualizationStyles, getVisualizationData);
  }
};