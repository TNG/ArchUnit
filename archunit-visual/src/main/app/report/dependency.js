'use strict';

import {Vector, vectors} from './vectors';

const OVERLAP_DELTA = 0.1;

const coloredDependencyTypes = new Set();
const dashedDependencyTypes = new Set();

const init = (View, nodeMap) => {

  const ownDependencyTypes = {
    INNERCLASS_DEPENDENCY: 'INNERCLASS_DEPENDENCY'
  };

  const nodes = nodeMap;
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

  const getOrCreateUniqueDependency = (type, from, to, isViolation, svgElement, callForAllViews, getDetailedDependencies) => {
    if (!allDependencies.has(`${from}-${to}`)) {
      allDependencies.set(`${from}-${to}`, new GroupedDependency(type, from, to, isViolation, svgElement, callForAllViews, getDetailedDependencies));
    }
    return allDependencies.get(`${from}-${to}`).withTypeAndViolation(type, isViolation)
  };

  const ElementaryDependency = class {
    //TODO: change parameter order
    constructor(type, description, from, to, isViolation = false) {
      this.type = type;
      this.description = description;
      this.from = from;
      this.to = to;
      this.isViolation = isViolation;
      this._matchesFilter = new Map();
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

    getStartNode() {
      return nodes.getByName(this.from);
    }

    getEndNode() {
      return nodes.getByName(this.to);
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
    constructor(type, from, to, isViolation, svgElement, callForAllViews, getDetailedDependencies) {
      super(type, '', from, to, '', isViolation);
      this._view = new View(svgElement, this, callForAllViews, () => getDetailedDependencies(this.from, this.to));
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
      return !containsPackage(this.from, this.to);
    }

    jumpToPosition() {
      this.visualData.jumpToPosition(this.getStartNode().nodeCircle.absoluteCircle, this.getEndNode().nodeCircle.absoluteCircle);
    }

    moveToPosition() {
      return this.visualData.moveToPosition(this.getStartNode().nodeCircle.absoluteCircle, this.getEndNode().nodeCircle.absoluteCircle);
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
      return `${this.from}-${this.to}`;
    }
  };

  const containsPackage = (from, to) => {
    return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
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

  const getUniqueDependency = (from, to, svgElement, callForAllViews, getDetailedDependencies) => ({
    byGroupingDependencies: dependencies => {
      if (containsPackage(from, to)) {
        return getOrCreateUniqueDependency('', from, to,
          dependencies.some(d => d.isViolation), svgElement, callForAllViews, getDetailedDependencies);
      } else {
        const colorType = getSingleStyledDependencyType(dependencies, coloredDependencyTypes, 'severalColors');
        const dashedType = getSingleStyledDependencyType(dependencies, dashedDependencyTypes, 'severalDashed');

        return getOrCreateUniqueDependency(joinStrings(' ', colorType, dashedType), from, to,
          dependencies.some(d => d.isViolation), svgElement, callForAllViews, getDetailedDependencies);
      }
    }
  });

  const shiftElementaryDependency = (dependency, newFrom, newTo) => {
    if (containsPackage(newFrom, newTo)) {
      return new ElementaryDependency('', '', newFrom, newTo, dependency.isViolation);
    }
    if (newFrom === dependency.from && newTo === dependency.to) {
      return dependency;
    }
    return new ElementaryDependency(ownDependencyTypes.INNERCLASS_DEPENDENCY, '', newFrom, newTo, dependency.isViolation);
  };

  const createElementaryDependency = jsonDependency =>
    new ElementaryDependency(jsonDependency.type, jsonDependency.description,
      jsonDependency.originClass, jsonDependency.targetClass);

  return {
    createElementaryDependency,
    getUniqueDependency: getUniqueDependency,
    shiftElementaryDependency: shiftElementaryDependency,
    getOwnDependencyTypes: () => [...Object.values(ownDependencyTypes)]
  };
};

export default init;