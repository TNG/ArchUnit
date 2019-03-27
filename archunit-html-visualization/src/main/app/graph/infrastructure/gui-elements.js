'use strict';

const d3 = require('d3');
const svg = require('./svg');
const document = require('./document');
const window = require('./window');
const visualizationStylesLoader = require('../visualization-styles');

const getEmbeddedVisualizationStyles = () => visualizationStylesLoader.from(d3.select('#visualization-styles').property('sheet'));

module.exports = {getEmbeddedVisualizationStyles, svg, document, window};