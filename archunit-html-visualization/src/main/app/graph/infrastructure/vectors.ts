'use strict';

const vectors = {
  // distance: (vector1, vector2) => Vector.between(vector1, vector2).length(),
  //
  // norm: (vector, scale) => Vector.from(vector).norm(scale),
  //
  // getRevertedVector: vector => Vector.from(vector).revert(),
  //
  // getOrthogonalVector: vector => new Vector(vector.y, -vector.x),

  add: (vector1: Vector, vector2: Vector): Vector => Vector.from(vector1).add(vector2)
};

class Vector {
  private _x: number
  private _y: number

  constructor(x: number, y: number) {
    if (isNaN(x) || isNaN(y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${x}, ${y})`);
    }
    this._x = x;
    this._y = y;
  }

  // revert(): Vector {
  //   this._x = -this._x;
  //   this._y = -this._y;
  //   return this;
  // }

  get x(): number {
    return this._x
  }

  get y(): number {
    return this._y
  }
  // revertIf(condition) {
  //   return condition ? this.revert() : this;
  // }

  // relativeTo(position: Vector) {
  //   return Vector.from(this).subtract(position);
  // }
  //
  // isWithinCircle(vector: Vector, radius: number) {
  //   return Vector.between(this, vector).length() <= radius;
  // }
  //
  // length() {
  //   return Math.sqrt(this.x * this.x + this.y * this.y);
  // }
  //
  // makeDefaultIfNull() {
  //   if (this.length() === 0) {
  //     return this.changeTo(defaultVector);
  //   }
  //   return this;
  // }

  changeTo(vector: Vector): Vector {
    if (isNaN(vector.x) || isNaN(vector.y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${vector.x}, ${vector.y})`);
    }
    this._x = vector.x;
    this._y = vector.y;
    return this;
  }

  add(vector: Vector): Vector {
    this._x += vector.x;
    this._y += vector.y;
    return this;
  }
  //
  // scale(factor) {
  //   this.x *= factor;
  //   this.y *= factor;
  //   return this;
  // }
  //
  // norm(scale) {
  //   const length = this.length() || 1;
  //   return this.scale(scale / length);
  // }
  //
  // subtract(vector = zeroVector) {
  //   this.x -= vector.x;
  //   this.y -= vector.y;
  //   return this;
  // }
  //
  // equals(otherVector) {
  //   return Vector.equal(this, otherVector);
  // }

  static from(vector: Vector): Vector {
    return new Vector(vector._x, vector._y);
  }
  //
  // static between(originPoint, targetPoint) {
  //   return new Vector(targetPoint.x - originPoint.x, targetPoint.y - originPoint.y);
  // }
  //
  // static equal(vector1, vector2) {
  //   return vector1.x === vector2.x && vector1.y === vector2.y;
  // }
}

class FixableVector extends Vector {
  private _fixed: boolean
  private _fx: number;
  private _fy: number;

  constructor(x: number, y: number) {
    super(x, y);
    this._fixed = false;
  }

  fix(): void {
    this._fixed = true;
    this._updateFixPosition();
  }

  get fixed(): boolean {
    return this._fixed;
  }

  unfix(): void {
    this._fixed = false;
    this._fx = undefined;
    this._fy = undefined;
  }

  _updateFixPosition(): void {
    if (this._fixed) {
      this._fx = this.x;
      this._fy = this.y;
    }
  }

  changeTo(position: Vector): Vector {
    const updatedPosition = super.changeTo(position);
    this._updateFixPosition();
    return updatedPosition;
  }
}

export{Vector, FixableVector, vectors};
