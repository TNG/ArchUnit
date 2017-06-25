'use strict';

const Assertion = require("chai").Assertion;

const MAX_RADIUS_DIFF = 0.05;

let radiusOfLeaf = (leaf, textwidth, CIRCLETEXTPADDING) => textwidth(leaf.getName()) / 2 + CIRCLETEXTPADDING;

let radiusOfInnerNode = (node, textwidth, circleTestPadding, textPosition) =>
radiusOfLeaf(node, textwidth, circleTestPadding) / Math.sqrt(1 - textPosition * textPosition);

let haveDiffBiggerThan = (value1, value2, diff) => Math.abs(value1 - value2) > diff;

let distance = (x1, y1, x2, y2) => {
  return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
};

Assertion.addMethod('haveExactlyPositions', function () {
  let actualNodes = Array.from(this._obj);
  let exp = arguments[0];

  let positionsAreCorrect = actualNodes.reduce((res, n) => {
        let pos = exp.get(n.getFullName());
        return res && n.visualData.x == pos[0] && n.visualData.y == pos[1];
      },
      true);

  this.assert(positionsAreCorrect)
});

Assertion.addMethod('haveTextWithinCircle', function () {
  let node = this._obj;
  let textWidth = arguments[0];
  let circleTextPadding = arguments[1];
  let textPosition = arguments[2];
  if (node.isCurrentlyLeaf()) {
    let expRadius = radiusOfLeaf(node, textWidth, circleTextPadding);
    this.assert(
        !haveDiffBiggerThan(expRadius, node.visualData.r, MAX_RADIUS_DIFF)
        , "expected #{this} to have radius #{exp} but got #{act}"
        , "expected #{this} to not be of type #{act}"
        , expRadius
        , node.visualData.r
    );
  }
  else {
    let expRadius = radiusOfInnerNode(node, textWidth, circleTextPadding, textPosition);
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
  let CIRCLE_PADDING = arguments[0];

  let childrenNotWithinNode = [];

  node.getOrigChildren().forEach(c => {
    let distanceFromNodeMiddleToChildRim = distance(node.visualData.x, node.visualData.y, c.visualData.x, c.visualData.y)
        + c.visualData.r;
    if (node.visualData.r - distanceFromNodeMiddleToChildRim < CIRCLE_PADDING / 2) {
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

Assertion.addMethod('doNotOverlap', function () {
  let nodes = this._obj;
  let CIRCLE_PADDING = arguments[0];

  let nodesOverlapping = new Set();

  nodes.forEach(c => {
    nodes.filter(d => d !== c).forEach(d => {
      let diff = distance(c.visualData.x, c.visualData.y, d.visualData.x, d.visualData.y);
      let minExpDiff = c.visualData.r + d.visualData.r + CIRCLE_PADDING;
      if (diff + MAX_RADIUS_DIFF < minExpDiff) {
        nodesOverlapping.add(c.getFullName(), c);
        nodesOverlapping.add(d.getFullName(), d);
      }
    });
  });

  this.assert(
      nodesOverlapping.size == 0
      , "expected #{this} not to overlap but got #{act} that are overlapping"
      , "expected #{this} to not be of type #{act}"
      , 0
      , Array.from(nodesOverlapping)
  );
});