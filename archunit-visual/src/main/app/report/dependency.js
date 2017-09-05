'use strict';

const dependencyTypes = require('./dependency-types.json');


const vectors = require('./vectors.js').vectors;

const init = (View, nodeMap) => {

  const nodes = nodeMap;

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

    recalc(mustShareNodes, absVisualStartNode, absVisualEndNode) {
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

      if (mustShareNodes) {
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
      return otherTypeName === ownTypeName ? otherTypeName : "several";
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

    toString() {
      return this.startCodeUnit + " " + this.typeName + " " + this.targetElement;
    }

    mergeAccessTypeWithOtherAccessType(accessTypeName) {
      return mergeTypeNames(this.typeName, accessTypeName);
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

    toString() {
      return this.typeName;
    }

    mergeInheritanceTypeWithOtherInheritanceType(inheritanceTypeName) {
      return mergeTypeNames(this.typeName, inheritanceTypeName);
    }
  };

  const GroupedDependencyDescription = class {
    constructor(accessTypeName = "", inheritanceTypeName = "") {
      this.accessTypeName = accessTypeName;
      this.inheritanceTypeName = inheritanceTypeName;
    }

    hasDetailedDescription() {
      return true;
    }

    getDependencyTypeNamesAsString() {
      return this.inheritanceTypeName + (this.inheritanceTypeName && this.accessTypeName ? " " : "") + this.accessTypeName;
    }

    toString() {
      return this.getDependencyTypeNamesAsString();
    }

    addDependencyDescription(dependencyDescription) {
      this.accessTypeName = dependencyDescription.mergeAccessTypeWithOtherAccessType(this.accessTypeName);
      this.inheritanceTypeName = dependencyDescription.mergeInheritanceTypeWithOtherInheritanceType(this.inheritanceTypeName);
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

  const combinePathAndCodeUnit = (path, codeUnit) => (path || "") + ((path && codeUnit) ? "." : "") + (codeUnit || "");

  const Dependency = class {
    constructor(from, to) {
      this.from = from;
      this.to = to;
      /**
       * true, if there are two Dependencies (with different directions) between the same two nodes.
       * In this case, the lines must have some space between each other
       * @type {boolean}
       */
      this.mustShareNodes = false;
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

    updateVisualData() {
      this.visualData.recalc(this.mustShareNodes, this.getStartNode().getAbsoluteVisualData(), this.getEndNode().getAbsoluteVisualData());
    }

    initView(svgElement) {
      this._view = new View(svgElement, this);
    }

    toString() {
      return this.from + "->" + this.to + "(" + this.description.toString() + ")";
    }

    getClass() {
      return "dependency " + this.description.getDependencyTypeNamesAsString();
    }

    toShortStringRelativeToPredecessors(from, to) {
      const start = combinePathAndCodeUnit(this.from.substring(from.length + 1), this.description.startCodeUnit);
      const end = combinePathAndCodeUnit(this.to.substring(to.length + 1), this.description.targetElement);
      return start + "->" + end;
    }
  };

  const containsPackage = (from, to) => {
    return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
  };

  const buildDependency = (from, to) => {
    const dependency = new Dependency(from, to);
    return {
      withSingleDependencyDescription: function (type, startCodeUnit = null, targetElement = null) {
        dependency.description = createDependencyDescription(type, startCodeUnit, targetElement);
        return dependency;
      },
      byGroupingDependencies: function (dependencies) {
        dependency.description = new GroupedDependencyDescription();
        dependencies.forEach(d => dependency.description.addDependencyDescription(d.description));
        return dependency;
      },
      afterFoldingOneNode: function (description, endNodeOfThisDependencyWasFolded) {
        if (containsPackage(from, to)) {
          dependency.description = new GroupedDependencyDescription();
        }
        else if (endNodeOfThisDependencyWasFolded) {
          dependency.description = description;
        }
        else {
          dependency.description = new GroupedDependencyDescription("childrenAccess");
        }
        return dependency;
      }
    };
  };

  return buildDependency;
}

module.exports.init = (View, nodeMap) => ({
  buildDependency: init(View, nodeMap)
});