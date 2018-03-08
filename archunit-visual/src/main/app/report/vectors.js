'use strict';

const defaultCoordinate = Math.sqrt(2) / 2;

const vectors = {
  distance: (vector1, vector2) => Vector.between(vector1, vector2).length(),

  norm: (vector, scale) => Vector.from(vector).norm(scale),

  getRevertedVector: vector => new Vector(-vector.x, -vector.y),

  getOrthogonalVector: vector => new Vector(vector.y, -vector.x),

  add: (vector1, vector2) => Vector.from(vector1).add(vector2)
};

//FIXME: the methods as add etc. are ugly, as they change the vector itself and return itself
//--> returning itself makes the vector seem to immutable, but it isn't
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

  getDefaultIfNull() {
    return this.length() === 0 ? new Vector(defaultCoordinate, defaultCoordinate) : this;
  }

  changeTo(vector) {
    this.x = vector.x;
    this.y = vector.y;
    //FIXME: return really necessary??
    return this;
  }

  add(vector) {
    this.x += vector.x;
    this.y += vector.y;
    return this;
  }

  scale(factor) {
    this.x *= factor;
    this.y *= factor;
    return this;
  }

  norm(scale) {
    const length = this.length() || 1;
    return this.scale(scale/length);
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