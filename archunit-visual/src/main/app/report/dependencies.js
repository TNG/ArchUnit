'use strict';

const dependencyTypes = require('./dependency-types.json');
const nodeTypes = require('./node-types.json');
const initDependency = require('./dependency.js').init;

const init = (View) => {

  const arrayDifference = (arr1, arr2) => arr1.filter(x => arr2.indexOf(x) < 0);

  let nodes = new Map();
  let dependencyCreator;

  const fullnameSeparators = {
    packageSeparator: '.',
    classSeparator: '$'
  };

  const isEmptyOrStartsWithFullnameSeparator = string => !string || string.startsWith(fullnameSeparators.packageSeparator) || string.startsWith(fullnameSeparators.classSeparator);

  const filter = dependencies => ({
    by: propertyFunc => ({
      startsWith: prefix => dependencies.filter(r =>
        propertyFunc(r).startsWith(prefix) && isEmptyOrStartsWithFullnameSeparator(propertyFunc(r).substring(prefix.length))),
      equals: fullName => dependencies.filter(r => propertyFunc(r) === fullName)
    })
  });

  const split = dependencies => ({
    by: propertyFunc => ({
      startsWith: prefix => {
        const matching = [];
        const notMatching = [];
        dependencies.forEach(d => (propertyFunc(d).startsWith(prefix)
        && isEmptyOrStartsWithFullnameSeparator(propertyFunc(d).substring(prefix.length)) ? matching : notMatching)
          .push(d));
        return {matching, notMatching};
      }
    })
  });

  const uniteDependencies = (dependencies, svgElement, callForAllViews, getDetailedDependencies) => {
    const tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
    const map = new Map();
    tmp.forEach(e => map.set(e[0], []));
    tmp.forEach(e => map.get(e[0]).push(e[1]));

    return Array.from(map).map(([, dependencies]) =>
      dependencyCreator.getUniqueDependency(dependencies[0].from, dependencies[0].to, svgElement, callForAllViews, getDetailedDependencies)
        .byGroupingDependencies(dependencies));
  };

  const transform = dependencies => ({
    where: propertyFunc => ({
      startsWith: prefix => ({
        eliminateSelfDeps: noSelfDeps => ({
          to: transformer => {
            const splitted = split(dependencies).by(propertyFunc).startsWith(prefix);
            const matching = splitted.matching;
            const rest = splitted.notMatching;
            let folded = matching.map(transformer);
            if (noSelfDeps) {
              folded = folded.filter(r => r.from !== r.to);
            }
            return [...rest, ...folded];
          }
        })
      })
    })
  });


  const foldTransformer = foldedElement => (
    dependencies => {
      const targetFolded = transform(dependencies).where(r => r.to).startsWith(foldedElement).eliminateSelfDeps(false)
        .to(r => dependencyCreator.shiftElementaryDependency(r, r.from, foldedElement));
      return transform(targetFolded).where(r => r.from).startsWith(foldedElement).eliminateSelfDeps(true)
        .to(r => dependencyCreator.shiftElementaryDependency(r, foldedElement, r.to));
    }
  );

  const applyTransformersOnDependencies = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

  //FIXME: optimize performance by using a set with from->to instead of iterating the array that often
  const setMustShareNodes = (dependency, dependencies) => {
    dependency.visualData.mustShareNodes =
      dependencies._visibleDependencies.filter(e => e.from === dependency.to && e.to === dependency.from).length > 0;
  };

  const reapplyFilters = (dependencies, filters) => {
    dependencies._filtered = Array.from(filters).reduce((filtered_deps, filter) => filter(filtered_deps),
      dependencies._elementary);
    dependencies.recreateVisible();
  };

  const newFilters = (dependencies) => ({
    typeFilter: () => null,
    nameFilter: () => null,
    violationsFilter: () => null,

    apply: function () {
      reapplyFilters(dependencies, this.values());
    },

    values: function () {
      return [this.nameFilter(), this.typeFilter(), this.violationsFilter()].filter(f => !!f); // FIXME: We should not pass this object around to other modules (this is the reason for the name for now)
    }
  });

  //TODO: maybe extract to own file and create tests??
  const Violations = class {
    constructor() {
      this._violationGroups = new Map();
      this.violationsSet = new Set();
    }

    containsDependency(dependency) {
      return this.violationsSet.has(dependency.getIdentifyingString());
    }

    isEmpty() {
      return this._violationGroups.size === 0;
    }

    _recreateViolationsSet() {
      this.violationsSet = new Set([].concat.apply([], Array.from(this._violationGroups.values())
        .map(violationGroup => violationGroup.violations))
        .map(violation => `${violation.origin}-${violation.target}`));
    }

    addViolationGroup(violationGroup) {
      this._violationGroups.set(violationGroup.rule, violationGroup);
      this._recreateViolationsSet();
    }

    removeViolationGroup(violationGroup) {
      this._violationGroups.delete(violationGroup.rule);
      this._recreateViolationsSet();
    }

    refreshMarkOfViolationDependencies(dependencies) {
      dependencies.forEach(dependency => dependency.unMarkAsViolation());
      dependencies.filter(dependency => this.containsDependency(dependency)).forEach(dependency => dependency.markAsViolation());
    }

    getFilter() {
      const violationsFilter = dependency => this.isEmpty() || this.containsDependency(dependency);
      return dependencies => dependencies.filter(violationsFilter);
    }
  };

  const makeUniqueByProperty = (arr, propertyFunc) => {
    const map = new Map();
    arr.forEach(d => map.set(propertyFunc(d), d));
    return [...map.values()];
  };

  /**
   * selects all transformers whose key-node has no predecessor in the transformers.key()-array
   * @param transformers Map nodeFullName->transformer
   * @return {*} all transformers that fulfill the property described above
   */
  const getTransformersOfTopMostNodes = transformers => {
    const sortedKeys = Array.from(transformers.keys()).sort((node1, node2) => node1.length - node2.length);
    const res = new Set();
    sortedKeys.forEach(nodeFullName => {
      //TODO: test whether filtering the array has better performance than getting the predecessors
      const selfAndPredecessors = nodes.getByName(nodeFullName).getSelfAndPredecessors();
      if (!selfAndPredecessors.some(predecessor => res.has(predecessor.getFullName()))) {
        res.add(nodeFullName);
      }
    });
    return Array.from(transformers.entries()).filter(entry => res.has(entry[0])).map(entry => entry[1]);
  };

  const Dependencies = class {
    constructor(jsonRoot, nodeMap, svgContainer) {
      nodes = nodeMap;
      dependencyCreator = initDependency(View, nodeMap);

      this._violations = new Violations();

      this._transformers = new Map();
      this._elementary = addAllDependenciesOfJsonElementToArray(jsonRoot, []);

      this._filtered = this._elementary;
      this._svgContainer = svgContainer;
      this.recreateVisible();
      this._filters = newFilters(this);
      this._updatePromise = Promise.resolve();
      this.doNext = fun => this._updatePromise = this._updatePromise.then(fun);
    }

    getAllLinks() {
      const createSimpleDependency = (from, to) => ({source: from, target: to});
      const simpleDependencies = this.getVisible().map(dependency => createSimpleDependency(dependency.from, dependency.to));

      const groupedTransferredSimpleDependencies = simpleDependencies.map(dep => {
        const sourceNode = nodes.getByName(dep.source);
        const targetNode = nodes.getByName(dep.target);

        if (sourceNode.isPredecessorOf(dep.target) || targetNode.isPredecessorOf(dep.source)) {
          return [dep];
        }

        const firstCommonPredecessor = sourceNode.getSelfOrFirstPredecessorMatching(node => node.isPredecessorOf(dep.target));
        const sourcePredecessors = sourceNode.getSelfAndPredecessorsUntilExclusively(firstCommonPredecessor);
        const targetPredecessors = targetNode.getSelfAndPredecessorsUntilExclusively(firstCommonPredecessor);

        const predecessorsTupleOrderByLengthAscending = [sourcePredecessors, targetPredecessors].sort((a, b) => a.length - b.length);
        const shortPredecessors = predecessorsTupleOrderByLengthAscending[0];
        const longPredecessors = predecessorsTupleOrderByLengthAscending[1];
        const peerLinks = shortPredecessors.map((node, i) => createSimpleDependency(node.getFullName(), longPredecessors[i].getFullName()));
        const lastNodeFullName = shortPredecessors[shortPredecessors.length - 1].getFullName();
        const remainingLinks = longPredecessors.slice(shortPredecessors.length).map(node => createSimpleDependency(lastNodeFullName, node.getFullName()));
        return [...peerLinks, ...remainingLinks];
      });

      const transferredSimpleDependencies = [].concat.apply([], groupedTransferredSimpleDependencies);
      const map = new Map();
      transferredSimpleDependencies.forEach(dep => map.set(dep.source + '->' + dep.target, dep));
      return Array.from(map.values());
    }

    showViolations(violationGroup) {
      this._violations.addViolationGroup(violationGroup);
      this._refreshViolationDependencies();
    }

    hideViolations(violationGroup) {
      this._violations.removeViolationGroup(violationGroup);
      this._refreshViolationDependencies();
    }

    _refreshViolationDependencies() {
      this._violations.refreshMarkOfViolationDependencies(this._elementary);
      this._applyFiltersAndRepositionDependencies();
    }

    createListener() {
      return {
        onDrag: node => this.jumpSpecificDependenciesToTheirPositions(node),
        onFold: node => this.updateOnNodeFolded(node.getFullName(), node.isFolded()),
        onInitialFold: node => this.noteThatNodeFolded(node.getFullName(), node.isFolded()),
        onAllNodesFoldedFinished: () => this.recreateVisible(),
        onLayoutChanged: () => this.moveAllToTheirPositions(),
        onNodesOverlapping: (fullNameOfOverlappedNode, positionOfOverlappingNode) => this._hideDependenciesOnNodesOverlapping(fullNameOfOverlappedNode, positionOfOverlappingNode),
        resetNodesOverlapping: () => this._resetVisibility(),
        finishOnNodesOverlapping: () => this.getVisible().forEach(d => d._view._showIfVisible(d))
      }
    }

    /**
     * FIXME: performance
     * Avoid the multiple calls of recreateVisible at the beginning:
     * 5 calls: in the Dep-constructor, when synchronizing the node- and the dep-filter (in filter.html)
     * and the violations (in violation-menu.html),
     * in foldAllNodes
     */
    recreateVisible() {
      const visibleDependenciesBefore = this._visibleDependencies || [];

      const relevantTransformers = getTransformersOfTopMostNodes(this._transformers);
      const transformedDependencies = applyTransformersOnDependencies(relevantTransformers, this._filtered);
      this._visibleDependencies = uniteDependencies(transformedDependencies,
        this._svgContainer,
        fun => this.getVisible().forEach(d => fun(d._view)),
        (from, to) => this.getDetailedDependenciesOf(from, to));

      this._visibleDependencies.forEach(d => setMustShareNodes(d, this));
      this._visibleDependencies.forEach(d => d._isVisible = true);
      this._updateViewsOnVisibleDependenciesChanged(visibleDependenciesBefore);
    }

    _resetVisibility() {
      this.getVisible().forEach(dependency => dependency._isVisible = true);
    }

    _hideDependenciesOnNodesOverlapping(fullNameOfOverlappedNode, positionOfOverlappingNode) {
      this.getVisible().filter(d => d.from === fullNameOfOverlappedNode).forEach(dependency => dependency.hideOnStartOverlapping(positionOfOverlappingNode));
      this.getVisible().filter(d => d.to === fullNameOfOverlappedNode).forEach(dependency => dependency.hideOnTargetOverlapping(positionOfOverlappingNode));
    }

    _updateViewsOnVisibleDependenciesChanged(dependenciesBefore) {
      arrayDifference(dependenciesBefore, this.getVisible()).forEach(d => d.hide());
    }

    _jumpAllToTheirPositions() {
      this.getVisible().forEach(d => d.jumpToPosition())
    }

    jumpSpecificDependenciesToTheirPositions(node) {
      this.getVisible().filter(d => node.isPredecessorOfOrNodeItself(d.from) || node.isPredecessorOfOrNodeItself(d.to)).forEach(d => d.jumpToPosition());
    }

    moveAllToTheirPositions() {
      return this.doNext(() => Promise.all(this.getVisible().map(d => d.moveToPosition())));
    }

    noteThatNodeFolded(foldedNode, isFolded) {
      if (isFolded) {
        this._transformers.set(foldedNode, foldTransformer(foldedNode));
      }
      else {
        this._transformers.delete(foldedNode);
      }
    }

    updateOnNodeFolded(foldedNode, isFolded) {
      if (isFolded) {
        this._transformers.set(foldedNode, foldTransformer(foldedNode));
      }
      else {
        this._transformers.delete(foldedNode);
      }
      this.recreateVisible();
    }

    onHideAllOtherDependenciesWhenViolationExists(hideAllOtherDependencies) {
      if (hideAllOtherDependencies) {
        this._filters.violationsFilter = () => this._violations.getFilter();
      }
      else {
        this._filters.violationsFilter = () => null;
      }
      this._refreshViolationDependencies();
    }

    setNodeFilters(filters) {
      this._filters.nameFilter = () => dependencies => Array.from(filters.values()).reduce((filteredDeps, filter) =>
        filteredDeps.filter(d => filter(nodes.getByName(d.from)) && filter(nodes.getByName(d.to))), dependencies);
      this._filters.apply();
    }

    filterByType(typeFilterConfig) {
      const typeFilter = dependency => {
        const type = dependency.description.getDependencyTypeNamesAsString();
        return (type !== dependencyTypes.allDependencies.implements || typeFilterConfig.showImplementing)
          && ((type !== dependencyTypes.allDependencies.extends || typeFilterConfig.showExtending))
          && ((type !== dependencyTypes.allDependencies.constructorCall || typeFilterConfig.showConstructorCall))
          && ((type !== dependencyTypes.allDependencies.methodCall || typeFilterConfig.showMethodCall))
          && ((type !== dependencyTypes.allDependencies.fieldAccess || typeFilterConfig.showFieldAccess))
          && ((type !== dependencyTypes.allDependencies.implementsAnonymous || typeFilterConfig.showAnonymousImplementation))
          && ((!dependency.getStartNode().isPredecessorOfOrNodeItself(dependency.getEndNode().getFullName())
            && !dependency.getEndNode().isPredecessorOfOrNodeItself(dependency.getStartNode().getFullName()))
            || typeFilterConfig.showDependenciesBetweenClassAndItsInnerClasses);
      };
      this._filters.typeFilter = () => dependencies => dependencies.filter(typeFilter);
      this._applyFiltersAndRepositionDependencies();
    }

    _applyFiltersAndRepositionDependencies() {
      this._filters.apply();
      this.doNext(() => this._jumpAllToTheirPositions());
    }

    getVisible() {
      return this._visibleDependencies;
    }

    //FIXME: probably also performance problems here
    getDetailedDependenciesOf(from, to) {
      const getDependenciesMatching = (dependencies, propertyFunc, depEnd) => {
        const matchingDependencies = filter(dependencies).by(propertyFunc);
        const startNode = nodes.getByName(depEnd);
        if (startNode.isPackage() || startNode.isCurrentlyLeaf()) {
          return matchingDependencies.startsWith(depEnd);
        }
        else {
          return matchingDependencies.equals(depEnd);
        }
      };
      let matching = this._filtered.filter(d => d.description.hasTitle());
      matching = getDependenciesMatching(matching, d => d.from, from);
      matching = getDependenciesMatching(matching, d => d.to, to);
      const detailedDeps = matching.map(d => ({
        description: d.toShortStringRelativeToPredecessors(from, to),
        cssClass: d.getTypeNames()
      }));
      return makeUniqueByProperty(detailedDeps, d => d.description);
    }
  };

  const addAllDependenciesOfJsonElementToArray = (jsonElement, arr) => {
    const allDependencyTypes = dependencyTypes.groupedDependencies.inheritance.types
      .concat(dependencyTypes.groupedDependencies.access.types);

    if (jsonElement.type !== nodeTypes.package) {
      const presentDependencyTypes = allDependencyTypes.filter(type => jsonElement.hasOwnProperty(type.name));
      presentDependencyTypes.forEach(type => {
          if (type.isUnique && jsonElement[type.name]) {
            arr.push(dependencyCreator.createElementaryDependency(jsonElement.fullName, jsonElement[type.name])
              .withDependencyDescription(type.dependency));
          }
          else if (!type.isUnique && jsonElement[type.name].length > 0) {
            jsonElement[type.name].forEach(d => arr.push(
              dependencyCreator.createElementaryDependency(jsonElement.fullName, d.target || d)
                .withDependencyDescription(type.dependency, d.startCodeUnit, d.targetCodeElement)));
          }
        }
      );
    }

    if (jsonElement.hasOwnProperty('children')) {
      jsonElement.children.forEach(c => addAllDependenciesOfJsonElementToArray(c, arr));
    }
    return arr;
  };

  return Dependencies;
};

module.exports.init = (View) => ({
  Dependencies: init(View)
});