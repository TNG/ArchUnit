'use strict';

const svg = require('../infrastructure/gui-elements').svg;

const d3 = require('d3');
const {Vector} = require('../infrastructure/vectors');

const init = (transitionDuration) => {
  class View {
    constructor({fullNodeName}, node) {
      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._svgElementForChildren = this._svgElement.addGroup();
      this._svgElementForDependencies = this._svgElement.addGroup();

      document.onkeyup = event => {
        if (event.key === 'Alt' || event.key === 'Control') {
          node.relayoutCompletely();
        }
      }
    }

    get svgElement() {
      return this._svgElement;
    }

    addChildView(childView) {
      this._svgElementForChildren.addChild(childView._svgElement);
    }

    get svgElementForDependencies() {
      return this._svgElementForDependencies.domElement;
    }

    jumpToPosition(position, directionVector) {
      const container = d3.select('#container').node();

      if (directionVector.x < 0) {
        container.scrollLeft += position.x - this._position.x;
      }
      if (directionVector.y < 0) {
        container.scrollTop += position.y - this._position.y;
      }

      d3.select(this._svgElement.domElement).attr('transform', `translate(${position.x}, ${position.y})`);

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