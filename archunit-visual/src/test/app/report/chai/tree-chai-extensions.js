'use strict';

const Assertion = require("chai").Assertion;

const nodesFrom = object => Array.from(object.root ? object.root.getSelfAndDescendants() : object);

const convertActualAndExpectedToStrings = (actual, args) => {
  const expectedNodeFullNames = Array.isArray(args[0]) ? args[0] : Array.from(args);

  const actualStrings = actual.map(n => n.getFullName()).sort();
  const expectedStrings = expectedNodeFullNames.sort();
  return {actualStrings, expectedStrings};
};

Assertion.addMethod('containOnlyNodes', function () {
  const {actualStrings, expectedStrings} = convertActualAndExpectedToStrings(nodesFrom(this._obj), arguments);

  new Assertion(actualStrings).to.deep.equal(expectedStrings);
});

Assertion.addMethod('containOnlyClasses', function () {
  const actual = nodesFrom(this._obj).filter(node => node.isLeaf());
  const {actualStrings, expectedStrings} = convertActualAndExpectedToStrings(actual, arguments);

  new Assertion(actualStrings).to.deep.equal(expectedStrings);
});

Assertion.addMethod('containNoClasses', function () {
  const actual = nodesFrom(this._obj).filter(node => node.isLeaf() && !node.isPackage());

  //noinspection BadExpressionStatementJS -> Chai magic
  new Assertion(actual).to.be.empty;
});

Assertion.addMethod('containNodes', function () {
  const {actualStrings, expectedStrings} = convertActualAndExpectedToStrings(nodesFrom(this._obj), arguments);

  new Assertion(actualStrings).to.include.members(expectedStrings);
});





