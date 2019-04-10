'use strict';

const chai = require('chai');
const expect = chai.expect;
require('./node-chai-extensions');

const createTreeFromNodeFullNames = require('./node-fullnames-to-tree-transformer').createTreeFromNodeFullNames;

const createMapWithFullNamesToSvgs = svgElement => {
  const svgGroupsWithAVisibleCircle = svgElement.getAllGroupsContainingAVisibleElementOfType('circle');
  return new Map(svgGroupsWithAVisibleCircle.map(svgGroup => [svgGroup.getAttribute('id'), svgGroup]));
};

const getLeavesFromTree = node => {
  if (node.children.length === 0 && node.fullName !== 'default') {
    return [node.fullName];
  } else {
    return [].concat.apply([], node.children.map(child => getLeavesFromTree(child)));
  }
};

const testOnRoot = (root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(root._view.svgElement);
  const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());

  const testApi = {
    it: {
      hasOnlyVisibleLeaves: (...expectedLeafFullNames) => {
        expect(getLeavesFromTree(treeRoot)).to.have.members(expectedLeafFullNames);
        return and;
      },
      hasNoVisibleNodes: () => {
        expect(getLeavesFromTree(treeRoot)).to.be.empty;
        return and;
      }
    },
    nodeWithFullName: (nodeFullName) => ({
      is: {
        foldable: () => {
          expect(fullNameToSvgElementMap.get(nodeFullName)).to.be.foldable();
          return and;
        },

        unfoldable: () => {
          expect(fullNameToSvgElementMap.get(nodeFullName)).to.be.unfoldable();
          return and;
        }
      }
    })
  };

  const that = {
    that: testApi
  };

  const and = {
    and: that
  };

  return that;
};

module.exports.testOnRoot = testOnRoot;

module.exports.createMapWithFullNamesToSvgs = createMapWithFullNamesToSvgs;