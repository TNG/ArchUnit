'use strict';

const dependencyTypes = require('./dependency-types.json');

const vectors = require('./vectors.js').vectors;

const init = (View, DetailedView, nodeMap) => {

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
      return this.inheritanceTypeName + (this.inheritanceTypeName && this.accessTypeName ? ' ' : '') + this.accessTypeName;
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
      /**
       * true, if there are two Dependencies (with different directions) between the same two nodes.
       * In this case, the lines must have some space between each other
       * @type {boolean}
       */
      this.containsPkg = containsPackage(this.from, this.to);
      this.visualData = new VisualData();
    }

    hasDetailedDescription() {
      return !this.containsPkg && this.description.hasDetailedDescription();
    }

    getStartNode() {
      return nodes.getByName(this.from);
    }

    getEndNode() {
      return nodes.getByName(this.to);
    }

    initView(svgElement, svgElementForDetailed, create, callForAllDetailedViews, visualizationStyles) {
      if (!this._view) {
        this._view = new View(svgElement, this);
      }
      else {
        this._view.refresh(this);
      }
      if (!this._detailedView) {
        this._detailedView = new DetailedView(svgElementForDetailed, this, create, callForAllDetailedViews, visualizationStyles);
        this._view.onMouseOver(coords => this._detailedView.fadeIn(coords));
        this._view.onMouseOut(() => this._detailedView.fadeOut());
      }

      this.visualData._updateViewOnJumpedToPosition = () => {
        this._view.jumpToPosition(this);
        this.show();
      };
      this.visualData._updateViewOnMovedToPosition = () => this._view.moveToPosition(this).then(() => this.show());
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
      return this.from + '-' + this.to;
    }

    toString() {
      return this.from + '->' + this.to + '(' + this.description.toString() + ')';
    }

    getClass() {
      return 'dependency ' + this.description.getDependencyTypeNamesAsString();
    }

    toShortStringRelativeToPredecessors(from, to) {
      const start = combinePathAndCodeUnit(this.from.substring(from.length + 1), this.description.startCodeUnit);
      const end = combinePathAndCodeUnit(this.to.substring(to.length + 1), this.description.targetElement);
      return start + '->' + end;
    }
  };

  const containsPackage = (from, to) => {
    return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
  };

  const createElementaryDependency = (from, to) => {
    const dependency = new Dependency(from, to);
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
    const dependency = allDependencies.has(`${from}-${to}`) ? allDependencies.get(`${from}-${to}`) : new Dependency(from, to);
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
    const dependency = new Dependency(from, to);
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

module.exports.init = (View, DetailedView, nodeMap) => init(View, DetailedView, nodeMap);