'use strict';

const Assertion = require("chai").Assertion;

const MAX_POSITION_DIFF = 0.005;
const MAX_LENGTH_DIFF = 0.05;

let haveDiffBiggerThan = (value1, value2, diff) => {
  return Math.abs(value1 - value2) > diff;
};

let distance = (x1, y1, x2, y2) => {
  return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
};

let endNodesAreOverlapping = d => {
  let startNode = d.getStartNode(), endNode = d.getEndNode();
  let middleDiff = distance(startNode.visualData.x, startNode.visualData.y, endNode.visualData.x, endNode.visualData.y);
  return middleDiff < startNode.visualData.r + endNode.visualData.r;
};

let endPositionsAreNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let startDistance = distance(start.visualData.x, start.visualData.y, d.visualData.startPoint.x, d.visualData.startPoint.y),
      endDistance = distance(end.visualData.x, end.visualData.y, d.visualData.endPoint.x, d.visualData.endPoint.y);
  return haveDiffBiggerThan(startDistance, start.visualData.r, MAX_POSITION_DIFF) ||
      haveDiffBiggerThan(endDistance, end.visualData.r, MAX_POSITION_DIFF);
};

let distanceIsNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let expDistance = distance(start.visualData.x, start.visualData.y, end.visualData.x, end.visualData.y);
  let actDistance;
  let edgeLength = distance(d.visualData.startPoint.x, d.visualData.startPoint.y, d.visualData.endPoint.x, d.visualData.endPoint.y);
  if (endNodesAreOverlapping(d)) {
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
  return (d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, 2 * d.lineDiff))
      || (!d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, MAX_LENGTH_DIFF));
};

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