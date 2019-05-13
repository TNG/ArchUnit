'use strict';

const clickAreaWidth = 10;

// FIXME: Write test for relevant logic (like style changes on property changes -> violation)
const init = (transitionDuration) => {

  const View = class {
    constructor(dependency) {
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
    }

    onContainerEndNodeChanged() {
      this._svgElement.detachFromParent();
      this._dependency.containerEndNode.svgSelectionForDependencies.addChild(this._svgElement);
      this._dependency.onContainerEndNodeApplied();
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

    _show() {
      this._refreshViolationCssClass();
      this._svgElement.show();
      this._refreshPointerEvents();
    }

    _hide() {
      this._svgElement.hide();
      this._hoverArea.disablePointerEvents();
    }

    refresh() {
      if (this._dependency.isVisible()) {
        this._show();
      } else {
        this._hide();
      }
    }

    _updateAreaPosition() {
      this._hoverArea.setStartAndEndPosition(this._dependency.relativeStartPoint, this._dependency.relativeEndPoint);
    }

    jumpToPositionAndRefresh() {
      this._line.setStartAndEndPosition(this._dependency.relativeStartPoint, this._dependency.relativeEndPoint);
      this._updateAreaPosition();
      this.refresh();
    }

    moveToPosition() {
      const promise = this._line.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => {
          svgSelection.setStartAndEndPosition(this._dependency.relativeStartPoint, this._dependency.relativeEndPoint);
        })
        .finish();
      this._updateAreaPosition();
      return promise;
    }

    onMouseOver(mouseOverHandler) {
      this._hoverArea.onMouseOver(mouseOverHandler);
    }

    onMouseOut(mouseOutHandler) {
      this._hoverArea.onMouseOut(mouseOutHandler);
    }
  };

  return View;
};

module.exports = {init};