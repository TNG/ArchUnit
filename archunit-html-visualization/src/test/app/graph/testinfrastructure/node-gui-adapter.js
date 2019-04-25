'use strict';

const expect = require('chai').expect;

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

const testGuiFromSvgElement = (svgElement, root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(svgElement);
  const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());

  const getCircleByFullName = nodeFullName => {
    const nodeSvgElement = fullNameToSvgElementMap.get(nodeFullName);
    return nodeSvgElement.getVisibleSubElementOfType('circle');
  };

  const interact = {
    clickNode: nodeFullName => {
      getCircleByFullName(nodeFullName).click({ctrlKey: false});
    },
    clickNodeAndAwait: async (nodeFullName) => {
      interact.clickNode(nodeFullName);
      return root._updatePromise;
    },
    ctrlClickNode: nodeFullName => {
      getCircleByFullName(nodeFullName).click({ctrlKey: true});
    },
    dragNode: (nodeFullname, {dx, dy}) => {
      fullNameToSvgElementMap.get(nodeFullname).drag(dx, dy);
    },
    dragNodeAndAwait: async (nodeFullName, {dx, dy}) => {
      interact.dragNode(nodeFullName, {dx, dy});
      await root._updatePromise;
    }
  };

  const inspect = {
    svgElementOf: nodeFullName => {
      return fullNameToSvgElementMap.get(nodeFullName);
    },
    positionOf: nodeFullName => {
      return inspect.svgElementOf(nodeFullName).absolutePosition;
    }
  };

  const test = {
    that: {
      onlyNodesAre: (...expectedLeafFullNames) => {
        expect(getLeavesFromTree(treeRoot)).to.have.members(expectedLeafFullNames);
        return and;
      },
      node: (nodeFullName) => {
        const svgElement = fullNameToSvgElementMap.get(nodeFullName);
        return {
          is: {
            markedAs: {
              foldable: () => {
                expect([...svgElement.cssClasses]).to.include('foldable');
                expect([...svgElement.cssClasses]).not.to.include('unfoldable');
                return and;
              },

              unfoldable: () => {
                expect([...svgElement.cssClasses]).to.include('unfoldable');
                expect([...svgElement.cssClasses]).not.to.include('foldable');
                return and;
              }
            },
            atPosition: ({x, y}) => {
              expect(svgElement.absolutePosition).to.deep.equal({x, y});
            }
          }
        }
      }
    }
  };

  const and = {
    and: test
  };

  return {
    test,
    inspect,
    interact
  };
};

const testGuiFromRoot = root => {
  return testGuiFromSvgElement(root._view.svgElement, root);
};

module.exports.testGuiFromRoot = testGuiFromRoot;
module.exports.createMapWithFullNamesToSvgs = createMapWithFullNamesToSvgs;
//TODO: maybe method for graph --> this method adds to the container-div and svg, and to this svg the graph's group-element