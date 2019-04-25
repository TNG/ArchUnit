'use strict';

const isNumber = n => !isNaN(parseFloat(n)) && !isNaN(n - 0);

const Assertion = require('chai').Assertion;

Assertion.addMethod('deepCloseTo', function (expObject, delta) {
  const actObject = this._obj;

  for (const key in expObject) {
    if (isNumber(actObject[key])) {
      new Assertion(actObject[key]).to.be.closeTo(expObject[key], delta);
    }
  }
});