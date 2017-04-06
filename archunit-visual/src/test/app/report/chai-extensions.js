'use strict';

const Assertion = require("chai").Assertion;

const MAXPOSITIONDIFF = 0.005;
const MAXLENGTHDIFF = 0.05;
const MAXRADIUSDIFF = 0.05;

let distance = (x1, y1, x2, y2) => {
  return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
};

let haveDiffBiggerThan = (value1, value2, diff) => {
  return Math.abs(value1 - value2) > diff;
};

let haveDiffLessThan = (value1, value2, diff) => {
  return value1 - value2 < diff;
};

let endPositionsAreNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let startDistance = distance(start.visualData.x, start.visualData.y, d.startPoint[0], d.startPoint[1]),
      endDistance = distance(end.visualData.x, end.visualData.y, d.endPoint[0], d.endPoint[1]);
  return haveDiffBiggerThan(startDistance, start.visualData.r, MAXPOSITIONDIFF) ||
      haveDiffBiggerThan(endDistance, end.visualData.r, MAXPOSITIONDIFF);
};

let distanceIsNotCorrect = d => {
  let start = d.getStartNode(), end = d.getEndNode();
  let expDistance = distance(start.visualData.x, start.visualData.y, end.visualData.x, end.visualData.y);
  let actDistance;
  let edgeLength = distance(d.startPoint[0], d.startPoint[1], d.endPoint[0], d.endPoint[1]);
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
  return (d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, 2 * d.lineDiff))
      || (!d.mustShareNodes && haveDiffBiggerThan(actDistance, expDistance, MAXLENGTHDIFF));
};

let radiusofleaf = (leaf, textwidth, CIRCLETEXTPADDING) => textwidth(leaf.projectData.name) / 2 + CIRCLETEXTPADDING;

let radiusofinnernode = (node, textwidth, CIRCLETEXTPADDING, TEXTPOSITION) => radiusofleaf(node, textwidth, CIRCLETEXTPADDING) /
Math.sqrt(1 - TEXTPOSITION * TEXTPOSITION);

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
});

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

Assertion.addMethod('haveTextWithinCircle', function () {
  let node = this._obj;
  let textwidth = arguments[0];
  let CIRCLETEXTPADDING = arguments[1];
  let TEXTPOSITION = arguments[2];
  if (node.isCurrentlyLeaf()) {
    let expRadius = radiusofleaf(node, textwidth, CIRCLETEXTPADDING);
    this.assert(
        !haveDiffBiggerThan(expRadius, node.visualData.r, MAXRADIUSDIFF)
        , "expected #{this} to have radius #{exp} but got #{act}"
        , "expected #{this} to not be of type #{act}"
        , expRadius
        , node.visualData.r
    );
  }
  else {
    let expRadius = radiusofinnernode(node, textwidth, CIRCLETEXTPADDING, TEXTPOSITION);
    this.assert(
        node.visualData.r > expRadius
        , "expected #{this} to have bigger radius than #{exp} but got #{act}"
        , "expected #{this} to not be of type #{act}"
        , expRadius
        , node.visualData.r
    );
  }
});

Assertion.addMethod('haveChildrenWithinCircle', function () {
  let node = this._obj;
  let CIRCLEPADDING = arguments[0];

  let childrenNotWithinNode = [];

  node.origChildren.forEach(c => {
    let distanceFromNodeMiddleToChildRim = distance(node.visualData.x, node.visualData.y, c.visualData.x, c.visualData.y)
        + c.visualData.r;
    if (haveDiffLessThan(node.visualData.r, distanceFromNodeMiddleToChildRim, CIRCLEPADDING / 2)) {
      childrenNotWithinNode.push(c);
    }
  });

  this.assert(
      childrenNotWithinNode.length == 0
      , "expected #{this} to have only children within its circle got #{act} being not within its circle"
      , "expected #{this} to not be of type #{act}"
      , 0
      , childrenNotWithinNode
  );
});

Assertion.addMethod('notOverlap', function () {
  let nodes = this._obj;
  let CIRCLEPADDING = arguments[0];

  let nodesoverlapping = new Set();

  nodes.forEach(c => {
    nodes.filter(d => d !== c).forEach(d => {
      let diff = distance(c.visualData.x, c.visualData.y, d.visualData.x, d.visualData.y);
      let minExpDiff = c.visualData.r + d.visualData.r + CIRCLEPADDING;
      if (diff + MAXRADIUSDIFF < minExpDiff) {
        nodesoverlapping.add(c.projectData.fullname, c);
        nodesoverlapping.add(d.projectData.fullname, d);
      }
    });
  });

  this.assert(
      nodesoverlapping.size == 0
      , "expected #{this} not to overlap but got #{act} that are overlapping"
      , "expected #{this} to not be of type #{act}"
      , 0
      , Array.from(nodesoverlapping)
  );
});