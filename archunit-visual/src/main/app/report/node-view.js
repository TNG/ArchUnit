'use strict';

const d3 = require('d3');

const createPromiseOnEndOfTransition = (transition, transitionRunner) => {
  if (transition.empty()) {
    return Promise.resolve();
  }
  else {
    return new Promise(resolve => transitionRunner(transition).on('end', resolve));
  }
};

const View = class {
  constructor(parentSvgElement, node) {
    this._svgElement = d3.select(parentSvgElement)
      .append('g')
      .attr('id', node.getFullName())
      .attr('class', node.getClass())
      .attr('transform', `translate(${node.visualData.x}, ${node.visualData.y})`)
      .node();
  }

  //FIXME: work with inherit
  hide() {
    d3.select(this._svgElement).style('visibility', 'hidden');
  }

  show() {
    d3.select(this._svgElement).style('visibility', 'visible');
  }

  update(nodeVisualData, textOffset, transitionDuration) {
    const transition = d3.select(this._svgElement).transition().duration(transitionDuration);
    const transformPromise = createPromiseOnEndOfTransition(transition, t => t.attr('transform', `translate(${nodeVisualData.x}, ${nodeVisualData.y})`));
    const radiusPromise = createPromiseOnEndOfTransition(transition.select('circle'), t => t.attr('r', nodeVisualData.r));
    const textPromise = createPromiseOnEndOfTransition(transition.select('text'), t => t.attr('dy', textOffset));
    return Promise.all([transformPromise, radiusPromise, textPromise]);
  }

  onClick(handler) {
    d3.select(this._svgElement).on('click', handler);
  }

  onDrag(handler) {
    const drag = d3.drag().on('drag', () => handler(d3.event.dx, d3.event.dy));
    d3.select(this._svgElement).call(drag);
  }
};

module.exports = View;