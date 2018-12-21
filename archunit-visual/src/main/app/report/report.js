'use strict';

const createGraph = require('./visualization');
const d3 = require('d3');
// We need this style tag to have a specific ID, which is only possible if we do an inline via Webpack (compare webpack.config.js)
require('./visualization-styles.css');

window.addEventListener('WebComponentsReady', () => {
  const graph = createGraph(d3.select('#visualization').node(), true);
  graph.attachToMenu(document.querySelector('#menu'));
  graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
});