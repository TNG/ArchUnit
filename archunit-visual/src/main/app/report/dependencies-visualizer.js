'use strict';

const vectors = require('./vectors.js').vectors;

const lineDiff = 20;

let oneEndNodeIsCompletelyWithinTheOtherOne = (node1, node2) => {
  let middleDiff = vectors.distance(node1, node2);
  return middleDiff + Math.min(node1.r, node2.r) < Math.max(node1.r, node2.r);
};

let getSmallerAndBiggerNode = (node1, node2) => {
  if (node2.r >= node1.r) {
    return {
      bigger: node2,
      smaller: node1
    }
  }
  else {
    return {
      bigger: node1,
      smaller: node2
    }
  }
};

let getTitleOffset = (angleRad, textPadding) => {
  return [Math.round(textPadding * Math.sin(angleRad)),
    Math.round(textPadding * Math.cos(angleRad))];
};

let VisualData = class {
  constructor() {
    this.startPoint = {};
    this.endPoint = {};
    this.middlePoint = {};
    this.angleDeg = 0;
    this.angleRad = 0;
    this.visible = false;
    //this.dragPro = new DragProtocol(this.x, this.y);
  }

  recalc(mustShareNodes, visualStartNode, visualEndNode) {
    let oneIsInOther = oneEndNodeIsCompletelyWithinTheOtherOne(visualStartNode, visualEndNode),
        nodes = getSmallerAndBiggerNode(visualStartNode, visualEndNode);

    let direction = vectors.vectorOf(visualEndNode.x - visualStartNode.x,
        visualEndNode.y - visualStartNode.y);

    let startDirectionVector = vectors.cloneVector(direction);
    if (oneIsInOther && visualStartNode === nodes.smaller) {
      startDirectionVector = vectors.getRevertedVector(startDirectionVector);
    }
    startDirectionVector = vectors.getDefaultIfNull(startDirectionVector);
    let endDirectionVector = oneIsInOther ? vectors.cloneVector(startDirectionVector) : vectors.getRevertedVector(startDirectionVector);

    if (mustShareNodes) {
      let orthogonalVector = vectors.norm(vectors.getOrthogonalVector(startDirectionVector), lineDiff / 2);
      if (oneIsInOther && visualStartNode === nodes.bigger) {
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
    this.middlePoint = vectors.vectorOf((this.endPoint.x + this.startPoint.x) / 2, (this.endPoint.y + this.startPoint.y) / 2);
    this.angleRad = vectors.angleToVector(vectors.getDefaultIfNull(direction));
    this.angleDeg = vectors.getAngleDeg(this.angleRad);
  }

  getEdgesTitleTranslation(textPadding) {
    let offset = getTitleOffset(this.angleRad, textPadding);
    return "translate(" + (this.middlePoint.x + offset[0]) + "," + (this.middlePoint.y - offset[1]) + ") " +
        "rotate(" + this.angleDeg + ")";
  }
};

let refreshVisualData = dependency => {
  dependency.visualData = new VisualData();
  dependency.visualData.recalc(dependency.mustShareNodes, dependency.getStartNode().visualData, dependency.getEndNode().visualData);
};

let refreshVisualDataOf = (nodeFullName, dependencies) => {
  dependencies.filter(d => d.from.startsWith(nodeFullName) || d.to.startsWith(nodeFullName)).forEach(d => refreshVisualData(d));
};

let refreshVisualDataOfDependencies = dependencies => {
  dependencies.forEach(d => refreshVisualData(d));
};

let visualizeDependencies = dependencies => {
  refreshVisualDataOfDependencies(dependencies.getVisible());
  dependencies.addObserver(refreshVisualDataOfDependencies);
};

module.exports = {
  refreshVisualDataOf: refreshVisualDataOf,
  visualizeDependencies: visualizeDependencies,
  refreshVisualDataOfDependencies: refreshVisualDataOfDependencies
};