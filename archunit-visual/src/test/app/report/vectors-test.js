'use strict';

const chai = require('chai');
const expect = chai.expect;
const generalExtensions = require('./chai/general-chai-extensions');
chai.use(generalExtensions);

const vectors = require('./main-files').get('vectors').vectors;
const Vector = require('./main-files').get('vectors').Vector;

const MAXIMUM_DELTA = 0.0001;

describe('vectors', () => {
  it('can calc the distance between two vectors', () => {
    const vector1 = new Vector(-6, 10);
    const vector2 = new Vector(-3, 6);
    const act = vectors.distance(vector1, vector2);
    expect(act).to.equal(5);
  });

  it('can norm a vector', () => {
    const vector = new Vector(3, 4);
    const act = vectors.norm(vector, 10);
    expect(act).to.deep.equal({x: 6, y: 8});
  });

  it('can revert a vector', () => {
    const vector = new Vector(3, -5);
    const act = vectors.getRevertedVector(vector);
    expect(act).to.deep.equal({x: -3, y: 5});
  });

  it('can create an orthogonal vector to a given vector', () => {
    const vector = new Vector(3, -5);
    const act = vectors.getOrthogonalVector(vector);
    expect(act).to.deep.equal({x: -5, y: -3});
  });

  it('can add two vectors', () => {
    const vector1 = new Vector(-6, 10);
    const vector2 = new Vector(-3, 6);
    const act = vectors.add(vector1, vector2);
    expect(act).to.deep.equal({x: -9, y: 16});
  });
});

describe('Vector', () => {
  it('calculates the correct length', () => {
    const vector = new Vector(3, 4);
    expect(vector.length()).to.equal(5);
  });

  it('can be reverted', () => {
    const vector = new Vector(3, -5).revert();
    expect(vector).to.deep.equal({x: -3, y: 5});
  });

  it('can check if it is within a given circle', () => {
    const vector = new Vector(3, 2);
    const circleMiddle = new Vector(-1, 0);
    expect(vector.isWithinCircle(circleMiddle, 1)).to.be.false;
    expect(vector.isWithinCircle(circleMiddle, 5)).to.be.true;
  });

  it('returns the default vector when getting the null vector', () => {
    const nullVector = new Vector(0, 0);
    nullVector.makeDefaultIfNull();
    const exp = new Vector(0.7071, 0.7071);
    expect(nullVector).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });

  it('returns the vector when getting not the null vector', () => {
    const vector = new Vector(5, 0);
    vector.makeDefaultIfNull();
    expect(vector).to.equal(vector);
  });

  it('can be added to another vector', () => {
    const vector = new Vector(3, -5);
    vector.add(new Vector(2, 1));
    expect(vector).to.deep.equal({x: 5, y: -4});
  });

  it('can be scaled', () => {
    const vector = new Vector(3, -5);
    vector.scale(3);
    expect(vector).to.deep.equal({x: 9, y: -15});
  });

  it('can be normed and then scaled', () => {
    const vector = new Vector(3, -5);
    vector.norm(3);
    const exp = new Vector(1.54349, -2.57248);
    expect(vector).to.deep.closeTo(exp, MAXIMUM_DELTA);
  });

  it('can be spanned between two other vectors', () => {
    const vector1 = new Vector(-3, 8);
    const vector2 = new Vector(5, 12);
    const act = Vector.between(vector1, vector2);
    expect(act).to.deep.equal({x: 8, y: 4});
  });

  it('can be reverted if a condition is fulfilled', () => {
    const vector = new Vector(-3, 8);
    vector.revertIf(false);
    expect(vector).to.deep.equal({x: -3, y: 8});
    vector.revertIf(true);
    expect(vector).to.deep.equal({x: 3, y: -8});
  });
});