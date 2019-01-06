'use strict';

const {Vector, vectors} = require('../infrastructure/vectors');

const OVERLAP_DELTA = 0.1;

const coloredDependencyTypes = new Set();
const dashedDependencyTypes = new Set();

const init = (View) => {

  const ownDependencyTypes = {
    INNERCLASS_DEPENDENCY: 'INNERCLASS_DEPENDENCY'
  };

  const allDependencies = new Map();

  const oneEndNodeIsCompletelyWithinTheOtherOne = (node1, node2) => {
    const middleDiff = vectors.distance(node1, node2);
    return middleDiff + Math.min(node1.r, node2.r) < Math.max(node1.r, node2.r);
  };

  const VisualData = class {
    constructor(listener) {
      this._listener = listener;
      this.startPoint = {};
      this.endPoint = {};
      this.mustShareNodes = false;
    }

    jumpToPosition(absVisualStartNode, absVisualEndNode) {
      this.recalc(absVisualStartNode, absVisualEndNode);
      this._listener.onJumpedToPosition();
    }

    moveToPosition(absVisualStartNode, absVisualEndNode) {
      this.recalc(absVisualStartNode, absVisualEndNode);
      return this._listener.onMovedToPosition();
    }

    recalc(absVisualStartNode, absVisualEndNode) {
      const lineDiff = 20;
      const oneIsInOther = oneEndNodeIsCompletelyWithinTheOtherOne(absVisualStartNode, absVisualEndNode);
      const nodes = [absVisualStartNode, absVisualEndNode].sort((a, b) => a.r - b.r);

      const direction = Vector.between(absVisualStartNode, absVisualEndNode);

      const startDirectionVector = Vector.from(direction);
      startDirectionVector.revertIf(oneIsInOther && absVisualStartNode === nodes[0]);
      startDirectionVector.makeDefaultIfNull();
      const endDirectionVector = Vector.from(startDirectionVector).revertIf(!oneIsInOther);

      if (this.mustShareNodes) {
        const orthogonalVector = vectors.getOrthogonalVector(startDirectionVector).norm(lineDiff / 2);
        orthogonalVector.revertIf(oneIsInOther && absVisualStartNode === nodes[1]);
        startDirectionVector.norm(absVisualStartNode.r);
        endDirectionVector.norm(absVisualEndNode.r);
        startDirectionVector.add(orthogonalVector);
        endDirectionVector.add(orthogonalVector);
      }

      startDirectionVector.norm(absVisualStartNode.r);
      endDirectionVector.norm(absVisualEndNode.r);

      this.startPoint = vectors.add(absVisualStartNode, startDirectionVector);
      this.endPoint = vectors.add(absVisualEndNode, endDirectionVector);
    }
  };

  const getOrCreateUniqueDependency = (originNode, targetNode, type, isViolation, svgElement, callForAllViews, getDetailedDependencies) => {
    const key = `${originNode.getFullName()}-${targetNode.getFullName()}`;
    if (!allDependencies.has(key)) {
      allDependencies.set(key, new GroupedDependency(originNode, targetNode, type, isViolation, svgElement, callForAllViews, getDetailedDependencies));
    }
    return allDependencies.get(key).withTypeAndViolation(type, isViolation)
  };

  const ElementaryDependency = class {
    constructor(originNode, targetNode, type, description, isViolation = false) {
      this._originNode = originNode;
      this._targetNode = targetNode;
      this.from = originNode.getFullName();
      this.to = targetNode.getFullName();
      this.type = type;
      this.description = description;
      this.isViolation = isViolation;
      this._matchesFilter = new Map();
    }

    get originNode() {
      return this._originNode;
    }

    get targetNode() {
      return this._targetNode;
    }

    setMatchesFilter(key, value) {
      this._matchesFilter.set(key, value);
    }

    matchesAllFilters() {
      return [...this._matchesFilter.values()].every(v => v);
    }

    matchesFilter(key) {
      return this._matchesFilter.get(key);
    }

    toString() {
      return this.description;
    }

    markAsViolation() {
      this.isViolation = true;
    }

    unMarkAsViolation() {
      this.isViolation = false;
    }
  };

  const joinStrings = (separator, ...stringArray) => stringArray.filter(element => element).join(separator);

  const GroupedDependency = class extends ElementaryDependency {
    constructor(originNode, targetNode, type, isViolation, svgElement, callForAllViews, getDetailedDependencies) {
      super(originNode, targetNode, type, '', '', isViolation);
      this._view = new View(svgElement, this, callForAllViews, () =>
        getDetailedDependencies(this.originNode.getFullName(), this.targetNode.getFullName()));
      this._isVisible = false;
      this.visualData = new VisualData({
        onJumpedToPosition: () => this._view.jumpToPositionAndShowIfVisible(this),
        onMovedToPosition: () => this._view.moveToPositionAndShowIfVisible(this)
      });
    }

    withTypeAndViolation(type, isViolation) {
      this.type = type;
      this.isViolation = isViolation;
      return this;
    }

    hasDetailedDescription() {
      return !(this.originNode.isPackage() || this.targetNode.isPackage());
    }

    jumpToPosition() {
      this.visualData.jumpToPosition(this.originNode.nodeShape.absoluteCircle, this.targetNode.nodeShape.absoluteCircle);
    }

    moveToPosition() {
      return this.visualData.moveToPosition(this.originNode.nodeShape.absoluteCircle, this.targetNode.nodeShape.absoluteCircle);
    }

    hide() {
      this._isVisible = false;
      this._view.hide();
    }

    isVisible() {
      return this._isVisible;
    }

    hideOnStartOverlapping(nodePosition) {
      this._hideOnOverlapping(this.visualData.startPoint, nodePosition);
    }

    hideOnTargetOverlapping(nodePosition) {
      this._hideOnOverlapping(this.visualData.endPoint, nodePosition);
    }

    _hideOnOverlapping(point, nodePosition) {
      if (point.isWithinCircle(nodePosition, nodePosition.r + OVERLAP_DELTA)) {
        this.hide();
      }
    }

    getProperties() {
      return joinStrings(' ', 'dependency', this.isViolation ? 'violation' : '', this.type);
    }

    toString() {
      return `${this.originNode.getFullName()}-${this.targetNode.getFullName()}`;
    }
  };

  const getSingleStyledDependencyType = (dependencies, styledDependencyTypes, mixedStyle) => {
    const currentDependencyTypes = new Set(dependencies.map(d => d.type));
    const currentStyledDependencyTypes = [...currentDependencyTypes].filter(t => styledDependencyTypes.has(t));
    if (currentStyledDependencyTypes.length === 0) {
      return '';
    } else if (currentStyledDependencyTypes.length === 1) {
      return currentStyledDependencyTypes[0];
    } else {
      return mixedStyle;
    }
  };

  const getUniqueDependency = (originNode, targetNode, svgElement, callForAllViews, getDetailedDependencies) => ({
    byGroupingDependencies: dependencies => {
      if (originNode.isPackage() || targetNode.isPackage()) {
        return getOrCreateUniqueDependency(originNode, targetNode, '',
          dependencies.some(d => d.isViolation), svgElement, callForAllViews, getDetailedDependencies);
      } else {
        const colorType = getSingleStyledDependencyType(dependencies, coloredDependencyTypes, 'severalColors');
        const dashedType = getSingleStyledDependencyType(dependencies, dashedDependencyTypes, 'severalDashed');

        return getOrCreateUniqueDependency(originNode, targetNode, joinStrings(' ', colorType, dashedType),
          dependencies.some(d => d.isViolation), svgElement, callForAllViews, getDetailedDependencies);
      }
    }
  });

  // FIXME: Why do we have to handle the identity case? In any case this should be moved into Dependency, if it makes sense...
  //        Also: Why does !from.isPackage && !to.isPackage && !isIdentity imply that the dependency type is 'inner class'? -> needs to be clearer
  const shiftElementaryDependency = (dependency, newOriginNode, newTargetNode) => {
    if (newOriginNode.isPackage() || newTargetNode.isPackage()) {
      return new ElementaryDependency(newOriginNode, newTargetNode, '', '', dependency.isViolation);
    }
    if (newOriginNode.getFullName() === dependency.originNode.getFullName()
      && newTargetNode.getFullName() === dependency.targetNode.getFullName()) {

      return dependency;
    }
    return new ElementaryDependency(newOriginNode, newTargetNode, ownDependencyTypes.INNERCLASS_DEPENDENCY, '', dependency.isViolation);
  };

  const createElementaryDependency = ({originNode, targetNode, type, description}) =>
    new ElementaryDependency(originNode, targetNode, type, description);

  return {
    createElementaryDependency,
    getUniqueDependency,
    shiftElementaryDependency,
    getOwnDependencyTypes: () => [...Object.values(ownDependencyTypes)]
  };
};

module.exports = init;