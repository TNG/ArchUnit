"use strict";

/*
 * Some poor man's DI solution...
 */

const visualizationFunctions = require('./visualization-functions');
const dependencyVisualizationFunctions = require('./dependency-visualization-functions');
const node = require('./nodes/node');
const dependencies = require('./dependencies/dependencies');
const guiElements = require('./infrastructure/gui-elements');
const nodeView = require('./nodes/node-view');
const rootView = require('./nodes/root-view');
const detailedDependencyView = require('./dependencies/detailed-dependency-view');
const dependencyView = require('./dependencies/dependency-view');
const graphView = require('./graph-view');
const visualizationData = require('./infrastructure/visualization-data');

const init = (getNodeView, getRootView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getVisualizationData) => {

  const getVisualizationFunctions = () => visualizationFunctions.newInstance();

  const getDependencyVisualizationFunctions = () => dependencyVisualizationFunctions.newInstance();

  const getRoot = () => node.init(getNodeView(), getRootView(), getVisualizationFunctions(), getVisualizationStyles());

  const getDependencies = () => dependencies.init(getDependencyView(), getDetailedDependencyView(), getDependencyVisualizationFunctions());

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

    const guiElementsInstance = overrides.guiElements || guiElements;

    const getVisualizationStyles = () => guiElementsInstance.getEmbeddedVisualizationStyles();
    const getNodeView = () => overrides.NodeView || nodeView.init(TRANSITION_DURATION, guiElementsInstance.svg);
    const getRootView = () => overrides.RootView || rootView.init(TRANSITION_DURATION, guiElementsInstance.svg, guiElementsInstance.document);
    const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(TRANSITION_DURATION, guiElementsInstance.svg, getVisualizationStyles(), 5);
    const getDependencyView = () => overrides.DependencyView || dependencyView.init(TRANSITION_DURATION);
    const getGraphView = () => overrides.GraphView || graphView.init(TRANSITION_DURATION, guiElementsInstance.svg, guiElementsInstance.document, guiElementsInstance.window);
    const getVisualizationData = () => overrides.visualizationData || visualizationData;

    return init(getNodeView, getRootView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getVisualizationData);
  }
};