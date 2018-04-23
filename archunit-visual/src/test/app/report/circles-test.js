'use strict';

const chai = require('chai');
const expect = chai.expect;
const generalExtensions = require('./chai/general-chai-extensions');
chai.use(generalExtensions);

const Vector = require('./main-files').get('vectors').Vector;
const Circle = require('./main-files').get('circles').Circle;

const MAXIMUM_DELTA = 0.0001;

describe('Circle', () => {
  it('can be translated within an enclosing circle as far as possible in a specific direction', () => {
    const circle = new Circle(10, 10, 5);
    circle.translateWithinEnclosingCircleAsFarAsPossibleInTheDirection(20, new Vector(-10, -25));
    const exp = new Vector(0, -15);
    expect(circle).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });

  it('can be translated into an enclosing circle of specific radius', () => {
    const circle = new Circle(8, 12, 5);
    circle.translateIntoEnclosingCircleOfRadius(10, 3);
    const exp = new Vector(1.10940, 1.66410);
    expect(circle).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });
});