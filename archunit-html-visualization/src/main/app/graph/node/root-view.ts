'use strict';

import {Vector} from "../infrastructure/vectors";
import {SVG} from "../infrastructure/svg"
import {SvgSelection} from "../infrastructure/svg-selection";

const init = (transitionDuration: number, svg: SVG, document: Document) => {
  class View {
    private _svgElement: SvgSelection
    private _svgElementForChildren: SvgSelection
    private _svgSelectionForDependencies: SvgSelection
    private _position: Vector

    constructor(fullNodeName: string, onkeyupHandler: ((this: GlobalEventHandlers, ev: KeyboardEvent) => any)) {
      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._svgElementForChildren = this._svgElement.addGroup();
      this._svgSelectionForDependencies = this._svgElement.addGroup();

      document.onkeyup = onkeyupHandler;
    }

    get position() {
      return this._position;
    }

    get svgElement() {
      return this._svgElement;
    }

    addChildView(childView: View) {
      this._svgElementForChildren.addChild(childView._svgElement);
    }

    get svgSelectionForDependencies() {
      return this._svgSelectionForDependencies;
    }

    jumpToPosition(position: Vector) {
      this._svgElement.translate(position);
      this._position = Vector.from(position);
    }

    moveToPosition(position: Vector) {
      this._position = Vector.from(position);
      return this._svgElement.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.translate(position))
        .finish();
    }
  }

  return View;
};

module.exports = {init};
