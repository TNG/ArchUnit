'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testObjects = require("./test-object-creator.js");
const testGraph = require('./test-object-creator').graph;

describe("Graph", () => {
  it("creates a correct node map", () => {
    const graph = testObjects.testGraph3().graph;
    const allNodes = testObjects.allNodes(graph.root);
    expect(allNodes.map(n => graph.root.getByName(n))).to.containOnlyNodes(allNodes);
  });

  describe('filter nodes by name', () => {
    it('should filter out a node not matching a simple part of the full class name', () => {
      const graph = testGraph(
        'my.company.SomeClass',
        'my.company.OtherClass');

      expect(graph).to.containOnlyClasses('my.company.SomeClass', 'my.company.OtherClass');

      graph.filterNodesByNameContaining('SomeClass');
      return graph.updatePromise.then(() => expect(graph).to.containOnlyClasses('my.company.SomeClass')).then(() => {
        graph.filterNodesByNameNotContaining('SomeClass');
        return graph.updatePromise.then(() => expect(graph).to.containOnlyClasses('my.company.OtherClass'));
      }) ;
    });

    it('should filter out a node not matching a part with wildcard', () => {
      const graph = testGraph(
        'my.company.first.SomeClass',
        'my.company.first.OtherClass',
        'my.company.second.SomeClass',
        'my.company.second.OtherClass');

      graph.filterNodesByNameContaining('my.*.first');
      return graph.updatePromise.then(() => expect(graph).to.containOnlyClasses('my.company.first.SomeClass', 'my.company.first.OtherClass'))
        .then(() => {
          graph.filterNodesByNameContaining('company*.Some');
          return graph.updatePromise.then(() => expect(graph).to.containOnlyClasses('my.company.first.SomeClass', 'my.company.second.SomeClass'))
            .then(() => {
              graph.filterNodesByNameNotContaining('company*.Some');
              return graph.updatePromise.then(() => expect(graph).to.containOnlyClasses('my.company.first.OtherClass', 'my.company.second.OtherClass'))
                .then(() => {
                  graph.filterNodesByNameContaining('company*.Some ');
                  return graph.updatePromise.then(() => expect(graph).to.containNoClasses());
                });
            });
        });
    });
  });
});