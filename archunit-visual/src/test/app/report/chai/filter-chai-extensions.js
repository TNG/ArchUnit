'use strict';

import chai from 'chai';

const Assertion = chai.Assertion;

Assertion.addMethod('containNumbers', function (numbers) {
  const numbersArrayObject = this._obj;
  new Assertion(numbersArrayObject.map(o => o.value)).to.deep.equal(numbers);
});