'use strict';

const d3 = require('d3');

const init = (appearDuration, hideDuration, textPadding, calculateTextWidth) => {

  const View = class {
    constructor(parentSvgElement, dependency, create, callForAllDetailedViews, visualizationStyles, getDetailedDependencies) {
      //FIXME: only save string out of from and to instead of whole object
      this._dependency = dependency;
      this._isFixed = false;
      this._createOfGraph = create;
      this._callForAllDetailedViews = callForAllDetailedViews;
      this._visualizationStyles = visualizationStyles;
      this._getDetailedDependencies = getDetailedDependencies;
      this._svgElement = null;
      this._createSvgElement = () => d3.select(parentSvgElement).append('g').attr('id', `${dependency.from}-${dependency.to}`).node();
    }

    show(coordinates) {
      const detailedDeps = this._getDetailedDependencies();
      if (detailedDeps.length > 0) {
        this.createIfNecessary();
        this._update(coordinates, detailedDeps);
        this._createOfGraph(this._dependency, coordinates);
      }
    }

    createIfNecessary() {
      if (!this._svgElement) {
        this._create();
      }
    }

    _create() {
      this._svgElement = this._createSvgElement();
      d3.select(this._svgElement).append('rect').attr('class', 'frame');
      d3.select(this._svgElement).append('text').attr('class', 'access');
      d3.select(this._svgElement).append('rect').attr('class', 'hoverArea')
        .on('mouseover', () => this._shouldBeHidden = false)
        .on('mouseout', () => this.fadeOut())
        .on('click', () => this.fix());

      const drag = d3.drag().on('drag', () => {
        this.fix();
        d3.select(this._svgElement).attr('transform', () => {
          const transform = d3.select(this._svgElement).attr('transform');
          const translateBefore = transform.substring(transform.indexOf("(") + 1, transform.indexOf(")")).split(",").map(s => parseInt(s));
          return `translate(${translateBefore[0] + d3.event.dx}, ${translateBefore[1] + d3.event.dy})`
        });
      });
      d3.select(this._svgElement).call(drag);
    }

    _update(coordinates, detailedDeps) {
      const maxWidth = Math.max.apply(null, detailedDeps.map(d => calculateTextWidth(d.description, 'access'))) + 2 * textPadding + 10;

      d3.select(this._svgElement).attr('transform', () => {
        //ensure that the rect is visible on the left side
        let x = Math.max(maxWidth / 2, coordinates[0]);
        //ensure that the rect is visible on the right side
        x = Math.min(x, d3.select('#visualization').attr('width') - maxWidth / 2);
        return `translate(${x}, ${coordinates[1]})`;
      });

      const tspans = d3.select(this._svgElement).select('text.access')
        .selectAll('tspan')
        .data(detailedDeps);

      const fontSize = this._visualizationStyles.getDependencyTitleFontSize();

      tspans.exit().remove();
      tspans.enter().append('tspan');

      d3.select(this._svgElement).select('text')
        .selectAll('tspan')
        .text(d => d.description)
        .attr('class', d => d.cssClass)
        .attr('x', -maxWidth / 2)
        .attr('dy', fontSize + textPadding);

      d3.select(this._svgElement).selectAll('rect')
        .attr('x', -maxWidth / 2 - textPadding)
        .attr('height', detailedDeps.length * (fontSize + textPadding) + 2 * textPadding)
        .attr('width', maxWidth + fontSize);
    }

    _unfix() {
      this._isFixed = false;
      this.hideIfNotFixed();
      d3.select(this._svgElement).select('text.closeButton').remove();
    }

    fix() {
      if (!this._isFixed) {
        const fontSize = this._visualizationStyles.getDependencyTitleFontSize();
        const dx = d3.select(this._svgElement).select('.hoverArea').attr('width') / 2 - fontSize / 2;
        d3.select(this._svgElement).append('text')
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
        d3.select(this._svgElement).style('visibility', 'hidden');
        d3.select(this._svgElement).select('.hoverArea').style('pointer-events', 'none');
      }
    }

    fadeIn(coordinates) {
      if (!this._isFixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._callForAllDetailedViews(d => d.hideIfNotFixed());
            this.show(coordinates);
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

module.exports.init = (appearDuration, hideDuration, textPadding, calculateTextWidth) => ({
  View: init(appearDuration, hideDuration, textPadding, calculateTextWidth)
});