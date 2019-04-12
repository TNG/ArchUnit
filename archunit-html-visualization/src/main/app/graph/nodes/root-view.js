'use strict';

const {Vector} = require('../infrastructure/vectors');

const init = (transitionDuration, svg, document) => {
  class View {
    constructor(fullNodeName, onkeyupHandler, svgContainerDivDomElement) {
      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._svgContainerDivSelection = document.selectDiv(svgContainerDivDomElement);

      this._svgElementForChildren = this._svgElement.addGroup();
      this._svgSelectionForDependencies = this._svgElement.addGroup();

      document.onKeyUp(onkeyupHandler);
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

    jumpToPosition(position, directionVector) {
      //TODO: test this
      if (directionVector.x < 0) {
        this._svgContainerDivSelection.scrollLeft += position.x - this._position.x;
      }
      if (directionVector.y < 0) {
        this._svgContainerDivSelection.scrollTop += position.y - this._position.y;
      }

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