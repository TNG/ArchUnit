'use strict';

const chai = require('chai');
const expect = chai.expect;

const createTreeFromNodeFullNames = require('./node-fullnames-to-tree-transformer').createTreeFromNodeFullNames;

const createMapWithFullNamesToSvgs = svgElement => {
  const svgGroupsWithAVisibleCircle = svgElement.getAllGroupsContainingAVisibleElementOfType('circle');
  return new Map(svgGroupsWithAVisibleCircle.map(svgGroup => [svgGroup.getAttribute('id'), svgGroup]));
};

const getLeavesFromTree = node => {
  if (node.children.length === 0) {
    return [node.fullName];
  } else {
    return [].concat.apply([], node.children.map(child => getLeavesFromTree(child)));
  }
};

const checkOn = (root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(root._view.svgElement);
  const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());

  const testApi = {
    it: {
      hasOnlyVisibleLeaves: (...expectedLeafFullNames) => {
        const actualLeaves = getLeavesFromTree(treeRoot);
        expect(actualLeaves).to.have.members(expectedLeafFullNames);
        return and;
      }
    },
    nodeWithFullName: (nodeFullName) => ({})
  };

  const that = {
    that: testApi
  };

  const and = {
    and: that
  };

  return that;
};

module.exports.checkOnRoot = checkOn;