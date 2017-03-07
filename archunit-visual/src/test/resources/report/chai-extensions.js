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

  Assertion.addMethod('haveCorrectEndPositions', function () {
    let deps = Array.from(this._obj);
    let incorrectDeps = [];
    let correct = true;
    //check if the length of the dependency-line is equal to the distance between the two nodes of the dependency
    deps.forEach(d => {
      let start = d.getStartNode();
      let end = d.getEndNode();
      let expDistance = Math.sqrt(Math.pow(end.visualData.x - start.visualData.x, 2)
          + Math.pow(end.visualData.y - start.visualData.y, 2));
      let actDistance = start.visualData.r + end.visualData.r + Math.sqrt(Math.pow(d.endPoint[0] - d.startPoint[0], 2)
              + Math.pow(d.endPoint[1] - d.startPoint[1], 2));
      if (Math.abs(actDistance - expDistance) > 0.05) {
        correct = false;
        incorrectDeps.push(d);
      }
    });
    this.assert(
        correct
        , "expected #{this} to have correct end point"
        , "expected #{this} to not be of type #{act}"
        , null
        , incorrectDeps
    );
  });
});