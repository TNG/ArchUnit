'use strict';

const d3 = require('d3');

const loadJsonResource = fileName => new Promise((resolve, reject) => {
  d3.json(fileName, function (error, json) {
    if (error) {
      reject(error);
    }
    resolve(json);
  });
});

const getClassesToVisualize = () => loadJsonResource('80/classes.json');
const getViolations = () => loadJsonResource('80/violations.json');

const getJsonResources = () => getClassesToVisualize().then(jsonRoot =>
  getViolations().then(violations => Promise.resolve({jsonRoot, violations})));

module.exports = {
  getJsonResources
};