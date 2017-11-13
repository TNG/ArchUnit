'use strict';

const Assertion = require("chai").Assertion;
const Vector = require('../main-files').get('vectors').Vector;

const MAXIMUM_DELTA = 0.0001;

Assertion.addMethod('locatedWithinWithPadding', function (parent, padding) {
  const node = this._obj;
  const distanceToNodeRim = new Vector(node.visualData.x, node.visualData.y).length() + node.visualData.r;
  new Assertion(distanceToNodeRim + padding).to.be.at.most(parent.getRadius() + MAXIMUM_DELTA);
});

Assertion.addMethod('notOverlapWith', function (sibling, padding) {
  const node = this._obj;
  const distanceBetweenMiddlePoints = Vector.between(node.visualData, sibling.visualData).length();
  const radiusSum = node.visualData.r + sibling.visualData.r;
  new Assertion(radiusSum + padding).to.be.at.most(distanceBetweenMiddlePoints + MAXIMUM_DELTA);
});