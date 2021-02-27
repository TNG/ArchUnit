"use strict";

/*
 * Some poor man's DI solution...
 */

// const dependencyVisualizationFunctions = require('./dependency-visualization-functions');
import {init as nodeInit, RootFactory} from './node/node';
// const dependency = require('./dependencies/dependency');
// const dependencies = require('./dependencies/dependencies');
import {getEmbeddedVisualizationStyles, svg, documentInit, windowInit} from './infrastructure/gui-elements';
import {init as nodeViewInit, NodeViewFactory} from './node/node-view';
import {init as rootViewInit, RootViewFactory} from './node/root-view';
import {init as graphViewInit, GraphViewFactory} from "./graph-view";
import {VisualizationStyles} from "./visualization-styles";
import {init as visualizationDataInit, VisualizationData} from "./infrastructure/visualization-data";
// const detailedDependencyView = require('./dependencies/detailed-dependency-view');
// const dependencyView = require('./dependencies/dependency-view');
// const graphView = require('./graph-view');
// const visualizationData = require('./infrastructure/visualization-data');

const TRANSITION_DURATION = 1000;
// const TEXT_PADDING = 5;

interface AppContextOverrides {
  nodeViewFactory?: NodeViewFactory
  rootViewFactory?: RootViewFactory
  graphViewFactory?: GraphViewFactory
  transitionDuration?: number
  visualizationData?: VisualizationData
}


interface AppContext {
  getRoot(): RootFactory,
  getGraphView(): GraphViewFactory,
  visualizationStyles: VisualizationStyles
  visualizationData: VisualizationData
}

const init = (getNodeViewFactory: () => NodeViewFactory, getRootViewFactory: () => RootViewFactory, getGraphViewFactory: () => GraphViewFactory, getVisualizationStyles: () => VisualizationStyles, getVisualizationData: () => VisualizationData): AppContext => {

  return {
    // getDependencyVisualizationFunctions: () => dependencyVisualizationFunctions.newInstance(),
    getRoot: () => nodeInit(getNodeViewFactory(), getRootViewFactory(), getVisualizationStyles()),
    // getDependencyView,
    // getDetailedDependencyView,
    // getDependencyCreator: () => dependency.init(getDependencyView(), result.getDetailedDependencyView(), result.getDependencyVisualizationFunctions()),
    // getDependencies: () => dependencies.init(() => result.getDependencyCreator()),
    visualizationStyles: getVisualizationStyles(),
    getGraphView: getGraphViewFactory,
    visualizationData: getVisualizationData()
    // getVisualizationData
  };
};

class AppContextFactory {
  public newInstance(overrides: AppContextOverrides): AppContext {
    overrides = overrides || {};

    const transitionDuration = overrides.transitionDuration || TRANSITION_DURATION;
    // const textPadding = overrides.textPadding || TEXT_PADDING;

    // const guiElementsInstance = overrides.guiElements || guiElements;

    const getVisualizationStyles = () => getEmbeddedVisualizationStyles();
    const getNodeViewFactory = () => overrides.nodeViewFactory || nodeViewInit(transitionDuration, svg);
    const getRootViewFactory = () => overrides.rootViewFactory || rootViewInit(transitionDuration, svg/*, guiElementsInstance.document*/);
    // const getDetailedDependencyView = () => overrides.DetailedDependencyView || detailedDependencyView.init(transitionDuration, guiElementsInstance.svg, getVisualizationStyles(), textPadding);
    // const getDependencyView = () => overrides.DependencyView || dependencyView.init(transitionDuration);
    const getGraphViewFactory = () => overrides.graphViewFactory || graphViewInit(transitionDuration, svg, documentInit, windowInit);
    const getVisualizationData = () => overrides.visualizationData || visualizationDataInit();

    return init(getNodeViewFactory, getRootViewFactory, getGraphViewFactory, getVisualizationStyles, getVisualizationData);
  }
}

export {AppContext, AppContextFactory}
