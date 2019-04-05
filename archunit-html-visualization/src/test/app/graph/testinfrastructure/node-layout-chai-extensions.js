'use strict';

const expect = require('chai').expect;
const Assertion = require('chai').Assertion;

//FIXME: move to own file or use Vector??
const getDistanceBetween = (point1, point2) => {
  const positionDiff = {
    x: point1.x - point2.x,
    y: point1.y - point2.y
  };
  return Math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y);
};

const getVisibleCircleOf = nodeSvg => nodeSvg.getVisibleSubElementOfType('circle');
const getVisibleTextOf = nodeSvg => nodeSvg.getVisibleSubElementOfType('text');

Assertion.addMethod('beWithin', function (parentSvg, padding) {
  const childSvg = this._obj;
  const childCircle = getVisibleCircleOf(childSvg);
  const parentCircle = getVisibleCircleOf(parentSvg);
  const parentRadius = parentCircle.getAttribute('r');
  const middleDistance = getDistanceBetween(childCircle.absolutePosition, parentCircle.absolutePosition);
  new Assertion(middleDistance + childCircle.getAttribute('r') + padding <= parentRadius).to.be.true;
});

Assertion.addMethod('havePaddingTo', function (otherNodeSvg, padding) {
  const nodeSvg = this._obj;
  const circle = getVisibleCircleOf(nodeSvg);
  const otherCircle = getVisibleCircleOf(otherNodeSvg);
  const middleDistance = getDistanceBetween(circle.absolutePosition, otherCircle.absolutePosition);
  new Assertion(middleDistance >= circle.getAttribute('r') + otherCircle.getAttribute('r') + padding).to.be.true;
});

Assertion.addMethod('haveLabelInTheMiddle', function () {
  const nodeSvg = this._obj;
  const circle = getVisibleCircleOf(nodeSvg);
  const text = getVisibleTextOf(nodeSvg);
  const textPosition = text.absolutePosition;
  const circlePosition = circle.absolutePosition;

  new Assertion(textPosition).to.deep.equal(circlePosition);
});

Assertion.addMethod('haveLabelAtTop', function () {
  const nodeSvg = this._obj;
  const circle = getVisibleCircleOf(nodeSvg);
  const text = getVisibleTextOf(nodeSvg);
  const textPosition = text.absolutePosition;
  const circlePosition = circle.absolutePosition;

  new Assertion(textPosition.y).to.be.below(circlePosition.y); //TODO: maybe a bit simple check, but only repeating the implementation does not make sense as well...
});

Assertion.addMethod('haveLabelWithinCircle', function () {
  const nodeSvg = this._obj;
  const circle = getVisibleCircleOf(nodeSvg);
  const text = getVisibleTextOf(nodeSvg);
  const textPosition = text.absolutePosition;
  const textWidth = text.textWidth;
  const circlePosition = circle.absolutePosition;
  const circleRadius = circle.getAttribute('r');
  const positionDistance = getDistanceBetween(circlePosition, textPosition);
  const circleWidthAtTextPosition = 2 * Math.sqrt(circleRadius * circleRadius - positionDistance * positionDistance);

  new Assertion(textWidth).to.be.below(circleWidthAtTextPosition);
});