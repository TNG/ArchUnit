'use strict';

const d3 = require('d3');

const View = class {
  constructor(parentSvgElement, node) {
    this._svgElement = d3.select(parentSvgElement)
      .append('g')
      .attr('id', node.getFullName())
      .attr('class', node.getClass())
      .attr('transform', `translate(${node.visualData.x}, ${node.visualData.y})`)
      .node();
  }

  hide() {
    d3.select(this._svgElement).style('visibility', 'hidden');
  }

  show() {
    d3.select(this._svgElement).style('visibility', 'visible');
  }

  update(nodeVisualData, transitionDuration) {
    const transition = d3.select(this._svgElement).transition().duration(transitionDuration);
    transition.attr('transform', `translate(${nodeVisualData.x}, ${nodeVisualData.y})`);
    transition.select('circle').attr('r', nodeVisualData.r);
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