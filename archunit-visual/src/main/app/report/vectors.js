'use strict';

const defaultCoordinate = Math.sqrt(2) / 2;

const subVectors = (vector1, vector2) => vectors.vectorOf(vector1.x - vector2.x, vector1.y - vector2.y);

const getLength = vector => Math.sqrt(vector.x * vector.x + vector.y * vector.y);

const vectors = {
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
    const length = getLength(vector) || 1;
    return vectors.vectorOf(scale * vector.x / length, scale * vector.y / length);
  },

  getRevertedVector: vector => vectors.vectorOf(-vector.x, -vector.y),

  getOrthogonalVector: vector => vectors.vectorOf(vector.y, -vector.x),

  addVectors: (vector1, vector2) => vectors.vectorOf(vector1.x + vector2.x, vector1.y + vector2.y),
};

const Vector = class {
  constructor(x, y) {
    if (isNaN(x) || isNaN(y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${x}, ${y})`);
    }
    this.x = x;
    this.y = y;
  }

  isWithin(vector, radius) {
    return Vector.between(this, vector).length() <= radius;
  }

  length() {
    return Math.sqrt(this.x * this.x + this.y * this.y);
  }

  changeTo(vector) {
    this.x = vector.x;
    this.y = vector.y;
    return this;
  }

  add(vector) {
    this.x += vector.x;
    this.y += vector.y;
    return this;
  }

  sub(vector) {
    this.x -= vector.x;
    this.y -= vector.y;
    return this;
  }

  static from(vector) {
    return new Vector(vector.x, vector.y);
  }

  static between(originPoint, targetPoint) {
    return new Vector(targetPoint.x - originPoint.x, targetPoint.y - originPoint.y);
  }

  static zeroVector() {
    return new Vector(0, 0);
  }
};

module.exports.Vector = Vector;
module.exports.vectors = vectors;