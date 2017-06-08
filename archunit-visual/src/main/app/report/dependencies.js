'use strict';

const dependencyKinds = require('./dependency-kinds.json');
const nodeKinds = require('./node-kinds.json');
const boolFuncs = require('./booleanutils').booleanFunctions;
let createDependencyBuilder = require('./dependency.js').buildDependency;
let buildDependency;
const KIND_FILTER = "kindfilter";
const NODE_FILTER = "nodefilter";

let nodes = new Map();

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
    }),
    equals: fullname => dependencies.filter(r => propertyFunc(r) === fullname)
  })
});


let unique = dependencies => {
  let tmp = Array.from(dependencies.map(r => [`${r.from}->${r.to}`, r]));
  let map = new Map();
  tmp.forEach(e => {
    if (map.has(e[0])) {
      let old = map.get(e[0]);
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


let foldTransformer = foldedElement => {
  return dependencies => {
    let targetFolded = transform(dependencies).where(r => r.to).startsWith(foldedElement).eliminateSelfDeps(false)
        .to(r => (
            buildDependency(r.from, foldedElement).withExistingDescription(r.description).whenTargetIsFolded(r.to)));
    return transform(targetFolded).where(r => r.from).startsWith(foldedElement).eliminateSelfDeps(true)
        .to(r => (
            buildDependency(foldedElement, r.to).withExistingDescription(r.description).whenStartIsFolded(r.from)));
  }
};

let recalculateVisible = (transformers, dependencies) => Array.from(transformers)
    .reduce((mappedDependencies, transformer) => transformer(mappedDependencies), dependencies);

let recreateVisible = dependencies => {
  let after = recalculateVisible(dependencies._transformers.values(), dependencies._uniqued);
  dependencies.setVisibleDependencies(after);
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
  dependencies.observers.forEach(f => f(dependencies.getVisible()));
};

let Dependencies = class {
  constructor(all) {
    this._filters = new Map();
    this._transformers = new Map();
    this._all = all;
    this._filtered = this._all;
    this._uniqued = unique(Array.from(this._filtered));
    this.setVisibleDependencies(this._uniqued);
    this.observers = [];
  }

  addObserver(observerFunction) {
    this.observers.push(observerFunction);
  }

  setVisibleDependencies(deps) {
    this._visibleDependencies = deps;
    this._visibleDependencies.forEach(d => d.mustShareNodes =
        this._visibleDependencies.filter(e => e.from === d.to && e.to === d.from).length > 0);
  }

  keyFunction() {
    return e => e.from + "->" + e.to;
  }

  changeFold(foldedElement, isFolded) {
    if (isFolded) {
      changeFold(this, dependencies => dependencies._transformers.set(foldedElement, foldTransformer(foldedElement)));
    }
    else {
      changeFold(this, dependencies => dependencies._transformers.delete(foldedElement));
    }
    //this.observers.forEach(f => f(this.getVisible()));
  }

  setNodeFilters(filters) {
    this._filters.set(NODE_FILTER, filtered_deps => Array.from(filters.values()).reduce((deps, filter) =>
        deps.filter(d => filter(nodes.get(d.from)) && filter(nodes.get(d.to))), filtered_deps));
    reapplyFilters(this, this._filters);
  }

  filterByKind() {
    let applyFilter = kindFilter => {
      this._filters.set(KIND_FILTER, filtered_deps => filtered_deps.filter(kindFilter));
      reapplyFilters(this, this._filters);
    };
    return {
      showImplementing: implementing => ({
        showExtending: extending => ({
          showConstructorCall: constructorCall => ({
            showMethodCall: methodCall => ({
              showFieldAccess: fieldAccess => ({
                showAnonymousImplementing: anonImpl => ({
                  showDepsBetweenChildAndParent: childAndParent => {
                    let kindFilter = d => {
                      let kinds = d.description.getAllKinds();
                      let deps = dependencyKinds.all_dependencies;
                      return boolFuncs(kinds === deps.implements).implies(implementing)
                          && boolFuncs(kinds === deps.extends).implies(extending)
                          && boolFuncs(kinds === deps.constructorCall).implies(constructorCall)
                          && boolFuncs(kinds === deps.methodCall).implies(methodCall)
                          && boolFuncs(kinds === deps.fieldAccess).implies(fieldAccess)
                          && boolFuncs(kinds === deps.implementsAnonymous).implies(anonImpl)
                          && boolFuncs(d.getStartNode().parent === d.getEndNode()
                              || d.getEndNode().parent === d.getStartNode()).implies(childAndParent);
                    };
                    applyFilter(kindFilter);
                  }
                })
              })
            })
          })
        })
      })
    };
  }

  resetFilterByKind() {
    this._filters.delete(KIND_FILTER);
    reapplyFilters(this, this._filters);
  }

  getVisible() {
    return this._visibleDependencies;
  }

  getDetailedDependenciesOf(from, to) {
    let getDetailedDependenciesMatching = (dependencies, propertyFunc, depEnd) => {
      let matching = filter(dependencies).by(propertyFunc);
      let startNode = nodes.get(depEnd);
      if (startNode.projectData.type === nodeKinds.package || startNode.isCurrentlyLeaf()) {
        return matching.startsWith(depEnd);
      }
      else {
        return matching.equals(depEnd);
      }
    };
    let startMatching = getDetailedDependenciesMatching(this._filtered, d => d.from, from);
    let targetMatching = getDetailedDependenciesMatching(startMatching, d => d.to, to);
    return targetMatching.map(d => {
      return {
        description: d.getDescriptionRelativeToPredecessors(from, to),
        cssClass: d.getClass()
      }
    });
  }
};

let addDependenciesOf = dependencyGroup => ({
  ofJsonElement: jsonElement => ({
    toArray: arr => {
      dependencyGroup.kinds.forEach(kind => {
        if (jsonElement.hasOwnProperty(kind.name)) {
          if (kind.isUnique && jsonElement[kind.name]) {
            arr.push(buildDependency(jsonElement.fullname, jsonElement[kind.name]).withNewDescription()
                .withKind(dependencyGroup.name, kind.dependency).build());
          }
          else if (!kind.isUnique && jsonElement[kind.name].length !== 0) {
            jsonElement[kind.name].forEach(d => arr.push(
                buildDependency(jsonElement.fullname, d.to || d).withNewDescription().withKind(dependencyGroup.name, kind.dependency).withStartCodeUnit(d.startCodeUnit)
                    .withTargetElement(d.targetElement).build()));
          }
        }
      });
    }
  })
});

let addAllDependenciesOfJsonElement = jsonElement => ({
  toArray: arr => {
    if (jsonElement.type !== nodeKinds.package) {
      let groupedDependencies = dependencyKinds.grouped_dependencies;
      addDependenciesOf(groupedDependencies.inheritance).ofJsonElement(jsonElement).toArray(arr);
      addDependenciesOf(groupedDependencies.access).ofJsonElement(jsonElement).toArray(arr);
    }

    if (jsonElement.hasOwnProperty("children")) {
      jsonElement.children.forEach(c => addAllDependenciesOfJsonElement(c).toArray(arr));
    }
  }
});

let jsonToDependencies = (jsonRoot, nodeMap) => {
  let arr = [];
  nodes = nodeMap;
  buildDependency = createDependencyBuilder(nodeMap);
  addAllDependenciesOfJsonElement(jsonRoot).toArray(arr);
  return new Dependencies(arr);
};

module.exports.jsonToDependencies = jsonToDependencies;