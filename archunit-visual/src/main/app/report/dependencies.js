'use strict';

let nodes = new Map();

let isWithin = (value, lowerLimit, upperLimit, trueSign) => {
  return (value >= lowerLimit && value <= upperLimit) ? trueSign : -trueSign;
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

let Dependency = class {
  constructor(from, to, kind, inheritanceKind, accessKind, startCodeUnit, targetElement) {
    this.from = from;
    this.to = to;
    this.kind = kind;
    this.inheritanceKind = inheritanceKind;
    this.accessKind = accessKind;
    this.startCodeUnit = startCodeUnit;
    this.targetElement = targetElement;
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
  }

  getStartNode() {
    return nodes.get(this.from);
  }

  getEndNode() {
    return nodes.get(this.to);
  }

  toString() {
    return this.from + "->" + this.to + "(" + (this.startCodeUnit || "") + " " + this.kind + " " + (this.targetElement || "") + ")";
  }

  getClass() {
    return "access " + this.kind;
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
      startdirX = isWithin(startAngle, -Math.PI / 2, Math.PI / 2, 1) * s;
      startdirY = isWithin(startAngle, -Math.PI, 0, -1) * s;
      enddirX = isWithin(endAngle, -Math.PI / 2, Math.PI / 2, -1) * s;
      enddirY = isWithin(endAngle, -Math.PI, 0, 1) * s;
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
    return this.startCodeUnit || this.targetElement; //TODO: besser && statt || ????
  }

  getDescriptionString() {
    return (this.startCodeUnit || "") + "->" + (this.targetElement || "");
  }

  getTitleOffset(TEXTPADDING) {
    return [Math.round(TEXTPADDING * Math.sin(this.angleRad)),
      Math.round(TEXTPADDING * Math.cos(this.angleRad))];
  }

  getEdgesTitleTranslation(TEXTPADDING) {
    let offset = this.getTitleOffset(TEXTPADDING);
    return "translate(" + (this.middlePoint[0] + offset[0]) + "," + (this.middlePoint[1] - offset[1]) + ") " +
        "rotate(" + this.angleDeg + ")";
  }
};

let filter = dependencies => ({
  by: propertyFunc => ({
    startsWith: prefix => dependencies.filter(r => propertyFunc(r).startsWith(prefix))
  })
});

let containsPackage = dep => {
  return nodes.get(dep.from).projectData.type === "package" || nodes.get(dep.to).projectData.type === "package";
};

let groupAccessKindOfDifferentDepsBetweenSameClasses = (accessKind1, accessKind2) => {
  if (!accessKind1) {
    return accessKind2;
  }
  else if (!accessKind2) {
    return accessKind1;
  }
  else {
    return accessKind1 === accessKind2 ? accessKind1 : "several";
  }
};

let groupKindOfDepsBetweenSameElements = (dep1, dep2) => {
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
    res.inheritanceKind = dep1.inheritanceKind;
    res.accessKind = groupAccessKindOfDifferentDepsBetweenSameClasses(dep1.accessKind, dep2.accessKind);
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
  let m = new Map();
  tmp.forEach(e => {
    if (m.has(e[0])) {
      let i = e[0].indexOf("->");
      let old = m.get(e[0]);
      let newKinds = groupKindOfDepsBetweenSameElements(old, e[1]);
      let newstartCodeUnit = groupEndElements(old.startCodeUnit, e[1].startCodeUnit);
      let newtargetElement = groupEndElements(old.targetElement, e[1].targetElement);
      let newDep = new Dependency(e[1].from, e[1].to, newKinds.kind, newKinds.inheritanceKind, newKinds.accessKind,
          newstartCodeUnit, newtargetElement);
      m.set(e[0], newDep);
    }
    else m.set(e[0], e[1]);
  });
  return [...m.values()];
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

/**
 *
 * @param element the startCodeUnit or the targetELement of the dependency
 * @param base the from- or to-element of the dependency
 * @param foldedPkg
 * @returns {string} new startCodeUnit respectively targetElement with the classname before,
 * that was hidden by folding the foldedPkg
 */
let getExtendedStartTargetName = (element, base, foldedPkg) => {
  return base !== foldedPkg ? (base.substring(foldedPkg.length + 1) + (element ? "." + element : "")) : element;
};

let foldTransformer = pkg => {
  return dependencies => {
    let targetFolded = transform(dependencies).where(r => r.to).startsWith(pkg).eliminateSelfDeps(false)
        .to(r => (new Dependency(r.from, pkg, r.kind, r.inheritanceKind, r.accessKind, r.startCodeUnit,
            getExtendedStartTargetName(r.targetElement, r.to, pkg))));
    return transform(targetFolded).where(r => r.from).startsWith(pkg).eliminateSelfDeps(true)
        .to(r => (new Dependency(pkg, r.to, r.kind, r.inheritanceKind, r.accessKind,
            getExtendedStartTargetName(r.startCodeUnit, r.from, pkg), r.targetElement)));
  }
};

let recalculateVisible = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

let recreateVisible = dependencies => {
  let after = recalculateVisible(dependencies._transformers.values(), dependencies._uniqued);
  dependencies.setVisibleDependencies(after);
  dependencies.calcEndCoordinatesForVisibleDependencies();
};

let changeFold = (dependencies, type, callback) => {
  callback(dependencies);
  recreateVisible(dependencies);
};

let setForAllMustShareNodes = visDeps => {
  visDeps.forEach(d => d.mustShareNodes =
      visDeps.filter(e => e.from === d.to && e.to === d.from).length > 0);
};

let reapplyFilters = (dependencies, filters) => {
  dependencies._filtered = Array.from(filters.values()).reduce((filtered_deps, filter) => filter(filtered_deps),
      dependencies._all);
  dependencies._uniqued = unique(Array.from(dependencies._filtered));
  recreateVisible(dependencies);
};

let getKindFilter = (implementing, extending, constructorCall, methodCall, fieldAccess, anonImpl) => d => {
  return (d.kind !== "implements" || implementing)
      && (d.kind !== "extends" || extending)
      && (d.kind !== "constructorCall" || constructorCall)
      && (d.kind !== "methodCall" || methodCall)
      && (d.kind !== "fieldAccess" || fieldAccess)
      && (d.kind !== "implementsAnonymous" || anonImpl);
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
    setForAllMustShareNodes(this._visibleDependencies);
  }

  calcEndCoordinatesForVisibleDependencies() {
    this._visibleDependencies.forEach(e => e.calcEndCoordinates());
  }

  /**
   * identifies the given dependency
   * @param d
   */
  keyFunction() {
    return e => e.from + "->" + e.to;
  }

  changeFold(pkg, isFolded) {
    if (isFolded) changeFold(this, 'fold', dependencies => dependencies._transformers.set(pkg, foldTransformer(pkg)));
    else changeFold(this, 'unfold', dependencies => dependencies._transformers.delete(pkg));
  }

  setNodeFilters(filters) {
    this._filters.set("nodefilter", filtered_deps => Array.from(filters.values()).reduce((deps, filter) =>
        deps.filter(d => filter(nodes.get(d.from)) && filter(nodes.get(d.to))), filtered_deps));
    reapplyFilters(this, this._filters);
  }

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
};

let addDeps = (arr, jsonEl, dep, kind, inheritanceKind, accessKind) => {
  if (jsonEl.hasOwnProperty(dep) && jsonEl[dep].length !== 0) {
    jsonEl[dep].forEach(i => arr.push(
        new Dependency(jsonEl.fullname, i.to || i, kind, inheritanceKind, accessKind, i.startCodeUnit, i.targetElement)));
  }
};

let addSubTreeDeps = (arr, jsonEl) => {
  if (jsonEl.type !== "package") {
    let from = jsonEl.fullname;
    //add super class, if there is one
    if (jsonEl.hasOwnProperty("superclass") && jsonEl.superclass !== "") {
      arr.push(new Dependency(from, jsonEl.superclass, "extends", "extends", ""));
    }
    addDeps(arr, jsonEl, "interfaces", "implements", "implements", "");
    addDeps(arr, jsonEl, "methodCalls", "methodCall", "", "methodCall");
    addDeps(arr, jsonEl, "fieldAccesses", "fieldAccess", "", "fieldAccess");
    addDeps(arr, jsonEl, "constructorCalls", "constructorCall", "", "constructorCall");
    addDeps(arr, jsonEl, "anonImpl", "implementsAnonymous", "", "implementsAnonymous");
  }

  if (jsonEl.hasOwnProperty("children")) {
    jsonEl.children.forEach(c => addSubTreeDeps(arr, c));
  }
};

let jsonToDependencies = (jsonRoot, nodeMap) => {
  let arr = [];
  addSubTreeDeps(arr, jsonRoot);
  return new Dependencies(arr, nodeMap);
};

module.exports.jsonToDependencies = jsonToDependencies;