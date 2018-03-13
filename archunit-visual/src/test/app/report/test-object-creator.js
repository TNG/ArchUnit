'use strict';

const stubs = require('./stubs');
const testJson = require("./test-json-creator");
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(10),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub, //FIXME: really necessary??
  DependencyView: stubs.DependencyViewStub
});

const Root = appContext.getRoot();

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

const mapToJson = jsonAsMap => {
  if (jsonAsMap.size !== 1) {
    throw new Error('Can only convert Map with a single root entry to Json');
  }

  const onlyEntry = Array.from(jsonAsMap.entries())[0];
  const currentElement = onlyEntry[0];
  const childrenMap = onlyEntry[1];

  if (childrenMap.size === 0) {
    return testJson.clazz(currentElement).build();
  } else {
    const result = testJson.package(currentElement);
    Array.from(childrenMap.entries())
      .map(e => mapToJson(new Map([e])))
      .forEach(json => result.add(json));
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
const removeTrivialToplevelPackages = jsonAsMap => {
  if (jsonAsMap.size === 1 && getOnlyValue(jsonAsMap).size === 1) {
    const packageSoFar = getOnlyKey(jsonAsMap);
    const child = getOnlyValue(jsonAsMap);
    const packageOfChild = getOnlyKey(child);
    return removeTrivialToplevelPackages(new Map([[`${packageSoFar}.${packageOfChild}`, getOnlyValue(child)]]));
  }
  return jsonAsMap;
};

const classNamesToJson = (classNames) => {
  let jsonAsMap = new Map();
  classNames.forEach(className => {
    const fullNameParts = className.split('.');
    const packagesAsMap = createMapFromSplitClassName(fullNameParts);
    jsonAsMap = merge(jsonAsMap, packagesAsMap);
  });

  jsonAsMap = removeTrivialToplevelPackages(jsonAsMap);

  return mapToJson(jsonAsMap);
};

module.exports.tree = (...classNames) => {
  return new Root(classNamesToJson(classNames));
};