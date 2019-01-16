'use strict';

const clickAreaWidth = 10;
const d3 = require('d3');

const positionLineSelectionAccordingToVisualData = (selection, visualData) => {
  return selection
    .attr('x1', visualData.relativeStartPoint.x)
    .attr('y1', visualData.relativeStartPoint.y)
    .attr('x2', visualData.relativeEndPoint.x)
    .attr('y2', visualData.relativeEndPoint.y);
};

const getCssClass = (dependency) => 'dependency' + (dependency.isViolation ? ' violation' : '');

// FIXME: Write test for relevant logic (like style changes on property changes -> violation)
const init = (DetailedView, transitionDuration) => {

  const createPromiseOnEndOfTransition = (transition, transitionRunner) => {
    if (transition.empty()) {
      return Promise.resolve();
    }
    else {
      return new Promise(resolve => transitionRunner(transition).on('end', resolve));
    }
  };

  const View = class {
    constructor(svgContainerForDetailedDependencies, dependency, callForAllViews, getDetailedDependencies) {
      this._dependency = dependency;

      this._svgElement =
        d3.select(this._dependency.endNodeInForeground.svgElementForDependencies)
          .append('g')
          .attr('id', dependency.toString())
          .style('visibility', 'hidden')
          .node();

      d3.select(this._svgElement)
        .append('line')
        .attr('class', getCssClass(this._dependency));

      d3.select(this._svgElement)
        .append('line')
        .attr('class', 'area')
        .style('visibility', 'hidden')
        .style('stroke-width', clickAreaWidth);

      this._createDetailedView(svgContainerForDetailedDependencies, dependency.toString(),
        fun => callForAllViews(view => fun(view._detailedView)), getDetailedDependencies);
    }

    _createDetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies) {
      this._detailedView = new DetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies);
      this.onMouseOver(parentSvgElement, coords => this._detailedView.fadeIn(coords));
      this.onMouseOut(() => this._detailedView.fadeOut());
    }

    show() {
      d3.select(this._svgElement).select('line.dependency').attr('class', getCssClass(this._dependency));
      d3.select(this._svgElement).style('visibility', 'visible');
      d3.select(this._svgElement).select('line.area').style('pointer-events', this._dependency.hasDetailedDescription() ? 'all' : 'none');
    }

    hide() {
      d3.select(this._svgElement).style('visibility', 'hidden');
      d3.select(this._svgElement).select('line.area').style('pointer-events', 'none');
    }

    refresh() {
      if (this._dependency.isVisible()) {
        this.show();
      } else {
        this.hide();
      }
    }

    _updateAreaPosition() {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.area'), this._dependency.visualData);
    }

    jumpToPositionAndShowIfVisible() {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.dependency'), this._dependency.visualData);
      this._updateAreaPosition();
      this.refresh();
    }

    moveToPositionAndShowIfVisible() {
      const transition = d3.select(this._svgElement).select('line.dependency').transition().duration(transitionDuration);
      const promise = createPromiseOnEndOfTransition(transition, transition => positionLineSelectionAccordingToVisualData(transition, this._dependency.visualData));
      this._updateAreaPosition();
      return promise.then(() => this.refresh());
    }

    onMouseOver(parentSvgElement, handler) {
      d3.select(this._svgElement).select('line.area').on('mouseover', function () {
        const coordinates = d3.mouse(d3.select(parentSvgElement).node());
        handler(coordinates);
      });
    }

    onMouseOut(handler) {
      d3.select(this._svgElement).select('line.area').on('mouseout', () => {
        handler();
      });
    }
  };

  return View;
};

module.exports = {init};