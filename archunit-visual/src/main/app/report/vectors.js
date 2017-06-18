'use strict';

const defaultCoordinate = Math.sqrt(2) / 2;

let subVectors = (vector1, vector2) => vectors.vectorOf(vector1.x - vector2.x, vector1.y - vector2.y);

let getLength = vector => Math.sqrt(vector.x * vector.x + vector.y * vector.y);

let vectors = {
  distance: (vector1, vector2) => getLength(subVectors(vector1, vector2)),

  vectorOf: (x, y) => {
    return {x, y}
  },

  cloneVector: vector => {
    return {
      x: vector.x,
      y: vector.y
    }
  },

  getDefaultIfNull: vector => getLength(vector) === 0 ? vectors.vectorOf(defaultCoordinate, defaultCoordinate) : vector,

  norm: (vector, scale) => {
    let length = getLength(vector);
    return vectors.vectorOf(scale * vector.x / length, scale * vector.y / length);
  },

  getRevertedVector: vector => vectors.vectorOf(-vector.x, -vector.y),

  getOrthogonalVector: vector => vectors.vectorOf(vector.y, -vector.x),

  addVectors: (vector1, vector2) => vectors.vectorOf(vector1.x + vector2.x, vector1.y + vector2.y),

  angleToVector: vector => Math.asin((Math.sign(vector.x) || 1) * vector.y / getLength(vector)),

  getAngleDeg: angleRad => Math.round(angleRad * (180 / Math.PI))
};

module.exports.vectors = vectors;