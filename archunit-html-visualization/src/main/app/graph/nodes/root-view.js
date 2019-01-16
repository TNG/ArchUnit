'use strict';

const d3 = require('d3');
const {Vector} = require('../infrastructure/vectors');

const init = (transitionDuration) => {
  const createPromiseOnEndOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('interrupt', () => resolve()).on('end', resolve));

  const View = class {
    constructor(parentSvgElement, node) {
      this._svgElement = d3.select(parentSvgElement)
        .append('g')
        .data([node])
        .attr('id', node.getFullName().replace(/\\$/g, '.-'))
        .node();

      this._svgElementForChildren = d3.select(this._svgElement).append('g').node();
      this._svgElementForDependencies = d3.select(this._svgElement).append('g').node();

      document.onkeyup = event => {
        if (event.key === 'Alt' || event.key === 'Control') {
          node.relayoutCompletely();
        }
      }
    }

    get svgElementForDependencies() {
      return this._svgElementForDependencies;
    }

    get svgElementForChildren() {
      return this._svgElementForChildren;
    }

    jumpToPosition(position, directionVector) {
      const container = d3.select('#container').node();

      if (directionVector.x < 0) {
        container.scrollLeft += position.x - this._position.x;
      }
      if (directionVector.y < 0) {
        container.scrollTop += position.y - this._position.y;
      }

      d3.select(this._svgElement).attr('transform', `translate(${position.x}, ${position.y})`);

      this._position = Vector.from(position);
    }

    moveToPosition(position) {
      this._position = Vector.from(position);
      return createPromiseOnEndOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    updateNodeType() {
    }
  };

  return View;
};


module.exports = {init};