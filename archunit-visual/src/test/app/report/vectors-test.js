'use strict';

require('./chai/vectors-chai-extensions');
const expect = require("chai").expect;

const vectors = require('./main-files').get('vectors').vectors;

describe("vectors", () => {
  const vector1 = {
    x: -6,
    y: 10
  };

  const vector2 = {
    x: -3,
    y: 6
  };
  it("can calc the distance between two vectors", () => {
    expect(vectors.distance(vector1, vector2)).to.equal(5);
  });

  it("can norm a vector", () => {
    const vector = {
      x: 3,
      y: 4
    };
    const normedVector = vectors.norm(vector, 10);
    expect(normedVector.x).to.equal(6);
    expect(normedVector.y).to.equal(8);
  });

  it("can calc an angle to a vector", () => {
    const vector1 = {
      x: 0,
      y: 4
    };
    expect(vectors.angleToVector(vector1)).to.equal(Math.PI / 2);

    const vector2 = {
      x: 5,
      y: 0
    };
    expect(vectors.angleToVector(vector2)).to.equal(0);

    const vector3 = {
      x: 5,
      y: 5
    };
    expect(vectors.angleToVector(vector3)).to.haveDiffLessThan(Math.PI / 4, 0.001);
  });
});