'use strict';

/**
 * calculates a topological order of the objects, that are implicit given through the descendants of the root-object
 * @param rootObject the first object of the graph, which is not a descendant of any other node
 * @param getDescendantsOfObject returns the descendant objects of a specific object
 * @return {Array} array with the objects, topological sorted
 */
const sortTopological = (rootObject, getDescendantsOfObject) => {
  const nodeMap = new Map();

  const Node = class {
    constructor(object) {
      nodeMap.set(object, this);
      this._object = object;
      this._predecessors = [];
      this._descendants = getDescendantsOfObject(this._object).map(descendantObject => {
        if (nodeMap.has(descendantObject)) {
          return nodeMap.get(descendantObject);
        } else {
          return new Node(descendantObject);
        }
      });
      this._descendants.forEach(d => d.addPredecessor(this));
    }

    addPredecessor(pred) {
      this._predecessors.push(pred);
    }
  };

  const root = new Node(rootObject);
  const topo = [];

  const queue = [root];
  while (queue.length > 0) {
    const next = queue.shift();
    topo.push(next._object);
    next._descendants.forEach(d => d._predecessors.splice(d._predecessors.indexOf(next), 1));
    next._descendants.filter(d => d._predecessors.length === 0).forEach(d => queue.push(d));

    if (next._descendants.length !== 0 && queue.length === 0) {
      throw new Error('the graph is cyclic');
    }
  }

  return topo;
};

module.exports = {sortTopological};