'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('containExactlyDependencies', function () {
  let actual = Array.from(this._obj);
  let expectedDepStrings = arguments[0];

  let actualStrings = actual.map(d => d.toString()).sort();
  let expectedStrings = expectedDepStrings.sort();

  let sizeMatches = actualStrings.length === expectedStrings.length;
  let elementsMatch = !actualStrings.map((v, i) => v !== expectedStrings[i]).includes(true);

  this.assert(
      sizeMatches && elementsMatch
      , "expected #{this} to contain dependencies #{exp} but got #{act}"
      , "expected #{this} to not be of type #{act}"
      , expectedStrings
      , actualStrings
  );
});