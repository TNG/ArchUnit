'use strict';

const d3 = require('d3');
const clickAreaWith = 10;

const positionLineSelectionAccordingToVisualData = (selection, visualData) => {
  selection
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
    constructor(parentSvgElement, dependency) {
      dependency.anyProperty = transitionDuration;

      this._svgElement =
        d3.select(parentSvgElement).select(`g[id='${dependency.getIdentifyingString()}']`).data([dependency]).node();
    }

    show(dependency) {
      d3.select(this._svgElement).style('visibility', 'visible');
      d3.select(this._svgElement).select('line.area').style('pointer-events', dependency.hasDetailedDescription() ? 'all' : 'none');
    }

    hide() {
      d3.select(this._svgElement).style('visibility', 'hidden');
      d3.select(this._svgElement).select('line.area').style('pointer-events', 'none');
    }

    updateClass(dependency) {
      d3.select(this._svgElement).select('line.dependency').attr('class', dependency.getClass());
    }

    _updateAreaPosition(dependency) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.area'), dependency.visualData);
    }

    updatePositionWithoutTransition(dependency) {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.dependency'), dependency.visualData);
      this._updateAreaPosition(dependency);
    }

    updatePositionWithTransition(dependency) {
      const transition = d3.select(this._svgElement).select('line.dependency').transition().duration(transitionDuration);
      const promise = createPromiseOnEndOfTransition(transition, transition => positionLineSelectionAccordingToVisualData(transition, dependency.visualData));
      this._updateAreaPosition(dependency);
      return promise;
    }

    createIfNotExisting(dependency) {
      if (d3.select(this._svgElement).empty()) {
        this._createNew(dependency);
      }
    }

    _createNew(dependency) {
      d3.select(this._svgElement)
        .append('g')
        .attr('id', dependency.getIdentifyingString())
        .data([dependency])
        .node();

      d3.select(this._svgElement)
        .append('line')
        .attr('class', dependency.getClass());

      if (dependency.hasDetailedDescription()) {
        d3.select(this._svgElement)
          .append('line')
          .attr('class', 'area')
          .style('visibility', 'hidden')
          .style('pointer-events', 'all')
          .style('stroke-width', clickAreaWidth);
      }
      this.updatePositionWithoutTransition();
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});