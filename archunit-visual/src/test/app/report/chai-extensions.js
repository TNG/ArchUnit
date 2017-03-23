'use strict';

const Assertion = require("chai").Assertion;

const MAXPOSITIONDIFF = 0.005;
const MAXLENGTHDIFF = 0.05;

let endPositionsAreNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let startDiff = Math.sqrt(Math.pow(d.startPoint[1] - start.visualData.y, 2)
          + Math.pow(d.startPoint[0] - start.visualData.x, 2)),
      endDiff = Math.sqrt(Math.pow(d.endPoint[1] - end.visualData.y, 2)
          + Math.pow(d.endPoint[0] - end.visualData.x, 2));
  return Math.abs(startDiff - start.visualData.r) > MAXPOSITIONDIFF ||
      Math.abs(endDiff - end.visualData.r) > MAXPOSITIONDIFF;
};

let distanceIsNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let expDistance = Math.sqrt(Math.pow(end.visualData.x - start.visualData.x, 2)
      + Math.pow(end.visualData.y - start.visualData.y, 2));
  let actDistance;
  let edgeLength = Math.sqrt(Math.pow(d.endPoint[0] - d.startPoint[0], 2)
      + Math.pow(d.endPoint[1] - d.startPoint[1], 2));
  if (d.endNodesAreOverlapping()) {
    let biggerRadius, smallerRadius;
    if (end.visualData.r >= start.visualData.r) {
      biggerRadius = end.visualData.r,
          smallerRadius = start.visualData.r;
    }
    else {
      biggerRadius = start.visualData.r,
          smallerRadius = end.visualData.r;
    }
    actDistance = edgeLength - biggerRadius + smallerRadius;
  }
  else {
    actDistance = start.visualData.r + end.visualData.r + edgeLength;
  }
  return (d.mustShareNodes && Math.abs(actDistance - expDistance) > 2 * d.lineDiff)
      || (!d.mustShareNodes && Math.abs(actDistance - expDistance) > MAXLENGTHDIFF);
};

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
    deps.forEach(d => {
      if (distanceIsNotCorrect(d) || endPositionsAreNotCorrect(d)) {
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