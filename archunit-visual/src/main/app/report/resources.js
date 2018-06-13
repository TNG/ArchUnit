'use strict';

import * as d3 from 'd3';

const loadJsonResource = fileName => new Promise((resolve, reject) => {
  d3.json(fileName, function (error, json) {
    if (error) {
      reject(error);
    }
    resolve(json);
  });
});

const getClassesToVisualize = () => loadJsonResource('classes.json');
const getViolations = () => loadJsonResource('violations.json');

const getJsonResources = () => getClassesToVisualize().then(jsonRoot =>
  getViolations().then(violations => Promise.resolve({jsonRoot, violations})));

export default {getJsonResources};