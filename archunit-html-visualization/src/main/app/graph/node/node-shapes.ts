'use strict';

import {Vector, vectors} from '../infrastructure/vectors'
import {Circle, CircleWithFixablePosition, Rect, Shape, ShapeListener, ZeroShape} from '../infrastructure/shapes'
import {Node} from './node'

abstract class NodeShape {
  protected _node: Node
  protected _listener: ShapeListener
  private readonly _relativePosition: Vector
  protected absoluteReferenceShape: Shape

  protected constructor(node: Node, listener: ShapeListener, x: number, y: number, absoluteReferenceShape: Shape) {
    this._node = node;
    this._listener = listener;
    this._relativePosition = new Vector(x, y);
    this.absoluteReferenceShape = absoluteReferenceShape;
  }

  abstract get absoluteShape(): Shape

  get relativePosition(): Vector {
    return this._relativePosition;
  }

  _updateAbsolutePosition(): void {
    this.absoluteShape.centerPosition.changeTo(vectors.add(this._relativePosition, this.absoluteReferenceShape.centerPosition));
  }
  //
  // _updateAbsolutePositionAndDescendants() {
  //   this._updateAbsolutePosition();
  //   this._node.getCurrentChildren().forEach(child => child._nodeShape._updateAbsolutePositionAndDescendants());
  // }
  //
  _updateAbsolutePositionAndChildren(): void {
    this._updateAbsolutePosition();
    this._node.getCurrentChildren().forEach(child => child.nodeShape._updateAbsolutePosition());
  }

  moveToPosition(x: number, y: number): Promise<void> {
    this._relativePosition.changeTo(new Vector(x, y));
    return this.completeMoveToIntermediatePosition();
  }

  abstract completeMoveToIntermediatePosition(): Promise<void>
    // throw new Error('not implemented');

  abstract changeRadius(r: number): Promise<void>
  //
  // _jumpToPosition() {
  //   throw new Error('not implemented');
  // }
  //
  // _expand() {
  //   throw new Error('not implemented');
  // }
}

class RootRect extends NodeShape {
  private absoluteRect: Rect

  constructor(node: Node, listener: ShapeListener, halfWidth = 0, halfHeight = 0, x = 0, y = 0) {
    super(node, listener, x, y, new ZeroShape());
    this.absoluteRect = new Rect(new Vector(x, y), halfWidth, halfHeight);
  }

  get absoluteShape(): Shape {
    return this.absoluteRect;
  }

  completeMoveToIntermediatePosition(): Promise<void> {
    this._updateAbsolutePositionAndChildren();
    return this._listener.onMovedToPosition();
  }

  changeRadius(r: number): Promise<void> {
    this.absoluteRect.halfWidth = r;
    this.absoluteRect.halfHeight = r;
    const promise = this.moveToPosition(r, r); // Shift root to the middle
    const listenerPromise = this._listener.onSizeChanged();
    return Promise.all([promise, listenerPromise]).then();
  }

  // _jumpToPosition(x, y) {
  //   const oldRelativePosition = Vector.from(this.relativePosition);
  //   this.relativePosition.changeTo(new Vector(x, y));
  //   this._updateAbsolutePositionAndDescendants();
  //   this._listener.onJumpedToPosition(Vector.between(oldRelativePosition, this.relativePosition));
  //   this._listener.onRimPositionChanged();
  // }
  //
  // _expand(relativeCircle, padding) {
  //   if (!this.absoluteRect.relativeCircleIsWithinWidth(relativeCircle, padding)) {
  //     this.absoluteRect.halfWidth = Math.abs(relativeCircle.centerPosition.x) + relativeCircle.r + padding;
  //   }
  //
  //   if (!this.absoluteRect.relativeCircleIsWithinHeight(relativeCircle, padding)) {
  //     this.absoluteRect.halfHeight = Math.abs(relativeCircle.centerPosition.y) + relativeCircle.r + padding;
  //   }
  //
  //   this._jumpToPosition(this.absoluteRect.halfWidth, this.absoluteRect.halfHeight);
  //   this._listener.onNodeRimChanged();
  // }
}

