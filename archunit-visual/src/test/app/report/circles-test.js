'use strict';

const expect = require('chai').expect;

const Vector = require('./main-files').get('vectors').Vector;
const Circle = require('./main-files').get('circles').Circle;

const MAXIMUM_DELTA = 0.0001;

describe('Circle', () => {
  it('can be translated within an enclosing circle as far as possible in a specific direction', () => {
    const circle = new Circle(10, 10, 5);
    circle.translateWithinEnclosingCircleAsFarAsPossibleInTheDirection(20, new Vector(-10, -25));
    expect(circle.x).to.be.closeTo(0, MAXIMUM_DELTA);
    expect(circle.y).to.be.closeTo(-15, MAXIMUM_DELTA);
  });

  it('can be translated into an enclosing circle of specific radius', () => {
    const circle = new Circle(8, 12, 5);
    circle.translateIntoEnclosingCircleOfRadius(10, 3);
    expect(circle.x).to.be.closeTo(1.10940, MAXIMUM_DELTA);
    expect(circle.y).to.be.closeTo(1.66410, MAXIMUM_DELTA);
  });
});