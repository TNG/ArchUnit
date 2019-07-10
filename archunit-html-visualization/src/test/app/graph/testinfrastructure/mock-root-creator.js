'use strict';

const createJsonFromClassNames = require('./class-names-to-json-transformer').createJsonFromClassNames;
const initNodeMock = require('./node-mock').init;

const createMockRootFromClassNames = (...args) => {
  let classNames = args;
  let svgContainerElement = null;
  if (args.length > 0) {
    const potentialSvgContainerElement = args[args.length - 1];
    if (typeof potentialSvgContainerElement !== 'string') {
      svgContainerElement = potentialSvgContainerElement;
      classNames = args.slice(0, args.length - 1);
    }
  }

  const jsonRoot = createJsonFromClassNames(...classNames);
  const mockRoot = new (initNodeMock())(jsonRoot, svgContainerElement);
  return mockRoot;
};

module.exports = {createMockRootFromClassNames};