class NodeCircle extends NodeShape {
  private readonly absoluteFixableCircle: CircleWithFixablePosition

  constructor(node: Node, listener: ShapeListener, x = 0, y = 0, r = 0) {
    super(node, listener, x, y, node.parent.nodeShape.absoluteShape);
    this.absoluteFixableCircle = new CircleWithFixablePosition(x, y, r, this._node.getFullName());
  }

  // unfix() {
  //   this.absoluteFixableCircle.unfix();
  // }
  //
  // containsPoint(vector: Vector) {
  //   return this.absoluteFixableCircle.containsPoint(vector);
  // }
  //
  get absoluteShape(): CircleWithFixablePosition {
    return this.absoluteFixableCircle;
  }

  changeRadius(r: number): Promise<void> {
    this.absoluteFixableCircle.r = r;
    return this._listener.onRadiusChanged();
  }
  //
  // jumpToRelativeDisplacement(dx, dy, padding) {
  //   const directionVector = new Vector(dx, dy);
  //   const position = vectors.add(this.relativePosition, directionVector);
  //   this._jumpToPosition(position.x, position.y, padding);
  // }
  //
  // _jumpToPosition(x, y, padding) {
  //   const newRelativeCircle = Circle.from(new Vector(x, y), this.absoluteFixableCircle.r);
  //   if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
  //     this._node.parent._nodeShape._expand(newRelativeCircle, padding);
  //   } else {
  //     //call the listener, if no node is expanded
  //     this._listener.onNodeRimChanged();
  //   }
  //   this.relativePosition.changeTo(newRelativeCircle.centerPosition);
  //   this._updateAbsolutePositionAndDescendants();
  //   this._listener.onJumpedToPosition();
  // }
  //
  startMoveToIntermediatePosition(): Promise<void> {
    if (!this.absoluteFixableCircle.fixed) {
      return this._listener.onMovedToIntermediatePosition();
    }
    return Promise.resolve();
  }

  completeMoveToIntermediatePosition(): Promise<void> {
    this._updateAbsolutePositionAndChildren();
    if (!this.absoluteFixableCircle.fixed) {
      this.absoluteFixableCircle.fix();
      return this._listener.onMovedToPosition();
    }
    return Promise.resolve();
  }

  takeAbsolutePosition(circlePadding: number): void {
    const newRelativePosition = this.absoluteFixableCircle.centerPosition.relativeTo(this.absoluteReferenceShape.centerPosition);
    const newRelativeCircle = Circle.from(newRelativePosition, this.absoluteFixableCircle.r);
    if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle, circlePadding)) {
      this.absoluteReferenceShape.translateEnclosedRelativeCircleIntoThis(newRelativeCircle, circlePadding);
    }
    this.relativePosition.changeTo(newRelativeCircle.centerPosition);
    this.absoluteFixableCircle.centerPosition.changeTo(vectors.add(this.relativePosition, this.absoluteReferenceShape.centerPosition));
  }
  //
  // _expand(relativeCircle, padding) {
  //   const r = relativeCircle.centerPosition.length() + relativeCircle.r;
  //   this.absoluteFixableCircle.r = r + padding;
  //   const newRelativeCircle = Circle.from(this.relativePosition, this.absoluteFixableCircle.r);
  //   if (!this.absoluteReferenceShape.containsRelativeCircle(newRelativeCircle)) {
  //     this._node.parent._nodeShape._expand(newRelativeCircle, padding);
  //   } else {
  //     //call the listener for the last expanded node
  //     this._listener.onNodeRimChanged();
  //   }
  //   this._listener.onRadiusSet();
  // }
  //
  // overlapsWith(otherCircle) {
  //   return this.absoluteFixableCircle.overlapsWith(otherCircle.absoluteFixableCircle);
  // }
}

export {NodeShape, NodeCircle, RootRect};
