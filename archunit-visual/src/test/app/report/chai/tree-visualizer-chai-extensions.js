'use strict';

const Assertion = require("chai").Assertion;
const Vector = require('../main-files').get('vectors').Vector;

const MAX_RADIUS_DIFF = 0.05;

const radiusOfLeaf = (leaf, textwidth, CIRCLETEXTPADDING) => textwidth(leaf.getName()) / 2 + CIRCLETEXTPADDING;

const radiusOfInnerNode = (node, textwidth, circleTestPadding, textPosition) =>
radiusOfLeaf(node, textwidth, circleTestPadding) / Math.sqrt(1 - textPosition * textPosition);

const haveDiffBiggerThan = (value1, value2, diff) => Math.abs(value1 - value2) > diff;

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

Assertion.addMethod('locatedWithin', function (parent) {
  const node = this._obj;

  const centerDifference = Vector.between(node.getAbsoluteCoords(), parent.getAbsoluteCoords()).length();
  const circleRadiusContainingNode = centerDifference + node.getRadius();

  new Assertion(circleRadiusContainingNode).to.be.at.most(parent.getRadius());
});