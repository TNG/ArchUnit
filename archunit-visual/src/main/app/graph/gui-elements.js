'use strict';

const d3 = require('d3');

const getVisualizationStyleSheet = () => d3.select('#visualization-styles').property('sheet');
const getTextSizeComputationSvg = () => d3.select('#text-size-computation');

module.exports = {getVisualizationStyleSheet, getTextSizeComputationSvg};