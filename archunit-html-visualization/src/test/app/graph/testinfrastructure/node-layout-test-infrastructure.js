'use strict';

const chai = require('chai');
const expect = chai.expect;
require('./node-layout-chai-extensions');

const createTreeFromNodeFullNames = require('./node-fullnames-to-tree-transformer').createTreeFromNodeFullNames;

const createMapWithFullNamesToSvgs = require('./node-test-infrastructure').createMapWithFullNamesToSvgs;

const testLayoutOnRoot = (root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(root._view.svgElement);

  const doTest = (doTestOnNode) => {
    const res = {
      where: filter => {
        const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());
        const doCheckRecursively = node => {
          if (filter(node)) {
            doTestOnNode(node);
          }
          node.children.forEach(doCheckRecursively);
        };
        doCheckRecursively(treeRoot);
      },
      everywhere: () => {
        res.where(() => true);
      }
    };

    return {
      skipRoot: {
        where: filter => res.where(node => filter(node) && node.fullName !== 'default'),
        everywhere: () => res.where(node => node.fullName !== 'default')
      },
      where: filter => res.where(filter),
      everywhere: () => res.everywhere()
    };
  };

  const testApi = {
    allNodes: {
      areWithinTheirParentWithRespectToPadding: (padding) => {
        const checkThatChildrenAreWithin = parentNode => {
          const parentSvg = fullNameToSvgElementMap.get(parentNode.fullName);
          parentNode.children.forEach(child => expect(fullNameToSvgElementMap.get(child.fullName)).to.beWithin(parentSvg, padding));
        };

        doTest(checkThatChildrenAreWithin).skipRoot.everywhere();
        return and;
      },

      havePaddingToTheirSiblings: (padding) => {
        const checkThatSiblingsHavePadding = parentNode => {
          const siblingSvgGroups = parentNode.children.map(child => fullNameToSvgElementMap.get(child.fullName));
          siblingSvgGroups.forEach((nodeSvg, index) =>
            siblingSvgGroups.slice(index + 1).forEach(otherNodeSvg =>
              expect(nodeSvg).to.havePaddingTo(otherNodeSvg, 2 * padding)));
        };

        doTest(checkThatSiblingsHavePadding).everywhere();
        return and;
      },

      haveTheirLabelWithinNode: () => {
        const checkThatLabelIsWithinNode = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelWithinCircle();
        doTest(checkThatLabelIsWithinNode).skipRoot.everywhere();
        return and;
      }
    },
    leaves: {
      haveTheirLabelInTheMiddle: () => {
        const checkThatLeavesHaveLabelInTheMiddle = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelInTheMiddle();
        doTest(checkThatLeavesHaveLabelInTheMiddle).skipRoot.where(node => node.children.length === 0);
        return and;
      }
    },
    innerNodes: {
      haveTheirLabelAtTheTop: () => {
        const checkThatInnerNodesHaveTheirLabelAtTop = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelAtTop();
        doTest(checkThatInnerNodesHaveTheirLabelAtTop).skipRoot.where(node => node.children.length !== 0);
        return and;
      },
      withOnlyOneChild: {
        haveTheirLabelAboveTheChildNode: () => {
          const checkThatInnerNodesWithOnlyOneChildHaveTheirLabelAboveChildNode =
            node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelAboveOtherCircle(fullNameToSvgElementMap.get(node.children[0].fullName));
          doTest(checkThatInnerNodesWithOnlyOneChildHaveTheirLabelAboveChildNode)
            .skipRoot.where(node => node.children.length === 1);
          return and;
        }
      }
    }
  };

  const that = {
    that: testApi
  };

  const and = {
    and: that
  };

  return that;
};

module.exports.testWholeLayoutOn = (root, circlePadding) => {
  testLayoutOnRoot(root)
    .that.allNodes.areWithinTheirParentWithRespectToPadding(circlePadding)
    .and.that.allNodes.havePaddingToTheirSiblings(circlePadding)
    .and.that.allNodes.haveTheirLabelWithinNode()
    .and.that.innerNodes.haveTheirLabelAtTheTop()
    .and.that.leaves.haveTheirLabelInTheMiddle()
    .and.that.innerNodes.withOnlyOneChild.haveTheirLabelAboveTheChildNode();
};

module.exports.testLayoutOnRoot = testLayoutOnRoot;