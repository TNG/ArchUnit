'use strict';

const d3 = require('d3');

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


      document.onkeyup = event => {
        if (event.key === 'Alt' || event.key === 'Control') {
          node.relayoutCompletely();
        }
      }
    }

    jumpToPosition(position) {
      d3.select(this._svgElement).attr('transform', `translate(${position.x}, ${position.y})`);
    }

    moveToPosition(position) {
      return createPromiseOnEndOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    updateNodeType() {
    }
  };

  return View;
};


module.exports = {init};