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

  onClick(handler) {
    d3.select(this._svgElement).on('click', handler);
  }

  onDrag(handler) {
    const drag = d3.drag().on('drag', () => handler(d3.event.dx, d3.event.dy));
    d3.select(this._svgElement).call(drag);
  }
};

module.exports = View;