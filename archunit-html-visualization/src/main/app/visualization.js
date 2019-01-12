'use strict';

const d3 = require('d3');
const appContext = require('./graph/app-context').newInstance();
const Graph = require('./graph/graph').init(appContext);

// We need this style tag to have a specific ID, which is only possible if we do an inline via Webpack (compare webpack.config.js)
require('./graph/visualization-styles.css');

window.addEventListener('WebComponentsReady', () => {
  const graph = Graph.create(d3.select('#visualization').node());
  graph.attachToMenu(document.querySelector('#menu'));
  graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
});