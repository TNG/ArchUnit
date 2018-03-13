'use strict';

const defaultCoordinate = Math.sqrt(2) / 2;

const vectors = {
  distance: (vector1, vector2) => Vector.between(vector1, vector2).length(),

  norm: (vector, scale) => Vector.from(vector).norm(scale),

  getRevertedVector: vector => Vector.from(vector).revert(),

  getOrthogonalVector: vector => new Vector(vector.y, -vector.x),

  add: (vector1, vector2) => Vector.from(vector1).add(vector2)
};

const Vector = class {
  constructor(x, y) {
    if (isNaN(x) || isNaN(y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${x}, ${y})`);
    }
    this.x = x;
    this.y = y;
  }

  revert() {
    this.x = -this.x;
    this.y = -this.y;
    return this;
  }

  revertIf(condition) {
    return condition ? this.revert() : this;
  }

  isWithinCircle(vector, radius) {
    return Vector.between(this, vector).length() <= radius;
  }

  length() {
    return Math.sqrt(this.x * this.x + this.y * this.y);
  }

  makeDefaultIfNull() {
    if (this.length() === 0) {
      return this.changeTo(defaultVector);
    }
    return this;
  }

  changeTo(vector) {
    if (isNaN(vector.x) || isNaN(vector.y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${vector.x}, ${vector.y})`);
    }
    this.x = vector.x;
    this.y = vector.y;
    return this;
  }

  add(vector = zeroVector) {
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
    return this.scale(scale / length);
  }

  sub(vector = zeroVector) {
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

const Circle = class extends Vector {
  constructor(x, y, r) {
    super(x, y);
    this.r = r;
  }

  containsRelativeCircle(relativeCircle, padding = 0) {
    return relativeCircle.length() + relativeCircle.r + padding <= this.r;
  }

  //FIXME: create tests for this
  /**
   * Takes an enclosing circle radius and a translation vector with respect to this circle.
   * Calculates the x- and y- coordinate for a maximal translation of this circle,
   * keeping this circle fully enclosed within the outer circle (whose position is (0,0)).
   *
   * @param enclosingCircleRadius radius of the outer circle
   * @param translationVector translation vector to be applied to this circle
   * @return this circle after translation, with respect to the enclosing circle's center.
   * Keeps this circle enclosed within the outer circle.
   */
  translateWithinEnclosingCircleAsFarAsPossibleInTheDirection(enclosingCircleRadius, translationVector) {
    const c1 = translationVector.x * translationVector.x + translationVector.y * translationVector.y;
    const c2 = Math.pow(enclosingCircleRadius - this.r, 2);
    const c3 = -Math.pow(this.y * translationVector.x - this.x * translationVector.y, 2);
    const c4 = -(this.x * translationVector.x + this.y * translationVector.y);
    const scale = (c4 + Math.sqrt(c3 + c2 * c1)) / c1;
    return this.changeTo({
      x: Math.trunc(this.x + scale * translationVector.x),
      y: Math.trunc(this.y + scale * translationVector.y)
    });
  }

  //FIXME: create tests for this
  /**
   * Shifts this circle towards to the center of the parent circle (which is (0, 0)), so that this circle
   * is completely within the enclosing circle
   * @param enclosingCircleRadius radius of the outer circle
   * @param circlePadding minimum distance from inner circles outer circle borders
   * @return this circle after the shift into the enclosing circle
   */
  translateIntoEnclosingCircleOfRadius(enclosingCircleRadius, circlePadding) {
    return this.norm(enclosingCircleRadius - this.r - circlePadding);
  }

  static from(vector, r) {
    return new Circle(vector.x, vector.y, r);
  }
};

const defaultVector = new Vector(defaultCoordinate, defaultCoordinate);
const zeroVector = new Vector(0, 0);

module.exports.Vector = Vector;
module.exports.Circle = Circle;
module.exports.vectors = vectors;