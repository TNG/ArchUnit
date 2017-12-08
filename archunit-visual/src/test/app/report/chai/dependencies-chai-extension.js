'use strict';

const Assertion = require('chai').Assertion;

Assertion.addMethod('haveDependencyStrings', function() {
  const actDependencies = Array.from(this._obj);
  const expStrings = arguments[0].sort();

  const actStrings = actDependencies.map(d => d.toString()).sort();

  new Assertion(actStrings).to.deep.equal(expStrings);
});