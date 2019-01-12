'use strict';

const {svgSelect} = require('./infrastructure/gui-elements');

const init = (transitionDuration) => {
  const View = class {
    constructor(svg) {
      this._svgElement = svgSelect(svg);
      this._svgElement.dimension = {width: 0, height: 0};
      this._translater = this._svgElement.addGroup({
        id: 'translater'
      });
      this.svgElementForNodes = this._translater.addGroup().domElement;
      this.svgElementForDependencies = this._translater.addGroup().domElement;
    }

    render(halfWidth, halfHeight) {
      this.renderSizeIfNecessary(halfWidth, halfHeight);
      this._translater.translate(this._toAbsoluteCoordinates({
        relativeX: halfWidth,
        relativeY: halfHeight
      }));
    }

    renderWithTransition(halfWidth, halfHeight) {
      this.renderSizeIfNecessary(halfWidth, halfHeight);
      return this._translater.createTransitionWithDuration(transitionDuration)
        .step(element => element.translate(this._toAbsoluteCoordinates(
          {
            relativeX: halfWidth,
            relativeY: halfHeight
          })))
        .finish();
    }

    renderSizeIfNecessary(halfWidth, halfHeight) {
      const calcRequiredSize = halfSize => parseInt(2 * halfSize + 4);
      const calcExpandedSize = halfSize => parseInt(2 * halfSize + 4);
      const getNewSize = (windowSize, requiredSize, maxSize) => requiredSize < windowSize ? windowSize : maxSize;

      const windowWidth = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
      const windowHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);

      const requiredWidth = calcRequiredSize(halfWidth);
      const expandedWidth = calcExpandedSize(halfWidth);
      const requiredHeight = calcRequiredSize(halfHeight);
      const expandedHeight = calcExpandedSize(halfHeight);

      const minWidth = Math.max(windowWidth, requiredWidth);
      const maxWidth = Math.max(windowWidth, expandedWidth);

      const minHeight = Math.max(windowHeight, requiredHeight);
      const maxHeight = Math.max(windowHeight, expandedHeight);

      if (this._svgElement.width < minWidth || maxWidth < this._svgElement.width) {
        this._svgElement.width = getNewSize(windowWidth, requiredWidth, maxWidth);
      }

      if (this._svgElement.height < minHeight || maxHeight < this._svgElement.height) {
        this._svgElement.height = getNewSize(windowHeight, requiredHeight, maxHeight);
      }
    }

    _toAbsoluteCoordinates({relativeX, relativeY}) {
      return {
        x: this._svgElement.width / 2 - relativeX,
        y: this._svgElement.height / 2 - relativeY
      };
    }
  };

  return View;
};

module.exports = {init};