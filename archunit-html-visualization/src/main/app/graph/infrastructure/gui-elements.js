'use strict';

const d3 = require('d3');
const SvgSelection = require('./svg-selection');

const getVisualizationStyleSheet = () => d3.select('#visualization-styles').property('sheet');
const getTextSizeComputationSvg = () => d3.select('#text-size-computation');
const svgSelect = (domElement) => SvgSelection.fromDom(domElement);

module.exports = {getVisualizationStyleSheet, getTextSizeComputationSvg, svgSelect};