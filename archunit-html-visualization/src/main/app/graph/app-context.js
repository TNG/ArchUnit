"use strict";

/*
 * Some poor man's DI solution...
 */

// const dependencyVisualizationFunctions = require('./dependency-visualization-functions');
import {init as nodeInit} from './node/node';
// const dependency = require('./dependencies/dependency');
// const dependencies = require('./dependencies/dependencies');
import {getEmbeddedVisualizationStyles, svg, documentInit, windowInit} from './infrastructure/gui-elements';
import {init as nodeViewInit} from './node/node-view';
import {init as rootViewInit} from './node/root-view';
// const detailedDependencyView = require('./dependencies/detailed-dependency-view');
// const dependencyView = require('./dependencies/dependency-view');
const graphView = require('./graph-view');
// const visualizationData = require('./infrastructure/visualization-data');

const TRANSITION_DURATION = 1000;
// const TEXT_PADDING = 5;

const init = (getNodeView, getRootView, getGraphView, getVisualizationStyles/*, getVisualizationData*/) => {

  return {
    // getDependencyVisualizationFunctions: () => dependencyVisualizationFunctions.newInstance(),
    getRoot: () => nodeInit(getNodeView(), getRootView(), getVisualizationStyles()),
    // getDependencyView,
    // getDetailedDependencyView,
    // getDependencyCreator: () => dependency.init(getDependencyView(), result.getDetailedDependencyView(), result.getDependencyVisualizationFunctions()),
    // getDependencies: () => dependencies.init(() => result.getDependencyCreator()),
    getVisualizationStyles,
    getGraphView,
    // getVisualizationData
  };
};


module.exports = {
  newInstance: overrides => {
    overrides = overrides || {};

    const transitionDuration = overrides.transitionDuration || TRANSITION_DURATION;
    // const textPadding = overrides.textPadding || TEXT_PADDING;

    // const guiElementsInstance = overrides.guiElements || guiElements;

    const getVisualizationStyles = () => getEmbeddedVisualizationStyles();
    const getNodeView = () => overrides.NodeView || nodeViewInit(transitionDuration, svg);
    const getRootView = () => overrides.RootView || rootViewInit(transitionDuration, svg/*, guiElementsInstance.document*/);
    // const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(transitionDuration, guiElementsInstance.svg, getVisualizationStyles(), textPadding);
    // const getDependencyView = () => overrides.DependencyView || dependencyView.init(transitionDuration);
    const getGraphView = () => overrides.GraphView || graphView.init(transitionDuration, svg, documentInit, windowInit);
    // const getVisualizationData = () => overrides.visualizationData || visualizationData;

    return init(getNodeView, getRootView, getGraphView, getVisualizationStyles/*, getVisualizationData*/);
  }
};
