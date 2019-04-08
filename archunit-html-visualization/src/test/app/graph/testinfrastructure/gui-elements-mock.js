'use strict';

const svg = require('./svg-mock');
const document = require('./document-mock');
const window = require('./window-mock');
const createVisualizationStylesMock = require('./visualization-styles-mock').createVisualizationStylesMock;

const getEmbeddedVisualizationStyles = () => createVisualizationStylesMock();

module.exports = {getEmbeddedVisualizationStyles, svg, document, window};