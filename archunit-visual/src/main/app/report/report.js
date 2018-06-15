'use strict';

import createGraph from './visualization';
import * as d3 from 'd3';
import visualizationstyles from './visualizationstyles.css';

const webComponentsInitialized = new Promise(resolve => {
  window.addEventListener('WebComponentsReady', function () {
    resolve();
  });
});

Promise.all([
  createGraph(d3.select('#visualization').node(), true),
  webComponentsInitialized
]).then(results => {
  const graph = results[0];
  graph.attachToMenu(document.querySelector('#menu'));
  graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
});