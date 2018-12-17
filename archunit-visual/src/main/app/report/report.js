'use strict';

import createGraph from './visualization';
import * as d3 from 'd3';
// We need this style tag to have a specific ID, which is only possible if we do an inline via Webpack (compare webpack.config.js)
// eslint-disable-next-line no-unused-vars
import not_directly_referenced from './visualization-styles.css';

window.addEventListener('WebComponentsReady', () => {
  const graph = createGraph(d3.select('#visualization').node(), true);
  graph.attachToMenu(document.querySelector('#menu'));
  graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
});