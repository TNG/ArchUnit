'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testObjects = require("./test-object-creator.js");

describe("Graph", () => {
  it("creates a correct node map", () => {
    const graph = testObjects.testGraph3().graph;
    const allNodes = testObjects.allNodes(graph.root);
    expect(allNodes.map(n => graph.root.getByName(n))).to.containExactlyNodes(allNodes);
  });
});