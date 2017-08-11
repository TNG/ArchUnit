'use strict';

const expect = require('chai').expect;
const testGraph = require('./test-object-creator').graph;
const nodePredicates = require('./main-files').get('node-predicates');

const testStringContains = (subString) => ({
  against: input => ({
    is: expected => {
      it(`stringContains('${subString}')('${input}') == ${expected}`, () => {
        expect(nodePredicates.stringContains(subString)(input)).to.equal(expected);
      });
    }
  })
});

describe('matching strings by checking for a certain substring', () => {
  describe('simple substrings', () => {
    testStringContains('foo').against('foobar').is(true);
    testStringContains('oba').against('foobar').is(true);
    testStringContains('bar').against('foobar').is(true);
    testStringContains('foobar').against('foobar').is(true);

    testStringContains('for').against('foobar').is(false);
  });

  describe('leading whitespace is ignored', () => {
    testStringContains(' foo').against('foobar').is(true);
    testStringContains('   foobar').against('foobar').is(true);

    testStringContains('fooar').against('foobar').is(false);
  });

  describe('substrings with trailing whitespace must end with pattern', () => {
    testStringContains('bar ').against('foobar').is(true);
    testStringContains('bar    ').against('foobar').is(true);
    testStringContains(' bar ').against('foobar').is(true);

    testStringContains('foo ').against('foobar').is(false);
    testStringContains('fooba ').against('foobar').is(false);
  });

  describe('only the asterisk (*) is interpreted as wildcard', () => {
    testStringContains('f*ar').against('foobar').is(true);
    testStringContains('some.r*.*Class').against('some.random.Class').is(true);
    testStringContains('.$?[]\\^+').against('.$?[]\\^+').is(true);

    testStringContains('some.r*.*Class').against('some.randomClass').is(false);
    testStringContains('.$?[]\\^+').against('.$?[.\\^+').is(false);
  });

  describe('some typical scenarios when filtering fully qualified class names', () => {
    testStringContains('SimpleClass').against('my.company.SimpleClass').is(true);
    testStringContains('Json').against('some.evil.long.pkg.JsonParser').is(true);
    testStringContains('Json ').against('some.evil.long.pkg.JsonParser').is(false);

    testStringContains('pkg').against('some.evil.long.pkg.SomeClass').is(true);
    testStringContains('.pkg.').against('some.evil.long.pkg.SomeClass').is(true);
    testStringContains('.long.pkg.').against('some.evil.long.pkg.SomeClass').is(true);
    testStringContains('.pk.').against('some.evil.long.pkg.SomeClass').is(false);
    testStringContains('.evil..pkg.').against('some.evil.long.pkg.SomeClass').is(false);
  });
});

describe('matching inverted predicates via "not"', () => {
  it("should match iff original predicate doesn't", () => {
    expect(nodePredicates.not(() => true)('anything')).to.equal(false);
    expect(nodePredicates.not(() => false)('anything')).to.equal(true);
  })
});

describe('Filter node names containing', () => {
  it('should filter out a node not matching a simple part of the full class name', () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodePredicates.nameContainsPredicate('SomeClass')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('ompany.So')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('NotThere')(classNode)).to.equal(false);
    expect(nodePredicates.nameContainsPredicate('pan.S')(classNode)).to.equal(false);
  });

  it('should filter out a node not matching a wildcard part of the full class name', () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodePredicates.nameContainsPredicate('*Class')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('my.*')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('*')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('my*any*meCl')(classNode)).to.equal(true);

    expect(nodePredicates.nameContainsPredicate('*Wrong*')(classNode)).to.equal(false);
    expect(nodePredicates.nameContainsPredicate('not*my*any*meCl')(classNode)).to.equal(false);
    expect(nodePredicates.nameContainsPredicate('my.co.*any*')(classNode)).to.equal(false);
  });

  it('should filter out a node not ending in a certain text, if the string ends in whitespace', () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodePredicates.nameContainsPredicate('Some')(classNode)).to.equal(true);
    expect(nodePredicates.nameContainsPredicate('Some ')(classNode)).to.equal(false);
    expect(nodePredicates.nameContainsPredicate('Class ')(classNode)).to.equal(true);
  });

  it('should invert the filter if exclude==true is passed', () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodePredicates.nameContainsPredicate('Some', true)(classNode)).to.equal(false);
    expect(nodePredicates.nameContainsPredicate('Wrong', true)(classNode)).to.equal(true);
  });
});