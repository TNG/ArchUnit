'use strict';

const textPadding = 5;
const d3 = require('d3');

const init = (transitionDuration, calculateTextWidth, visualizationStyles) => {

  const View = class {
    constructor(svgContainer, callForAllDetailedViews, getDetailedDependencies) {
      this._fixed = false;
      this._callForAllDetailedViews = callForAllDetailedViews;
      this._getDetailedDependencies = getDetailedDependencies;
      this._svgElement = null;
      this._svgContainer = svgContainer;
    }

    show(coordinates) {
      const detailedDeps = this._getDetailedDependencies();
      if (detailedDeps.length > 0) {
        d3.select(this._svgElement).remove();
        this._create();
        this._update(coordinates, detailedDeps);
      }
    }

    _create() {
      this._svgElement = this._svgContainer.addGroup();
      this._frame = this._svgElement.addRect();
      this._frame.addCssClass('frame');

      this._text = this._svgElement.addText();
      this._text.addCssClass('access');

      this._hoverArea = this._svgElement.addRect();
      this._hoverArea.addCssClass('hoverArea');
      this._hoverArea.onMouseOver(() => this._shouldBeHidden = false);
      this._hoverArea.onMouseOut(() => this.fadeOut());
      this._hoverArea.onClick(() => this._fix());

      this._svgElement.onDrag((dx, dy) => {
        this._fix();
        const translation = this._svgElement.getTranslation();
        translation.x += dx;
        translation.y += dy;
        this._svgElement.translate(translation)
      });
    }

    _update(coordinates, detailedDeps) {
      const maxWidth = Math.max.apply(null, detailedDeps.map(d => calculateTextWidth(d, 'access'))) + 2 * textPadding + 10;

      d3.select(this._svgElement.domElement).attr('transform', () => {
        const transform = d3.select('#translater').attr('transform');
        const translateX = parseFloat(transform.substring(transform.indexOf('(') + 1, transform.indexOf(')')).split(',')[0]);

        //ensure that the rect is visible on the left side
        let x = Math.max(maxWidth / 2, translateX + coordinates[0]);
        //ensure that the rect is visible on the right side
        x = Math.min(x, d3.select('#visualization').attr('width') - maxWidth / 2);

        return `translate(${x - translateX}, ${coordinates[1]})`;
      });

      const tspans = d3.select(this._svgElement.domElement).select('text.access')
        .selectAll('tspan')
        .data(detailedDeps);

      const fontSize = visualizationStyles.getDependencyTitleFontSize();

      tspans.exit().remove();
      tspans.enter().append('tspan');

      d3.select(this._svgElement.domElement).select('text')
        .selectAll('tspan')
        .text(d => d)
        .attr('x', -maxWidth / 2)
        .attr('dy', fontSize + textPadding);

      d3.select(this._svgElement.domElement).selectAll('rect')
        .attr('x', -maxWidth / 2 - textPadding)
        .attr('height', detailedDeps.length * (fontSize + textPadding) + 2 * textPadding)
        .attr('width', maxWidth + fontSize);

      this._shouldBeHidden = false;
      d3.select(this._svgElement.domElement).style('visibility', 'visible');
      d3.select(this._svgElement.domElement).select('.hoverArea').style('pointer-events', 'all');
    }

    _fix() {
      if (!this._fixed) {
        const fontSize = visualizationStyles.getDependencyTitleFontSize();
        const dx = d3.select(this._svgElement.domElement).select('.hoverArea').attr('width') / 2 - fontSize / 2;
        d3.select(this._svgElement.domElement).append('text')
          .attr('class', 'closeButton')
          .text('x')
          .attr('dx', dx)
          .attr('dy', fontSize)
          .on('click', () => this._unfix());
        this._fixed = true;
      }
    }

    _unfix() {
      this._fixed = false;
      this._hideIfNotFixed();
      d3.select(this._svgElement.domElement).select('text.closeButton').remove();
    }

    _hideIfNotFixed() {
      if (!this._fixed && this._svgElement) {
        //d3.select(this._svgElement).style('visibility', 'hidden');
        //d3.select(this._svgElement).select('.hoverArea').style('pointer-events', 'none');
        d3.select(this._svgElement.domElement).remove();
      }
    }

    fadeIn() {
      const coordinates = this._svgContainer.getMousePosition();
      if (!this._fixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._callForAllDetailedViews(d => d._hideIfNotFixed());
            this.show(coordinates);
          }
        }, transitionDuration);
      }
    }

    fadeOut() {
      this._shouldBeHidden = true;
      setTimeout(() => {
        if (this._shouldBeHidden) {
          this._hideIfNotFixed();
        }
      }, transitionDuration);
    }
  };

  return View;
};

module.exports = {init};