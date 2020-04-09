'use strict';

const {expect} = require('chai');
const Assertion = require('chai').Assertion;

const SAFETY_MARGIN = 0.0001;
const getDistanceBetween = (point1, point2) => {
  const positionDiff = {
    x: point1.x - point2.x,
    y: point1.y - point2.y
  };
  return Math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y);
};

Assertion.addMethod('haveToOtherCircleAtLeastPadding', function (otherSvgCircle, padding) {
  const svgCircle = this._obj;
  const middleDistance = getDistanceBetween(svgCircle.absolutePosition, otherSvgCircle.absolutePosition);
  new Assertion(middleDistance + SAFETY_MARGIN >= svgCircle.getAttribute('r') + otherSvgCircle.getAttribute('r') + padding).to.be.true;
});

Assertion.addMethod('haveSiblingNodesWithPaddingAtLeast', function (padding) {
  const rootUi = this._obj;
  const nodeFullNames = Array.isArray(arguments[1]) ? arguments[1] : Array.from(arguments).slice(1);
  const expectedNodesNotToOverlap = nodeFullNames.map(nodeFullName => rootUi.getNodeWithFullName(nodeFullName));

  expectedNodesNotToOverlap.forEach((node, index) => {
    const circle = node.circle;
    expectedNodesNotToOverlap.slice(index + 1).forEach(otherSvgGroup => {
      const otherCircle = otherSvgGroup.circle;
      expect(circle).to.haveToOtherCircleAtLeastPadding(otherCircle, padding);
    });
  });
});

Assertion.addMethod('haveNodesWithPaddingToParentAtLeast', function (padding, parentFullNames) {
  const rootUi = this._obj;
  const nodeFullNames = Array.isArray(arguments[2]) ? arguments[2] : Array.from(arguments).slice(2);
  const expectedNodesToHavePaddingToParent = nodeFullNames.map(nodeFullName => rootUi.getNodeWithFullName(nodeFullName));
  const parentNode = rootUi.getNodeWithFullName(parentFullNames);
  const parentCircle = parentNode.circle;
  const parentPosition = parentCircle.absolutePosition;
  const parentRadius = parentCircle.getAttribute('r');

  expectedNodesToHavePaddingToParent.forEach(node => {
    const circle = node.circle;
    const middleDistance = getDistanceBetween(circle.absolutePosition, parentPosition);
    new Assertion(middleDistance + circle.getAttribute('r') + padding <= parentRadius);
  });
});

Assertion.addMethod('haveCssClass', function (cssClass) {
  const svgGroup = this._obj;
  expect(svgGroup._cssClasses).to.include(cssClass);
});
