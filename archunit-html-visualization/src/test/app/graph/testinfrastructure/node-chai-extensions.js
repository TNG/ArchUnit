'use strict';

const Assertion = require('chai').Assertion;

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
