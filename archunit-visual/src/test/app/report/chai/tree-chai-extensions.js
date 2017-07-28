'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('containExactlyNodes', function () {
  const actual = Array.from(this._obj);
  const expectedNodeFullNames = arguments[0];

  const actualStrings = actual.map(n => n.getFullName()).sort();
  const expectedStrings = expectedNodeFullNames.sort();

  const sizeMatches = actualStrings.length === expectedStrings.length;
  const elementsMatch = !actualStrings.map((v, i) => v !== expectedStrings[i]).includes(true);

  this.assert(
      sizeMatches && elementsMatch
      , "expected #{this} to contain nodes #{exp} but got #{act}"
      , "expected #{this} to not be of type #{act}"
      , expectedStrings        // expected
      , actualStrings   // actual
  );
});





