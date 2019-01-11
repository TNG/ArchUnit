'use strict';

const d3 = require('d3');

const init = (transitionDuration) => {

  const createPromiseOnEndOfTransition = (transition, transitionRunner) => {
    if (transition.empty()) {
      return Promise.resolve();
    }
    else {
      return new Promise(resolve => transitionRunner(transition).on('end', () => resolve()));
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

    render(halfWidth, halfHeight) {
      this.renderSizeIfNecessary(halfWidth, halfHeight);
      this.renderPosition(d3.select(this._translater), halfWidth, halfHeight);
    }

    renderWithTransition(halfWidth, halfHeight) {
      this.renderSizeIfNecessary(halfWidth, halfHeight);
      return createPromiseOnEndOfTransition(d3.select(this._translater).transition().duration(transitionDuration), t => this.renderPosition(t, halfWidth, halfHeight));
    }

    renderSizeIfNecessary(halfWidth, halfHeight) {
      const windowWidth = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
      const windowHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
      const requiredWidth = parseInt(2 * halfWidth + 4);
      const expandedWidth = parseInt(2 * halfWidth + 4);
      const requiredHeight = parseInt(2 * halfHeight + 4);
      const expandedHeight = parseInt(2 * halfHeight + 4);
      const minWidth = Math.max(windowWidth, requiredWidth);
      const maxWidth = Math.max(windowWidth, expandedWidth);
      const minHeight = Math.max(windowHeight, requiredHeight);
      const maxHeight = Math.max(windowHeight, expandedHeight);
      if (minWidth > this._width || maxWidth < this._width) {
        this._width = Math.max(expandedWidth, windowWidth);
        d3.select(this._svg).attr('width', this._width);
      }

      if (minHeight > this._height || maxHeight < this._height) {
        this._height = Math.max(expandedHeight, windowHeight);
        d3.select(this._svg).attr('height', this._height);
      }
    }

    renderPosition(selection, halfWidth, halfHeight) {
      return selection.attr('transform',
        `translate(${parseInt(d3.select(this._svg).attr('width')) / 2 - halfWidth}, ${parseInt(d3.select(this._svg).attr('height')) / 2 - halfHeight})`);
    }
  };

  return View;
};

module.exports = {init};