'use strict';

const Assertion = require("chai").Assertion;

Assertion.addMethod('containExactlyNodes', function () {
  let actual = Array.from(this._obj);
  let expectedNodeFullNames = arguments[0]; //Array.from(arguments);

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

Assertion.addMethod('haveExactlyPositions', function () {
  let actualNodes = Array.from(this._obj);
  let exp = arguments[0];

  let positionsAreCorrect = actualNodes.reduce((res, n) => {
        let pos = exp.get(n.projectData.fullname);
        return res && n.visualData.x == pos[0] && n.visualData.y == pos[1];
      },
      true);

  this.assert(
      positionsAreCorrect
  )
});