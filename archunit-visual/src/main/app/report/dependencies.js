'use strict';

const dependencyGroups = require('./dependency-kinds.json');

let nodes = new Map();

let isWithin = (value, lowerLimit, upperLimit) => {
  return (value >= lowerLimit && value <= upperLimit);
};

let normalizeAngle = angleRad => {
  while (angleRad > Math.PI) {
    angleRad -= 2 * Math.PI;
  }
  while (angleRad < -Math.PI) {
    angleRad += 2 * Math.PI;
  }
  return angleRad;
};

let getTitleOffset = (angleRad, textPadding) => {
  return [Math.round(textPadding * Math.sin(angleRad)),
    Math.round(textPadding * Math.cos(angleRad))];
};

let getDescriptionRelativeToPredecessors = (dep, from, to) => {
  let start = dep.from.substring(from.length + 1);
  start += ((start && dep.description.startCodeUnit.title) ? "." : "") + (dep.description.startCodeUnit.title || "");
  let end = dep.to.substring(to.length + 1);
  end += ((end && dep.description.targetElement.title) ? "." : "") + (dep.description.targetElement.title || "");
  return start + "->" + end;
};

let groupCodeElements = (codeElement1, codeElement2) => {
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
      if (!containsPackage(from, to)) {
        dependency.description.inheritanceKind = groupKindsOfDifferentDepsBetweenSameClasses(description1.inheritanceKind, description2.inheritanceKind); // description1.inheritanceKind || description2.inheritanceKind;
        dependency.description.accessKind = groupKindsOfDifferentDepsBetweenSameClasses(description1.accessKind, description2.accessKind);
        dependency.description.startCodeUnit = groupCodeElements(description1.startCodeUnit, description2.startCodeUnit);
        dependency.description.targetElement = groupCodeElements(description1.targetElement, description2.targetElement);
      }
      return dependency;
    },
    withExistingDescription: function (description) {
      let setKinds = () => {
        dependency.description.inheritanceKind = description.inheritanceKind;
        dependency.description.accessKind = description.accessKind;
      };
      return {
        whenTargetIsFolded: function (foldedElement) {
          if (!containsPackage(from, to)) {
            setKinds();
            dependency.description.startCodeUnit = description.startCodeUnit;
            dependency.description.targetElement = getCodeElementWhenParentFolded(description.targetElement, to, foldedElement);
            console.log(dependency.description.targetElement);
          }
          return dependency;
        },
        whenStartIsFolded: function (foldedElement) {
          if (!containsPackage(from, to)) {
            setKinds();
            dependency.description.startCodeUnit = getCodeElementWhenParentFolded(description.startCodeUnit, from, foldedElement);
            dependency.description.targetElement = description.targetElement;
            console.log(dependency.description.startCodeUnit);
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

/**
 * @param codeElement the startCodeUnit or the targetELement of the dependency
 * @param dependencyEnd the from- or to-element of the dependency
 * @param foldedElement
 * @returns {string} new startCodeUnit respectively targetElement with the classname before,
 * that was hidden by folding the foldedPkg
 */
let getCodeElementWhenParentFolded = (codeElement, dependencyEnd, foldedElement) => {
  return dependencyEnd === foldedElement ? codeElement : CodeElement.single(dependencyEnd.substring(foldedElement.length + 1) + (!CodeElement.isAbsent(codeElement) ? "." + codeElement.title : ""));
};

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
    title: "..."
  },
  isAbsent: function (codeElement) {
    return codeElement.key === CodeElement.absent.key;
  },
  areEqual: function (codeElement1, codeElement2) {
    return codeElement1.key === codeElement2.key &&
        codeElement1.title === codeElement2.title;
  }
};

let DependencyDescription = class {
  constructor() {
    this.inheritanceKind = "";
    this.accessKind = "";
    this.startCodeUnit = CodeElement.absent;
    this.targetElement = CodeElement.absent;
  }

  getAllKinds() {
    return this.inheritanceKind + (this.inheritanceKind && this.accessKind ? " " : "") + this.accessKind;
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
    this.startPoint = [];
    this.endPoint = [];
    this.middlePoint = [];
    this.angleDeg = 0;
    this.angleRad = 0;
    this.lineDiff = 10;
    /**
     * true, if there are two Dependencies (with different directions) between the same two nodes.
     * In this case, the lines must have some space between each other
     * @type {boolean}
     */
    this.mustShareNodes = false;
    this.description = new DependencyDescription();
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

  endNodesAreOverlapping() {
    let startNode = this.getStartNode(), endNode = this.getEndNode();
    let middleDiff = Math.sqrt(Math.pow(endNode.visualData.y - startNode.visualData.y, 2) +
        Math.pow(endNode.visualData.x - startNode.visualData.x, 2));
    return middleDiff < startNode.visualData.r + endNode.visualData.r;
  }

  calcEndCoordinates() {
    let startNode = this.getStartNode(), endNode = this.getEndNode();
    let areOverlapping = this.endNodesAreOverlapping(),
        endNodeIsBigger = endNode.visualData.r >= startNode.visualData.r;

    if (endNode.visualData.x === startNode.visualData.x) {
      this.angleRad = Math.PI / 2;
    }
    else {
      this.angleRad = Math.atan((endNode.visualData.y - startNode.visualData.y)
          / (endNode.visualData.x - startNode.visualData.x));
    }
    this.angleDeg = Math.round(this.angleRad * (180 / Math.PI));

    let startAngle, endAngle;
    let angleDiff = Math.asin(this.lineDiff / startNode.visualData.r);
    if (this.mustShareNodes) {
      startAngle = this.angleRad - angleDiff;
      endAngle = this.angleRad + angleDiff;
      if (areOverlapping) {
        if (endNodeIsBigger) {
          endAngle = -(Math.PI - this.angleRad) - angleDiff;
        }
        else {
          startAngle = -(Math.PI - this.angleRad) + angleDiff;
        }
      }
    }
    else {
      startAngle = this.angleRad;
      endAngle = this.angleRad;
    }
    let startDX = Math.abs(Math.cos(startAngle) * startNode.visualData.r),
        startDY = Math.abs(Math.sin(startAngle) * startNode.visualData.r),
        endDX = Math.abs(Math.cos(endAngle) * endNode.visualData.r),
        endDY = Math.abs(Math.sin(endAngle) * endNode.visualData.r);

    let startdirX, startdirY, enddirX, enddirY;
    if (this.mustShareNodes) {
      let s = Math.sign(endNode.visualData.x - startNode.visualData.x);
      startAngle = normalizeAngle(startAngle);
      endAngle = normalizeAngle(endAngle);
      startdirX = (isWithin(startAngle, -Math.PI / 2, Math.PI / 2) ? 1 : -1) * s;
      startdirY = (isWithin(startAngle, -Math.PI, 0) ? -1 : 1) * s;
      enddirX = (isWithin(endAngle, -Math.PI / 2, Math.PI / 2) ? -1 : 1) * s;
      enddirY = (isWithin(endAngle, -Math.PI, 0) ? 1 : -1) * s;
    }
    else {
      startdirX = Math.sign(endNode.visualData.x - startNode.visualData.x);
      startdirY = Math.sign(endNode.visualData.y - startNode.visualData.y);
      enddirY = -Math.sign(endNode.visualData.y - startNode.visualData.y);
      enddirX = -Math.sign(endNode.visualData.x - startNode.visualData.x);
      if (areOverlapping) {
        if (endNodeIsBigger) {
          enddirX = -enddirX;
          enddirY = -enddirY;
        }
        else {
          startdirX = -startdirX;
          startdirY = -startdirY;
        }
      }
    }

    if (startdirX === 0 && startdirY === 0) {
      startdirX = 1;
      startdirY = 1;
      enddirX = 1;
      enddirY = 1;
    }

    this.startPoint = [startNode.visualData.x + startdirX * startDX, startNode.visualData.y + startdirY * startDY];
    this.endPoint = [endNode.visualData.x + enddirX * endDX, endNode.visualData.y + enddirY * endDY];
    this.middlePoint = [(this.endPoint[0] + this.startPoint[0]) / 2, (this.endPoint[1] + this.startPoint[1]) / 2];
    this.length = Math.round(Math.sqrt(Math.pow(this.endPoint[0] - this.startPoint[0], 2) +
        Math.pow(this.endPoint[1] - this.startPoint[1], 2)));

    this.angleRad = Math.atan((this.endPoint[1] - this.startPoint[1]) / (this.endPoint[0] - this.startPoint[0]));
    this.angleDeg = Math.round(this.angleRad * (180 / Math.PI));
  }

  hasDescription() {
    return !CodeElement.isAbsent(this.description.startCodeUnit) || !CodeElement.isAbsent(this.description.targetElement);
  }

  getDescription() {
    return this.description.startCodeUnit.title + "->" + this.description.targetElement.title;
  }

  getEdgesTitleTranslation(textPadding) {
    let offset = getTitleOffset(this.angleRad, textPadding);
    return "translate(" + (this.middlePoint[0] + offset[0]) + "," + (this.middlePoint[1] - offset[1]) + ") " +
        "rotate(" + this.angleDeg + ")";
  }
};

let filter = dependencies => ({
  by: propertyFunc => ({
    startsWith: prefix => dependencies.filter(r => {
      let property = propertyFunc(r);
      if (property.startsWith(prefix)) {
        let rest = property.substring(prefix.length);
        return rest ? rest.startsWith(".") : true;
      }
      else {
        return false;
      }
    })
  })
});

let containsPackage = (from, to) => {
  return nodes.get(from).projectData.type === "package" || nodes.get(to).projectData.type === "package";
};

let groupKindsOfDifferentDepsBetweenSameClasses = (kind1, kind2) => {
  //FIXME: wahrscheinlich kürzer schreibbar, indem letztes Statement an den Anfang gezogen wird
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

let oldgroupKindOfDepsBetweenSameElements = (dep1, dep2) => {
  let res = {};
  if (dep1.kind === dep2.kind) {
    res.kind = dep1.kind;
    res.inheritanceKind = dep1.inheritanceKind;
    res.accessKind = dep1.accessKind;
  }
  else if (containsPackage(dep1)) {
    res.kind = "several";
    res.accessKind = "";
    res.inheritanceKind = "";
  }
  else {
    res.inheritanceKind = dep1.inheritanceKind || dep2.inheritanceKind;
    res.accessKind = groupKindsOfDifferentDepsBetweenSameClasses(dep1.accessKind, dep2.accessKind);
    res.kind = res.inheritanceKind + (res.inheritanceKind ? " " : "") + res.accessKind;
  }
  return res;
};


let groupEndElements = (element1, element2) => {
  if (!element1) {
    return element2;
  }
  if (!element2) {
    return element1;
  }
  return element1 === element2 ? element1 : "several";
};

let unique = dependencies => {
  let tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
  let map = new Map();
  tmp.forEach(e => {
    if (map.has(e[0])) {
      let i = e[0].indexOf("->");
      let old = map.get(e[0]);
      /*let newKinds = groupKindOfDepsBetweenSameElements(old, e[1]);
       let newstartCodeUnit = groupEndElements(old.startCodeUnit, e[1].startCodeUnit);
       let newtargetElement = groupEndElements(old.targetElement, e[1].targetElement);
       let newDep = buildDependency(e[1].from, e[1].to).kind(dependencyGroups.inheritance.name, newKinds.inheritanceKind).kind(dependencyGroups.access.name, newKinds.accessKind).obskind(newKinds.kind).start(newstartCodeUnit).target(newtargetElement).build();
       map.set(e[0], newDep);*/
      //let newDescription = groupDependencyDescriptions(old.description, e[1].description).between(old.from, old.to);
      let newDep = buildDependency(e[1].from, e[1].to).withMergedDescriptions(old.description, e[1].description);
      map.set(e[0], newDep);
    }
    else map.set(e[0], e[1]);
  });
  return [...map.values()];
};

let transform = dependencies => ({
  where: propertyFunc => ({
    startsWith: prefix => ({
      eliminateSelfDeps: yes => ({
        to: transformer => {
          let matching = filter(dependencies).by(propertyFunc).startsWith(prefix);
          let rest = dependencies.filter(r => !matching.includes(r));
          let folded = unique(matching.map(transformer));
          if (yes) folded = folded.filter(r => r.from !== r.to);
          return [...rest, ...folded];
        }
      })
    })
  })
});


let foldTransformer = pkg => {
  return dependencies => {
    let targetFolded = transform(dependencies).where(r => r.to).startsWith(pkg).eliminateSelfDeps(false)
        .to(r => (
            buildDependency(r.from, pkg).withExistingDescription(r.description).whenTargetIsFolded(pkg))); //.obskind(r.kind).kind(dependencyGroups.inheritance.name, r.inheritanceKind).kind(dependencyGroups.access.name, r.accessKind).start(r.startCodeUnit).target(getCodeElementWhenParentFolded(r.targetElement, r.to, pkg)).build()));
    return transform(targetFolded).where(r => r.from).startsWith(pkg).eliminateSelfDeps(true)
        .to(r => (
            buildDependency(pkg, r.to).withExistingDescription(r.description).whenStartIsFolded(pkg))); //.obskind(r.kind).kind(dependencyGroups.inheritance.name, r.inheritanceKind).kind(dependencyGroups.access.name, r.accessKind).start(getCodeElementWhenParentFolded(r.startCodeUnit, r.from, pkg)).target(r.targetElement).build()));
  }
};

let recalculateVisible = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

let recreateVisible = dependencies => {
  let after = recalculateVisible(dependencies._transformers.values(), dependencies._uniqued);
  dependencies.setVisibleDependencies(after);
  dependencies.calcEndCoordinatesForVisibleDependencies();
};

let changeFold = (dependencies, callback) => {
  callback(dependencies);
  recreateVisible(dependencies);
};

let reapplyFilters = (dependencies, filters) => {
  dependencies._filtered = Array.from(filters.values()).reduce((filtered_deps, filter) => filter(filtered_deps),
      dependencies._all);
  dependencies._uniqued = unique(Array.from(dependencies._filtered));
  recreateVisible(dependencies);
};

let getKindFilter = (implementing, extending, constructorCall, methodCall, fieldAccess, anonImpl) => d => {
  return (d.description.getAllKinds() !== "implements" || implementing)
      && (d.description.getAllKinds() !== "extends" || extending)
      && (d.description.getAllKinds() !== "constructorCall" || constructorCall)
      && (d.description.getAllKinds() !== "methodCall" || methodCall)
      && (d.description.getAllKinds() !== "fieldAccess" || fieldAccess)
      && (d.description.getAllKinds() !== "implementsAnonymous" || anonImpl);
};

let Dependencies = class {
  constructor(all, nodeMap) {
    this._filters = new Map();
    this._transformers = new Map();
    nodes = nodeMap;
    this._all = all;
    this._filtered = this._all;
    this._uniqued = unique(Array.from(this._filtered));
    this.setVisibleDependencies(this._uniqued);
  }

  setVisibleDependencies(deps) {
    this._visibleDependencies = deps;
    this._visibleDependencies.forEach(d => d.mustShareNodes =
        this._visibleDependencies.filter(e => e.from === d.to && e.to === d.from).length > 0);
  }

  calcEndCoordinatesForVisibleDependencies() {
    this._visibleDependencies.forEach(e => e.calcEndCoordinates());
  }

  keyFunction() {
    return e => e.from + "->" + e.to;
  }

  changeFold(pkg, isFolded) {
    if (isFolded) {
      changeFold(this, dependencies => dependencies._transformers.set(pkg, foldTransformer(pkg)));
    }
    else {
      changeFold(this, dependencies => dependencies._transformers.delete(pkg));
    }
  }

  setNodeFilters(filters) {
    this._filters.set("nodefilter", filtered_deps => Array.from(filters.values()).reduce((deps, filter) =>
        deps.filter(d => filter(nodes.get(d.from)) && filter(nodes.get(d.to))), filtered_deps));
    reapplyFilters(this, this._filters);
  }

  // FIXME: Tooooo many parameters
  filterByKind(implementing, extending, constructorCall, methodCall, fieldAccess, anonImpl) {
    let kindFilter = getKindFilter(implementing, extending, constructorCall, methodCall, fieldAccess, anonImpl);
    this._filters.set("kindfilter", filtered_deps => filtered_deps.filter(kindFilter));
    reapplyFilters(this, this._filters);
  }

  resetFilterByKind() {
    this._filters.delete("kindfilter");
    reapplyFilters(this, this._filters);
  }

  recalcEndCoordinatesOf(node) {
    this._visibleDependencies.filter(d => d.from === node || d.to === node).forEach(d => d.calcEndCoordinates());
  }

  getVisible() {
    return this._visibleDependencies;
  }

  getDetailedDeps(from, to) {
    let startMatching = filter(this._filtered).by(d => d.from).startsWith(from);
    let targetMatching = filter(startMatching).by(d => d.to).startsWith(to);
    return targetMatching.map(d => {
      return {
        description: getDescriptionRelativeToPredecessors(d, from, to),
        cssClass: d.getClass()
      }
    });
  }
};

let collectDependencies = () => {
  return {
    ofDependencyGroup: dependencyGroup => {
      return {
        ofJsonElement: function (jsonElement) {
          return {
            inArray: function (arr) {
              dependencyGroup.kinds.forEach(kind => {
                if (jsonElement.hasOwnProperty(kind.name)) {
                  if (kind.isUnique && jsonElement[kind.name] !== "") {
                    arr.push(buildDependency(jsonElement.fullname, jsonElement[kind.name]).withNewDescription().withKind(dependencyGroup.name, kind.dependency).build());
                  }
                  else if (!kind.isUnique && jsonElement[kind.name].length !== 0) {
                    jsonElement[kind.name].forEach(d => arr.push(
                        buildDependency(jsonElement.fullname, d.to || d).withNewDescription().withKind(dependencyGroup.name, kind.dependency).withStartCodeUnit(CodeElement.single(d.startCodeUnit)).withTargetElement(CodeElement.single(d.targetElement)).build()));
                  }
                }
              });
            }
          }
        }
      };
    }
  };
};

let addAllDependencies = () => {
  return {
    ofJsonElement: function (jsonElement) {
      return {
        toArray: function (arr) {
          if (jsonElement.type !== "package") {
            collectDependencies().ofDependencyGroup(dependencyGroups.inheritance).ofJsonElement(jsonElement).inArray(arr);
            collectDependencies().ofDependencyGroup(dependencyGroups.access).ofJsonElement(jsonElement).inArray(arr);
          }

          if (jsonElement.hasOwnProperty("children")) {
            jsonElement.children.forEach(c => addAllDependencies().ofJsonElement(c).toArray(arr));
          }
        }
      }
    }
  };
};

let jsonToDependencies = (jsonRoot, nodeMap) => {
  let arr = [];
  addAllDependencies().ofJsonElement(jsonRoot).toArray(arr);
  return new Dependencies(arr, nodeMap);
};

// FIXME: > 400 lines is too long, to be understandable
module.exports.jsonToDependencies = jsonToDependencies;