'use strict';

const {Vector, FixableVector} = require('../infrastructure/vectors');

// FIXME: 'position' unclear -> centerPosition?
const Shape = class {

  constructor(position) {
    this.position = position;
  }

  get x() {
    return this.position.x;
  }

  get y() {
    return this.position.y;
  }

  set x(value) {
    this.position.x = value;
  }

  set y(value) {
    this.position.y = value;
  }

  /**
   * calculates if the given circle with a position relative to this shape is completely within this shape,
   * considering a minimum distance from inner circles to the outer shape border
   */
  containsRelativeCircle() {
    throw new Error('not implemented');
  }

  /**
   * Shifts the given circle it is completely within this circle,
   * considering a minimum distance from inner circles to the outer shape border
   */
  translateEnclosedRelativeCircleIntoThis() {
    throw new Error('not implemented');
  }
};

const Circle = class extends Shape {
  constructor(position, r) {
    super(position);
    this.r = r;
  }

  containsRelativeCircle(relativeCircle, padding = 0) {
    return relativeCircle.position.length() + relativeCircle.r + padding <= this.r;
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
    enclosedCircle.position.norm(this.r - enclosedCircle.r - padding);
  }

  overlapsWith(otherCircle) {
    return Vector.between(this, otherCircle).length() <= (this.r + otherCircle.r);
  }

  static from(vector, r) {
    return new Circle(Vector.from(vector), r);
  }
};

const Rect = class extends Shape {
  constructor(position, halfWidth, halfHeight) {
    super(position);
    this.halfWidth = halfWidth;
    this.halfHeight = halfHeight;
  }

  _relativeCircleIsWithinWidth(relativeCircle, padding = 0) {
    return Math.abs(relativeCircle.position.x) + relativeCircle.r + padding <= this.halfWidth;
  }

  _relativeCircleIsWithinHeight(relativeCircle, padding = 0) {
    return Math.abs(relativeCircle.position.y) + relativeCircle.r + padding <= this.halfHeight;
  }

  containsRelativeCircle(relativeCircle, padding = 0) {
    return this._relativeCircleIsWithinWidth(relativeCircle, padding)
      && this._relativeCircleIsWithinHeight(relativeCircle, padding);
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
    if (!this._relativeCircleIsWithinWidth(enclosedCircle, padding)) {
      enclosedCircle.position.x = Math.sign(enclosedCircle.position.x) * (this.halfWidth - enclosedCircle.r - padding);
    }
    if (!this._relativeCircleIsWithinHeight(enclosedCircle, padding)) {
      enclosedCircle.position.y = Math.sign(enclosedCircle.position.y) * (this.halfHeight - enclosedCircle.r - padding);
    }
  }
};

const ZeroShape = class extends Shape {
  constructor() {
    super(new Vector(0, 0));
  }

  containsRelativeCircle() {
    return true;
  }

  translateEnclosedRelativeCircleIntoThis() {
  }
};

const CircleWithFixablePosition = class extends Circle {
  constructor(x, y, r, id) {
    super(new FixableVector(x, y), r);
    this.id = id;
  }

  get fx() {
    return this.position.fx;
  }

  get fy() {
    return this.position.fy;
  }
};

module.exports = {Circle, Rect, ZeroShape, CircleWithFixablePosition};