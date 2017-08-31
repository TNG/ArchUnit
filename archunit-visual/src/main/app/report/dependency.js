'use strict';

const dependencyTypes = require('./dependency-types.json');

let nodes;

const DependencyDescription = class {
  constructor() {
  }
};

const SingleDependencyDescription = class extends DependencyDescription {
  constructor(type) {
    super();
    this.type = type;
  }

  getDependencyTypesAsString() {
    return this.type;
  }

  getInheritanceType() {
    return "";
  }

  getAccessType() {
    return "";
  }
};

const AccessDescription = class extends SingleDependencyDescription {
  constructor(type, startCodeUnit, targetElement) {
    super(type);
    this.startCodeUnit = startCodeUnit;
    this.targetElement = targetElement;
  }

  hasDescription() {
    return true;
  }

  getAccessType() {
    return this.type;
  }

  hasTitle() {
    return true;
  }

  toString() {
    return this.startCodeUnit + " " + this.type + " " + this.targetElement;
  }
};

const InheritanceDescription = class extends SingleDependencyDescription {
  constructor(type) {
    super(type);
  }

  hasDescription() {
    return false;
  }

  getInheritanceType() {
    return this.type;
  }

  hasTitle() {
    return false;
  }

  toString() {
    return this.type;
  }
};

const GroupedDependencyDescription = class extends DependencyDescription {
  constructor() {
    super();
    this.inheritanceType = "";
    this.accessType = "";
  }

  hasDescription() {
    return true;
  }

  getInheritanceType() {
    return this.inheritanceType;
  }

  getAccessType() {
    return this.accessType;
  }

  getDependencyTypesAsString() {
    return this.inheritanceType + (this.inheritanceType && this.accessType ? " " : "") + this.accessType;
  }

  toString() {
    return this.getDependencyTypesAsString();
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
  }

  hasDetailedDescription() {
    return !this.containsPkg && this.description.hasDescription();
  }

  getStartNode() {
    return nodes.getByName(this.from);
  }

  getEndNode() {
    return nodes.getByName(this.to);
  }

  toString() {
    return this.from + "->" + this.to + "(" + this.description.toString() + ")";
  }

  getClass() {
    return "dependency " + this.description.getDependencyTypesAsString();
  }

  getDescriptionRelativeToPredecessors(from, to) {
    let start = this.from.substring(from.length + 1);
    start += ((start && this.description.startCodeUnit) ? "." : "") + (this.description.startCodeUnit);
    let end = this.to.substring(to.length + 1);
    end += ((end && this.description.targetElement) ? "." : "") + (this.description.targetElement);
    return start + "->" + end;
  }
};

const groupTypesOfDifferentDepsBetweenSameElements = (type1, type2) => {
  if (!type1) {
    return type2;
  }
  else if (!type2) {
    return type1;
  }
  else {
    return type1 === type2 ? type1 : "several";
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
    withGroupedDependencyDescription: function (description1, description2) {
      dependency.description = new GroupedDependencyDescription();
      dependency.description.inheritanceType = groupTypesOfDifferentDepsBetweenSameElements(description1.getInheritanceType(), description2.getInheritanceType());
      dependency.description.accessType = groupTypesOfDifferentDepsBetweenSameElements(description1.getAccessType(), description2.getAccessType());
      return dependency;
    },
    withExistingDescription: function (description) {
      return {
        whenTargetIsFolded: function (targetBeforeFolding) {
          if (!containsPackage(from, to)) {
            if (targetBeforeFolding === to) {
              dependency.description = description;
            }
            else {
              dependency.description = new GroupedDependencyDescription();
              dependency.description.accessType = "childrenAccess";
            }
          }
          else {
            dependency.description = new GroupedDependencyDescription();
          }
          return dependency;
        },
        whenStartIsFolded: function (startBeforeFolding) {
          if (!containsPackage(from, to)) {
            if (startBeforeFolding === from) {
              dependency.description = description;
            }
            else {
              dependency.description = new GroupedDependencyDescription();
              dependency.description.accessType = "childrenAccess";
            }
          }
          else {
            dependency.description = new GroupedDependencyDescription();
          }
          return dependency;
        }
      }
    }
  };
};

module.exports.buildDependency = nodeMap => {
  nodes = nodeMap;
  return buildDependency;
};