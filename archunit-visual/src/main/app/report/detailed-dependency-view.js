'use strict';

const d3 = require('d3');

const init = (appearDuration, hideDuration) => {

  const View = class {
    constructor(parentSvgElement, dependency, create, callForAllDetailedViews, visualizationStyles) {
      //FIXME: only save string out of from and to instead of whole object
      this._dependency = dependency;
      this._isFixed = false;
      this._create = create;
      this._callForAllDetailedViews = callForAllDetailedViews;
      this._parentSvgElement = parentSvgElement;
      this._visualizationStyles = visualizationStyles;
    }

    createIfNecessary() {
      if (d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`).empty()) {

        const svg = d3.select(this._parentSvgElement).append('g').attr('id', `${this._dependency.from}-${this._dependency.to}`);

        svg.append('rect').attr('class', 'frame');
        svg.append('text').attr('class', 'access');

        svg.append('rect').attr('class', 'hoverArea')
          .on('mouseover', () => this._shouldBeHidden = false)
          .on('mouseout', () => this.fadeOut())
          .on('click', () => this.fix());

        const drag = d3.drag().on('drag', () => {
          this.fix();
          svg.attr('transform', () => {
            const transform = svg.attr('transform');
            const translateBefore = transform.substring(transform.indexOf("(") + 1, transform.indexOf(")")).split(",").map(s => parseInt(s));
            return `translate(${translateBefore[0] + d3.event.dx}, ${translateBefore[1] + d3.event.dy})`
          });
        });
        svg.call(drag);
      }
    }

    _unfix() {
      //FIXME: save this instead of the parent-svg
      const svg = d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`);
      this._isFixed = false;
      this.hideIfNotFixed();
      svg.select('text.closeButton').remove();
    }

    fix() {
      if (!this._isFixed) {
        const svg = d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`);
        const fontSize = this._visualizationStyles.getDependencyTitleFontSize();
        const dx = svg.select('.hoverArea').attr('width') / 2 - fontSize / 2;
        svg.append('text')
          .attr('class', 'closeButton')
          .text('x')
          .attr('dx', dx)
          .attr('dy', fontSize)
          .on('click', () => this._unfix());
        this._isFixed = true;
      }
    }

    hideIfNotFixed() {
      if (!this._isFixed) {
        const svgElement = d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`);
        svgElement.style('visibility', 'hidden');
        svgElement.select('.hoverArea').style('pointer-events', 'none');
      }
    }

    fadeIn(coordinates) {
      if (!this._isFixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._callForAllDetailedViews(d => d.hideIfNotFixed());
            this.createIfNecessary();
            this._create(this._dependency, coordinates);
          }
        }, appearDuration);
      }
    }

    fadeOut() {
      this._shouldBeHidden = true;
      setTimeout(() => {
        if (this._shouldBeHidden) {
          this.hideIfNotFixed();
        }
      }, hideDuration);
    }
  };

  return View;
};

module.exports.init = (appearDuration, hideDuration) => ({
  View: init(appearDuration, hideDuration)
});