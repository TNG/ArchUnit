'use strict';
const testJson = require("./test-json-creator");

const visualizationStyles = require('./stubs').visualizationStylesStub();
visualizationStyles.setCirclePadding(10);
const calculateTextWidth = text => text.length * 7;

const GraphView = class {
  renderWithTransition() {
    return Promise.resolve();
  }
};

const NodeView = class {
  show() {
  }

  hide() {
  }

  jumpToPosition() {
  }

  moveToPosition() {
    return Promise.resolve();
  }

  moveToRadius() {
    return Promise.resolve();
  }

  onClick() {
  }

  onDrag() {
  }

  updateNodeType() {
  }
};
const DependencyView = class {
  show() {
  }

  hide() {
  }

  refresh() {
  }

  jumpToPositionAndShow() {
  }

  moveToPositionAndShow() {
    return Promise.resolve();
  }
};

const appContext = require('./main-files').get('app-context').newInstance({
  GraphView, NodeView, DependencyView,
  visualizationStyles, calculateTextWidth
});

const jsonToRoot = appContext.getJsonToRoot();
const jsonToGraph = appContext.getJsonToGraph();

const testTree1 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass").build())
      .build())
    .add(testJson.clazz("class2", "class").build())
    .add(testJson.clazz("class3", "interface").build())
    .build();
  return jsonToRoot(simpleJsonTree);
};

const testTree2 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass").build())
      .build())
    .add(testJson.package("test")
      .add(testJson.clazz("testclass1", "class").build())
      .add(testJson.package("subtest")
        .add(testJson.clazz("subtestclass1", "class").build())
        .build())
      .build())
    .add(testJson.clazz("class2", "class").build())
    .add(testJson.clazz("class3", "interface").build())
    .build();
  return jsonToRoot(simpleJsonTree);
};

const testTree3 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "class").build())
      .add(testJson.clazz("class2", "interface").build())
      .build())
    .add(testJson.clazz("class3", "interface").build())
    .add(testJson.package("subpkg")
      .add(testJson.clazz("subclass1", "interface").build())
      .build())
    .build();
  return jsonToRoot(simpleJsonTree);
};

const testGraph1 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass")
        .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
        .build())
      .build())
    .add(testJson.package("test")
      .add(testJson.clazz("testclass1", "class")
        .accessingField("com.tngtech.class2", "testclass1()", "field1")
        .build())
      .add(testJson.package("subtest")
        .add(testJson.clazz("subtestclass1", "class")
          .implementing("com.tngtech.interface1")
          .callingConstructor("com.tngtech.test.testclass1", "startMethod(arg)", "testclass1()")
          .build())
        .build())
      .build())
    .add(testJson.clazz("class2", "class")
      .extending("com.tngtech.main.class1")
      .implementing("com.tngtech.interface1")
      .build())
    .add(testJson.clazz("interface1", "interface").build())
    .build();
  return jsonToGraph(simpleJsonTree);
};

const allDeps1 = [
  "com.tngtech.main.class1->com.tngtech.interface1(methodCall)",
  "com.tngtech.test.testclass1->com.tngtech.class2(fieldAccess)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];


const testGraph2 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass")
        .implementing("com.tngtech.interface1")
        .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
        .build())
      .build())
    .add(testJson.package("test")
      .add(testJson.clazz("testclass1", "class")
        .accessingField("com.tngtech.class2", "testclass1()", "field1")
        .callingMethod("com.tngtech.class2", "testclass1()", "targetMethod()")
        .accessingField("com.tngtech.main.class1", "startMethod1()", "field1")
        .accessingField("com.tngtech.main.class1", "startMethod2()", "field1")
        .implementingAnonymous("com.tngtech.interface1")
        .build())
      .add(testJson.package("subtest")
        .add(testJson.clazz("subtestclass1", "class")
          .implementing("com.tngtech.interface1")
          .callingMethod("com.tngtech.class2", "startMethod1()", "targetMethod()")
          .callingConstructor("com.tngtech.test.testclass1", "doSmth(arg)", "testclass1()")
          .callingConstructor("com.tngtech.test.testclass1", "startMethod1()", "testclass1(arg)")
          .build())
        .build())
      .build())
    .add(testJson.clazz("class2", "class")
      .extending("com.tngtech.main.class1")
      .implementing("com.tngtech.interface1")
      .build())
    .add(testJson.clazz("interface1", "interface").build())
    .build();
  return jsonToGraph(simpleJsonTree);
};

