'use strict';

const {nodeCreator} = require('./test-json-creator');

const createMapFromSplitClassName = fullNameParts => {
  if (fullNameParts.length === 0) {
    return new Map();
  }
  const entry = [fullNameParts.shift(), createMapFromSplitClassName(fullNameParts)];
  return new Map([entry]);
};

const subtractMap = (map, subtract) => new Map(Array.from(map.entries()).filter(([key]) => !subtract.has(key)));
const addEntries = (map, toAdd) => toAdd.forEach((value, key) => map.set(key, value));

const merge = (firstMap, secondMap) => {
  const result = new Map();
  addEntries(result, subtractMap(firstMap, secondMap));
  addEntries(result, subtractMap(secondMap, firstMap));

  Array.from(firstMap.entries())
    .filter(([key]) => secondMap.has(key))
    .forEach(([key, value]) => result.set(key, merge(value, secondMap.get(key))));

  return result;
};

const mapToTreeStructure = nodeAsMap => {
  if (nodeAsMap.size !== 1) {
    throw new Error('Can only convert Map with a single root entry to tree');
  }

  const onlyEntry = Array.from(nodeAsMap.entries())[0];
  const currentElement = onlyEntry[0];
  const childrenMap = onlyEntry[1];

  const type = currentElement.toLowerCase().includes('interface') ? 'interface' : 'class';
  if (currentElement.endsWith('$')) {
    const result = nodeCreator.clazz(currentElement.slice(0, -1), type);
    Array.from(childrenMap.entries())
      .map(e => mapToTreeStructure(new Map([e])))
      .forEach(innerClass => result.havingInnerClass(innerClass));
    return result.build();
  } else {
    const result = nodeCreator.package(currentElement);
    Array.from(childrenMap.entries())
      .map(e => mapToTreeStructure(new Map([e])))
      .forEach(childNode => result.add(childNode));
    return result.build();
  }
};

const getOnly = (map, func) => {
  const result = Array.from(func(map));
  if (result.length !== 1) {
    throw new Error(`${func}(${map}) didn't produce exactly one element`);
  }
  return result[0];
};

const getOnlyKey = map => {
  return getOnly(map, map => map.keys());
};

const getOnlyValue = map => {
  return getOnly(map, map => map.values());
};

// NOTE: The Json exporter chooses the first package that has anything other than just a further single package
//       as its child as the root (e.g. skips 'com', if only package 'tngtech' resides within 'com')
const removeTrivialToplevelPackages = nodeAsMap => {
  if (nodeAsMap.size === 1 && getOnlyValue(nodeAsMap).size === 1
    && !getOnlyKey(getOnlyValue(nodeAsMap)).endsWith('$') && getOnlyValue(getOnlyValue(nodeAsMap)).size > 0) {
    const packageSoFar = getOnlyKey(nodeAsMap);
    const child = getOnlyValue(nodeAsMap);
    const packageOfChild = getOnlyKey(child);
    return removeTrivialToplevelPackages(new Map([[`${packageSoFar}.${packageOfChild}`, getOnlyValue(child)]]));
  }
  return nodeAsMap;
};

/**
 * @param classNames the full qualified names of only the classes, that have no further inner classes (i.e. all leaf nodes); for all classes with
 * 'interface' in their names, an interface is created
 * @return json structure with the classes
 */
const classNamesToTreeStructure = (...classNames) => {
  classNames = classNames.map(className => className + '$');
  let nodeAsMap = new Map();
  classNames.forEach(className => {
    let fullNameParts = className.split('.');
    const fullNameOfClassPart = fullNameParts[fullNameParts.length - 1];
    fullNameParts.pop();
    fullNameParts = fullNameParts.concat(fullNameOfClassPart.split(/(?<=\$)/g));
    const packagesAsMap = createMapFromSplitClassName(fullNameParts);
    nodeAsMap = merge(nodeAsMap, packagesAsMap);
  });

  const arrayOfNodeAsMap = Array.from(nodeAsMap.entries()).map(e => removeTrivialToplevelPackages(new Map([e])));

  const defaultPackage = nodeCreator.defaultPackage();
  arrayOfNodeAsMap.forEach(nodeAsMap => defaultPackage.add(mapToTreeStructure(nodeAsMap)));
  return defaultPackage.build();
};

module.exports.createJsonFromClassNames = classNamesToTreeStructure;