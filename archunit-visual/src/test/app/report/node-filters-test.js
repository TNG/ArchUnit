'use strict';

const expect = require("chai").expect;
const testGraph = require('./test-object-creator').graph;
const nodeFilters = require('./main-files').get('node-filters');

describe("Filter node names containing", () => {
  it("should filter out a node not matching a simple part of the full class name", () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodeFilters.nameContainsFilter('SomeClass')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('ompany.So')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('NotThere')(classNode)).to.equal(false);
    expect(nodeFilters.nameContainsFilter('pan.S')(classNode)).to.equal(false);
  });

  it("should filter out a node not matching a wildcard part of the full class name", () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodeFilters.nameContainsFilter('*Class')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('my.*')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('*')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('my*any*meCl')(classNode)).to.equal(true);

    expect(nodeFilters.nameContainsFilter('*Wrong*')(classNode)).to.equal(false);
    expect(nodeFilters.nameContainsFilter('not*my*any*meCl')(classNode)).to.equal(false);
    expect(nodeFilters.nameContainsFilter('my.co.*any*')(classNode)).to.equal(false);
  });

  it("should filter out a node not ending in a certain text, if the string ends in whitespace", () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodeFilters.nameContainsFilter('Some')(classNode)).to.equal(true);
    expect(nodeFilters.nameContainsFilter('Some ')(classNode)).to.equal(false);
    expect(nodeFilters.nameContainsFilter('Class ')(classNode)).to.equal(true);
  });

  it("should invert the filter if exclude==true is passed", () => {
    const graph = testGraph([
      'my.company.SomeClass'
    ]);

    const classNode = graph.root.getCurrentChildren()[0].getCurrentChildren()[0];

    expect(nodeFilters.nameContainsFilter('Some', true)(classNode)).to.equal(false);
    expect(nodeFilters.nameContainsFilter('Wrong', true)(classNode)).to.equal(true);
  });
});