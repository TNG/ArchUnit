'use strict';

import chai from 'chai';
import nodePredicates from '../../../main/app/report/predicates';

const expect = chai.expect;

const testStringEquals = (subString) => ({
  against: input => ({
    is: expected => {
      it(`stringContains('${subString}')('${input}') == ${expected}`, () => {
        expect(nodePredicates.stringEquals(subString)(input)).to.equal(expected);
      });
    }
  })
});

describe('matching strings by checking for a certain equal string or prefix-string', () => {
  describe('simple substrings', () => {
    testStringEquals('foobar').against('foobar').is(true);
    testStringEquals('foo.bar').against('foo.bar').is(true);
    testStringEquals('foo.bar').against('foo.bar.baz').is(true);
    testStringEquals('foo.bar').against('foo.bar$baz').is(true);

    testStringEquals('foo.bar').against('foo.barbaz').is(false);
    testStringEquals('bar').against('foobar').is(false);

    testStringEquals('for').against('foobar').is(false);
  });

  describe('leading or concluding whitespace is ignored', () => {
    testStringEquals(' foobar').against('foobar').is(true);
    testStringEquals('   foobar').against('foobar').is(true);
    testStringEquals('foobar ').against('foobar').is(true);
    testStringEquals('foobar   ').against('foobar').is(true);
    testStringEquals(' foobar ').against('foobar').is(true);
    testStringEquals('  foobar  ').against('foobar').is(true);

    testStringEquals('bar ').against('foobar').is(false);
    testStringEquals('bar   ').against('foobar').is(false);
    testStringEquals(' foo').against('foobar').is(false);
    testStringEquals(' fooar ').against('foobar').is(false);
  });

  describe('leading and concluding whitespaces before | are ignored', () => {
    testStringEquals(' foo| bar').against('foo').is(true);
    testStringEquals(' foo| bar').against('bar').is(true);
    testStringEquals(' foo|   bar').against('bar').is(true);
    testStringEquals(' foo| bar| test').against('test').is(true);
    testStringEquals('foo |bar ').against('foo').is(true);
    testStringEquals('foo |bar ').against('bar').is(true);
    testStringEquals('foo |bar |test ').against('test').is(true);

    testStringEquals('foo |bar ').against('fo').is(false);
    testStringEquals('foo |bar ').against('ba').is(false);
    testStringEquals('foo |bar |test ').against('test1').is(false);
    testStringEquals(' foo| bar').against('anyOther').is(false);
    testStringEquals(' foo | bar ').against('anyOther').is(false);
  });

  describe('only the asterisk (*) is interpreted as wildcard', () => {
    testStringEquals('f*ar').against('foobar').is(true);
    testStringEquals('f*ar').against('foobar.baz').is(true);
    testStringEquals('f*ar').against('foobar$baz').is(true);
    testStringEquals('some.r*.*Class').against('some.random.Class').is(true);
    testStringEquals('.$?[]\\^+').against('.$?[]\\^+').is(true);

    testStringEquals('some.r*.*Class').against('some.randomClass').is(false);
    testStringEquals('.$?[]\\^+').against('.$?[.\\^+').is(false);
  });

  describe('| separates different options', () => {
    testStringEquals('foo|bar').against('foo').is(true);
    testStringEquals('foo|bar').against('bar').is(true);
    testStringEquals('foo|bar|test').against('test').is(true);
    testStringEquals('foo|bar|test').against('test.foo').is(true);
    testStringEquals('foo|bar|test').against('test$foo').is(true);

    testStringEquals('foo|bar').against('anyOther').is(false);
    testStringEquals('foo|bar|test').against('notMatching').is(false);
  });

  describe('options starting with ~ are excluded from the included set', () => {
    testStringEquals('foo|~bar').against('foo').is(true);
    testStringEquals('foo*|~*baz').against('foobar').is(true);

    testStringEquals('foo|~bar').against('bar').is(false);
    testStringEquals('foo|~bar').against('bar.foo').is(false);
    testStringEquals('foo|~bar').against('bar$foo').is(false);
    testStringEquals('foo*|~*bar').against('foobar').is(false);
    testStringEquals('*bar*|~*foo*').against('foobar').is(false);
    testStringEquals('foo*|~*bar|~*baz').against('foobaz').is(false);
    testStringEquals('foo|~foo').against('foo').is(false);
    testStringEquals('foo|~foo').against('anyOther').is(false);
  });

  describe('some typical scenarios when filtering fully qualified class names', () => {
    testStringEquals('my.company.*').against('my.company.SimpleClass').is(true);
    testStringEquals('my.company').against('my.company.SimpleClass').is(true);
    testStringEquals('my.company.SimpleClass').against('my.company.SimpleClass$InnerClass').is(true);
    testStringEquals('*.SimpleClass').against('my.company.SimpleClass').is(true);
    testStringEquals('*Json*').against('some.evil.long.pkg.JsonParser').is(true);
    testStringEquals('*Json').against('some.evil.long.pkg.JsonParser').is(false);

    testStringEquals('*pkg*').against('some.evil.long.pkg.SomeClass').is(true);
    testStringEquals('*.pkg.*').against('some.evil.long.pkg.SomeClass').is(true);
    testStringEquals('*.long.pkg.*').against('some.evil.long.pkg.SomeClass').is(true);
    testStringEquals('*.pk.*').against('some.evil.long.pkg.SomeClass').is(false);
    testStringEquals('*.evil..pkg.*').against('some.evil.long.pkg.SomeClass').is(false);
    testStringEquals('my.company.*|~*.SimpleClass').against('my.company.SimpleClass').is(false);
  });
});

describe('matching inverted predicates via "not"', () => {
  it("should match iff original predicate doesn't", () => {
    expect(nodePredicates.not(() => true)('anything')).to.equal(false);
    expect(nodePredicates.not(() => false)('anything')).to.equal(true);
  })
});

const testAnd = (...bools) => ({
  evaluatesTo: (expected) => {
    it(`should evaluate to ${expected}, if the supplied predicates evaluate to ${bools}`, () => {
      const predicates = bools.map(b => () => b);
      expect(nodePredicates.and(...predicates)('anything')).to.equal(expected);
    })
  }
});

describe('AND-ing predicates via "and"', () => {
  testAnd(true, true).evaluatesTo(true);
  testAnd(false, true).evaluatesTo(false);
  testAnd(true, false).evaluatesTo(false);
  testAnd(false, false).evaluatesTo(false);
  testAnd(true, false, true).evaluatesTo(false);
  testAnd(true, true, false).evaluatesTo(false);
  testAnd(true, true, true, false).evaluatesTo(false);
  testAnd(true, true, false, true).evaluatesTo(false);
  testAnd(true, true, true, true).evaluatesTo(true);
});