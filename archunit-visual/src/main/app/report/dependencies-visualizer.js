'use strict';

const vectors = require('./vectors.js').vectors;

const oneEndNodeIsCompletelyWithinTheOtherOne = (node1, node2) => {
  const middleDiff = vectors.distance(node1, node2);
  return middleDiff + Math.min(node1.r, node2.r) < Math.max(node1.r, node2.r);
};

const VisualData = class {
  constructor() {
    this.startPoint = {};
    this.endPoint = {};
    this.visible = false;
  }

  recalc(mustShareNodes, visualStartNode, visualEndNode) {
    const lineDiff = 20;
    const oneIsInOther = oneEndNodeIsCompletelyWithinTheOtherOne(visualStartNode, visualEndNode),
      nodes = [visualStartNode, visualEndNode].sort((a, b) => a.r - b.r);

    const direction = vectors.vectorOf(visualEndNode.x - visualStartNode.x,
      visualEndNode.y - visualStartNode.y);

    let startDirectionVector = vectors.cloneVector(direction);
    if (oneIsInOther && visualStartNode === nodes[0]) {
      startDirectionVector = vectors.getRevertedVector(startDirectionVector);
    }
    startDirectionVector = vectors.getDefaultIfNull(startDirectionVector);
    let endDirectionVector = oneIsInOther ? vectors.cloneVector(startDirectionVector) : vectors.getRevertedVector(startDirectionVector);

    if (mustShareNodes) {
      let orthogonalVector = vectors.norm(vectors.getOrthogonalVector(startDirectionVector), lineDiff / 2);
      if (oneIsInOther && visualStartNode === nodes[1]) {
        orthogonalVector = vectors.getRevertedVector(orthogonalVector);
      }
      startDirectionVector = vectors.norm(startDirectionVector, visualStartNode.r);
      endDirectionVector = vectors.norm(endDirectionVector, visualEndNode.r);
      startDirectionVector = vectors.addVectors(startDirectionVector, orthogonalVector);
      endDirectionVector = vectors.addVectors(endDirectionVector, orthogonalVector);
    }

    startDirectionVector = vectors.norm(startDirectionVector, visualStartNode.r);
    endDirectionVector = vectors.norm(endDirectionVector, visualEndNode.r);

    this.startPoint = vectors.vectorOf(visualStartNode.x + startDirectionVector.x, visualStartNode.y + startDirectionVector.y);
    this.endPoint = vectors.vectorOf(visualEndNode.x + endDirectionVector.x, visualEndNode.y + endDirectionVector.y);
  }
};

const refreshVisualData = dependency => {
  dependency.visualData = new VisualData();
  dependency.visualData.recalc(dependency.mustShareNodes, dependency.getStartNode().getAbsoluteVisualData(), dependency.getEndNode().getAbsoluteVisualData());
};

const refreshVisualDataOf = (nodeFullName, dependencies) => {
  dependencies.filter(d => d.from.startsWith(nodeFullName) || d.to.startsWith(nodeFullName)).forEach(d => refreshVisualData(d));
};

const refreshVisualDataOfDependencies = dependencies => {
  dependencies.forEach(d => refreshVisualData(d));
};

const visualizeDependencies = dependencies => {
  refreshVisualDataOfDependencies(dependencies.getVisible());
  dependencies.addObserver(refreshVisualDataOfDependencies);
};

module.exports = {
  refreshVisualDataOf: refreshVisualDataOf,
  visualizeDependencies: visualizeDependencies,
  refreshVisualDataOfDependencies: refreshVisualDataOfDependencies
};