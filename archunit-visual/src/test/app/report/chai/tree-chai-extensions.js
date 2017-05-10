'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('containExactlyNodes', function () {
  let actual = Array.from(this._obj);
  let expectedNodeFullNames = arguments[0];

  let actualStrings = actual.map(n => n.projectData.fullname).sort();
  let expectedStrings = expectedNodeFullNames.sort();

  let sizeMatches = actualStrings.length === expectedStrings.length;
  let elementsMatch = !actualStrings.map((v, i) => v !== expectedStrings[i]).includes(true);

  this.assert(
      sizeMatches && elementsMatch
      , "expected #{this} to contain nodes #{exp} but got #{act}"
      , "expected #{this} to not be of type #{act}"
      , expectedStrings        // expected
      , actualStrings   // actual
  );
});





