'use strict';

const dependencyTypes = require('./dependency-types.json');

const vectors = require('./vectors.js').vectors;
const Vector = require('./vectors.js').Vector;
const OVERLAP_DELTA = 0.1;

const init = (View, nodeMap) => {

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

  const mergeTypeNames = (ownTypeName, otherTypeName) => {
    if (otherTypeName) {
      return otherTypeName === ownTypeName ? otherTypeName : 'several';
    } else {
      return ownTypeName;
    }
  };

  const SingleDependencyDescription = class {
    constructor(typeName) {
      this.typeName = typeName;
    }

    getDependencyTypeNamesAsString() {
      return this.typeName;
    }

    mergeAccessTypeWithOtherAccessType(accessTypeName) {
      return accessTypeName;
    }

    mergeInheritanceTypeWithOtherInheritanceType(inheritanceTypeName) {
      return inheritanceTypeName;
    }

    toString() {
      return this.typeName;
    }
  };

  const AccessDescription = class extends SingleDependencyDescription {
    constructor(typeName, startCodeUnit, targetElement) {
      super(typeName);
      this.startCodeUnit = startCodeUnit;
      this.targetElement = targetElement;
    }

    hasDetailedDescription() {
      return true;
    }

    hasTitle() {
      return true;
    }

    mergeAccessTypeWithOtherAccessType(accessTypeName) {
      return mergeTypeNames(this.typeName, accessTypeName);
    }

    toString() {
      return `${this.startCodeUnit} ${this.typeName} ${this.targetElement}`;
    }
  };

  const InheritanceDescription = class extends SingleDependencyDescription {
    constructor(typeName) {
      super(typeName);
    }

    hasDetailedDescription() {
      return false;
    }

    hasTitle() {
      return false;
    }

    mergeInheritanceTypeWithOtherInheritanceType(inheritanceTypeName) {
      return mergeTypeNames(this.typeName, inheritanceTypeName);
    }
  };

  const ChildAccessDescription = class extends SingleDependencyDescription {
    constructor(hasDetailedDescription) {
      super('childrenAccess');
      this._hasDetailedDescription = hasDetailedDescription;
    }

    hasDetailedDescription() {
      return this._hasDetailedDescription;
    }

    mergeAccessTypeWithOtherAccessType(accessTypeName) {
      return mergeTypeNames(this.typeName, accessTypeName);
    }
  };

  const EmptyDependencyDescription = class extends SingleDependencyDescription {
    hasDetailedDescription() {
      return false;
    }

    getDependencyTypeNamesAsString() {
      return '';
    }

    toString() {
      return this.getDependencyTypeNamesAsString();
    }
  };

  const GroupedDependencyDescription = class {
    constructor(hasDetailedDescription = false, accessTypeName = '', inheritanceTypeName = '') {
      this.accessTypeName = accessTypeName;
      this.inheritanceTypeName = inheritanceTypeName;
      this._hasDetailedDescription = hasDetailedDescription;
    }

    hasDetailedDescription() {
      return this._hasDetailedDescription;
    }

    getDependencyTypeNamesAsString() {
      const separator = this.inheritanceTypeName && this.accessTypeName ? ' ' : '';
      return this.inheritanceTypeName + separator + this.accessTypeName;
    }

    toString() {
      return this.getDependencyTypeNamesAsString();
    }

    addDependencyDescription(dependencyDescription) {
      this.accessTypeName = dependencyDescription.mergeAccessTypeWithOtherAccessType(this.accessTypeName);
      this.inheritanceTypeName = dependencyDescription.mergeInheritanceTypeWithOtherInheritanceType(this.inheritanceTypeName);
      this._hasDetailedDescription = this._hasDetailedDescription || dependencyDescription.hasDetailedDescription();
    }
  };

  const getOrCreateUniqueDependency = (from, to, description, svgElement, callForAllViews, getDetailedDependencies) => {
    if (!allDependencies.has(`${from}-${to}`)) {
      allDependencies.set(`${from}-${to}`, new GroupedDependency(from, to, description, svgElement, callForAllViews, getDetailedDependencies));
    }
    return allDependencies.get(`${from}-${to}`).withDescription(description)
  };

  const createDependencyDescription = (type, startCodeUnit, targetElement) => {
    if (dependencyTypes.groupedDependencies.access.types.filter(accessType => accessType.dependency === type).length > 0) {
      return new AccessDescription(type, startCodeUnit, targetElement);
    }
    else if (dependencyTypes.groupedDependencies.inheritance.types.filter(inheritanceType => inheritanceType.dependency === type).length > 0) {
      return new InheritanceDescription(type);
    }
  };

  const combinePathAndCodeUnit = (path, codeUnit) => (path || '') + ((path && codeUnit) ? '.' : '') + (codeUnit || '');

  const ElementaryDependency = class {
    constructor(from, to, description) {
      this.from = from;
      this.to = to;
      this.description = description;
    }

    getStartNode() {
      return nodes.getByName(this.from);
    }

    getEndNode() {
      return nodes.getByName(this.to);
    }

    toShortStringRelativeToPredecessors(from, to) {
      const start = combinePathAndCodeUnit(this.from.substring(from.length + 1), this.description.startCodeUnit);
      const end = combinePathAndCodeUnit(this.to.substring(to.length + 1), this.description.targetElement);
      return `${start}->${end}`;
    }

    getTypeNames() {
      return `dependency ${this.description.getDependencyTypeNamesAsString()}`;
    }

    toString() {
      return `${this.from}->${this.to}(${this.description.toString()})`;
    }
  };

  const GroupedDependency = class extends ElementaryDependency {
    constructor(from, to, description, svgElement, callForAllViews, getDetailedDependencies) {
      super(from, to, description);
      this._view = new View(svgElement, this, callForAllViews, () => getDetailedDependencies(this.from, this.to));
      this._isVisible = false;
      this.visualData = new VisualData({
        onJumpedToPosition: () => this._view.jumpToPositionAndShowIfVisible(this),
        onMovedToPosition: () => this._view.moveToPositionAndShowIfVisible(this)
      });
    }

    withDescription(description) {
      this.description = description;
      return this;
    }

    hasDetailedDescription() {
      return !containsPackage(this.from, this.to) && this.description.hasDetailedDescription();
    }

    jumpToPosition() {
      this.visualData.jumpToPosition(this.getStartNode().circleData.absoluteCircle, this.getEndNode().circleData.absoluteCircle);
    }

    moveToPosition() {
      return this.visualData.moveToPosition(this.getStartNode().circleData.absoluteCircle, this.getEndNode().circleData.absoluteCircle);
    }

    hide() {
      this._isVisible = false;
      this._view.hide();
    }

    isVisible() {
      return this._isVisible;
    }

    getIdentifyingString() {
      return `${this.from}-${this.to}`;
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
  };

  const containsPackage = (from, to) => {
    return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
  };

  const createElementaryDependency = (from, to) => ({
    withDependencyDescription: (type, startCodeUnit = null, targetElement = null) => {
      return new ElementaryDependency(from, to, createDependencyDescription(type, startCodeUnit, targetElement));
    }
  });

  const getUniqueDependency = (from, to, svgElement, callForAllViews, getDetailedDependencies) => ({
    byGroupingDependencies: (dependencies) => {
      if (containsPackage(from, to)) {
        return getOrCreateUniqueDependency(from, to, new EmptyDependencyDescription(), svgElement, callForAllViews, getDetailedDependencies);
      }
      else {
        const description = new GroupedDependencyDescription();
        dependencies.forEach(d => description.addDependencyDescription(d.description));
        return getOrCreateUniqueDependency(from, to, description, svgElement, callForAllViews, getDetailedDependencies);
      }
    }
  });

  const shiftElementaryDependency = (dependency, newFrom, newTo) => {
    if (containsPackage(newFrom, newTo)) {
      return new ElementaryDependency(newFrom, newTo, new EmptyDependencyDescription());
    }
    if (newFrom === dependency.from && newTo === dependency.to) {
      return dependency;
    }
    return new ElementaryDependency(newFrom, newTo, new ChildAccessDescription(dependency.description.hasDetailedDescription()));
  };

  return {
    createElementaryDependency: createElementaryDependency,
    getUniqueDependency: getUniqueDependency,
    shiftElementaryDependency: shiftElementaryDependency
  };
};

module.exports.init = (View, nodeMap) => init(View, nodeMap);