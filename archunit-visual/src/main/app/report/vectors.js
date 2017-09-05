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
    const length = getLength(vector);
    return vectors.vectorOf(scale * vector.x / length, scale * vector.y / length);
  },

  getRevertedVector: vector => vectors.vectorOf(-vector.x, -vector.y),

  getOrthogonalVector: vector => vectors.vectorOf(vector.y, -vector.x),

  addVectors: (vector1, vector2) => vectors.vectorOf(vector1.x + vector2.x, vector1.y + vector2.y),

  angleToVector: vector => Math.asin((Math.sign(vector.x) || 1) * vector.y / getLength(vector)),

  getAngleDeg: angleRad => Math.round(angleRad * (180 / Math.PI)),

  /**
   * @param radiusOuterCircle radius of the outer circle
   * @param innerCircleVisualData visualData of the inner circle (including radius, x and y coordinate relative
   * to the middle point of the outer circle)
   * @param shiftVector vector that corresponds to the user's shift
   * @return the vector of the middle point of the inner circle, so that the inner circle is just withing the
   * outer circle (considering the shift direction)
   */
  getMaximumShiftedVectorWithinCircle: (radiusOuterCircle, innerCircleVisualData, shiftVector) => {
    const c1 = shiftVector.x * shiftVector.x + shiftVector.y * shiftVector.y;
    const c2 = Math.pow(radiusOuterCircle - innerCircleVisualData.r, 2);
    const c3 = -Math.pow(innerCircleVisualData.y * shiftVector.x - innerCircleVisualData.x * shiftVector.y, 2);
    const c4 = -(innerCircleVisualData.x * shiftVector.x + innerCircleVisualData.y * shiftVector.y);
    const scale = (c4 + Math.sqrt(c3 + c2 * c1)) / c1;
    return {
      x: innerCircleVisualData.x + scale * shiftVector.x,
      y: innerCircleVisualData.y + scale * shiftVector.y
    };
  }
};

const Vector = class {
  constructor(x, y) {
    if (isNaN(x) || isNaN(y)) {
      throw new Error(`Vector must be initialized with numbers 'x' and 'y', but was (${x}, ${y})`);
    }
    this.x = x;
    this.y = y;
  }

  length() {
    return Math.sqrt(this.x * this.x + this.y * this.y);
  }

  static between(originPoint, targetPoint) {
    return new Vector(targetPoint.x - originPoint.x, targetPoint.y - originPoint.y);
  }
};

module.exports.Vector = Vector;
module.exports.vectors = vectors;