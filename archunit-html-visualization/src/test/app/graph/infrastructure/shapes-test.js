'use strict';

const chai = require('chai');
const generalExtensions = require('../testinfrastructure/general-chai-extensions');
const {Vector} = require('../../../../main/app/graph/infrastructure/vectors');
const {Circle} = require('../../../../main/app/graph/infrastructure/shapes');

const expect = chai.expect;
chai.use(generalExtensions);

const MAXIMUM_DELTA = 0.0001;

describe('Circle', () => {
  it('can be translated into an enclosing circle of specific radius', () => {
    const circle = new Circle(new Vector(8, 12), 5);
    const circleEnclosingCircle = new Circle(new Vector(0, 0), 10);
    circleEnclosingCircle.translateEnclosedRelativeCircleIntoThis(circle, 3);
    const exp = new Vector(1.10940, 1.66410);
    expect(circle).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });

  it('detects overlap with another circle', () => {
    let first = new Circle({x: 1, y: 1}, 0.5);
    let second = new Circle({x: 1, y: -1}, 0.5);

    expect(first.overlapsWith(second)).to.be.false;

    first = new Circle({x: 1, y: 1}, 1);
    second = new Circle({x: 1, y: -1}, 1);

    expect(first.overlapsWith(second)).to.be.true;

    first = new Circle({x: 0, y: 0}, 2);
    second = new Circle({x: 1, y: 0}, 3);

    expect(first.overlapsWith(second)).to.be.true;
  });
});