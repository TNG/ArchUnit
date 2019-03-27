'use strict';

const chai = require('chai');
const generalExtensions = require('../testinfrastructure/general-chai-extensions');
const {Vector} = require('../../../../main/app/graph/infrastructure/vectors');
const {Circle, Rect} = require('../../../../main/app/graph/infrastructure/shapes');

const expect = chai.expect;
chai.use(generalExtensions);

const MAXIMUM_DELTA = 0.0001;

describe('Circle', () => {
  it('can translate an other circle so that this circle lies completely within the own one', () => {
    const circle = new Circle(new Vector(8, 12), 5);
    const circleEnclosingCircle = new Circle(new Vector(0, 0), 10);
    circleEnclosingCircle.translateEnclosedRelativeCircleIntoThis(circle, 3);
    const exp = new Vector(1.10940, 1.66410);
    expect(circle.centerPosition).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });

  it('knows if it contains an other relative circle, considering a minimum padding between the circle rims', () => {
    let containingCircle = Circle.from({x: 1, y: 1}, 5);

    let circleCompletelyOutside = Circle.from({x: -6, y: 6}, 5);
    expect(containingCircle.containsRelativeCircle(circleCompletelyOutside)).to.be.false;

    let circlePartiallyOutside = Circle.from({x: -1, y: 1}, 5);
    expect(containingCircle.containsRelativeCircle(circlePartiallyOutside)).to.be.false;

    let circleInsideCircleButViolatingPadding = Circle.from({x: -1, y: 1}, 1);
    expect(containingCircle.containsRelativeCircle(circleInsideCircleButViolatingPadding, 5)).to.be.false;

    let circleInside = Circle.from({x: -1, y: 1}, 1);
    expect(containingCircle.containsRelativeCircle(circleInside)).to.be.true;
  });

  it('detects overlap with another circle', () => {
    let first = Circle.from({x: 1, y: 1}, 0.5);
    let second = Circle.from({x: 1, y: -1}, 0.5);

    expect(first.overlapsWith(second)).to.be.false;

    first = Circle.from({x: 1, y: 1}, 1);
    second = Circle.from({x: 1, y: -1}, 1);

    expect(first.overlapsWith(second)).to.be.true;

    first = Circle.from({x: 0, y: 0}, 2);
    second = Circle.from({x: 1, y: 0}, 3);

    expect(first.overlapsWith(second)).to.be.true;
  });
});

describe('Rect', () => {
  it('can translate a circle so that this circle lies completely within the own rect', () => {
    const rect = new Rect(new Vector(0, 0), 20, 10);

    const circleRightToTheRect = new Circle(new Vector(20, 2), 5);
    rect.translateEnclosedRelativeCircleIntoThis(circleRightToTheRect, 3);
    const expPositionOfCircleRightToTheRect = new Vector(12, 2);
    expect(circleRightToTheRect.centerPosition).to.deep.closeTo(expPositionOfCircleRightToTheRect, MAXIMUM_DELTA);

    const circleUnderTheRect = new Circle(new Vector(2, 40), 5);
    rect.translateEnclosedRelativeCircleIntoThis(circleUnderTheRect, 3);
    const expPositionOfCircleUnderTheRect = new Vector(2, 2);
    expect(circleUnderTheRect.centerPosition).to.deep.closeTo(expPositionOfCircleUnderTheRect, MAXIMUM_DELTA);

    const circleOverAndLeftToTheRect = new Circle(new Vector(-30, -30), 5);
    rect.translateEnclosedRelativeCircleIntoThis(circleOverAndLeftToTheRect, 3);
    const expPositionOfCircleOverAndLeftToTheRect = new Vector(-12, -2);
    expect(circleOverAndLeftToTheRect.centerPosition).to.deep.closeTo(expPositionOfCircleOverAndLeftToTheRect, MAXIMUM_DELTA);
  });

  it('knows if it contains a relative circle, considering a minimum padding between the circle and the rect rim', () => {
    let rect = new Rect(new Vector(1, 1), 10, 5);

    let circleCompletelyOutside = Circle.from({x: 20, y: 15}, 5);
    expect(rect.containsRelativeCircle(circleCompletelyOutside)).to.be.false;

    let circlePartiallyOutsideAtTop = Circle.from({x: -1, y: 1}, 5);
    expect(rect.containsRelativeCircle(circlePartiallyOutsideAtTop)).to.be.false;

    let circlePartiallyOutsideAtLeft = Circle.from({x: -9, y: 0}, 3);
    expect(rect.containsRelativeCircle(circlePartiallyOutsideAtLeft)).to.be.false;

    let circlePartiallyOutsideAtTopLeft = Circle.from({x: -9, y: -3}, 3);
    expect(rect.containsRelativeCircle(circlePartiallyOutsideAtLeft)).to.be.false;

    let circleInsideButViolatingPadding = Circle.from({x: -1, y: 1}, 3);
    expect(rect.containsRelativeCircle(circleInsideButViolatingPadding, 5)).to.be.false;

    let circleInside = Circle.from({x: -1, y: 1}, 1);
    expect(rect.containsRelativeCircle(circleInside)).to.be.true;
  });
});