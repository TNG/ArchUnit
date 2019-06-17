"use strict";

/*
 * Some poor man's DI solution...
 */

const visualizationFunctions = require('./visualization-functions');
const dependencyVisualizationFunctions = require('./dependency-visualization-functions');
const node = require('./nodes/node');
const dependency = require('./dependencies/dependency');
const dependencies = require('./dependencies/dependencies');
const guiElements = require('./infrastructure/gui-elements');
const nodeView = require('./nodes/node-view');
const rootView = require('./nodes/root-view');
const detailedDependencyView = require('./dependencies/detailed-dependency-view');
const dependencyView = require('./dependencies/dependency-view');
const graphView = require('./graph-view');
const visualizationData = require('./infrastructure/visualization-data');

const init = (getNodeView, getRootView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getVisualizationData) => {

  const result = {
    getVisualizationFunctions: () => visualizationFunctions.newInstance(),
    getDependencyVisualizationFunctions: () => dependencyVisualizationFunctions.newInstance(),
    getRoot: () => node.init(getNodeView(), getRootView(), result.getVisualizationFunctions(), getVisualizationStyles()),
    getDetailedDependencyView,
    getDependencyCreator: () => dependency.init(getDependencyView(), result.getDetailedDependencyView(), result.getDependencyVisualizationFunctions()),
    getDependencies: () => dependencies.init(() => result.getDependencyCreator()),
    getVisualizationStyles,
    getGraphView,
    getVisualizationData
  };

  return result;
};

const TRANSITION_DURATION = 1000;
const TEXT_PADDING = 5;

module.exports = {
  newInstance: overrides => {
    overrides = overrides || {};

    const transitionDuration = overrides.transitionDuration || TRANSITION_DURATION;
    const textPadding = overrides.textPadding || TEXT_PADDING;

    const guiElementsInstance = overrides.guiElements || guiElements;

    const getVisualizationStyles = () => guiElementsInstance.getEmbeddedVisualizationStyles();
    const getNodeView = () => overrides.NodeView || nodeView.init(transitionDuration, guiElementsInstance.svg);
    const getRootView = () => overrides.RootView || rootView.init(transitionDuration, guiElementsInstance.svg, guiElementsInstance.document);
    const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(transitionDuration, guiElementsInstance.svg, getVisualizationStyles(), textPadding);
    const getDependencyView = () => overrides.DependencyView || dependencyView.init(transitionDuration);
    const getGraphView = () => overrides.GraphView || graphView.init(transitionDuration, guiElementsInstance.svg, guiElementsInstance.document, guiElementsInstance.window);
    const getVisualizationData = () => overrides.visualizationData || visualizationData;

    return init(getNodeView, getRootView, getDependencyView, getDetailedDependencyView, getGraphView, getVisualizationStyles, getVisualizationData);
  }
};