'use strict';

import * as d3 from 'd3';

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
      this._width = 0;
      this._height = 0;
    }

    render(rootRadius) {
      this.renderSizeIfNecessary(rootRadius);
      this.renderPosition(d3.select(this._translater), rootRadius);
    }

    renderWithTransition(rootRadius) {
      this.renderSizeIfNecessary(rootRadius);
      return createPromiseOnEndOfTransition(d3.select(this._translater).transition().duration(transitionDuration), t => this.renderPosition(t, rootRadius));
    }

    renderSizeIfNecessary(rootRadius) {
      const windowWidth = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
      const windowHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
      const requiredSize = parseInt(2 * rootRadius + 4);
      const expandedSize = parseInt(3 * rootRadius + 4);
      const requiredWidth = Math.max(windowWidth, requiredSize);
      const requiredHeight = Math.max(windowHeight, requiredSize);
      if (requiredWidth > this._width) {
        this._width = requiredSize < windowWidth ? windowWidth : expandedSize;
        d3.select(this._svg).attr('width', this._width);
      }

      if (requiredHeight > this._height) {
        this._height = requiredSize < windowHeight ? windowHeight : expandedSize;
        d3.select(this._svg).attr('height', this._height);
      }
    }

    renderPosition(selection, rootRadius) {
      return selection.attr('transform',
        `translate(${parseInt(d3.select(this._svg).attr('width')) / 2 - rootRadius}, ${parseInt(d3.select(this._svg).attr('height')) / 2 - rootRadius})`);
    }
  };

  return View;
};

export default {init};