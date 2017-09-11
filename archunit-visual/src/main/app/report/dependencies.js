'use strict';

const dependencyTypes = require('./dependency-types.json');
const nodeTypes = require('./node-types.json');
const initDependency = require('./dependency.js').init;

const d3 = require('d3');

const init = (View) => {
  let nodes = new Map();
  let buildDependency;

  const fullnameSeparators = {
    packageSeparator: ".",
    classSeparator: "$"
  };

  const isEmptyOrStartsWithFullnameSeparator = string => !string || string.startsWith(fullnameSeparators.packageSeparator) || string.startsWith(fullnameSeparators.classSeparator);

  const filter = dependencies => ({
    by: propertyFunc => ({
      startsWith: prefix => dependencies.filter(r =>
      propertyFunc(r).startsWith(prefix) && isEmptyOrStartsWithFullnameSeparator(propertyFunc(r).substring(prefix.length))),
      equals: fullName => dependencies.filter(r => propertyFunc(r) === fullName)
    })
  });


  const uniteDependencies = dependencies => {
    const tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
    const map = new Map();
    tmp.forEach(e => map.set(e[0], []));
    tmp.forEach(e => map.get(e[0]).push(e[1]));

    return Array.from(map).map(([, dependencies]) => {
      if (dependencies.length === 1) {
        return dependencies[0];
      }
      else {
        return buildDependency(dependencies[0].from, dependencies[0].to).byGroupingDependencies(dependencies);
      }
    });
  };

  const transform = dependencies => ({
    where: propertyFunc => ({
      startsWith: prefix => ({
        eliminateSelfDeps: noSelfDeps => ({
          to: transformer => {
            const matching = filter(dependencies).by(propertyFunc).startsWith(prefix);
            const rest = dependencies.filter(r => !matching.includes(r));
            let folded = uniteDependencies(matching.map(transformer));
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
        .to(r => buildDependency(r.from, foldedElement).afterFoldingOneNode(r.description, r.to === foldedElement));
      return transform(targetFolded).where(r => r.from).startsWith(foldedElement).eliminateSelfDeps(true)
        .to(r => buildDependency(foldedElement, r.to).afterFoldingOneNode(r.description, r.from === foldedElement));
    }
  );

  const applyTransformersOnDependencies = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

  const setMustShareNodes = (dependency, dependencies) => {
    dependency.mustShareNodes =
      dependencies._visibleDependencies.filter(e => e.from === dependency.to && e.to === dependency.from).length > 0;
  };

  const recreateVisibleDependencies = dependencies => {
    dependencies._visibleDependencies = applyTransformersOnDependencies(dependencies._transformers.values(), dependencies._filteredUniqued);
    dependencies._visibleDependencies.forEach(d => setMustShareNodes(d, dependencies));
    dependencies._visibleDependencies.forEach(d => d.updateVisualData());
  };

  const reapplyFilters = (dependencies, filters) => {
    dependencies._filtered = Array.from(filters).reduce((filtered_deps, filter) => filter(filtered_deps),
      dependencies._all);
    dependencies._filteredUniqued = uniteDependencies(Array.from(dependencies._filtered));
    recreateVisibleDependencies(dependencies);
  };

  const newFilters = (dependencies) => ({
    typeFilter: null,
    nameFilter: null,

    apply: function () {
      reapplyFilters(dependencies, this.values());
    },

    values: function () {
      return [this.typeFilter, this.nameFilter].filter(f => !!f); // FIXME: We should not pass this object around to other modules (this is the reason for the name for now)
    }
  });

  const makeUniqueByProperty = (arr, propertyFunc) => {
    const map = new Map();
    arr.forEach(d => map.set(propertyFunc(d), d));
    return [...map.values()];
  };

  const Dependencies = class {
    constructor(all) {
      this._transformers = new Map();
      this._all = all;
      this._filtered = this._all;
      this._filteredUniqued = uniteDependencies(Array.from(this._filtered));
      recreateVisibleDependencies(this);
      this._filters = newFilters(this);
    }

    updateOnNodeDragged(node) {
      this.getVisible().filter(d => d.from.startsWith(node.getFullName()) || d.to.startsWith(node.getFullName())).forEach(d => d.updateVisualData())
    }

    _reassignViews(svgElement, callback) {
      const map = new Map();
      this.getVisible().forEach(d => map.set(d.getIdentifyingString(), d));
      d3.select(svgElement).selectAll('g').filter(d => !map.has(d.getIdentifyingString())).each(d => d.hide());
      this.getVisible().forEach(d => d.initView(svgElement, callback));
    }

    _showAllVisibleDependencies() {
      this.getVisible().forEach(d => d.show());
    }

    initViews(svgElement, callback) {
      this._svgElement = svgElement;
      this._callback = callback;
    }

    refreshViews() {
      this._reassignViews(this._svgElement, this._callback);
      this._showAllVisibleDependencies();
    }

    updateViewsWithoutTransition() {
      this.getVisible().forEach(d => d.updateViewWithoutTransition());
    }

    updateViewsWithoutTransitionOfNode(node) {
      this.getVisible().filter(d => d.from.startsWith(node.getFullName()) || d.to.startsWith(node.getFullName())).forEach(d => d.updateViewWithoutTransition());
    }

    updateViewsWithTransition() {
      return Promise.all(this.getVisible().map(d => d.updateViewWithTransition()));
    }

    changeFold(foldedElement, isFolded) {
      if (isFolded) {
        this._transformers.set(foldedElement, foldTransformer(foldedElement));
      }
      else {
        this._transformers.delete(foldedElement);
      }
      recreateVisibleDependencies(this);
    }

    setNodeFilters(filters) {
      this._filters.nameFilter = dependencies => Array.from(filters.values()).reduce((filteredDeps, filter) =>
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
          && ((dependency.getStartNode().getParent() !== dependency.getEndNode()
          && dependency.getEndNode().getParent() !== dependency.getStartNode())
          || typeFilterConfig.showDependenciesBetweenClassAndItsInnerClasses);
      };
      this._filters.typeFilter = dependencies => dependencies.filter(typeFilter);
      this._filters.apply();
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
        cssClass: d.getClass()
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
            arr.push(buildDependency(jsonElement.fullName, jsonElement[type.name])
              .withSingleDependencyDescription(type.dependency));
          }
          else if (!type.isUnique && jsonElement[type.name].length > 0) {
            jsonElement[type.name].forEach(d => arr.push(
              buildDependency(jsonElement.fullName, d.target || d)
                .withSingleDependencyDescription(type.dependency, d.startCodeUnit, d.targetCodeElement)));
          }
        }
      );
    }

    if (jsonElement.hasOwnProperty("children")) {
      jsonElement.children.forEach(c => addAllDependenciesOfJsonElementToArray(c, arr));
    }
  };

  const collectAllDependenciesOfJsonElement = jsonElement => {
    const res = [];
    addAllDependenciesOfJsonElementToArray(jsonElement, res);
    return res;
  };

  const jsonToDependencies = (jsonRoot, nodeMap) => {
    nodes = nodeMap;
    buildDependency = initDependency(View, nodeMap).buildDependency;
    const allDependencies = collectAllDependenciesOfJsonElement(jsonRoot);
    return new Dependencies(allDependencies);
  };

  return jsonToDependencies;
};

module.exports.init = (View) => ({
  jsonToDependencies: init(View)
});