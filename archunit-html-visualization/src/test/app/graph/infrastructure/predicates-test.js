'use strict';

const chai = require('chai');
const nodePredicates = require('../../../../main/app/graph/infrastructure/predicates');

const expect = chai.expect;

const testMatchesPattern = (subString) => ({
  against: input => ({
    is: expected => {
      it(`matchesPattern('${subString}')('${input}') == ${expected}`, () => {
        expect(nodePredicates.matchesPattern(subString)(input)).to.equal(expected);
      });
    }
  })
});

describe('matching strings by checking for a certain equal string or prefix-string', () => {
  describe('empty string matches everything', () => {
    testMatchesPattern('').against('foobar').is(true);
  });

  describe('simple substrings', () => {
    testMatchesPattern('foobar').against('foobar').is(true);
    testMatchesPattern('foo.bar').against('foo.bar').is(true);
    testMatchesPattern('foo.bar').against('foo.bar.baz').is(true);
    testMatchesPattern('foo.bar').against('foo.bar$baz').is(true);
    testMatchesPattern('foo-bar').against('foo-bar').is(true);
    testMatchesPattern('foo/bar').against('foo/bar').is(true);

    testMatchesPattern('foo.bar').against('foo.barbaz').is(false);
    testMatchesPattern('bar').against('foobar').is(false);

    testMatchesPattern('for').against('foobar').is(false);
  });

  describe('leading or concluding whitespace is ignored', () => {
    testMatchesPattern(' foobar').against('foobar').is(true);
    testMatchesPattern('   foobar').against('foobar').is(true);
    testMatchesPattern('foobar ').against('foobar').is(true);
    testMatchesPattern('foobar   ').against('foobar').is(true);
    testMatchesPattern(' foobar ').against('foobar').is(true);
    testMatchesPattern('  foobar  ').against('foobar').is(true);

    testMatchesPattern('bar ').against('foobar').is(false);
    testMatchesPattern('bar   ').against('foobar').is(false);
    testMatchesPattern(' foo').against('foobar').is(false);
    testMatchesPattern(' fooar ').against('foobar').is(false);
  });

  describe('leading and concluding whitespaces before | are ignored', () => {
    testMatchesPattern(' foo| bar').against('foo').is(true);
    testMatchesPattern(' foo| bar').against('bar').is(true);
    testMatchesPattern(' foo|   bar').against('bar').is(true);
    testMatchesPattern(' foo| bar| test').against('test').is(true);
    testMatchesPattern('foo |bar ').against('foo').is(true);
    testMatchesPattern('foo |bar ').against('bar').is(true);
    testMatchesPattern('foo |bar |test ').against('test').is(true);

    testMatchesPattern('foo |bar ').against('fo').is(false);
    testMatchesPattern('foo |bar ').against('ba').is(false);
    testMatchesPattern('foo |bar |test ').against('test1').is(false);
    testMatchesPattern(' foo| bar').against('anyOther').is(false);
    testMatchesPattern(' foo | bar ').against('anyOther').is(false);
  });

  describe('only the asterisk (*) is interpreted as wildcard', () => {
    testMatchesPattern('f*ar').against('foobar').is(true);
    testMatchesPattern('f*ar').against('foobar.baz').is(true);
    testMatchesPattern('f*ar').against('foobar$baz').is(true);
    testMatchesPattern('some.r*.*Class').against('some.random.Class').is(true);
    testMatchesPattern('.$?[]\\^+').against('.$?[]\\^+').is(true);

    testMatchesPattern('some.r*.*Class').against('some.randomClass').is(false);
    testMatchesPattern('.$?[]\\^+').against('.$?[.\\^+').is(false);
  });

  describe('| separates different options', () => {
    testMatchesPattern('foo|bar').against('foo').is(true);
    testMatchesPattern('foo|bar').against('bar').is(true);
    testMatchesPattern('foo|bar|test').against('test').is(true);
    testMatchesPattern('foo|bar|test').against('test.foo').is(true);
    testMatchesPattern('foo|bar|test').against('test$foo').is(true);

    testMatchesPattern('foo|bar').against('anyOther').is(false);
    testMatchesPattern('foo|bar|test').against('notMatching').is(false);
  });

  describe('options starting with ~ are excluded from the included set', () => {
    testMatchesPattern('foo|~bar').against('foo').is(true);
    testMatchesPattern('foo*|~*baz').against('foobar').is(true);

    testMatchesPattern('foo|~bar').against('bar').is(false);
    testMatchesPattern('foo|~bar').against('bar.foo').is(false);
    testMatchesPattern('foo|~bar').against('bar$foo').is(false);
    testMatchesPattern('foo*|~*bar').against('foobar').is(false);
    testMatchesPattern('*bar*|~*foo*').against('foobar').is(false);
    testMatchesPattern('foo*|~*bar|~*baz').against('foobaz').is(false);
    testMatchesPattern('foo|~foo').against('foo').is(false);
    testMatchesPattern('foo|~foo').against('anyOther').is(false);
  });

  describe('some typical scenarios when filtering fully qualified class names', () => {
    testMatchesPattern('my.company.*').against('my.company.SimpleClass').is(true);
    testMatchesPattern('my.company').against('my.company.SimpleClass').is(true);
    testMatchesPattern('my.company.SimpleClass').against('my.company.SimpleClass$InnerClass').is(true);
    testMatchesPattern('*.SimpleClass').against('my.company.SimpleClass').is(true);
    testMatchesPattern('*Json*').against('some.evil.long.pkg.JsonParser').is(true);
    testMatchesPattern('*Json').against('some.evil.long.pkg.JsonParser').is(false);

    testMatchesPattern('*pkg*').against('some.evil.long.pkg.SomeClass').is(true);
    testMatchesPattern('*.pkg.*').against('some.evil.long.pkg.SomeClass').is(true);
    testMatchesPattern('*.long.pkg.*').against('some.evil.long.pkg.SomeClass').is(true);
    testMatchesPattern('*.pk.*').against('some.evil.long.pkg.SomeClass').is(false);
    testMatchesPattern('*.evil..pkg.*').against('some.evil.long.pkg.SomeClass').is(false);
    testMatchesPattern('my.company.*|~*.SimpleClass').against('my.company.SimpleClass').is(false);
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
