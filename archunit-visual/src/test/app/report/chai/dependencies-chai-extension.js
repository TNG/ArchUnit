'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('containExactlyDependencies', function () {
  const actual = Array.from(this._obj);
  const expectedDepStrings = arguments[0];

  const actualStrings = actual.map(d => d.toString()).sort();
  const expectedStrings = expectedDepStrings.sort();

  const sizeMatches = actualStrings.length === expectedStrings.length;
  const elementsMatch = !actualStrings.map((v, i) => v !== expectedStrings[i]).includes(true);

  this.assert(
      sizeMatches && elementsMatch
      , "expected #{this} to contain dependencies #{exp} but got #{act}"
      , "expected #{this} to not be of type #{act}"
      , expectedStrings
      , actualStrings
  );
});