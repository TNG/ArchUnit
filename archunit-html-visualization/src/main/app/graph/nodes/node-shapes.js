'use strict';

const {Vector, vectors} = require('../infrastructure/vectors');
const {Circle, CircleWithFixablePosition, Rect, ZeroShape} = require('../infrastructure/shapes');

const NodeShape = class {
  constructor(node, listener, x, y, absoluteReferenceShape) {
    this._node = node;
    this._listener = listener;
    this.relativePosition = new Vector(x, y);
    this.absoluteReferenceShape = absoluteReferenceShape;
  }

  get _absoluteShape() {
    throw new Error('not implemented');
  }

  _updateAbsolutePosition() {
    this._absoluteShape.centerPosition.changeTo(vectors.add(this.relativePosition, this.absoluteReferenceShape.centerPosition));
  }

  _updateAbsolutePositionAndDescendants() {
    this._updateAbsolutePosition();
    this._node.getCurrentChildren().forEach(child => child._nodeShape._updateAbsolutePositionAndDescendants());
  }

  _updateAbsolutePositionAndChildren() {
    this._updateAbsolutePosition();
    this._node.getCurrentChildren().forEach(child => child._nodeShape._updateAbsolutePosition());
  }

  moveToPosition(x, y) {
    this.relativePosition.changeTo(new Vector(x, y));
    return this.completeMoveToIntermediatePosition();
  }

  completeMoveToIntermediatePosition() {
    throw new Error('not implemented');
  }

  _jumpToPosition() {
    throw new Error('not implemented');
  }

  _expand() {
    throw new Error('not implemented');
  }
};

const RootRect = class extends NodeShape {
  constructor(node, listener, halfWidth = 0, halfHeight = 0, x = 0, y = 0) {
    super(node, listener, x, y, new ZeroShape());
    this.absoluteRect = new Rect(new Vector(x, y), halfWidth, halfHeight);
  }

  get _absoluteShape() {
    return this.absoluteRect;
  }

  completeMoveToIntermediatePosition() {
    this._updateAbsolutePositionAndChildren();
    return this._listener.onMovedToPosition();
  }

  changeRadius(r) {
    this.absoluteRect.halfWidth = r;
    this.absoluteRect.halfHeight = r;
    const promise = this.moveToPosition(r, r); // Shift root to the middle
    const listenerPromise = this._listener.onRadiusChanged();
    return Promise.all([promise, listenerPromise]);
  }

  _jumpToPosition(x, y, directionVector) {
    this.relativePosition.changeTo(new Vector(x, y));
    this._updateAbsolutePositionAndDescendants();
    this._listener.onJumpedToPosition(directionVector);
    this._listener.onRimPositionChanged();
  }

  _expand(relativeCircle, padding, directionVector) {
    if (!this.absoluteRect.relativeCircleIsWithinWidth(relativeCircle, padding)) {
      this.absoluteRect.halfWidth = Math.abs(relativeCircle.centerPosition.x) + relativeCircle.r + padding;
    }

    if (!this.absoluteRect.relativeCircleIsWithinHeight(relativeCircle, padding)) {
      this.absoluteRect.halfHeight = Math.abs(relativeCircle.centerPosition.y) + relativeCircle.r + padding;
    }

    this._jumpToPosition(this.absoluteRect.halfWidth, this.absoluteRect.halfHeight, directionVector);
    this._listener.onRadiusSet();
  }
};

const NodeCircle = class extends NodeShape {
  constructor(node, listener, x = 0, y = 0, r = 0) {
    super(node, listener, x, y, node._parent._nodeShape._absoluteShape);
    this.absoluteFixableCircle = new CircleWithFixablePosition(x, y, r, this._node.getFullName());
  }

  unfix() {
    this.absoluteFixableCircle.unfix();
  }

  containsPoint(vector) {
    return this.absoluteFixableCircle.containsPoint(vector);
  }

  get _absoluteShape() {
    return this.absoluteFixableCircle;
  }

  changeRadius(r) {
    this.absoluteFixableCircle.r = r;
    return this._listener.onRadiusChanged();
  }

  jumpToRelativeDisplacement(dx, dy, padding) {
    const directionVector = new Vector(dx, dy);
    const position = vectors.add(this.relativePosition, directionVector);
    this._jumpToPosition(position.x, position.y, padding, directionVector);
  }

  _jumpToPosition(x, y, padding, directionVector) {
    const newRelativeCircle = Circle.from(new Vector(x, y), this.absoluteFixableCircle.r);
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
      this._node.getParent()._nodeShape._expand(newRelativeCircle, padding, directionVector);
    }
    this.relativePosition.changeTo(newRelativeCircle.centerPosition);
    this._updateAbsolutePositionAndDescendants();
    this._listener.onJumpedToPosition();
  }

  startMoveToIntermediatePosition() {
    if (!this.absoluteFixableCircle.fixed) {
      return this._listener.onMovedToIntermediatePosition();
    }
    return Promise.resolve();
  }

  completeMoveToIntermediatePosition() {
    this._updateAbsolutePositionAndChildren();
    if (!this.absoluteFixableCircle.fixed) {
      this.absoluteFixableCircle.fix();
      return this._listener.onMovedToPosition();
    }
    return Promise.resolve();
  }

  takeAbsolutePosition(circlePadding) {
    const newRelativePosition = this.absoluteFixableCircle.centerPosition.relativeTo(this.absoluteReferenceShape.centerPosition);
    const newRelativeCircle = Circle.from(newRelativePosition, this.absoluteFixableCircle.r);
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle, circlePadding)) {
      this.absoluteReferenceShape.translateEnclosedRelativeCircleIntoThis(newRelativeCircle, circlePadding);
    }
    this.relativePosition.changeTo(newRelativeCircle.centerPosition);
    this.absoluteFixableCircle.centerPosition.changeTo(vectors.add(this.relativePosition, this.absoluteReferenceShape.centerPosition));
  }

  _expand(relativeCircle, padding, directionVector) {
    const r = relativeCircle.centerPosition.length() + relativeCircle.r;
    this.absoluteFixableCircle.r = r + padding;
    const newRelativeCircle = Circle.from(this.relativePosition, this.absoluteFixableCircle.r);
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
      this._node.getParent()._nodeShape._expand(newRelativeCircle, padding, directionVector);
    }
    this._listener.onRadiusSet();
  }

  overlapsWith(otherCircle) {
    return this.absoluteFixableCircle.overlapsWith(otherCircle.absoluteFixableCircle);
  }
};

module.exports = {NodeCircle, RootRect};