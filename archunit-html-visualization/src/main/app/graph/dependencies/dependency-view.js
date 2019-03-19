'use strict';

const clickAreaWidth = 10;
const d3 = require('d3');

const svg = require('../infrastructure/gui-elements').svg;

const positionLineSelectionAccordingToVisualData = (selection, visualData) => {
  return selection
    .attr('x1', visualData.relativeStartPoint.x)
    .attr('y1', visualData.relativeStartPoint.y)
    .attr('x2', visualData.relativeEndPoint.x)
    .attr('y2', visualData.relativeEndPoint.y);
};

// FIXME: Write test for relevant logic (like style changes on property changes -> violation)
const init = (DetailedView, transitionDuration) => {

  const createPromiseOnEndOfTransition = (transition, transitionRunner) => {
    if (transition.empty()) {
      return Promise.resolve();
    } else {
      return new Promise(resolve => transitionRunner(transition).on('end', resolve));
    }
  };

  const View = class {
    constructor(svgContainerForDetailedDependencies, dependency, callForAllViews, getDetailedDependencies) {
      this._dependency = dependency;

      this._svgElement = this._dependency.containerEndNode.svgSelectionForDependencies.addGroup({id: dependency.toString()});
      this._svgElement.hide();

      this._line = this._svgElement.addLine();
      this._line.addCssClass('dependency');
      this._refreshViolationCssClass();

      this._hoverArea = this._svgElement.addLine();
      this._hoverArea.addCssClass('area');
      this._hoverArea.hide();
      this._hoverArea.strokeWidth = clickAreaWidth;

      this._createDetailedView(svgContainerForDetailedDependencies, dependency.toString(),
        fun => callForAllViews(view => fun(view._detailedView)), getDetailedDependencies);
    }

    onContainerEndNodeChanged() {
      this._svgElement.detachFromParent();
      this._dependency.containerEndNode.svgSelectionForDependencies.addChild(this._svgElement);
      this._dependency.onContainerEndNodeApplied();
    }

    _createDetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies) {
      this._detailedView = new DetailedView(parentSvgElement, dependencyIdentifier, callForAllDetailedViews, getDetailedDependencies);
      this.onMouseOver(parentSvgElement, coords => this._detailedView.fadeIn(coords));
      this.onMouseOut(() => this._detailedView.fadeOut());
    }

    _refreshViolationCssClass() {
      if (this._dependency.isViolation) {
        this._line.addCssClass('violation');
      } else {
        this._line.removeCssClasses('violation');
      }
    }

    _refreshPointerEvents() {
      if (this._dependency.hasDetailedDescription()) {
        this._hoverArea.enablePointerEvents();
      } else {
        this._hoverArea.disablePointerEvents();
      }
    }

    show() {
      this._refreshViolationCssClass();
      this._svgElement.show();
      this._refreshPointerEvents();
    }

    hide() {
      this._svgElement.hide();
      this._hoverArea.disablePointerEvents();
    }

    refresh() {
      if (this._dependency.isVisible()) {
        this.show();
      } else {
        this.hide();
      }
    }

    _updateAreaPosition() {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement.domElement).select('line.area'), this._dependency.visualData);
    }

    jumpToPositionAndShowIfVisible() {
      positionLineSelectionAccordingToVisualData(d3.select(this._svgElement.domElement).select('line.dependency'), this._dependency.visualData);
      this._updateAreaPosition();
      this.refresh();
    }

    moveToPositionAndShowIfVisible() {
      const transition = d3.select(this._svgElement.domElement).select('line.dependency').transition().duration(transitionDuration);
      const promise = createPromiseOnEndOfTransition(transition, transition => positionLineSelectionAccordingToVisualData(transition, this._dependency.visualData));
      this._updateAreaPosition();
      return promise.then(() => this.refresh());
    }

    onMouseOver(parentSvgElement, handler) {
      d3.select(this._svgElement.domElement).select('line.area').on('mouseover', function () {
        const coordinates = d3.mouse(d3.select(parentSvgElement).node());
        handler(coordinates);
      });
    }

    onMouseOut(handler) {
      d3.select(this._svgElement.domElement).select('line.area').on('mouseout', () => {
        handler();
      });
    }
  };

  return View;
};

module.exports = {init};