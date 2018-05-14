'use strict';

const resources = require('./resources');
const appContext = require('./app-context').newInstance();
module.exports.createGraph = (svgElement, foldAllNodes) => require('./graph').create(appContext, resources, svgElement, foldAllNodes);
module.exports.d3 = require('d3');