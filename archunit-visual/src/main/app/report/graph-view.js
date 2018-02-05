'use strict';

const d3 = require('d3');

const init = (transitionDuration) => {

  const createPromiseOnEndOfTransition = (transition, transitionRunner) => {
    if (transition.empty()) {
      return Promise.resolve();
    }
    else {
      return new Promise(resolve => transitionRunner(transition).on('end', resolve));
    }
  };

  const View = class {
    constructor(svg) {
      this._svg = svg;
      this._translater = d3.select(this._svg).append('g').attr('id', 'translater').node();
      this.svgElementForNodes = d3.select(this._translater).append('g').node();
      this.svgElementForDependencies = d3.select(this._translater).append('g').node();
    }

    render(rootRadius) {
      this.renderSize(rootRadius);
      this.renderPosition(d3.select(this._translater), rootRadius);
    }

    renderWithTransition(rootRadius) {
      this.renderSize(rootRadius);
      return createPromiseOnEndOfTransition(d3.select(this._translater).transition().duration(transitionDuration), t => this.renderPosition(t, rootRadius));
    }

    renderSize(rootRadius) {
      const windowWidth = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
      const windowHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
      d3.select(this._svg).attr('width', Math.max(parseInt(2 * rootRadius + 4), windowWidth));
      d3.select(this._svg).attr('height', Math.max(parseInt(2 * rootRadius + 4), windowHeight));
    }

    renderPosition(selection, rootRadius) {
      return selection.attr('transform',
        `translate(${parseInt(d3.select(this._svg).attr('width')) / 2 - rootRadius}, ${parseInt(d3.select(this._svg).attr('height')) / 2 - rootRadius})`);
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});