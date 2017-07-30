'use strict';

const d3 = require('d3');

module.exports.visualizationStyleSheet = () => d3.select('#visualization-styles').property('sheet');
module.exports.textSizeComputationSvg = () => d3.select('#text-size-computation');