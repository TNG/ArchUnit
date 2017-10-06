'use strict';

const d3 = require('d3');

const init = (transitionDuration) => {

  const View = class {
    constructor(svg) {
      this._svg = svg;
      this._translater = d3.select(this._svg).append('g').attr('id', 'translater').node();
      this._gTree = d3.select(this._translater).append('g').node();
      this._gEdges = d3.select(this._translater).append('g').node();
    }

    render(rootRadius) {
      this.renderSize(rootRadius);
      this.renderPosition(d3.select(this._translater), rootRadius);
    }

    renderWithTransition(rootRadius) {
      this.renderSize(rootRadius);
      this.renderPosition(d3.select(this._translater).transition().duration(transitionDuration), rootRadius);
    }

    renderSize(rootRadius) {
      d3.select(this._svg).attr('width', Math.max(parseInt(2 * rootRadius + 4),
        d3.select('#container').node().getBoundingClientRect().width));
      d3.select(this._svg).attr('height', Math.max(parseInt(2 * rootRadius + 4),
        d3.select('#container').node().getBoundingClientRect().height));
    }

    renderPosition(selection, rootRadius) {
      selection.attr('transform',
        `translate(${parseInt(d3.select(this._svg).attr('width')) / 2 - rootRadius}, ${parseInt(d3.select(this._svg).attr('height')) / 2 - rootRadius})`);
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});