'use strict';

import {Vector, FixableVector} from "./vectors";

abstract class Shape {
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
  abstract containsRelativeCircle(circle: Circle, padding: number): boolean

  // /**
  //  * Shifts the given circle it is completely within this circle,
  //  * considering a minimum distance from inner circles to the outer shape border
  //  */
  abstract translateEnclosedRelativeCircleIntoThis(enclosedCircle: Circle, padding: number): void
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

  containsRelativeCircle(relativeCircle: Circle, padding: number = 0): boolean {
    return relativeCircle.centerPosition.length() + relativeCircle.r + padding <= this.r;
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle: Circle, padding: number): void {
    enclosedCircle.centerPosition.norm(this.r - enclosedCircle.r - padding);
  }
  //
  // overlapsWith(otherCircle) {
  //   return Vector.between(this.centerPosition, otherCircle.centerPosition).length() <= (this.r + otherCircle.r);
  // }
  //
  // containsPoint(vector) {
  //   return Vector.between(this.centerPosition, vector).length() <= this.r;
  // }
  //
  static from(vector: Vector, r: number): Circle {
    return new Circle(Vector.from(vector), r);
  }
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
  containsRelativeCircle(relativeCircle: Circle, padding: number = 0): boolean {
    return this.relativeCircleIsWithinWidth(relativeCircle, padding)
      && this.relativeCircleIsWithinHeight(relativeCircle, padding);
  }

  translateEnclosedRelativeCircleIntoThis(enclosedCircle: Circle, padding: number): void {
    let x = enclosedCircle.centerPosition.x, y = enclosedCircle.centerPosition.y;
    if (!this.relativeCircleIsWithinWidth(enclosedCircle, padding)) {
      x = Math.sign(enclosedCircle.centerPosition.x) * (this.halfWidth - enclosedCircle.r - padding);
    }
    if (!this.relativeCircleIsWithinHeight(enclosedCircle, padding)) {
      y = Math.sign(enclosedCircle.centerPosition.y) * (this.halfHeight - enclosedCircle.r - padding);
    }
    enclosedCircle.centerPosition.changeTo(new Vector(x, y))
  }

  private relativeCircleIsWithinWidth(relativeCircle: Circle, padding: number = 0) {
    return Math.abs(relativeCircle.centerPosition.x) + relativeCircle.r + padding <= this.halfWidth;
  }

  private relativeCircleIsWithinHeight(relativeCircle: Circle, padding: number = 0) {
    return Math.abs(relativeCircle.centerPosition.y) + relativeCircle.r + padding <= this.halfHeight;
  }
}

class ZeroShape extends Shape {
  constructor() {
    super(new Vector(0, 0));
  }

  containsRelativeCircle(): boolean {
    return true;
  }

  translateEnclosedRelativeCircleIntoThis(): void {
  }
}

interface ShapeListener {
  onMovedToPosition: () => Promise<void>
  onRadiusChanged: () => Promise<void>
  onSizeChanged: () => Promise<void>
  onMovedToIntermediatePosition: () => Promise<void>
}

export {Circle, Rect, ZeroShape, CircleWithFixablePosition, Shape, ShapeListener};
