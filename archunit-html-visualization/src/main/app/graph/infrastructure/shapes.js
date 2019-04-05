'use strict';

const {Vector, FixableVector} = require('../infrastructure/vectors');

const Shape = class {

  constructor(centerPosition) {
    this.centerPosition = centerPosition;
  }

  /**
   * calculates if the given circle with a centerPosition relative to this shape is completely within this shape,
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
  constructor(centerPosition, r) {
    super(centerPosition);
    this.r = r;
  }

  containsRelativeCircle(relativeCircle, padding = 0) {
    return relativeCircle.centerPosition.length() + relativeCircle.r + padding <= this.r;
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
    enclosedCircle.centerPosition.norm(this.r - enclosedCircle.r - padding);
  }

  overlapsWith(otherCircle) {
    return Vector.between(this, otherCircle).length() <= (this.r + otherCircle.r);
  }

  containsPoint(vector) {
    return Vector.between(this.centerPosition, vector).length() <= this.r;
  }

  static from(vector, r) {
    return new Circle(Vector.from(vector), r);
  }
};

const Rect = class extends Shape {
  constructor(centerPosition, halfWidth, halfHeight) {
    super(centerPosition);
    this.halfWidth = halfWidth;
    this.halfHeight = halfHeight;
  }

  containsRelativeCircle(relativeCircle, padding = 0) {
    return this.relativeCircleIsWithinWidth(relativeCircle, padding)
      && this.relativeCircleIsWithinHeight(relativeCircle, padding);
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
    if (!this.relativeCircleIsWithinWidth(enclosedCircle, padding)) {
      enclosedCircle.centerPosition.x = Math.sign(enclosedCircle.centerPosition.x) * (this.halfWidth - enclosedCircle.r - padding);
    }
    if (!this.relativeCircleIsWithinHeight(enclosedCircle, padding)) {
      enclosedCircle.centerPosition.y = Math.sign(enclosedCircle.centerPosition.y) * (this.halfHeight - enclosedCircle.r - padding);
    }
  }

  relativeCircleIsWithinWidth(relativeCircle, padding = 0) {
    return Math.abs(relativeCircle.centerPosition.x) + relativeCircle.r + padding <= this.halfWidth;
  }

  relativeCircleIsWithinHeight(relativeCircle, padding = 0) {
    return Math.abs(relativeCircle.centerPosition.y) + relativeCircle.r + padding <= this.halfHeight;
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

/**
 * Hint: the getter and setter for x and y are needed by the method Root._forceLayout(), as the used d3 force simulation
 * operates on the absoluteFixableCircle of a node's _nodeShape and needs direct set- and get-access to the x and y property
 */
const CircleWithFixablePosition = class extends Circle {
  constructor(x, y, r, id) {
    super(new FixableVector(x, y), r);
    this.id = id;
  }

  get x() {
    return this.centerPosition.x;
  }

  get y() {
    return this.centerPosition.y;
  }

  set x(value) {
    this.centerPosition.x = value;
  }

  set y(value) {
    this.centerPosition.y = value;
  }

  get fx() {
    return this.centerPosition.fx;
  }

  get fy() {
    return this.centerPosition.fy;
  }

  fix() {
    this.centerPosition.fix();
  }

  unfix() {
    this.centerPosition.unfix();
  }

  get fixed() {
    return this.centerPosition.fixed;
  }
};

module.exports = {Circle, Rect, ZeroShape, CircleWithFixablePosition};