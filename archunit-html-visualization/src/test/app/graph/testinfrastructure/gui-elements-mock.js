'use strict';

const svg = require('./svg-mock');
const document = require('./document-mock');
const window = require('./window-mock');
const visualizationStylesLoaderMock = require('./stubs').visualizationStylesStub;

let _circlePadding, _nodeFontSize;

const initVisualizationStyles = (circlePadding, nodeFontSize) => {
  _circlePadding = circlePadding;
  _nodeFontSize = nodeFontSize;
};

const getEmbeddedVisualizationStyles = () => visualizationStylesLoaderMock(_circlePadding, _nodeFontSize);

module.exports = {getEmbeddedVisualizationStyles, svg, document, window, initVisualizationStyles};