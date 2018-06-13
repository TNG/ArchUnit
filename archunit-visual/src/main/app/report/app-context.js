"use strict";

/*
 * Some poor man's DI solution...
 */

import visualizationFunctions from './visualization-functions';
import nodeText from './node-text';
import tree from './tree';
import dependencies from './dependencies';
import textWidthCalculator from './text-width-calculator';
import visualizationStyles from './visualization-styles';
import nodeView from './node-view';
import detailedDependencyView from './detailed-dependency-view';
import dependencyView from './dependency-view';
import graphView from './graph-view';

const init = (getNodeView, getDependencyView, getGraphView, getVisualizationStyles) => {

  const getVisualizationFunctions = () => visualizationFunctions.newInstance();

  const getNodeText = () => nodeText.init(getVisualizationStyles());

  const getRoot = () => tree.init(getNodeView(), getNodeText(), getVisualizationFunctions(), getVisualizationStyles());

  const getDependencies = () => dependencies.init(getDependencyView());

  return {
    getVisualizationStyles,
    getRoot,
    getDependencies,
    getGraphView,
  }
};

const TRANSITION_DURATION = 1000;

export default {
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