'use strict';

const d3 = require('d3');
const clickAreaWidth = 10;

const positionLineSelectionAccordingToVisualData = (selection, visualData) => {
  return selection
    .attr('x1', visualData.startPoint.x)
    .attr('y1', visualData.startPoint.y)
    .attr('x2', visualData.endPoint.x)
    .attr('y2', visualData.endPoint.y);
};

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
    constructor(parentSvgElement, dependency, callForAllViews, getDetailedDependencies) {
      this._svgElement =
        d3.select(parentSvgElement)
          .append('g')
          .attr('id', dependency.getIdentifyingString())
          .style('visibility', 'hidden')
          .node();

      d3.select(this._svgElement)
        .append('line')
        .attr('class', dependency.getTypeNames());

      d3.select(this._svgElement)
        .append('line')
        .attr('class', 'area')
        .style('visibility', 'hidden')
        .style('stroke-width', clickAreaWidth);

      this._createDetailedView(parentSvgElement, dependency.getIdentifyingString(),
        fun => callForAllViews(view => fun(view._detailedView)), getDetailedDependencies);
    }

    _createDetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies) {
      this._detailedView = new DetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies);
      this.onMouseOver(coords => this._detailedView.fadeIn(coords));
      this.onMouseOut(() => this._detailedView.fadeOut());
    }

    _show(dependency) {
      d3.select(this._svgElement).select('line.dependency').attr('class', dependency.getTypeNames());
      d3.select(this._svgElement).style('visibility', 'visible');
      d3.select(this._svgElement).select('line.area').style('pointer-events', dependency.hasDetailedDescription() ? 'all' : 'none');
    }

    hide() {
      d3.select(this._svgElement).style('visibility', 'hidden');
      d3.select(this._svgElement).select('line.area').style('pointer-events', 'none');
    }

    _showIfVisible(dependency) {
      if (dependency.isVisible()) {
        this._show(dependency);
      }
    }

    _updateAreaPosition(dependencyVisualData) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.area'), dependencyVisualData);
    }

    jumpToPositionAndShowIfVisible(dependency) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.dependency'), dependency.visualData);
      this._updateAreaPosition(dependency.visualData);
      this._showIfVisible(dependency);
    }

    moveToPositionAndShowIfVisible(dependency) {
      const transition = d3.select(this._svgElement).select('line.dependency').transition().duration(transitionDuration);
      const promise = createPromiseOnEndOfTransition(transition, transition => positionLineSelectionAccordingToVisualData(transition, dependency.visualData));
      this._updateAreaPosition(dependency.visualData);
      return promise.then(() => this._showIfVisible(dependency));
    }

    onMouseOver(handler) {
      d3.select(this._svgElement).select('line.area').on('mouseover', function () {
        const coordinates = d3.mouse(d3.select('#visualization').node());
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

module.exports.init = (DetailedView, transitionDuration) => ({
  View: init(DetailedView, transitionDuration)
});