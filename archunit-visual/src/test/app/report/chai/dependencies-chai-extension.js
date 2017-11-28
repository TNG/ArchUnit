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

Assertion.addMethod('haveDependencyStrings', function() {
  const actDependencies = Array.from(this._obj);
  const expStrings = arguments[0].sort();

  const actStrings = actDependencies.map(d => d.toString()).sort();

  new Assertion(actStrings).to.deep.equal(expStrings);
});