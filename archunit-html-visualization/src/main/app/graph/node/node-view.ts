'use strict';

import {NodeDescription} from "./node";
import {SvgSelection} from "../infrastructure/svg-selection";
import {Vector} from "../infrastructure/vectors";
import {SVG} from "../infrastructure/svg";


interface NodeViewFactory {
  getNodeView(nodeDescription: NodeDescription): NodeView
}

class NodeView {
  _svgElement: SvgSelection
  private _svgElementForChildren: SvgSelection;
  private _text: SvgSelection;
  private _circle: SvgSelection;
  private _transitionDuration: number;

  constructor(nodeDescription: NodeDescription, svg: SVG, transitionDuration: number) {
    this._transitionDuration = transitionDuration;

    this._svgElement = svg.createGroup(nodeDescription.fullName.replace(/\\$/g, '.-'));
    this._svgElement.cssClasses = ['node', nodeDescription.type];

    this._circle = this._svgElement.addCircle();

    this._text = this._svgElement.addText(nodeDescription.name);

    this._svgElementForChildren = this._svgElement.addGroup();
    // this._svgSelectionForDependencies = this._svgElement.addGroup();

    // this._onDrag(dragHandler);
    // this._onClick(clickHandler);
  }

  addChildView(childView: NodeView) {
    this._svgElementForChildren.addChild(childView._svgElement);
  }

  // get svgSelectionForDependencies() {
  //   return this._svgSelectionForDependencies;
  // }

  // detachFromParent() {
  //   this._svgElement.detachFromParent();
  // }

  getTextWidth() {
    return this._text.textWidth;
  }

  // set foldable(foldable) {
  //   this._svgElement.removeCssClasses('foldable', 'unfoldable');
  //   if (foldable) {
  //     this._svgElement.addCssClass('foldable');
  //   } else {
  //     this._svgElement.addCssClass('unfoldable');
  //   }
  // }

  // hide() {
  //   this._svgElement.hide();
  // }
  //
  // show() {
  //   this._svgElement.show();
  // }
  //
  // jumpToPosition(position: Vector) {
  //   this._svgElement.translate(position);
  // }

  // changeRadius(r: number, textOffset: number) {
  //   const radiusPromise = this._circle.createTransitionWithDuration(transitionDuration)
  //     .step(svgSelection => svgSelection.radius = r)
  //     .finish();
  //   const textPromise = this._text.createTransitionWithDuration(transitionDuration)
  //     .step(svgSelection => svgSelection.offsetY = textOffset)
  //     .finish();
  //   return Promise.all([radiusPromise, textPromise]);
  // }

  setRadius(r: number, textOffset: number) {
    this._circle.radius = r;
    this._text.offsetY = textOffset;
  }

  startMoveToPosition(position: Vector) {
    return this._svgElement.createTransitionWithDuration(this._transitionDuration)
      .step(svgSelection => svgSelection.translate(position))
      .finish();
  }

  moveToPosition(position: Vector) {
    return this._svgElement.createTransitionWithDuration(this._transitionDuration)
      .step(svgSelection => svgSelection.translate(position))
      .finish();
  }

  // _onClick(clickHandler) {
  //   this._circle.onClick(clickHandler);
  //   this._text.onClick(clickHandler);
  // }
  //
  // _onDrag(handler) {
  //   this._svgElement.onDrag(handler);
  // }
}

const init = (transitionDuration: number, svg: SVG): NodeViewFactory => ({
  getNodeView: (nodeDescription: NodeDescription) => new NodeView(nodeDescription, svg, transitionDuration),
});


export {init, NodeView};
