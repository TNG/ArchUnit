'use strict';

const dependencyKinds = require('./dependency-kinds.json');

let nodes;

const CodeElement = {
  absent: {
    key: 0,
    title: ""
  },
  single: function (name) {
    if (name) {
      return {
        key: 1,
        title: name
      }
    }
    else {
      return CodeElement.absent;
    }
  },
  several: {
    key: 2,
    title: "[...]"
  },
  groupExistingCodeElements: (codeElement1, codeElement2) => {
    if (CodeElement.areEqual(codeElement1, codeElement2)) {
      return codeElement1;
    }
    else if (CodeElement.isAbsent(codeElement1)) {
      return codeElement2;
    }
    else if (CodeElement.isAbsent(codeElement2)) {
      return codeElement1;
    }
    else {
      return CodeElement.several;
    }
  },
  isAbsent: codeElement => {
    return codeElement.key === CodeElement.absent.key;
  },
  areEqual: (codeElement1, codeElement2) => {
    return codeElement1.key === codeElement2.key &&
        codeElement1.title === codeElement2.title;
  }
};

const DependencyDescription = class {
  constructor(containsPkg) {
    this.containsPkg = containsPkg;
    this.inheritanceKind = "";
    this.accessKind = "";
    this.startCodeUnit = CodeElement.absent;
    this.targetElement = CodeElement.absent;
  }

  getAllKinds() {
    return this.inheritanceKind + (this.inheritanceKind && this.accessKind ? " " : "") + this.accessKind;
  }

  hasDetailedDescription() {
    return !this.containsPkg && this.hasDescription();
  }

  hasDescription() {
    return !CodeElement.isAbsent(this.startCodeUnit) || !CodeElement.isAbsent(this.targetElement);
  }

  toString() {
    const allKinds = this.getAllKinds();
    return this.startCodeUnit.title + (this.startCodeUnit.title && allKinds ? " " : "") + allKinds + (this.targetElement.title && allKinds ? " " : "") + this.targetElement.title;
  }
};

const AccessDescription = class extends DependencyDescription {
  constructor(containsPkg) {
    super(containsPkg);
  }
};

const InheritanceDescription = class extends DependencyDescription {
  constructor(containsPkg) {
    super(containsPkg);
  }
};

const createDependencyDescription = (dependencyGroup, containsPkg) => {
  if (dependencyGroup === dependencyKinds.groupedDependencies.access.name) {
    return new AccessDescription(containsPkg);
  }
  else if (dependencyGroup === dependencyKinds.groupedDependencies.inheritance.name) {
    return new InheritanceDescription(containsPkg);
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
    this.description = new DependencyDescription(containsPackage(this.from, this.to));
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
    start += ((start && !CodeElement.isAbsent(this.description.startCodeUnit)) ? "." : "") + (this.description.startCodeUnit.title);
    let end = this.to.substring(to.length + 1);
    end += ((end && !CodeElement.isAbsent(this.description.targetElement)) ? "." : "") + (this.description.targetElement.title);
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

const getCodeElementWhenParentFolded = (description, codeElement, dependencyEnd, foldedElement) => {
  if (description.inheritanceKind) {
    return codeElement;
  }
  return CodeElement.single(dependencyEnd.substring(foldedElement.length + 1) + (!CodeElement.isAbsent(codeElement) ? "." + codeElement.title : ""));
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
          dependency.description = createDependencyDescription(kindgroup, dependency.description.containsPkg);
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
      dependency.description.inheritanceKind = groupKindsOfDifferentDepsBetweenSameElements(description1.inheritanceKind, description2.inheritanceKind);
      dependency.description.accessKind = groupKindsOfDifferentDepsBetweenSameElements(description1.accessKind, description2.accessKind);
      dependency.description.startCodeUnit = CodeElement.groupExistingCodeElements(description1.startCodeUnit, description2.startCodeUnit);
      dependency.description.targetElement = CodeElement.groupExistingCodeElements(description1.targetElement, description2.targetElement);
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
              dependency.description.accessKind = "childrenAccess";
              dependency.description.startCodeUnit = description.startCodeUnit;
              dependency.description.targetElement = getCodeElementWhenParentFolded(description, description.targetElement, targetBeforeFolding, to);
            }
          }
          return dependency;
        },
        whenStartIsFolded: function (startBeforeFolding) {
          if (!containsPackage(from, to)) {
            if (startBeforeFolding === from) {
              dependency.description = description;
            }
            else {
              dependency.description.accessKind = "childrenAccess";
              dependency.description.startCodeUnit = getCodeElementWhenParentFolded(description, description.startCodeUnit, startBeforeFolding, from);
              dependency.description.targetElement = description.targetElement;
            }
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