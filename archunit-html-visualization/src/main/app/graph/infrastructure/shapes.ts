'use strict';

import {Vector, FixableVector} from "./vectors";

class Shape {
  protected _centerPosition: Vector

  constructor(centerPosition: Vector) {
    this._centerPosition = centerPosition;
  }

  get centerPosition(): Vector {
    return this._centerPosition;
  }
  // /**
  //  * calculates if the given circle with a centerPosition relative to this shape is completely within this shape,
  //  * considering a minimum distance from inner circles to the outer shape border
  //  */
  // containsRelativeCircle() {
  //   throw new Error('not implemented');
  // }

  // /**
  //  * Shifts the given circle it is completely within this circle,
  //  * considering a minimum distance from inner circles to the outer shape border
  //  */
  // translateEnclosedRelativeCircleIntoThis() {
  //   throw new Error('not implemented');
  // }
}

class Circle extends Shape {
  private _r: number

  constructor(centerPosition: Vector, r: number) {
    super(centerPosition);
    this._r = r;
  }

  get r(): number {
    return this._r;
  }
  set r(value: number) {
    this._r = value
  }
  // containsRelativeCircle(relativeCircle, padding = 0) {
  //   return relativeCircle.centerPosition.length() + relativeCircle.r + padding <= this.r;
  // }
  //
  // translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
  //   enclosedCircle.centerPosition.norm(this.r - enclosedCircle.r - padding);
  // }
  //
  // overlapsWith(otherCircle) {
  //   return Vector.between(this.centerPosition, otherCircle.centerPosition).length() <= (this.r + otherCircle.r);
  // }
  //
  // containsPoint(vector) {
  //   return Vector.between(this.centerPosition, vector).length() <= this.r;
  // }
  //
  // static from(vector, r) {
  //   return new Circle(Vector.from(vector), r);
  // }
}

/**
 * Hint: the getter and setter for x and y are needed by the method Root._forceLayout(), as the used d3 force simulation
 * operates on the absoluteFixableCircle of a node's _nodeShape and needs direct set- and get-access to the x and y property
 */
class CircleWithFixablePosition extends Circle {
  private id: string

  constructor(x: number, y: number, r: number, id: string) {
    super(new FixableVector(x, y), r);
    this.id = id;
  }

  // get x() {
  //   return this.centerPosition.x;
  // }
  //
  // get y() {
  //   return this.centerPosition.y;
  // }
  //
  // set x(value) {
  //   this.centerPosition.x = value;
  // }
  //
  // set y(value) {
  //   this.centerPosition.y = value;
  // }
  //
  // get fx() {
  //   return this.centerPosition.fx;
  // }
  //
  // get fy() {
  //   return this.centerPosition.fy;
  // }

  fix(): void {
    (this._centerPosition as FixableVector).fix();
  }

  unfix(): void {
    (this._centerPosition as FixableVector).unfix();
  }

  get fixed(): boolean {
    return (this._centerPosition as FixableVector).fixed;
  }
}

class Rect extends Shape {
  halfWidth: number
  halfHeight: number

  constructor(centerPosition: Vector, halfWidth: number, halfHeight: number) {
    super(centerPosition);
    this.halfWidth = halfWidth;
    this.halfHeight = halfHeight;
  }
  //
  // containsRelativeCircle(relativeCircle, padding = 0) {
  //   return this.relativeCircleIsWithinWidth(relativeCircle, padding)
  //     && this.relativeCircleIsWithinHeight(relativeCircle, padding);
  // }
  //
  // translateEnclosedRelativeCircleIntoThis(enclosedCircle, padding) {
  //   if (!this.relativeCircleIsWithinWidth(enclosedCircle, padding)) {
  //     enclosedCircle.centerPosition.x = Math.sign(enclosedCircle.centerPosition.x) * (this.halfWidth - enclosedCircle.r - padding);
  //   }
  //   if (!this.relativeCircleIsWithinHeight(enclosedCircle, padding)) {
  //     enclosedCircle.centerPosition.y = Math.sign(enclosedCircle.centerPosition.y) * (this.halfHeight - enclosedCircle.r - padding);
  //   }
  // }
  //
  // relativeCircleIsWithinWidth(relativeCircle, padding = 0) {
  //   return Math.abs(relativeCircle.centerPosition.x) + relativeCircle.r + padding <= this.halfWidth;
  // }
  //
  // relativeCircleIsWithinHeight(relativeCircle, padding = 0) {
  //   return Math.abs(relativeCircle.centerPosition.y) + relativeCircle.r + padding <= this.halfHeight;
  // }
}

class ZeroShape extends Shape {
  constructor() {
    super(new Vector(0, 0));
  }

  // containsRelativeCircle() {
  //   return true;
  // }
  //
  // translateEnclosedRelativeCircleIntoThis() {
  // }
}

interface ShapeListener {
  onMovedToPosition: () => Promise<void>
  onRadiusChanged: () => Promise<void>
  onSizeChanged: () => Promise<void>
}

export {Circle, Rect, ZeroShape, CircleWithFixablePosition, Shape, ShapeListener};
