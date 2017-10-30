'use strict';

const dependencyTypes = require('./dependency-types.json');

const vectors = require('./vectors.js').vectors;

const init = (View, nodeMap) => {

  const nodes = nodeMap;
  //FIXME: maybe store in dependencies instead of here??
  const allDependencies = new Map();

  const oneEndNodeIsCompletelyWithinTheOtherOne = (node1, node2) => {
    const middleDiff = vectors.distance(node1, node2);
    return middleDiff + Math.min(node1.r, node2.r) < Math.max(node1.r, node2.r);
  };

  const VisualData = class {
    constructor() {
      this.startPoint = {};
      this.endPoint = {};
      this.mustShareNodes = false;
      this._updateViewOnJumpedToPosition = () => {
      };
      this._updateViewOnMovedToPosition = () => Promise.resolve();
    }

    jumpToPosition(absVisualStartNode, absVisualEndNode) {
      this.recalc(absVisualStartNode, absVisualEndNode);
      this._updateViewOnJumpedToPosition();
    }

    moveToPosition(absVisualStartNode, absVisualEndNode) {
      this.recalc(absVisualStartNode, absVisualEndNode);
      return this._updateViewOnMovedToPosition();
    }

    // FIXME AU-24: Insufficient tests, I can comment out lines, e.g. in the mustShareNodes case, and everything is still green
    recalc(absVisualStartNode, absVisualEndNode) {
      const lineDiff = 20;
      const oneIsInOther = oneEndNodeIsCompletelyWithinTheOtherOne(absVisualStartNode, absVisualEndNode),
        nodes = [absVisualStartNode, absVisualEndNode].sort((a, b) => a.r - b.r);

      const direction = vectors.vectorOf(absVisualEndNode.x - absVisualStartNode.x,
        absVisualEndNode.y - absVisualStartNode.y);

      let startDirectionVector = vectors.cloneVector(direction);
      if (oneIsInOther && absVisualStartNode === nodes[0]) {
        startDirectionVector = vectors.getRevertedVector(startDirectionVector);
      }
      startDirectionVector = vectors.getDefaultIfNull(startDirectionVector);
      let endDirectionVector = oneIsInOther ? vectors.cloneVector(startDirectionVector) : vectors.getRevertedVector(startDirectionVector);

      if (this.mustShareNodes) {
        let orthogonalVector = vectors.norm(vectors.getOrthogonalVector(startDirectionVector), lineDiff / 2);
        if (oneIsInOther && absVisualStartNode === nodes[1]) {
          orthogonalVector = vectors.getRevertedVector(orthogonalVector);
        }
        startDirectionVector = vectors.norm(startDirectionVector, absVisualStartNode.r);
        endDirectionVector = vectors.norm(endDirectionVector, absVisualEndNode.r);
        startDirectionVector = vectors.addVectors(startDirectionVector, orthogonalVector);
        endDirectionVector = vectors.addVectors(endDirectionVector, orthogonalVector);
      }

      startDirectionVector = vectors.norm(startDirectionVector, absVisualStartNode.r);
      endDirectionVector = vectors.norm(endDirectionVector, absVisualEndNode.r);

      this.startPoint = vectors.vectorOf(absVisualStartNode.x + startDirectionVector.x, absVisualStartNode.y + startDirectionVector.y);
      this.endPoint = vectors.vectorOf(absVisualEndNode.x + endDirectionVector.x, absVisualEndNode.y + endDirectionVector.y);
    }
  };

  const mergeTypeNames = (ownTypeName, otherTypeName) => {
    if (otherTypeName) {
      return otherTypeName === ownTypeName ? otherTypeName : 'several';
    } else {
      return ownTypeName;
    }
  };

  const ElementaryDependencyDescription = class {
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

  const AccessDescription = class extends ElementaryDependencyDescription {
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
  };

  const InheritanceDescription = class extends ElementaryDependencyDescription {
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

    mergeAccessTypeWithOtherAccessType(accessTypeName) {
      return mergeTypeNames(this.accessTypeName, accessTypeName);
    }

    mergeInheritanceTypeWithOtherInheritanceType(inheritanceTypeName) {
      return mergeTypeNames(this.inheritanceTypeName, inheritanceTypeName);
    }
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

  const Dependency = class {
    constructor(from, to) {
      this.from = from;
      this.to = to;
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
  };

  const ElementaryDependency = class extends Dependency {
    constructor(from, to) {
      super(from, to);
    }
  };

  const GroupedDependency = class extends Dependency {
    constructor(from, to) {
      super(from, to);
      this.visualData = new VisualData();
    }

    hasDetailedDescription() {
      return !containsPackage(this.from, this.to) && this.description.hasDetailedDescription();
    }

    initView(svgElement, callForAllViews, getDetailedDependencies) {
      if (!this._view) {
        this._view = new View(svgElement, this, callForAllViews, () => getDetailedDependencies(this.from, this.to));
      }
      else {
        this._view.refresh(this);
      }

      this.visualData._updateViewOnJumpedToPosition = () => {
        this._view.jumpToPosition(this.visualData);
        this.show();
      };
      this.visualData._updateViewOnMovedToPosition = () => this._view.moveToPosition(this.visualData).then(() => this.show());
    }

    jumpToPosition() {
      this.visualData.jumpToPosition(this.getStartNode().getAbsoluteCoords(), this.getEndNode().getAbsoluteCoords());
    }

    moveToPosition() {
      return this.visualData.moveToPosition(this.getStartNode().getAbsoluteCoords(), this.getEndNode().getAbsoluteCoords());
    }

    show() {
      this._view.show(this);
    }

    hide() {
      this._view.hide();
    }

    getIdentifyingString() {
      return `${this.from}-${this.to}`;
    }

    toString() {
      return `${this.from}->${this.to}(${this.description.toString()})`;
    }
  };

  const containsPackage = (from, to) => {
    return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
  };

  const createElementaryDependency = (from, to) => {
    const dependency = new ElementaryDependency(from, to);
    return {
      withDependencyDescription: function (type, startCodeUnit = null, targetElement = null) {
        dependency.description = createDependencyDescription(type, startCodeUnit, targetElement);
        return dependency;
      }
    };
  };

  //TODO: maybe rename into getDependencyGroup and maybe separate classes for such a DependencyGroup and a
  // ElementaryDependency
  const getUniqueDependency = (from, to) => {
    const dependency = allDependencies.has(`${from}-${to}`) ? allDependencies.get(`${from}-${to}`) : new GroupedDependency (from, to);
    allDependencies.set(`${from}-${to}`, dependency);
    return {
      byGroupingDependencies: function (dependencies) {
        const newDescription = new GroupedDependencyDescription(dependencies.length === 1);
        dependencies.forEach(d => newDescription.addDependencyDescription(d.description));
        dependency.description = newDescription;
        return dependency;
      }
    };
  };

  const transformDependency = (from, to) => {
    const dependency = new ElementaryDependency(from, to);
    return {
      afterFoldingOneNode: function (description, endNodeOfThisDependencyWasFolded) {
        if (containsPackage(from, to)) {
          dependency.description = new GroupedDependencyDescription(false);
        }
        else if (endNodeOfThisDependencyWasFolded) {
          dependency.description = description;
        }
        else {
          dependency.description = new GroupedDependencyDescription(description.hasDetailedDescription(), 'childrenAccess');
        }
        return dependency;
      }
    };
  };

  return {
    createElementaryDependency: createElementaryDependency,
    getUniqueDependency: getUniqueDependency,
    transformDependency: transformDependency
  };
};

module.exports.init = (View, nodeMap) => init(View, nodeMap);