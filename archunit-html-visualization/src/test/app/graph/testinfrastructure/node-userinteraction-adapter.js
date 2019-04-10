'use strict';

const createMapWithFullNamesToSvgs = require('./node-test-infrastructure').createMapWithFullNamesToSvgs;

const testGuiFromSvgElement = svgElement => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(svgElement);

  const getCircleByFullName = nodeFullName => {
    const nodeSvgElement = fullNameToSvgElementMap.get(nodeFullName);
    return nodeSvgElement.getVisibleSubElementOfType('circle');
  };

  return {
    clickNode: nodeFullName => {
      getCircleByFullName(nodeFullName).click();
    },

    ctrlClickNode: nodeFullName => {
      getCircleByFullName(nodeFullName).click({ctrlKey: true});
    }
  };
};

const testGuiFromRoot = root => {
  return testGuiFromSvgElement(root._view.svgElement);
};

module.exports.testGuiFromSvgElement = testGuiFromSvgElement;
module.exports.testGuiFromRoot = testGuiFromRoot;