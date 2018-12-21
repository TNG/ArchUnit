'use strict';

const resources = require('./resources');
const appContext = require('./app-context');
const createGraph = require('./graph').create;

module.exports = (svgElement, foldAllNodes) => createGraph(appContext.newInstance(), resources, svgElement, foldAllNodes);