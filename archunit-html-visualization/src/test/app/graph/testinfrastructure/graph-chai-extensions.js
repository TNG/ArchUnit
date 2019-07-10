'use strict';

const {expect} = require('chai');
const Assertion = require('chai').Assertion;

//FIXME: maybe better into testGui...
const svgGroupsContainingAVisible = (graph, svgType) => graph._view.svgElement.getAllGroupsContainingAVisibleElementOfType(svgType);

const svgGroupsContainingAVisibleCircle = graph => svgGroupsContainingAVisible(graph, 'circle');
const svgGroupsContainingAVisibleLine = graph => svgGroupsContainingAVisible(graph, 'line');

Assertion.addMethod('haveOnlyVisibleNodes', function () {
  const graph = this._obj;
  const allGroupsWithAVisibleCircle = svgGroupsContainingAVisibleCircle(graph);
  const expectedNodeNames = Array.isArray(arguments[0]) ? arguments[0] : Array.from(arguments);
  const textElementsOfVisibleCircles = allGroupsWithAVisibleCircle.map(g => g.getVisibleSubElementOfType('text'));
  const actualNodeNames = textElementsOfVisibleCircles.map(textElement => textElement.getAttribute('text'));
  new Assertion(actualNodeNames).to.have.members(expectedNodeNames);
});

Assertion.addMethod('haveOnlyVisibleDependencies', function () {
  const graph = this._obj;
  const expectedVisibleDependencies = Array.isArray(arguments[0]) ? arguments[0] : Array.from(arguments);
  const allGroupsWithAVisibleLine = svgGroupsContainingAVisibleLine(graph);

  new Assertion(allGroupsWithAVisibleLine.map(g => g.getAttribute('id'))).to.have.members(expectedVisibleDependencies);
});

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
  new Assertion(middleDistance >= svgCircle.getAttribute('r') + otherSvgCircle.getAttribute('r') + padding).to.be.true;
});

Assertion.addMethod('haveSiblingNodesWithPaddingAtLeast', function (padding) {
  const svgGroupMap = this._obj;
  const nodeFullNames = Array.isArray(arguments[1]) ? arguments[1] : Array.from(arguments).slice(1);
  const expectedSvgGroupsNotToOverlap = nodeFullNames.map(nodeFullName => svgGroupMap.get(nodeFullName));

  expectedSvgGroupsNotToOverlap.forEach((svgGroup, index) => {
    const circle = svgGroup.getVisibleSubElementOfType('circle');
    expectedSvgGroupsNotToOverlap.slice(index + 1).forEach(otherSvgGroup => {
      const otherCircle = otherSvgGroup.getVisibleSubElementOfType('circle');
      expect(circle).to.haveToOtherCircleAtLeastPadding(otherCircle, padding);
    });
  });
});

Assertion.addMethod('haveNodesWithPaddingToParentAtLeast', function (padding, parentFullNames) {
  const svgGroupMap = this._obj;
  const nodeFullNames = Array.isArray(arguments[2]) ? arguments[2] : Array.from(arguments).slice(2);
  const expectedSvgGroupsToHavePaddingToParent = nodeFullNames.map(nodeFullName => svgGroupMap.get(nodeFullName));
  const parentSvgGroup = svgGroupMap.get(parentFullNames);
  const parentCircle = parentSvgGroup.getVisibleSubElementOfType('circle');
  const parentPosition = parentCircle.absolutePosition;
  const parentRadius = parentCircle.getAttribute('r');

  expectedSvgGroupsToHavePaddingToParent.forEach(svgGroup => {
    const circle = svgGroup.getVisibleSubElementOfType('circle');
    const middleDistance = getDistanceBetween(circle.absolutePosition, parentPosition);
    new Assertion(middleDistance + circle.getAttribute('r') + padding <= parentRadius);
  });
});

Assertion.addMethod('haveCssClass', function (cssClass) {
  const svgGroup = this._obj;
  expect(svgGroup._cssClasses).to.include(cssClass);
});