const allDeps2 = [
  "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
  "com.tngtech.test.testclass1->com.tngtech.class2(several)",
  "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

const testGraphWithOverlappingNodesAndMutualDependencies = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass")
        .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
        .callingMethod("com.tngtech.test.testclass1$InnerTestClass1", "startMethod(arg1, arg2)", "targetMethod()")
        .build())
      .build())
    .add(testJson.package("test")
      .add(testJson.clazz("testclass1", "class")
        .havingInnerClass(testJson.clazz("InnerTestClass1", "class")
          .accessingField("com.tngtech.class2", "innertestclass1()", "field1").build())
        .accessingField("com.tngtech.class2", "testclass1()", "field1")
        .build())
      .add(testJson.package("subtest")
        .add(testJson.clazz("subtestclass1", "class")
          .implementing("com.tngtech.interface1")
          .callingConstructor("com.tngtech.test.testclass1", "startMethod(arg)", "testclass1()")
          .build())
        .build())
      .build())
    .add(testJson.clazz("class2", "class")
      .extending("com.tngtech.main.class1")
      .implementing("com.tngtech.interface1")
      .havingInnerClass(testJson.clazz("InnerClass2", "class")
        .accessingField("com.tngtech.class2", "startCodeUnit()", "targetField")
        .build())
      .accessingField("com.tngtech.class2$InnerClass2", "startCodeUnit()", "innerTargetField")
      .build())
    .add(testJson.clazz("interface1", "interface")
      .callingMethod("com.tngtech.test.subtest.subtestclass1", "startMethod()", "targetMethod()")
      .build())
    .build();
  return jsonToGraph(simpleJsonTree);
};

const testGraph3 = () => {
  const simpleJsonTree = testJson.package("com.tngtech")
    .add(testJson.package("main")
      .add(testJson.clazz("class1", "abstractclass")
        .implementing("com.tngtech.interface1")
        .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
        .build())
      .add(testJson.clazz("class3", "class")
        .implementing("com.tngtech.interface1")
        .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
        .build())
      .build())
    .add(testJson.package("test")
      .add(testJson.clazz("testclass1", "class")
        .extending("com.tngtech.class2")
        .accessingField("com.tngtech.class2", "testclass1()", "field1")
        .callingMethod("com.tngtech.class2", "testclass1()", "targetMethod()")
        .accessingField("com.tngtech.main.class1", "startMethod1()", "field1")
        .accessingField("com.tngtech.main.class1", "startMethod2()", "field1")
        .implementingAnonymous("com.tngtech.interface1")
        .build())
      .add(testJson.package("subtest")
        .add(testJson.clazz("subtestclass1", "class")
          .implementing("com.tngtech.interface1")
          .callingMethod("com.tngtech.class2", "startMethod1()", "targetMethod()")
          .callingConstructor("com.tngtech.test.testclass1", "doSmth(arg)", "testclass1()")
          .callingConstructor("com.tngtech.test.testclass1", "startMethod1()", "testclass1(arg)")
          .build())
        .build())
      .build())
    .add(testJson.clazz("class2", "class")
      .extending("com.tngtech.main.class1")
      .implementing("com.tngtech.interface1")
      .build())
    .add(testJson.clazz("interface1", "interface").build())
    .build();
  return jsonToGraph(simpleJsonTree);
};

const allDeps3 = [
  "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
  "com.tngtech.main.class3->com.tngtech.interface1(implements methodCall)",
  "com.tngtech.test.testclass1->com.tngtech.class2(extends several)",
  "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

const treeWrapperOf = root => {
  const nodeMap = createNodeMap(root);
  return {
    root: root,
    getNode: fullName => nodeMap.get(fullName)
  }
};

const graphWrapperOf = (graph, allDependencies) => ({
  graph: graph,
  getNode: fullName => graph.root.getByName(fullName),
  allDependencies: allDependencies
});

const allNodes = root => root.getSelfAndDescendants().map(node => node.getFullName());

const createNodeMap = root => {
  const nodeMap = new Map();
  root.getSelfAndDescendants().forEach(node => nodeMap.set(node.getFullName(), node));
  return nodeMap;
};

// FIXME: Whatever 'wrapper' is supposed to mean, it's defenitely weird
// FIXME: Global test objects pattern is bad, too. Don't just define graph1, graph2, ... and use those, because you'll use all information, what is really relevant for the test
module.exports = {
  testTree1: () => treeWrapperOf(testTree1()),
  testTree2: () => treeWrapperOf(testTree2()),
  testTree3: () => treeWrapperOf(testTree3()),
  testGraph1: () => graphWrapperOf(testGraph1(), allDeps1),
  testGraph2: () => graphWrapperOf(testGraph2(), allDeps2),
  testGraphWithOverlappingNodesAndMutualDependencies: () => graphWrapperOf(testGraphWithOverlappingNodesAndMutualDependencies(), []),
  testGraph3: () => graphWrapperOf(testGraph3(), allDeps3),
  allNodes: allNodes,
};

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
  return jsonToRoot(classNamesToJson(classNames));
};

module.exports.graph = (...classNames) => {
  return jsonToGraph(classNamesToJson(classNames));
};

module.exports.calculateTextWidth = calculateTextWidth;
module.exports.visualizationStyles = visualizationStyles;