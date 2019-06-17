'use strict';

const expect = require('chai').expect;
const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;

const sleep = (timeInMs) => {
  return new Promise(resolve => {
    setTimeout(resolve, timeInMs);
  });
};

const createMapWithDependencyStringsToSvgs = svgElement => {
  const svgGroupsWithALine = svgElement.getAllGroupsContainingAnElementOfType('line');
  return new Map(svgGroupsWithALine.map(svgGroup => [svgGroup.getAttribute('id'), svgGroup]));
};

const getVisibleLineOfDependencySvgElement = (svgGroup) => svgGroup.getDescendantElementByTypeAndCssClasses('line', 'dependency');
const getHoverLineOfDependencySvgElement = (svgGroup) => svgGroup.getDescendantElementByTypeAndCssClasses('line', 'area');

const checkThat = (svgContainer) => ({
  containsExactlyDependencies: (...dependencyStrings) => {
    const dependencySvgGroups = svgContainer.getAllGroupsContainingAVisibleElementOfType('line');
    expect(dependencySvgGroups.map(g => g.getAttribute('id'))).to.have.members(dependencyStrings);
  },
  dependency: (dependencyString) => {
    const dependencyStringToSvgMap = createMapWithDependencyStringsToSvgs(svgContainer);
    const dependencySvgElement = dependencyStringToSvgMap.get(dependencyString);

    const getEndNodes = () => {
      const endNodeFullNames = dependencyString.split('-');
      const startNodeSvgElement = svgContainer.getSubSvgElementWithId(endNodeFullNames[0]);
      const targetNodeSvgElement = svgContainer.getSubSvgElementWithId(endNodeFullNames[1]);
      return {startNodeSvgElement, targetNodeSvgElement};
    };

    const is = checkForCorrectness => ({
      markedAs: {
        violation: () => {
          expect(getVisibleLineOfDependencySvgElement(dependencySvgElement).cssClasses.has('violation')).to.equal(checkForCorrectness);
        }
      },
      hoverable: () => {
        expect(getHoverLineOfDependencySvgElement(dependencySvgElement).pointerEventsEnabled).to.be.equal(checkForCorrectness);
      },
      inFrontOf: {
        bothEndNodes: () => {
          const {startNodeSvgElement, targetNodeSvgElement} = getEndNodes();
          expect(dependencySvgElement.isInFrontOf(startNodeSvgElement) && dependencySvgElement.isInFrontOf(targetNodeSvgElement))
            .to.be.equal(checkForCorrectness);
        }
      },
      between: {
        bothEndNodes: () => {
          const {startNodeSvgElement, targetNodeSvgElement} = getEndNodes();
          const result = dependencySvgElement.isInFrontOf(startNodeSvgElement) ^ dependencySvgElement.isInFrontOf(targetNodeSvgElement);
          expect(result).to.equal(checkForCorrectness ? 1 : 0);
        }
      }
    });

    const isTrue = is(true);

    return {
      is: {
        markedAs: isTrue.markedAs,
        hoverable: isTrue.hoverable,
        inFrontOf: isTrue.inFrontOf,
        between: isTrue.between,
        not: is(false)
      }
    }
  }
});

const interactOn = (svgContainer) => {
  const dependencyStringToSvgMap = createMapWithDependencyStringsToSvgs(svgContainer);
  return {
    hoverOverDependencyAndWaitFor: async (dependencyString, timeInMs) => {
      getHoverLineOfDependencySvgElement(dependencyStringToSvgMap.get(dependencyString)).mouseOver();
      await sleep(timeInMs);
    },
    leaveDependencyWithMouseAndWaitFor: async (dependencyString, timeInMs) => {
      getHoverLineOfDependencySvgElement(dependencyStringToSvgMap.get(dependencyString)).mouseOut();
      await sleep(timeInMs);
    }
  }
};

const inspect = (svgContainer) => {
  const dependencyStringToSvgMap = createMapWithDependencyStringsToSvgs(svgContainer);
  const result = {
    svgElementOf: (dependencyString) => {
      return dependencyStringToSvgMap.get(dependencyString);
    },
    linePositionOf: (dependencyString) => {
      const svgElement = result.svgElementOf(dependencyString);
      const line = getVisibleLineOfDependencySvgElement(svgElement);
      return {
        startPosition: line.absoluteStartPosition,
        endPosition: line.absoluteEndPosition
      };
    }
  };
  return result;
};

module.exports = {
  checkThat,
  interactOn,
  inspect
};