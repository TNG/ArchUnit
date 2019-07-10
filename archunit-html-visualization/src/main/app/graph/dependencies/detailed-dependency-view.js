'use strict';

const SingleDetailedDependencyView = require('./single-detailed-dependency-view');

const init = (transitionDuration, svg, visualizationStyles, textPadding = 5) => {

  const View = class {
    constructor(svgContainer, getContainerWidth, callForAllDetailedViews, getDetailedDependencies) {
      this._fixed = false;
      this._callForAllDetailedViews = callForAllDetailedViews;
      this._getDetailedDependencies = getDetailedDependencies;

      this._svgContainer = svgContainer;
      this._getContainerWidth = getContainerWidth;

      this._isCreated = false;
      this._isVisible = false;

      this._detailedDependencyViews = [];
    }

    _createIfNecessary() {
      if (!this._isCreated) {
        this._create();
        this._isCreated = true;
      }
    }

    _attachToContainerIfNecessary() {
      if (!this._isVisible) {
        this._svgContainer.addChild(this._svgElement);
        this._isVisible = true;
      }
    }

    _show(coordinates) {
      const detailedDeps = this._getDetailedDependencies();
      if (detailedDeps.length > 0) {
        this._createIfNecessary();
        this._attachToContainerIfNecessary();
        this._update(coordinates, detailedDeps);
      }
    }

    _create() {
      this._svgElement = svg.createGroup();
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
      this._detailedDependencyViews.forEach(detailedDependencyView => detailedDependencyView.remove());

      const fontSize = visualizationStyles.getDependencyTitleFontSize();

      this._detailedDependencyViews = detailedDeps.map(detailedDependency =>
        new SingleDetailedDependencyView(this._text, detailedDependency, fontSize + textPadding));

      const maxWidth = Math.max.apply(null, this._detailedDependencyViews.map(d => d.textWidth));
      const halfMaxWidthWithPadding = maxWidth / 2 + textPadding;

      this._detailedDependencyViews.forEach(d => d.positionX = -maxWidth / 2);

      //ensure that the rect is visible on the left side
      let x = Math.max(halfMaxWidthWithPadding, coordinates[0]);
      //ensure that the rect is visible on the right side
      x = Math.min(x, this._getContainerWidth() - halfMaxWidthWithPadding);
      this._svgElement.translate({x, y: coordinates[1]});

      const xShift = -halfMaxWidthWithPadding;
      this._frame.positionX = xShift;
      this._hoverArea.positionX = xShift;

      const height = detailedDeps.length * (fontSize + textPadding) + 2 * textPadding;
      this._frame.height = height;
      this._hoverArea.height = height;

      const width = maxWidth + 2 * textPadding + fontSize;
      this._frame.width = width;
      this._hoverArea.width = width;

      this._shouldBeHidden = false;
      this._svgElement.show();
      this._hoverArea.enablePointerEvents();
    }

    _fix() {
      if (!this._fixed) {
        const fontSize = visualizationStyles.getDependencyTitleFontSize();
        this._closeButton = this._svgElement.addText('x');
        this._closeButton.addCssClass('closeButton');
        this._closeButton.offsetX = this._hoverArea.width / 2 + fontSize / 2 - this._closeButton.textWidth / 2;
        this._closeButton.offsetY = fontSize;
        this._closeButton.onClick(() => this._unfix());
        this._fixed = true;
      }
    }

    _unfix() {
      this._fixed = false;
      this._hideIfNotFixed();
      this._closeButton.detachFromParent();
    }

    _hideIfNotFixed() {
      if (this._isVisible && !this._fixed) {
        this._svgElement.detachFromParent();
        this._isVisible = false;
      }
    }

    fadeIn() {
      const coordinates = this._svgContainer.getMousePosition();
      if (!this._fixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._callForAllDetailedViews(d => d._hideIfNotFixed());
            this._show(coordinates);
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