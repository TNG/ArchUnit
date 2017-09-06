'use strict';

const Assertion = require("chai").Assertion;

const MAX_POSITION_DIFF = 0.005;
const MAX_LENGTH_DIFF = 0.05;

const haveDiffBiggerThan = (value1, value2, diff) => {
  return Math.abs(value1 - value2) > diff;
};

const distance = (x1, y1, x2, y2) => {
  return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
};

const endNodesAreOverlapping = d => {
  const startNode = d.getStartNode(), endNode = d.getEndNode();
  const startAbsVisualData = startNode.getAbsoluteCoords();
  const endAbsVisualData = endNode.getAbsoluteCoords();
  const middleDiff = distance(startAbsVisualData.x, startAbsVisualData.y, endAbsVisualData.x, endAbsVisualData.y);
  return middleDiff < startAbsVisualData.r + endAbsVisualData.r;
};

const endPositionsAreNotCorrect = d => {
  const start = d.getStartNode(), end = d.getEndNode();
  const startAbsVisualData = start.getAbsoluteCoords();
  const endAbsVisualData = end.getAbsoluteCoords();
  const startDistance = distance(startAbsVisualData.x, startAbsVisualData.y, d.visualData.startPoint.x, d.visualData.startPoint.y),
    endDistance = distance(endAbsVisualData.x, endAbsVisualData.y, d.visualData.endPoint.x, d.visualData.endPoint.y);
  return haveDiffBiggerThan(startDistance, startAbsVisualData.r, MAX_POSITION_DIFF) ||
    haveDiffBiggerThan(endDistance, endAbsVisualData.r, MAX_POSITION_DIFF);
};

const distanceIsNotCorrect = d => {
  const start = d.getStartNode(), end = d.getEndNode();
  const startAbsVisualData = start.getAbsoluteCoords();
  const endAbsVisualData = end.getAbsoluteCoords();
  const expDistance = distance(startAbsVisualData.x, startAbsVisualData.y, endAbsVisualData.x, endAbsVisualData.y);
  let actDistance;
  const edgeLength = distance(d.visualData.startPoint.x, d.visualData.startPoint.y, d.visualData.endPoint.x, d.visualData.endPoint.y);
  if (endNodesAreOverlapping(d)) {
    let biggerRadius, smallerRadius;
    if (endAbsVisualData.r >= startAbsVisualData.r) {
      biggerRadius = endAbsVisualData.r,
        smallerRadius = startAbsVisualData.r;
    }
    else {
      biggerRadius = startAbsVisualData.r,
        smallerRadius = endAbsVisualData.r;
    }
    actDistance = edgeLength - biggerRadius + smallerRadius;
  }
  else {
    actDistance = startAbsVisualData.r + endAbsVisualData.r + edgeLength;
  }
  return (d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, 2 * d.lineDiff))
      || (!d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, MAX_LENGTH_DIFF));
};

Assertion.addMethod('haveCorrectEndPositions', function () {
  const deps = Array.from(this._obj);
  const incorrectDeps = [];
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