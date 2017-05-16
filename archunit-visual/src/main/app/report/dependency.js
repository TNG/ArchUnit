'use strict';

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

let DependencyDescription = class {
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
    return this.containsPkg || this.hasDescription();
  }

  hasDescription() {
    return !CodeElement.isAbsent(this.startCodeUnit) || !CodeElement.isAbsent(this.targetElement);
  }

  getDescription() {
    return this.startCodeUnit.title + "->" + this.targetElement.title;
  }

  toString() {
    let allKinds = this.getAllKinds();
    return this.startCodeUnit.title + (this.startCodeUnit.title && allKinds ? " " : "") + allKinds + (this.targetElement.title && allKinds ? " " : "") + this.targetElement.title;
  }
};

let Dependency = class {
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
    return nodes.get(this.from);
  }

  getEndNode() {
    return nodes.get(this.to);
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
  };
};

let groupKindsOfDifferentDepsBetweenSameElements = (kind1, kind2) => {
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

let getCodeElementWhenParentFolded = (codeElement, dependencyEnd, foldedElement) => {
  return CodeElement.single(dependencyEnd.substring(foldedElement.length + 1) + (!CodeElement.isAbsent(codeElement) ? "." + codeElement.title : ""));
};

let containsPackage = (from, to) => {
  return nodes.get(from).projectData.type === "package" || nodes.get(to).projectData.type === "package";
};

let buildDependency = (from, to) => {
  let dependency = new Dependency(from, to);
  let builder = {
    withNewDescription: function () {
      let descriptionBuilder = {
        withKind: function (kindgroup, kind) {
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
      let setKinds = () => {
        dependency.description.inheritanceKind = description.inheritanceKind;
        dependency.description.accessKind = description.accessKind;
      };
      return {
        whenTargetIsFolded: function (targetBeforeFolding) {
          if (!containsPackage(from, to)) {
            if (targetBeforeFolding === to) {
              dependency.description = description;
            }
            else {
              dependency.description.accessKind = "childrenAccess";
              dependency.description.startCodeUnit = description.startCodeUnit;
              dependency.description.targetElement = getCodeElementWhenParentFolded(description.targetElement, targetBeforeFolding, to);
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
              dependency.description.startCodeUnit = getCodeElementWhenParentFolded(description.startCodeUnit, startBeforeFolding, from);
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