'use strict';

const dependencyKinds = require('./dependency-kinds.json');

let nodes;

const CodeElement = {
  single: function (name) {
    return {
      key: 1,
      title: name
    }
  }
};

const DependencyDescription = class {
  constructor() {
    this.inheritanceKind = "";
    this.accessKind = "";
  }

  getAllKinds() {
    return this.inheritanceKind + (this.inheritanceKind && this.accessKind ? " " : "") + this.accessKind;
  }
};

const SingleDependencyDescription = class extends DependencyDescription {
  constructor() {
    super();
  }
};

const AccessDescription = class extends SingleDependencyDescription {
  constructor() {
    super();
  }

  hasDescription() {
    return true;
  }

  toString() {
    const allKinds = this.getAllKinds();
    return this.startCodeUnit.title + (this.startCodeUnit.title && allKinds ? " " : "") + allKinds + (this.targetElement.title && allKinds ? " " : "") + this.targetElement.title;
  }
};

const InheritanceDescription = class extends SingleDependencyDescription {
  constructor() {
    super();
  }

  hasDescription() {
    return false;
  }

  toString() {
    return this.getAllKinds();
  }
};

const GroupedDependencyDescription = class extends DependencyDescription {
  constructor() {
    super();
  }

  hasDescription() {
    return true;
  }

  toString() {
    return this.getAllKinds();
  }
};

const createDependencyDescription = (dependencyGroup) => {
  if (dependencyGroup === dependencyKinds.groupedDependencies.access.name) {
    return new AccessDescription();
  }
  else if (dependencyGroup === dependencyKinds.groupedDependencies.inheritance.name) {
    return new InheritanceDescription();
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
    return "access " + this.description.getAllKinds();
  }

  getDescriptionRelativeToPredecessors(from, to) {
    let start = this.from.substring(from.length + 1);
    start += ((start && this.description.startCodeUnit) ? "." : "") + (this.description.startCodeUnit.title);
    let end = this.to.substring(to.length + 1);
    end += ((end && this.description.targetElement) ? "." : "") + (this.description.targetElement.title);
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
          dependency.description = createDependencyDescription(kindgroup);
          dependency.description[kindgroup] = kind;
          return descriptionBuilder;
        },
        withStartCodeUnit: function (startCodeUnit) {
          dependency.description.startCodeUnit = CodeElement.single(startCodeUnit);
          return descriptionBuilder;
        },
        withTargetElement: function (targetElement) {
          dependency.description.targetElement = CodeElement.single(targetElement);
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
      dependency.description.inheritanceKind = groupKindsOfDifferentDepsBetweenSameElements(description1.inheritanceKind, description2.inheritanceKind);
      dependency.description.accessKind = groupKindsOfDifferentDepsBetweenSameElements(description1.accessKind, description2.accessKind);
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