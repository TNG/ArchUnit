'use strict';

const isNumber = n => !isNaN(parseFloat(n)) && !isNaN(n - 0);

module.exports = function (chai, utils) {
  const Assertion = chai.Assertion;

  Assertion.overwriteMethod('closeTo', function (_super) {
    return function assertCloseTo(expObject, delta) {
      if (utils.flag(this, 'deep')) {
        const actObject = this._obj;

        for (const key in expObject) {
          if (isNumber(actObject[key])) {
            new Assertion(actObject[key]).to.closeTo(expObject[key], delta);
          }
        }
      }
      else {
        _super.apply(this, arguments);
      }
    };
  });
};