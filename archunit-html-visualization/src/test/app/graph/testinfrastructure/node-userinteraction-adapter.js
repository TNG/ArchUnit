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
      getCircleByFullName(nodeFullName).click({ctrlKey: false});
    },

    ctrlClickNode: nodeFullName => {
      getCircleByFullName(nodeFullName).click({ctrlKey: true});
    }
  };
};

const testGuiFromRoot = root => {
  const synchronousTesGui = testGuiFromSvgElement(root._view.svgElement);
  return {
    clickNode: synchronousTesGui.clickNode,
    ctrlClickNode: synchronousTesGui.ctrlClickNode,
    clickNodeAndAwait: async (nodeFullName) => {
      synchronousTesGui.clickNode(nodeFullName);
      return root._updatePromise;
    }
  }
};

module.exports.testGuiFromSvgElement = testGuiFromSvgElement;
module.exports.testGuiFromRoot = testGuiFromRoot;