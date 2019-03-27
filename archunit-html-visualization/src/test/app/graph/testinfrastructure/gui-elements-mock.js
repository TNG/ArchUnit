'use strict';

const svg = require('./svg-mock');
const document = require('./document-mock');
const window = require('./window-mock');
const visualizationStylesLoaderMock = require('./stubs').visualizationStylesStub;

const getEmbeddedVisualizationStyles = (circlePadding = 1, nodeFontSize = 10) => visualizationStylesLoaderMock(circlePadding, nodeFontSize);

module.exports = {getEmbeddedVisualizationStyles, svg, document, window};