'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('haveDiffLessThan', function () {
  let result = this._obj - arguments[0] < arguments[1];
  this.assert(
      result
      , "expected #{this} to have the given diff to #{exp}"
      , "expected #{this} to not be of type #{act}"
      , arguments[0]
      , this._obj
  )
});