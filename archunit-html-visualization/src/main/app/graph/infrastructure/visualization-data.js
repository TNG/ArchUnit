'use strict';

module.exports = class {
  static get jsonGraph() {
    return JSON.parse(window.jsonGraph);
  }

  static get jsonViolations() {
    return JSON.parse(window.jsonViolations);
  }
};