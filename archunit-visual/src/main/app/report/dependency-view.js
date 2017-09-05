'use strict';

//const d3 = require('d3');

const init = (transitionDuration) => {

  const View = class {
    constructor(parentSvgElement, dependency) {
      dependency.anyProperty = transitionDuration;
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});