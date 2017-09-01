'use strict';

const dependencyTypes = require('./dependency-types.json');
const nodeTypes = require('./node-types.json');
const createDependencyBuilder = require('./dependency.js').buildDependency;
let buildDependency;

let nodes = new Map();

const fullnameSeparators = {
  packageSeparator: ".",
  classSeparator: "$"
};

const startsWithFullnameSeparator = string => string.startsWith(fullnameSeparators.packageSeparator) || string.startsWith(fullnameSeparators.classSeparator);

const filter = dependencies => ({
  by: propertyFunc => ({
    startsWith: prefix => dependencies.filter(r => {
      const property = propertyFunc(r);
      if (property.startsWith(prefix)) {
        const rest = property.substring(prefix.length);
        return rest ? startsWithFullnameSeparator(rest) : true;
      }
      else {
        return false;
      }
    }),
    equals: fullName => dependencies.filter(r => propertyFunc(r) === fullName)
  })
});


const unite = dependencies => {
  const tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
  const map = new Map();
  tmp.forEach(e => {
    if (map.has(e[0])) {
      map.get(e[0]).push(e[1]);
    }
    else {
      map.set(e[0], [e[1]]);
    }
  });
  const unitedDependencies = Array.from(map).map(([, dependencies]) => {
    if (dependencies.length === 1) {
      return dependencies[0];
    }
    else {
      return buildDependency(dependencies[0].from, dependencies[0].to).byGroupingDependencies(dependencies);
    }
  });
  return unitedDependencies;
};

const transform = dependencies => ({
  where: propertyFunc => ({
    startsWith: prefix => ({
      eliminateSelfDeps: yes => ({
        to: transformer => {
          const matching = filter(dependencies).by(propertyFunc).startsWith(prefix);
          const rest = dependencies.filter(r => !matching.includes(r));
          let folded = unite(matching.map(transformer));
          if (yes) {
            folded = folded.filter(r => r.from !== r.to);
          }
          return [...rest, ...folded];
        }
      })
    })
  })
});


const foldTransformer = foldedElement => {
  return dependencies => {
    const targetFolded = transform(dependencies).where(r => r.to).startsWith(foldedElement).eliminateSelfDeps(false)
      .to(r =>
        buildDependency(r.from, foldedElement).withExistingDescription(r.description).whenTargetIsFolded(r.to));
    return transform(targetFolded).where(r => r.from).startsWith(foldedElement).eliminateSelfDeps(true)
      .to(r =>
        buildDependency(foldedElement, r.to).withExistingDescription(r.description).whenStartIsFolded(r.from));
  }
};

const recalculateVisible = (transformers, dependencies) => Array.from(transformers)
  .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

const recreateVisible = dependencies => {
  const after = recalculateVisible(dependencies._transformers.values(), dependencies._uniqued);
  dependencies.setVisibleDependencies(after);
};

const changeFold = (dependencies, callback) => {
  callback(dependencies);
  recreateVisible(dependencies);
};

const reapplyFilters = (dependencies, filters) => {
  dependencies._filtered = Array.from(filters).reduce((filtered_deps, filter) => filter(filtered_deps),
    dependencies._all);
  dependencies._uniqued = unite(Array.from(dependencies._filtered));
  recreateVisible(dependencies);
  dependencies.observers.forEach(f => f(dependencies.getVisible()));
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

const Dependencies = class {
  constructor(all) {
    this._transformers = new Map();
    this._all = all;
    this._filtered = this._all;
    this._uniqued = unite(Array.from(this._filtered));
    this.setVisibleDependencies(this._uniqued);
    this.observers = [];
    this._filters = newFilters(this);
  }

  addObserver(observerFunction) {
    this.observers.push(observerFunction);
  }

  setVisibleDependencies(deps) {
    this._visibleDependencies = deps;
    this._visibleDependencies.forEach(d => d.mustShareNodes =
      this._visibleDependencies.filter(e => e.from === d.to && e.to === d.from).length > 0);
  }

  changeFold(foldedElement, isFolded) {
    if (isFolded) {
      changeFold(this, dependencies => dependencies._transformers.set(foldedElement, foldTransformer(foldedElement)));
    }
    else {
      changeFold(this, dependencies => dependencies._transformers.delete(foldedElement));
    }
  }

  setNodeFilters(filters) {
    this._filters.nameFilter = filtered_deps => Array.from(filters.values()).reduce((deps, filter) =>
      deps.filter(d => filter(nodes.getByName(d.from)) && filter(nodes.getByName(d.to))), filtered_deps);
    this._filters.apply();
  }

  filterByType(typeFilterConfig) {
    const typeFilter = dependency => {
      const type = dependency.description.getDependencyTypesAsString();
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

  resetFilterByType() {
    this._filters.typeFilter = null;
    this._filters.apply();
  }

  getVisible() {
    return this._visibleDependencies;
  }

  getDetailedDependenciesOf(from, to) {
    const getDetailedDependenciesMatching = (dependencies, propertyFunc, depEnd) => {
      const matching = filter(dependencies).by(propertyFunc);
      const startNode = nodes.getByName(depEnd);
      if (startNode.isPackage() || startNode.isCurrentlyLeaf()) {
        return matching.startsWith(depEnd);
      }
      else {
        return matching.equals(depEnd);
      }
    };
    const startMatching = getDetailedDependenciesMatching(this._filtered, d => d.from, from);
    let targetMatching = getDetailedDependenciesMatching(startMatching, d => d.to, to);
    targetMatching = targetMatching.filter(d => d.description.hasTitle());
    const detailedDeps = targetMatching.map(d => ({
      description: d.getDescriptionRelativeToPredecessors(from, to),
      cssClass: d.getClass()
    }));
    const map = new Map();
    detailedDeps.forEach(d => map.set(d.description, d));
    return [...map.values()];
  }
};

const addDependenciesOf = dependencyGroup => ({
  ofJsonElement: jsonElement => ({
    toArray: arr => {
      dependencyGroup.types.forEach(type => {
        if (jsonElement.hasOwnProperty(type.name)) {
          if (type.isUnique && jsonElement[type.name]) {
            arr.push(buildDependency(jsonElement.fullName, jsonElement[type.name]).withSingleDependencyDescription(type.dependency));
          }
          else if (!type.isUnique && jsonElement[type.name].length !== 0) {
            jsonElement[type.name].forEach(d => arr.push(
              buildDependency(jsonElement.fullName, d.target || d).withSingleDependencyDescription(type.dependency, d.startCodeUnit, d.targetCodeElement)));
          }
        }
      });
    }
  })
});

const addAllDependenciesOfJsonElement = jsonElement => ({
  toArray: arr => {
    if (jsonElement.type !== nodeTypes.package) {
      const groupedDependencies = dependencyTypes.groupedDependencies;
      addDependenciesOf(groupedDependencies.inheritance).ofJsonElement(jsonElement).toArray(arr);
      addDependenciesOf(groupedDependencies.access).ofJsonElement(jsonElement).toArray(arr);
    }

    if (jsonElement.hasOwnProperty("children")) {
      jsonElement.children.forEach(c => addAllDependenciesOfJsonElement(c).toArray(arr));
    }
  }
});

const jsonToDependencies = (jsonRoot, nodeMap) => {
  const arr = [];
  nodes = nodeMap;
  buildDependency = createDependencyBuilder(nodeMap);
  addAllDependenciesOfJsonElement(jsonRoot).toArray(arr);
  return new Dependencies(arr);
};

module.exports.jsonToDependencies = jsonToDependencies;