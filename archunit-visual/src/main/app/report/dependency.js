'use strict';

const dependencyKinds = require('./dependency-kinds.json');

let nodes;

const DependencyDescription = class {
  constructor() {
  }
};

const SingleDependencyDescription = class extends DependencyDescription {
  constructor(kind) {
    super();
    this.type = kind;
  }

  getDependencyTypesAsString() {
    return this.type;
  }
};

const AccessDescription = class extends SingleDependencyDescription {
  constructor(kind) {
    super(kind);
    this.startCodeUnit = "";
    this.targetElement = "";
  }

  hasDescription() {
    return true;
  }

  getInheritanceKind() {
    return "";
  }

  getAccessKind() {
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
  constructor(kind) {
    super(kind);
  }

  hasDescription() {
    return false;
  }

  getInheritanceKind() {
    return this.type;
  }

  getAccessKind() {
    return "";
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
    this.inheritanceKind = "";
    this.accessKind = "";
  }

  hasDescription() {
    return true;
  }

  getInheritanceKind() {
    return this.inheritanceKind;
  }

  getAccessKind() {
    return this.accessKind;
  }

  getDependencyTypesAsString() {
    return this.inheritanceKind + (this.inheritanceKind && this.accessKind ? " " : "") + this.accessKind;
  }

  toString() {
    return this.getDependencyTypesAsString();
  }
};

const createDependencyDescription = (dependencyGroup, kind) => {
  if (dependencyGroup === dependencyKinds.groupedDependencies.access.name) {
    return new AccessDescription(kind);
  }
  else if (dependencyGroup === dependencyKinds.groupedDependencies.inheritance.name) {
    return new InheritanceDescription(kind);
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

const groupKindsOfDifferentDepsBetweenSameElements = (kind1, kind2) => {
  if (!kind1) {
    return kind2;
  }
  else if (!kind2) {
    return kind1;
  }
  else {
    return kind1 === kind2 ? kind1 : "several";
  }
};

const containsPackage = (from, to) => {
  return nodes.getByName(from).isPackage() || nodes.getByName(to).isPackage();
};

const buildDependency = (from, to) => {
  const dependency = new Dependency(from, to);
  const builder = {
    withNewDescription: function () {
      const descriptionBuilder = {
        withKind: function (kindgroup, kind) {
          dependency.description = createDependencyDescription(kindgroup, kind);
          return descriptionBuilder;
        },
        withStartCodeUnit: function (startCodeUnit) {
          dependency.description.startCodeUnit = startCodeUnit;
          return descriptionBuilder;
        },
        withTargetElement: function (targetElement) {
          dependency.description.targetElement = targetElement;
          return descriptionBuilder;
        },
        build: function () {
          return dependency;
        }
      };
      return descriptionBuilder;
    },
    withMergedDescriptions: function (description1, description2) {
      dependency.description = new GroupedDependencyDescription();
      dependency.description.inheritanceKind = groupKindsOfDifferentDepsBetweenSameElements(description1.getInheritanceKind(), description2.getInheritanceKind());
      dependency.description.accessKind = groupKindsOfDifferentDepsBetweenSameElements(description1.getAccessKind(), description2.getAccessKind());
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
              dependency.description.accessKind = "childrenAccess";
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
              dependency.description.accessKind = "childrenAccess";
            }
          }
          else {
            dependency.description = new GroupedDependencyDescription();
          }
          return dependency;
        }
      }
    },
    build: function () {
      return dependency;
    }
  };
  return builder;
};

module.exports.buildDependency = nodeMap => {
  nodes = nodeMap;
  return buildDependency;
};