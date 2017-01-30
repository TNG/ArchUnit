'use strict';

let nodes = new Map();

let Dependency = class {
  constructor(from, to, kind, startCodeUnit, targetElement) {
    this.from = from;
    this.to = to;
    this.kind = kind;
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

  calcEndCoordinates() {
    let startNode = this.getStartNode();
    let endNode = this.getEndNode();
    this.angleRad = Math.atan((endNode.visualData.y - startNode.visualData.y) / (endNode.visualData.x - startNode.visualData.x));
    this.angleDeg = Math.round(this.angleRad * (180 / Math.PI));
    let startAngle, endAngle;
    let angleDiff = Math.asin(this.lineDiff / startNode.visualData.r);
    if (this.mustShareNodes) {
      startAngle = this.angleRad - angleDiff;
      endAngle = this.angleRad + angleDiff;
    }
    else {
      startAngle = this.angleRad;
      endAngle = this.angleRad;
    }
    let startDX = Math.abs(Math.cos(startAngle) * startNode.visualData.r);
    let startDY = Math.abs(Math.sin(startAngle) * startNode.visualData.r);
    let endDX = Math.abs(Math.cos(endAngle) * endNode.visualData.r);
    let endDY = Math.abs(Math.sin(endAngle) * endNode.visualData.r);

    let startdirX, startdirY, enddirX, enddirY;
    if (this.mustShareNodes) {
      let s = Math.sign(endNode.visualData.x - startNode.visualData.x);
      startdirX = (startAngle >= -Math.PI / 2 && startAngle <= Math.PI / 2 ? 1 : -1) * s;
      startdirY = (startAngle >= -Math.PI && startAngle <= 0 ? -1 : 1) * s;
      enddirX = (endAngle >= -Math.PI / 2 && endAngle <= Math.PI / 2 ? -1 : 1) * s;
      enddirY = (endAngle >= -Math.PI && endAngle <= 0 ? 1 : -1) * s;
    }
    else {
      startdirX = Math.sign(endNode.visualData.x - startNode.visualData.x);
      startdirY = Math.sign(endNode.visualData.y - startNode.visualData.y);
      enddirY = -Math.sign(endNode.visualData.y - startNode.visualData.y);
      enddirX = -Math.sign(endNode.visualData.x - startNode.visualData.x);
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
    return this.startCodeUnit && this.targetElement;
  }

  getDescriptionString() {
    return this.startCodeUnit + "->" + this.targetElement;
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

let unique = dependencies => {
  let tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
  let m = new Map();
  tmp.forEach(e => {
    if (m.has(e[0])) {
      /**TODO: evtl. diese Bedingung hinzufuegen -> bewirkt, dass nur Duplikate entfernt werden,
       * die Packages betreffen; möchte man zwischen zwei Klassen evtl. zwei Pfeile haben, so muss man
       * neben Map extra Array verwalten, da bei der else-Zuweisung sonst das Duplikat überschrieben wird
       **/
          //(nodes.get(e[0].substring(0, i)).data.type === "package"
          //|| nodes.get(e[0].substring(i + 2)).data.type === "package")

      let i = e[0].indexOf("->");
      let old = m.get(e[0]);
      //TODO: evtl. several durch alle Elemente ersetzen
      let newDep = new Dependency(e[1].from, e[1].to, old.kind === e[1].kind ? e[1].kind : "several",
          old.startCodeUnit === e[1].startCodeUnit ? e[1].startCodeUnit : "several",
          old.targetElement === e[1].targetElement ? e[1].targetElement : "several");
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
        .to(r => (new Dependency(r.from, pkg, r.kind, r.startCodeUnit,
            getExtendedStartTargetName(r.targetElement, r.to, pkg))));
    return transform(targetFolded).where(r => r.from).startsWith(pkg).eliminateSelfDeps(true)
        .to(r => (new Dependency(pkg, r.to, r.kind,
            getExtendedStartTargetName(r.startCodeUnit, r.from, pkg), r.targetElement)));
  }
};

let recalculateVisible = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

let recreateVisible = dependencies => {
  let after = recalculateVisible(dependencies._transformers.values(), dependencies._filtered); //_all
  dependencies.setVisibleDependencies(after);
};

let changeFold = (dependencies, type, callback) => {
  //let before = dependencies._visibleDependencies;
  callback(dependencies);
  recreateVisible(dependencies);
  //dependencies.getVisible().forEach(e => e.calcEndCoordinates());
};

let setForAllMustShareNodes = visDeps => {
  visDeps.forEach(d => d.mustShareNodes =
      visDeps.filter(e => e.from === d.to && e.to === d.from).length > 0);
};

let Dependencies = class {
  constructor(all, nodeMap) {
    this._transformers = new Map();
    nodes = nodeMap;
    this._all = unique(Array.from(all));
    this._filtered = this._all;
    this.setVisibleDependencies(this._filtered); //this._all
    //this._all.forEach(e => e.calcEndCoordinates());
  }

  setVisibleDependencies(deps) {
    this._visibleDependencies = deps;
    setForAllMustShareNodes(this._visibleDependencies);
    this._visibleDependencies.forEach(e => e.calcEndCoordinates()); //new
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

  /**
   * filters the dependencies: only dependencies, whose start and target pass the filterFunction, are taken over
   * @param filterFunction
   */
  filter(filterFunction) {
    this._filtered = this._all.filter(d => filterFunction(nodes.get(d.from)) && filterFunction(nodes.get(d.to)));
    recreateVisible(this);
  }

  resetFilter() {
    this._filtered = this._all;
    recreateVisible(this);
  }

  recalcEndCoordinatesOf(node) {
    this._visibleDependencies.filter(d => d.from === node || d.to === node).forEach(d => d.calcEndCoordinates());
  }

  getVisible() {
    return this._visibleDependencies;
  }
};

let addDeps = (arr, jsonEl, dep, kind) => {
  if (jsonEl.hasOwnProperty(dep) && jsonEl[dep].length !== 0) {
    jsonEl[dep].forEach(i => arr.push(
        new Dependency(jsonEl.fullname, i.to || i, kind, i.startCodeUnit, i.targetElement)))
  }
};

let addSubTreeDeps = (arr, jsonEl) => {
  if (jsonEl.type !== "package") {
    let from = jsonEl.fullname;
    //add super class, if there is one
    if (jsonEl.hasOwnProperty("superclass") && jsonEl.superclass !== "") {
      arr.push(new Dependency(from, jsonEl.superclass, "extends"));
    }
    addDeps(arr, jsonEl, "interfaces", "implements");
    addDeps(arr, jsonEl, "methodCalls", "methodCall");
    addDeps(arr, jsonEl, "fieldAccesses", "fieldAccess");
    addDeps(arr, jsonEl, "constructorCalls", "constructorCall");
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