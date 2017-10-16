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
    constructor(parentSvgElement, dependency, callback) {
      this._svgElement = d3.select(parentSvgElement).select(`g[id='${dependency.getIdentifyingString()}']`).node();
      if (d3.select(this._svgElement).empty()) {
        this._createNewSvgElements(parentSvgElement, dependency, callback);
      }
      d3.select(this._svgElement).data([dependency]);
      d3.select(this._svgElement).select('line.dependency').attr('class', dependency.getClass());
    }

    _createNewSvgElements(parentSvgElement, dependency, callback) {
      this._svgElement =
        d3.select(parentSvgElement)
          .append('g')
          .attr('id', dependency.getIdentifyingString())
          .style('visibility', 'hidden')
          .node();

      d3.select(this._svgElement)
        .append('line')
        .attr('class', 'dependency');

      d3.select(this._svgElement)
        .append('line')
        .attr('class', 'area')
        .style('visibility', 'hidden')
        .style('stroke-width', clickAreaWidth);

      callback(d3.select(this._svgElement).select('line.area'));

      this.jumpToPosition(dependency);
    }

    show(dependency) {
      d3.select(this._svgElement).style('visibility', 'visible');
      d3.select(this._svgElement).select('line.area').style('pointer-events', dependency.hasDetailedDescription() ? 'all' : 'none');
    }

    hide() {
      d3.select(this._svgElement).style('visibility', 'hidden');
      d3.select(this._svgElement).select('line.area').style('pointer-events', 'none');
    }

    _updateAreaPosition(dependency) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.area'), dependency.visualData);
    }

    jumpToPosition(dependency) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.dependency'), dependency.visualData);
      this._updateAreaPosition(dependency);
    }

    updatePositionWithTransition(dependency) {
      const transition = d3.select(this._svgElement).select('line.dependency').transition().duration(transitionDuration);
      const promise = createPromiseOnEndOfTransition(transition, transition => positionLineSelectionAccordingToVisualData(transition, dependency.visualData));
      this._updateAreaPosition(dependency);
      return promise;
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});