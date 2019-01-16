'use strict';

const {Vector, vectors} = require('../infrastructure/vectors');

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

    jumpToPosition(absVisualStartNode, absVisualEndNode, absReferencePosition) {
      this._recalculate(absVisualStartNode, absVisualEndNode, absReferencePosition);
      this._listener.onJumpedToPosition();
    }

    moveToPosition(absVisualStartNode, absVisualEndNode, absReferencePosition) {
      this._recalculate(absVisualStartNode, absVisualEndNode, absReferencePosition);
      return this._listener.onMovedToPosition();
    }

    _recalculate(absVisualStartNode, absVisualEndNode, absoluteReferencePosition) {
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

      this.relativeStartPoint = Vector.between(absoluteReferencePosition, this.startPoint);
      this.relativeEndPoint = Vector.between(absoluteReferencePosition, this.endPoint);
    }
  };

  const getOrCreateUniqueDependency = (originNode, targetNode, type, isViolation, svgContainerForDetailedDependencies, callForAllViews, getDetailedDependencies) => {
    const key = `${originNode.getFullName()}-${targetNode.getFullName()}`;
    if (!allDependencies.has(key)) {
      allDependencies.set(key, new GroupedDependency(originNode, targetNode, type, isViolation, svgContainerForDetailedDependencies, callForAllViews, getDetailedDependencies));
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

  const argMax = (arr, firstIsGreaterThanSecond) => arr.reduce((elementWithMaxSoFar, e) => firstIsGreaterThanSecond(e, elementWithMaxSoFar) ? e : elementWithMaxSoFar, arr ? arr[0] : null);

  const GroupedDependency = class extends ElementaryDependency {
    constructor(originNode, targetNode, type, isViolation, svgContainerForDetailedDependencies, callForAllViews, getDetailedDependencies) {
      super(originNode, targetNode, type, '', '', isViolation);
      this._endNodeInForeground = this._calcEndNodeInForeground();
      this._view = new View(svgContainerForDetailedDependencies, this, callForAllViews, () =>
        getDetailedDependencies(this.originNode.getFullName(), this.targetNode.getFullName()));
      this._isVisible = true;
      this.visualData = new VisualData({
        onJumpedToPosition: () => this._view.jumpToPositionAndShowIfVisible(),
        onMovedToPosition: () => this._view.moveToPositionAndShowIfVisible()
      });
    }

    get endNodeInForeground() {
      return this._endNodeInForeground;
    }

    onNodesFocused() {
      this._endNodeInForeground = this._calcEndNodeInForeground();
      this._view.onEndNodeInForegroundChanged();
      this.jumpToPosition();
    }

    withTypeAndViolation(type, isViolation) {
      this.type = type;
      this.isViolation = isViolation;
      return this;
    }

    _calcEndNodeInForeground() {
      return argMax([this._originNode, this._targetNode], (node1, node2) => node1.liesInFrontOf(node2));
    }

    hasDetailedDescription() {
      return !(this.originNode.isPackage() || this.targetNode.isPackage());
    }

    jumpToPosition() {
      this.visualData.jumpToPosition(this.originNode.nodeShape.absoluteCircle, this.targetNode.nodeShape.absoluteCircle, this._endNodeInForeground.nodeShape.absoluteShape.position);
    }

    moveToPosition() {
      return this.visualData.moveToPosition(this.originNode.nodeShape.absoluteCircle, this.targetNode.nodeShape.absoluteCircle, this._endNodeInForeground.nodeShape.absoluteShape.position); //TODO: create getter for position
    }

    hide() {
      this._isVisible = false;
      this._view.refresh();
    }

    show() {
      this._isVisible = true;
      this._view.refresh();
    }

    isVisible() {
      return this._isVisible;
    }

    refresh() {
      if (this.originNode.overlapsWith(this.targetNode)) {
        this.hide();
      } else {
        this.show();
      }
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

  const getUniqueDependency = (originNode, targetNode, svgElementForDetailedDependencies, callForAllViews, getDetailedDependencies) => ({
    byGroupingDependencies: dependencies => {
      if (originNode.isPackage() || targetNode.isPackage()) {
        return getOrCreateUniqueDependency(originNode, targetNode, '',
          dependencies.some(d => d.isViolation), svgElementForDetailedDependencies, callForAllViews, getDetailedDependencies);
      } else {
        const colorType = getSingleStyledDependencyType(dependencies, coloredDependencyTypes, 'severalColors');
        const dashedType = getSingleStyledDependencyType(dependencies, dashedDependencyTypes, 'severalDashed');

        return getOrCreateUniqueDependency(originNode, targetNode, joinStrings(' ', colorType, dashedType),
          dependencies.some(d => d.isViolation), svgElementForDetailedDependencies, callForAllViews, getDetailedDependencies);
      }
    }
  });

  const shiftElementaryDependency = (dependency, newOriginNode, newTargetNode) => {
    return new ElementaryDependency(newOriginNode, newTargetNode, '', '', dependency.isViolation);
  };

  const createElementaryDependency = ({originNode, targetNode, type, description}) =>
    new ElementaryDependency(originNode, targetNode, type, description);

  return {
    createElementaryDependency,
    getUniqueDependency,
    shiftElementaryDependency,
    // FIXME: 'own' dependency types -> default dependency types
    getOwnDependencyTypes: () => [...Object.values(ownDependencyTypes)]
  };
};

module.exports = init;