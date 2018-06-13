'use strict';

import * as d3 from 'd3';

const getVisualizationStyleSheet = () => d3.select('#visualization-styles').property('sheet');
const getTextSizeComputationSvg = () => d3.select('#text-size-computation');

export {getVisualizationStyleSheet, getTextSizeComputationSvg};