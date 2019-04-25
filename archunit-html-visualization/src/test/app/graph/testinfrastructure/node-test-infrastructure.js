'use strict';

//TODO: remove this file

const chai = require('chai');
const expect = chai.expect;
require('./node-chai-extensions');

const createTreeFromNodeFullNames = require('./node-fullnames-to-tree-transformer').createTreeFromNodeFullNames;

const createMapWithFullNamesToSvgs = require('./node-gui-adapter').createMapWithFullNamesToSvgs;

const getLeavesFromTree = node => {
  if (node.children.length === 0 && node.fullName !== 'default') {
    return [node.fullName];
  } else {
    return [].concat.apply([], node.children.map(child => getLeavesFromTree(child)));
  }
};

//FIXME: rename to clarify that this is only for accessing the drawn nodes
const testOnRoot = (root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(root._view.svgElement);
  const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());

  const testApi = {
    it: {
      hasClasses: (...expectedLeafFullNames) => {
        expect(getLeavesFromTree(treeRoot)).to.have.members(expectedLeafFullNames);
        return and;
      }
    },
    nodeWithFullName: (nodeFullName) => ({
      is: {
        markedAs: {
          foldable: () => {
            expect(fullNameToSvgElementMap.get(nodeFullName)).to.be.foldable();
            return and;
          },

          unfoldable: () => {
            expect(fullNameToSvgElementMap.get(nodeFullName)).to.be.unfoldable();
            return and;
          }
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