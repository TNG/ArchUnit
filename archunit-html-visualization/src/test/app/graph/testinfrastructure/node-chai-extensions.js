'use strict';

const Assertion = require('chai').Assertion;

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
