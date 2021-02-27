'use strict';

const d3 = require('d3');
import {AppContextFactory} from "./graph/app-context";
import {init as graphInit} from "./graph/graph"

const graphFactory = graphInit(new AppContextFactory().newInstance(null));

// We need this style tag to have a specific ID, which is only possible if we do an inline via Webpack (compare webpack.config.js)
require('./graph/visualization-styles.css');

// window.addEventListener('onload', () => {
  const graph = graphFactory.create(d3.select('#visualization').node(), d3.select('#container').node());
//   graph.attachToMenu(document.querySelector('#menu'));
//   graph.attachToViolationMenu(document.querySelector('#violations'));
  document.body.onresize = graph.render;
// });
