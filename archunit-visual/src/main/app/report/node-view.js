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

  onClick(handler) {
    d3.select(this._svgElement).on('click', handler);
  }
};

module.exports = View;