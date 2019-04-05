'use strict';

const chai = require('chai');
const expect = chai.expect;
require('../testinfrastructure/node-layout-chai-extensions');
const guiElementsMock = require('../testinfrastructure/gui-elements-mock');

const createJsonFromClassNames = require('../testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;
const createTreeFromNodeFullNames = require('../testinfrastructure/node-fullnames-to-tree-transformer').createTreeFromNodeFullNames;

const AppContext = require('../../../../main/app/graph/app-context');

let circlePadding = 10;
let nodeFontSize = 14;
guiElementsMock.initVisualizationStyles(circlePadding, nodeFontSize);
const Root = AppContext.newInstance({guiElements: guiElementsMock}).getRoot();

const createRootWithLayout = async jsonRoot => {
  const root = new Root(jsonRoot, null, () => Promise.resolve(), () => void 0, () => void 0);
  let onLayoutChangedWasCalled = false;
  root.addListener({onLayoutChanged: () => onLayoutChangedWasCalled = true});
  root.getLinks = () => [];
  root.relayoutCompletely();
  await root._updatePromise;
  return {
    root,
    onLayoutChangedWasCalled: () => onLayoutChangedWasCalled
  }
};

const createMapWithFullNamesToSvgs = svgElement => {
  const svgGroupsWithAVisibleCircle = svgElement.getAllGroupsContainingAVisibleElementOfType('circle');
  return new Map(svgGroupsWithAVisibleCircle.map(svgGroup => [svgGroup.getAttribute('id'), svgGroup]));
};

const checkOn = (root) => {
  const fullNameToSvgElementMap = createMapWithFullNamesToSvgs(root._view.svgElement);

  const doCheck = (doCheckOnNode) => {
    const res = {
      where: filter => {
        const treeRoot = createTreeFromNodeFullNames(...fullNameToSvgElementMap.keys());
        const doCheckRecursively = node => {
          if (filter(node)) {
            doCheckOnNode(node);
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

  return {
    that: {
      allNodes: {
        areWithinTheirParentWithRespectToPadding: (padding) => {
          const checkThatChildrenAreWithin = parentNode => {
            const parentSvg = fullNameToSvgElementMap.get(parentNode.fullName);
            parentNode.children.forEach(child => expect(fullNameToSvgElementMap.get(child.fullName)).to.beWithin(parentSvg, padding));
          };

          doCheck(checkThatChildrenAreWithin).skipRoot.everywhere();
        },

        havePaddingToTheirSiblings: (padding) => {
          const checkThatSiblingsHavePadding = parentNode => {
            const siblingSvgGroups = parentNode.children.map(child => fullNameToSvgElementMap.get(child.fullName));
            siblingSvgGroups.forEach((nodeSvg, index) =>
              siblingSvgGroups.slice(index + 1).forEach(otherNodeSvg =>
                expect(nodeSvg).to.havePaddingTo(otherNodeSvg, padding)));
          };

          doCheck(checkThatSiblingsHavePadding).everywhere();
        },

        haveTheirLabelWithinNode: () => {
          const checkThatLabelIsWithinNode = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelWithinCircle();
          doCheck(checkThatLabelIsWithinNode).skipRoot.everywhere();
        }
      },
      leaves: {
        haveTheirLabelInTheMiddle: () => {
          const checkThatLeavesHaveLabelInTheMiddle = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelInTheMiddle();
          doCheck(checkThatLeavesHaveLabelInTheMiddle).skipRoot.where(node => node.children.length === 0);
        }
      },
      innerNodes: {
        haveTheirLabelAtTheTop: () => {
          const checkThatInnerNodesHaveTheirLabelAtTop = node => expect(fullNameToSvgElementMap.get(node.fullName)).to.haveLabelAtTop();
          doCheck(checkThatInnerNodesHaveTheirLabelAtTop).skipRoot.where(node => node.children.length !== 0);
        }
      }
    },
  }
};

describe('Node layout', () => {
  describe('positions the nodes, so that', () => {
    it('they are within their parent and have a padding to it', async () => {
      const jsonRoot = createJsonFromClassNames('com.pkg1.SomeClass1$SomeInnerClass', 'com.pkg1.SomeClass2$SomeInnerClass1',
        'com.pkg1.SomeClass2$SomeInnerClass2', 'com.pkg2.SomeClass');
      const {root} = await createRootWithLayout(jsonRoot);
      checkOn(root).that.allNodes.areWithinTheirParentWithRespectToPadding(circlePadding);
    });

    it('they have a padding to their sibling nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1$SomeInnerClass1', 'pkg1.SomeClass1$SomeInnerClass2',
        'pkg1.SomeClass1$SomeInnerClass3', 'pkg1.SomeClass2', 'pkg1.SomeClass3', 'pkg2.SomeClass');
      const {root} = await createRootWithLayout(jsonRoot);
      checkOn(root).that.allNodes.havePaddingToTheirSiblings(circlePadding);
    });
  });

  describe('positions the labels of the nodes, so that', () => {
    it('the labels are within the nodes', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClassWithAVeryLongName', 'pkg1.SomeClass', 'somePkgWithAVeryLongName.SomeClass');
      const {root} = await createRootWithLayout(jsonRoot);
      checkOn(root).that.allNodes.haveTheirLabelWithinNode();
    });

    it('the labels of the leaves are in the middle', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass', 'pkg3');
      const {root} = await createRootWithLayout(jsonRoot);
      checkOn(root).that.leaves.haveTheirLabelInTheMiddle();
    });

    it('the labels of the inner nodes are at the top', async () => {
      const jsonRoot = createJsonFromClassNames('pkg1.SomeClass1', 'pkg1.SomeClass2$SomeInnerClass');
      const {root} = await createRootWithLayout(jsonRoot);
      checkOn(root).that.innerNodes.haveTheirLabelAtTheTop();
    });
  });

  it('notifies its listeners about the changed layout', async () => {
    const jsonRoot = createJsonFromClassNames('com.pkg.SomeClass');
    const {onLayoutChangedWasCalled} = await createRootWithLayout(jsonRoot);
    expect(onLayoutChangedWasCalled()).to.be.true;
  });
});