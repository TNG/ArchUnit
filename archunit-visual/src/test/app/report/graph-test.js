'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testObjects = require("./test-object-creator.js");

describe("Graph", () => {
  it("creates a correct node map", () => {
    let graph = testObjects.testGraph3().graph;
    let allNodes = testObjects.allNodes(graph.root);
    expect(allNodes.map(n => graph.nodeMap.get(n))).to.containExactlyNodes(allNodes);
  });
});