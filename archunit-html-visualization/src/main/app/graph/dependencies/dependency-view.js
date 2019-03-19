'use strict';

const clickAreaWidth = 10;

// FIXME: Write test for relevant logic (like style changes on property changes -> violation)
const init = (DetailedView, transitionDuration) => {

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
      this._hoverArea.onMouseOver(() => this._detailedView.fadeIn());
      this._hoverArea.onMouseOut(() => this._detailedView.fadeOut());
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
      this._hoverArea.setStartAndEndPosition(this._dependency.visualData.relativeStartPoint, this._dependency.visualData.relativeEndPoint);
    }

    jumpToPositionAndShowIfVisible() {
      this._line.setStartAndEndPosition(this._dependency.visualData.relativeStartPoint, this._dependency.visualData.relativeEndPoint);
      this._updateAreaPosition();
      this.refresh();
    }

    moveToPositionAndShowIfVisible() {
      const promise = this._line.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => {
          svgSelection.setStartAndEndPosition(this._dependency.visualData.relativeStartPoint, this._dependency.visualData.relativeEndPoint);
        })
        .finish();
      this._updateAreaPosition();
      return promise.then(() => this.refresh());
    }
  };

  return View;
};

module.exports = {init};