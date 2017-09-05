'use strict';

const Assertion = require("chai").Assertion;
const Vector = require('../main-files').get('vectors').Vector;

const MAX_RADIUS_DIFF = 0.05;

const radiusOfLeaf = (leaf, textwidth, CIRCLETEXTPADDING) => textwidth(leaf.getName()) / 2 + CIRCLETEXTPADDING;

const radiusOfInnerNode = (node, textwidth, circleTestPadding, textPosition) =>
radiusOfLeaf(node, textwidth, circleTestPadding) / Math.sqrt(1 - textPosition * textPosition);

const haveDiffBiggerThan = (value1, value2, diff) => Math.abs(value1 - value2) > diff;

const distance = (x1, y1, x2, y2) => {
  return Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
};

Assertion.addMethod('haveTextWithinCircle', function (textWidth, circleTextPadding, textPosition) {
  const node = this._obj;
  if (node.isCurrentlyLeaf()) {
    const expRadius = radiusOfLeaf(node, textWidth, circleTextPadding);
    this.assert(
        !haveDiffBiggerThan(expRadius, node.visualData.r, MAX_RADIUS_DIFF)
        , "expected #{this} to have radius #{exp} but got #{act}"
        , "expected #{this} to not be of type #{act}"
        , expRadius
        , node.visualData.r
    );
  }
  else {
    const expRadius = radiusOfInnerNode(node, textWidth, circleTextPadding, textPosition);
    this.assert(
        node.visualData.r > expRadius
        , "expected #{this} to have bigger radius than #{exp} but got #{act}"
        , "expected #{this} to not be of type #{act}"
        , expRadius
        , node.visualData.r
    );
  }
});

Assertion.addMethod('haveChildrenWithinCircle', function (circlePadding) {
  const node = this._obj;

  const childrenNotWithinNode = [];

  node.getOriginalChildren().forEach(c => {
    const nodeAbsVisualData = node.getAbsoluteVisualData();
    const cAbsVisualData = c.getAbsoluteVisualData();
    const distanceFromNodeMiddleToChildRim = distance(nodeAbsVisualData.x, nodeAbsVisualData.y, cAbsVisualData.x, cAbsVisualData.y)
      + cAbsVisualData.r;
    if (nodeAbsVisualData.r - distanceFromNodeMiddleToChildRim < circlePadding / 2) {
      childrenNotWithinNode.push(c);
    }
  });

  this.assert(
    childrenNotWithinNode.length === 0
      , "expected #{this} to have only children within its circle got #{act} being not within its circle"
      , "expected #{this} to not be of type #{act}"
      , 0
      , childrenNotWithinNode
  );
});

Assertion.addMethod('doNotOverlap', function (circlePadding) {
  const nodes = this._obj;

  const nodesOverlapping = new Set();

  nodes.forEach(c => {
    nodes.filter(d => d !== c).forEach(d => {
      const cAbsVisualData = c.getAbsoluteVisualData();
      const dAbsVisualData = d.getAbsoluteVisualData();
      const diff = distance(cAbsVisualData.x, cAbsVisualData.y, dAbsVisualData.x, dAbsVisualData.y);
      const minExpDiff = cAbsVisualData.r + dAbsVisualData.r + circlePadding;
      if (diff + MAX_RADIUS_DIFF < minExpDiff) {
        nodesOverlapping.add(c.getFullName(), c);
        nodesOverlapping.add(d.getFullName(), d);
      }
    });
  });

  this.assert(
    nodesOverlapping.size === 0
      , "expected #{this} not to overlap but got #{act} that are overlapping"
      , "expected #{this} to not be of type #{act}"
      , 0
      , Array.from(nodesOverlapping)
  );
});

Assertion.addMethod('locatedWithin', function (parent) {
  const node = this._obj;

  const centerDifference = Vector.between(node.getAbsoluteVisualData(), parent.getAbsoluteVisualData()).length();
  const circleRadiusContainingNode = centerDifference + node.getRadius();

  new Assertion(circleRadiusContainingNode).to.be.at.most(parent.getRadius());
});