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

  get absoluteShape() {
    throw new Error('not implemented');
  }

  _updateAbsolutePosition() {
    this.absoluteShape.position.changeTo(vectors.add(this.relativePosition, this.absoluteReferenceShape.position));
  }

  _updateAbsolutePositionAndDescendants() {
    this._updateAbsolutePosition();
    this._node.getCurrentChildren().forEach(child => child.nodeShape._updateAbsolutePositionAndDescendants());
  }

  _updateAbsolutePositionAndChildren() {
    this._updateAbsolutePosition();
    this._node.getCurrentChildren().forEach(child => child.nodeShape._updateAbsolutePosition());
  }

  moveToPosition(x, y) {
    this.relativePosition.changeTo(new Vector(x, y));
    return this.completeMoveToIntermediatePosition();
  }

  completeMoveToIntermediatePosition() {
    throw new Error('not implemented');
  }

  jumpToPosition() {
    throw new Error('not implemented');
  }

  expand() {
    throw new Error('not implemented');
  }
};

const RootRect = class extends NodeShape {
  constructor(node, listener, halfWidth = 0, halfHeight = 0, x = 0, y = 0) {
    super(node, listener, x, y, new ZeroShape());
    this.absoluteRect = new Rect(new Vector(x, y), halfWidth, halfHeight);
  }

  get absoluteShape() {
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

  jumpToPosition(x, y, directionVector) {
    this.relativePosition.changeTo(new Vector(x, y));
    this._updateAbsolutePositionAndDescendants();
    this._listener.onJumpedToPosition(directionVector);
    this._listener.onRimPositionChanged();
  }

  expand(relativeCircle, padding, directionVector) {
    if (!this.absoluteRect._relativeCircleIsWithinWidth(relativeCircle, padding)) {
      this.absoluteRect.halfWidth = Math.abs(relativeCircle.position.x) + relativeCircle.r + padding;
    }

    if (!this.absoluteRect._relativeCircleIsWithinHeight(relativeCircle, padding)) {
      this.absoluteRect.halfHeight = Math.abs(relativeCircle.position.y) + relativeCircle.r + padding;
    }

    this.jumpToPosition(this.absoluteRect.halfWidth, this.absoluteRect.halfHeight, directionVector);
    this._listener.onRadiusSet();
  }
};

const NodeCircle = class extends NodeShape {
  constructor(node, listener, x = 0, y = 0, r = 0) {
    super(node, listener, x, y, node._parent.nodeShape.absoluteShape);
    this.absoluteCircle = new CircleWithFixablePosition(x, y, r, this._node.getFullName());
  }

  get absoluteShape() {
    return this.absoluteCircle;
  }

  getRadius() {
    return this.absoluteCircle.r;
  }

  changeRadius(r) {
    this.absoluteCircle.r = r;
    return this._listener.onRadiusChanged();
  }

  jumpToRelativeDisplacement(dx, dy, padding) {
    const directionVector = new Vector(dx, dy);
    const position = vectors.add(this.relativePosition, directionVector);
    this.jumpToPosition(position.x, position.y, padding, directionVector);
  }

  jumpToPosition(x, y, padding, directionVector) {
    const newRelativeCircle = Circle.from(new Vector(x, y), this.getRadius());
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
      this._node.getParent().nodeShape.expand(newRelativeCircle, padding, directionVector);
    }
    this.relativePosition.changeTo(newRelativeCircle.position);
    this._updateAbsolutePositionAndDescendants();
    this._listener.onJumpedToPosition();
  }

  startMoveToIntermediatePosition() {
    if (!this.absoluteCircle.position.fixed) {
      return this._listener.onMovedToIntermediatePosition();
    }
    return Promise.resolve();
  }

  completeMoveToIntermediatePosition() {
    this._updateAbsolutePositionAndChildren();
    if (!this.absoluteCircle.position.fixed) {
      this.absoluteCircle.position.fix();
      return this._listener.onMovedToPosition();
    }
    return Promise.resolve();
  }

  takeAbsolutePosition(circlePadding) {
    const newRelativePosition = this.absoluteCircle.position.relativeTo(this.absoluteReferenceShape.position);
    const newRelativeCircle = Circle.from(newRelativePosition, this.getRadius());
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle, circlePadding)) {
      this.absoluteReferenceShape.translateEnclosedRelativeCircleIntoThis(newRelativeCircle, circlePadding);
    }
    this.relativePosition.changeTo(newRelativeCircle.position);
    this.absoluteCircle.position.changeTo(vectors.add(this.relativePosition, this.absoluteReferenceShape.position));
  }

  expand(relativeCircle, padding, directionVector) {
    const r = relativeCircle.position.length() + relativeCircle.r;
    this.absoluteCircle.r = r + padding;
    const newRelativeCircle = Circle.from(this.relativePosition, this.getRadius());
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
      this._node.getParent().nodeShape.expand(newRelativeCircle, padding, directionVector);
    }
    this._listener.onRadiusSet();
  }

  overlapsWith(otherCircle) {
    return this.absoluteCircle.overlapsWith(otherCircle.absoluteCircle);
  }
};

module.exports = {NodeCircle, RootRect};