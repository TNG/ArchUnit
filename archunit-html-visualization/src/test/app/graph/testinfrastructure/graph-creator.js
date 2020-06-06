'use strict';
const {createGraph} = require('./test-json-creator');
const AppContext = require('../../../../main/app/graph/app-context');
const GraphUi = require('./graph-ui');
const guiElementsMock = require('./gui-elements-mock');
const svgMock = require('./svg-mock');
const realInitGraph = require('../../../../main/app/graph/graph').init;

const getGraphUi = async (jsonRoot, jsonDependencies = [], violations = []) => {
  const appContext = appContextWith(createGraph(jsonRoot, jsonDependencies), violations);
  const graph = realInitGraph(appContext).create(svgMock.createSvgRoot(), svgMock.createEmptyElement());
  const graphUi = GraphUi.of(graph);

  await graphUi.waitForUpdateFinished();

  return graphUi;
};

const appContextWith = (graph, violations) => AppContext.newInstance({
  guiElements: guiElementsMock,
  visualizationData: {
    jsonGraph: graph,
    jsonViolations: violations
  }
});


module.exports = {getGraphUi};
