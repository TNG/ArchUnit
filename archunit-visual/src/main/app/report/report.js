'use strict';

import createGraph from './visualization';
import * as d3 from 'd3';
// eslint-disable-next-line no-unused-vars
import visualizationstyles from './visualizationstyles.css';

window.addEventListener('WebComponentsReady', () => {
  const graph = createGraph(d3.select('#visualization').node(), true);
  graph.attachToMenu(document.querySelector('#menu'));
  graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
});