'use strict';

const expect = require('chai').expect;

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
    expect(act).to.deep.equal({x: 6, y:8});
  });

  it('can revert a vector', () => {
    const vector = new Vector(3, -5);
    const act = vectors.getRevertedVector(vector);
    expect(act).to.deep.equal({x: -3, y:5});
  });

  it('can create an orthogonal vector to a given vector', () => {
    const vector = new Vector(3, -5);
    const act = vectors.getOrthogonalVector(vector);
    expect(act).to.deep.equal({x: -5, y:-3});
  });

  it('can add two vectors', () => {
    const vector1 = new Vector(-6, 10);
    const vector2 = new Vector(-3, 6);
    const act = vectors.add(vector1, vector2);
    expect(act).to.deep.equal({x: -9, y: 16});
  });

  it('returns the default vector when getting the null vector', () => {
    const act = new Vector(0, 0).getDefaultIfNull();
    expect(act.x).to.closeTo(0.7071, MAXIMUM_DELTA);
    expect(act.y).to.closeTo(0.7071, MAXIMUM_DELTA);
  });

  it('returns the vector when getting not the null vector', () => {
    const vector = new Vector(5, 0);
    const act = vector.getDefaultIfNull();
    expect(act).to.equal(vector);
  });
});

describe('vector', () => {
  it('calculates the correct length', () => {
    const vector = new Vector(3, 4);
    expect(vector.length()).to.equal(5);
  });

  it('can be spanned between two other vectors', () => {
    const vector1 = new Vector(-3, 8);
    const vector2 = new Vector(5, 12);
    const act = Vector.between(vector1, vector2);
    expect(act).to.deep.equal({x: 8, y: 4});
  });
});