'use strict';

const d3 = require('d3');

const init = (appearDuration, hideDuration) => {

  const View = class {
    constructor(parentSvgElement, dependency, create, callForAllDetailedViews) {
      //FIXME: only save string out of from and to instead of whole object
      this._dependency = dependency;
      this._isFixed = false;
      this._create = create;
      this._callForAllDetailedViews = callForAllDetailedViews;
      this._parentSvgElement = parentSvgElement;
    }

    createIfNecessary() {
      if (d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`).empty()) {

      }
    }

    hideIfNotFixed() {
      if (!this._isFixed) {
        const svgElement = d3.select(this._parentSvgElement).select(`g[id='${this._dependency.from}-${this._dependency.to}']`);
        svgElement.style('visibility', 'hidden');
        svgElement.select('.hoverArea').style('pointer-events', 'none');
      }
    }

    _createSvgElements() {

    }

    fadeIn(coordinates) {
      if (!this._isFixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._callForAllDetailedViews(d => d.hideIfNotFixed());
            this._create(this._dependency, coordinates);
          }
        }, appearDuration);
      }
    }

    fadeOut() {
      this._shouldBeHidden = true;
      setTimeout(() => {
        if (this._shouldBeHidden) {
          this.hideIfNotFixed();
        }
      }, hideDuration);
    }
  };

  return View;
};

module.exports.init = (appearDuration, hideDuration) => ({
  View: init(appearDuration, hideDuration)
});