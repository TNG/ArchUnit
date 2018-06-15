'use strict';

import chai from 'chai';
import generalExtensions from './chai/general-chai-extensions';
import {Vector} from '../../../main/app/report/vectors';
import {Circle} from '../../../main/app/report/circles';

const expect = chai.expect;
chai.use(generalExtensions);

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