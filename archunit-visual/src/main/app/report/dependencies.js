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
            const matching = filter(dependencies).by(propertyFunc).startsWith(prefix);
            const rest = dependencies.filter(r => !matching.includes(r));
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

  const setMustShareNodes = (dependency, dependencies) => {
    dependency.visualData.mustShareNodes =
      dependencies._visibleDependencies.filter(e => e.from === dependency.to && e.to === dependency.from).length > 0;
  };

  const recreateVisibleDependencies = dependencies => {
    const visibleDependenciesBefore = dependencies._visibleDependencies || [];
    dependencies._visibleDependencies = uniteDependencies(
      applyTransformersOnDependencies(dependencies._transformers.values(), dependencies._filtered),
      dependencies._svgContainer,
      fun => dependencies.getVisible().forEach(d => fun(d._view)),
      (from, to) => dependencies.getDetailedDependenciesOf(from, to));
    dependencies._visibleDependencies.forEach(d => setMustShareNodes(d, dependencies));
    dependencies._visibleDependencies.forEach(d => d._isVisible = true);
    dependencies._updateViewsOnVisibleDependenciesChanged(visibleDependenciesBefore);
  };

  const reapplyFilters = (dependencies, filters) => {
    dependencies._filtered = Array.from(filters).reduce((filtered_deps, filter) => filter(filtered_deps),
      dependencies._elementary);
    recreateVisibleDependencies(dependencies);
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
      const getFullNameOfViolationFullPath = fullPath => {
        let lastIndexOfOpeningBracket = fullPath.lastIndexOf('('); //FIXME: brackets are false for fields //no, correct!
        lastIndexOfOpeningBracket = lastIndexOfOpeningBracket === -1 ? fullPath.length : lastIndexOfOpeningBracket;
        const endIndex = fullPath.substring(0, lastIndexOfOpeningBracket).lastIndexOf('.');
        return fullPath.substring(0, endIndex);
      };
      this.violationsSet = new Set([].concat.apply([], Array.from(this._violationGroups.values())
        .map(violationGroup => violationGroup.violations))
        .map(violation => `${violation.origin}-${violation.target}`));
    }

    addViolationGroup(violationGroup) {
      this._violationGroups.set(violationGroup.rule, violationGroup);
      this._recreateViolationsSet();
      //TODO: mark violation-dependencies red, when the other dependencies are not hidden!! (and also in the detailed deps)
    }

    removeViolationGroup(violationGroup) {
      this._violationGroups.delete(violationGroup.rule);
      this._recreateViolationsSet();
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

  const Dependencies = class {
    constructor(jsonRoot, nodeMap, svgContainer) {
      nodes = nodeMap;
      dependencyCreator = initDependency(View, nodeMap);

      this._violations = new Violations();

      this._transformers = new Map();
      this._elementary = addAllDependenciesOfJsonElementToArray(jsonRoot, []);

      this._filtered = this._elementary;
      this._svgContainer = svgContainer;
      recreateVisibleDependencies(this);
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
      this._applyFiltersAndRepositionDependencies();
    }

    hideViolations(violationGroup) {
      this._violations.removeViolationGroup(violationGroup);
      this._applyFiltersAndRepositionDependencies();
    }

    createListener() {
      return {
        onDrag: node => this.jumpSpecificDependenciesToTheirPositions(node),
        onFold: node => this.updateOnNodeFolded(node.getFullName(), node.isFolded()),
        onLayoutChanged: () => this.moveAllToTheirPositions(),
        onNodesOverlapping: (fullNameOfOverlappedNode, positionOfOverlappingNode) => this._hideDependenciesOnNodesOverlapping(fullNameOfOverlappedNode, positionOfOverlappingNode),
        resetNodesOverlapping: () => this._resetVisibility(),
        finishOnNodesOverlapping: () => this.getVisible().forEach(d => d._view._showIfVisible(d))
      }
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
      this.getVisible().filter(d => d.from.startsWith(node.getFullName()) || d.to.startsWith(node.getFullName())).forEach(d => d.jumpToPosition());
    }

    moveAllToTheirPositions() {
      return this.doNext(() => Promise.all(this.getVisible().map(d => d.moveToPosition())));
    }

    updateOnNodeFolded(foldedNode, isFolded) {
      if (isFolded) {
        this._transformers.set(foldedNode, foldTransformer(foldedNode));
      }
      else {
        this._transformers.delete(foldedNode);
      }
      recreateVisibleDependencies(this);
    }

    onHideAllOtherDependenciesWhenViolationExists(hideAllOtherDependencies) {
      if (!hideAllOtherDependencies) {
        this._filters.violationsFilter = () => null;
      }
      else {
        this._filters.violationsFilter = () => this._violations.getFilter();
      }
      this._applyFiltersAndRepositionDependencies();
    }

    setNodeFilters(filters) {
      this._filters.nameFilter = () => dependencies => Array.from(filters.values()).reduce((filteredDeps, filter) =>
        filteredDeps.filter(d => filter(nodes.getByName(d.from)) && filter(nodes.getByName(d.to))), dependencies);
      this._filters.apply();
    }

    filterByType(typeFilterConfig) {
      const typeFilter = dependency => {
        if (this._violations.containsDependency(dependency)) {
          return true;
        }

        const type = dependency.description.getDependencyTypeNamesAsString();
        return (type !== dependencyTypes.allDependencies.implements || typeFilterConfig.showImplementing)
          && ((type !== dependencyTypes.allDependencies.extends || typeFilterConfig.showExtending))
          && ((type !== dependencyTypes.allDependencies.constructorCall || typeFilterConfig.showConstructorCall))
          && ((type !== dependencyTypes.allDependencies.methodCall || typeFilterConfig.showMethodCall))
          && ((type !== dependencyTypes.allDependencies.fieldAccess || typeFilterConfig.showFieldAccess))
          && ((type !== dependencyTypes.allDependencies.implementsAnonymous || typeFilterConfig.showAnonymousImplementation))
          && ((dependency.getStartNode().getParent() !== dependency.getEndNode()
            && dependency.getEndNode().getParent() !== dependency.getStartNode())
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