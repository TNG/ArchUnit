'use strict';

import {Vector} from "../infrastructure/vectors";
import {SVG} from "../infrastructure/svg"
import {SvgSelection} from "../infrastructure/svg-selection";
import {NodeView} from "./node-view";
import {NodeDescription} from "./node";

interface RootViewFactory {
  getRootView(nodeDescription: NodeDescription): RootView
}

export class RootView {
  private _svgElement: SvgSelection
  private _svgElementForChildren: SvgSelection
  private _svgSelectionForDependencies: SvgSelection
  private _position: Vector
  private _transitionDuration: number;

  constructor(fullNodeName: string,svg: SVG, transitionDuration: number) {
    this._transitionDuration = transitionDuration; // eslint-disable-line no-unused-vars
    this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

    this._svgElementForChildren = this._svgElement.addGroup();
    this._svgSelectionForDependencies = this._svgElement.addGroup();
  }

  get position(): Vector {
    return this._position;
  }

  get svgElement(): SvgSelection {
    return this._svgElement;
  }

  addChildView(childView: NodeView): void {
    this._svgElementForChildren.addChild(childView._svgElement);
  }

  get svgSelectionForDependencies(): SvgSelection {
    return this._svgSelectionForDependencies;
  }

  jumpToPosition(position: Vector): void {
    this._svgElement.translate(position);
    this._position = Vector.from(position);
  }

  moveToPosition(position: Vector): Promise<void> {
    this._position = Vector.from(position);
    return this._svgElement.createTransitionWithDuration(this._transitionDuration)
      .step(svgSelection => svgSelection.translate(position))
      .finish();
  }
}

const init = (transitionDuration: number, svg: SVG): RootViewFactory => ({
  getRootView: (nodeDescription: NodeDescription) => new RootView(nodeDescription.fullName, svg, transitionDuration)
});

export {init, RootViewFactory};
