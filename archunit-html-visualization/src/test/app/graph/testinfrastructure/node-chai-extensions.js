'use strict';

const {Vector} = require('../../../../main/app/graph/infrastructure/vectors');
const {Circle} = require('../../../../main/app/graph/infrastructure/shapes');
const Assertion = require('chai').Assertion;

const nodesFrom = root => Array.from(root.getSelfAndDescendants());

const getHiddenChildrenOfNode = node => node._originalChildren.filter(child => !node.getCurrentChildren().includes(child));

const assertThatVisibilityOfNodesIsConsistent = root => {
  root._callOnSelfThenEveryDescendant(node => {
    new Assertion(node._view._svgElement.isVisible).to.be.true;

    const h = getHiddenChildrenOfNode(node);
    h.forEach(child =>
      new Assertion(child._view._svgElement.isVisible).to.be.false);
  });
};

Assertion.addMethod('haveFullQualifiedName', function (fqn) {
  const node = this._obj;
  new Assertion(node.getFullName()).to.equal(fqn);
});

Assertion.addMethod('haveVisibleNodes', function () {
  const root = this._obj;
  const allNodes = nodesFrom(root);

  const actFullNames = allNodes.map(node => node.getFullName());
  const expectedNodeFullNames = Array.isArray(arguments[0]) ? arguments[0] : Array.from(arguments);
  new Assertion(actFullNames).to.have.members(expectedNodeFullNames);

  assertThatVisibilityOfNodesIsConsistent(root);
});

Assertion.addMethod('locatedWithinWithPadding', function (parent, padding) {
  const node = this._obj;
  const relativeCircle = Circle.from(node.nodeShape.relativePosition, node.nodeShape.absoluteCircle.r);
  new Assertion(parent.nodeShape.absoluteShape.containsRelativeCircle(relativeCircle, padding)).to.be.true;
});

Assertion.addMethod('notOverlapWith', function (sibling, padding) {
  const node = this._obj;
  const distanceBetweenMiddlePoints = Vector.between(node.nodeShape.relativePosition, sibling.nodeShape.relativePosition).length();
  const radiusSum = node.nodeShape.getRadius() + sibling.nodeShape.getRadius();
  //here is added 1, because the collide-force-layout does not guarantee that the circle do not overlap
  new Assertion(radiusSum + padding).to.be.at.most(distanceBetweenMiddlePoints + 1);
});

Assertion.addMethod('containExactlyNodes', function (nodes) {
  const actFullNames = Array.from(this._obj, node => node.getFullName()).sort();
  const expFullNames = Array.from(nodes).sort();
  if (actFullNames.includes('default') && !expFullNames.includes('default')) {
    expFullNames.push('default');
  }
  new Assertion(actFullNames).to.have.members(expFullNames);
});

////FIXME: remove most of the methods above when the clean up of the tests is finished

Assertion.addMethod('foldable', function () {
  const svgElement = this._obj;
  new Assertion([...svgElement.cssClasses]).to.include('foldable');
  new Assertion([...svgElement.cssClasses]).not.to.include('unfoldable');
});

Assertion.addMethod('unfoldable', function () {
  const svgElement = this._obj;
  new Assertion([...svgElement.cssClasses]).to.include('unfoldable');
  new Assertion([...svgElement.cssClasses]).not.to.include('foldable');
});

Assertion.addMethod('onlyContainNodes', function (...expectedNodeFullNames) {
  const actualNodes = this._obj;
  const actualNodesFullNames = actualNodes.map(node => node.getFullName());
  new Assertion(actualNodesFullNames).to.have.members(expectedNodeFullNames);
});

Assertion.addMethod('onlyContainOrderedNodes', function (...expectedNodeFullNames) {
  const actualNodes = this._obj;
  const actualNodesFullNames = actualNodes.map(node => node.getFullName());
  new Assertion(actualNodesFullNames).to.deep.equal(expectedNodeFullNames);
});