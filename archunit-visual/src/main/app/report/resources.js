'use strict';

const d3 = require('d3');

const loadJsonResource = fileName => new Promise((resolve, reject) => {
  d3.json(fileName, function (error, json) {
    if (error) {
      return reject(error);
    }
    resolve(json);
  });
});

module.exports.resources = {
  getClassesToVisualize: () => loadJsonResource('80/classes.json'),
  getViolations: () => loadJsonResource('80/violations.json')
};