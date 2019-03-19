'use strict';

const svg = require('../infrastructure/gui-elements').svg;

const d3 = require('d3');
const {Vector} = require('../infrastructure/vectors');

const init = (transitionDuration) => {
  const createPromiseOnEndOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('interrupt', () => resolve()).on('end', resolve));

  class View {
    constructor({fullNodeName}, node) {
      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._svgElementForChildren = d3.select(this._svgElement.domElement()).append('g').node();
      this._svgElementForDependencies = d3.select(this._svgElement.domElement()).append('g').node();

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
      this._svgElementForChildren.appendChild(childView._svgElement.domElement);
    }

    get svgElementForDependencies() {
      return this._svgElementForDependencies;
    }

    jumpToPosition(position, directionVector) {
      const container = d3.select('#container').node();

      if (directionVector.x < 0) {
        container.scrollLeft += position.x - this._position.x;
      }
      if (directionVector.y < 0) {
        container.scrollTop += position.y - this._position.y;
      }

      d3.select(this._svgElement.domElement()).attr('transform', `translate(${position.x}, ${position.y})`);

      this._position = Vector.from(position);
    }

    moveToPosition(position) {
      this._position = Vector.from(position);
      return createPromiseOnEndOfTransition(d3.select(this._svgElement.domElement()).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    updateNodeType() {
    }
  }

  return View;
};


module.exports = {init};