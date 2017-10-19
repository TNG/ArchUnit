'use strict';

const init = (appearDuration, hideDuration) => {

  const View = class {
    constructor(parentSvgElement, dependency, create, hide) {
      this._dependency = dependency;
      this._isFixed = false;
      this._create = create;
      this._hide = hide;
    }

    _createSvgElements() {

    }

    fadeIn(coordinates) {
      if (!this._isFixed) {
        this._shouldBeHidden = false;
        setTimeout(() => {
          if (!this._shouldBeHidden) {
            this._create(this._dependency, coordinates);
          }
        }, appearDuration);
      }
    }

    fadeOut() {
      this._shouldBeHidden = true;
      setTimeout(() => {
        if (this._shouldBeHidden) {
          this._hide(this._dependency);
        }
      }, hideDuration);
    }
  };

  return View;
};

module.exports.init = (appearDuration, hideDuration) => ({
  View: init(appearDuration, hideDuration)
});