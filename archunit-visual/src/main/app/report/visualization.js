'use strict';

const resources = require('./resources');
const appContext = require('./app-context').newInstance();
module.exports.createGraph = svgElement => require('./graph').create(appContext, resources, svgElement);
module.exports.d3 = require('d3');