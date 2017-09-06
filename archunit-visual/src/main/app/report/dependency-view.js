'use strict';

const d3 = require('d3');

const positionLineSelectionAccordingToVisualData = (selection, visualData) => {
  selection
    .attr('x1', visualData.startPoint.x)
    .attr('y1', visualData.startPoint.y)
    .attr('x2', visualData.endPoint.x)
    .attr('y2', visualData.endPoint.y);
};

const init = (transitionDuration) => {

  const View = class {
    constructor(parentSvgElement, dependency) {
      dependency.anyProperty = transitionDuration;

      this._svgElement =
        d3.select(parentSvgElement).select(`g[id='${dependency.getIdentifyingString()}']`).node();
    }

    createIfNotExisting() {
      if (d3.select(this._svgElement).empty()) {
        this.createNew();
      }
    }

    createNew() {
      d3.select(this._svgElement)
        .append('g')
        .attr('id', dependency.getIdentifyingString());

      d3.select(this._svgElement)
        .append('line')
        .attr('class', dependency.getClass());

      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.dependency'));

      if (dependency.hasDetailedDescription()) {
        d3.select(this._svgElement)
          .append('line')
          .attr('class', 'area')
          .style('visibility', 'hidden')
          .style('pointer-events', 'all')
          .style('stroke-width', clickAreaWidth);

        positionLineSelectionAccordingToVisualData(d3.select(this._svgElement).select('line.area'));
      }
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});