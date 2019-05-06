'use strict';

const {Vector} = require('../infrastructure/vectors');

const init = (transitionDuration, svg, document) => {
  class View {
    constructor(fullNodeName, onkeyupHandler) {
      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._svgElementForChildren = this._svgElement.addGroup();
      this._svgSelectionForDependencies = this._svgElement.addGroup();

      document.onKeyUp(onkeyupHandler);
    }

    get position() {
      return this._position;
    }

    get svgElement() {
      return this._svgElement;
    }

    addChildView(childView) {
      this._svgElementForChildren.addChild(childView._svgElement);
    }

    get svgSelectionForDependencies() {
      return this._svgSelectionForDependencies;
    }

    jumpToPosition(position) {
      this._svgElement.translate(position);
      this._position = Vector.from(position);
    }

    moveToPosition(position) {
      this._position = Vector.from(position);
      return this._svgElement.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.translate(position))
        .finish();
    }
  }

  return View;
};

module.exports = {init};