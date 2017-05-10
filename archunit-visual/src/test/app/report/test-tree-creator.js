'use strict';
const testJson = require("./test-json-creator");

const jsonToRoot = require("../../../main/app/report/tree.js").jsonToRoot;
const jsonToDependencies = require("../../../main/app/report/dependencies.js").jsonToDependencies;

let emptyDependencies = {
  changeFold: () => {
  },
  resetVisualDataOf: () => {
  },
  setNodeFilters: filters => {
  }
};

let testTree1 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass").build())
          .build())
      .add(testJson.clazz("class2", "class").build())
      .add(testJson.clazz("class3", "interface").build())
      .build();
  let root = jsonToRoot(simpleJsonTree);
  root.setDependencies(emptyDependencies);
  return root;
};

let testTree2 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
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
  let root = jsonToRoot(simpleJsonTree);
  root.setDependencies(emptyDependencies);
  return root;
};

let testTree3 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "class").build())
          .add(testJson.clazz("class2", "interface").build())
          .build())
      .add(testJson.clazz("class3", "interface").build())
      .add(testJson.package("subpkg")
          .add(testJson.clazz("subclass1", "interface").build())
          .build())
      .build();
  let root = jsonToRoot(simpleJsonTree);
  root.setDependencies(emptyDependencies);
  return root;
};

let testTreeWithDependencies1 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
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
  let root = jsonToRoot(simpleJsonTree);
  let deps = jsonToDependencies(simpleJsonTree, root.nodeMap);
  root.setDependencies(deps);
  return root;
};

let allDeps1 = [
  "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) methodCall targetMethod())",
  "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() fieldAccess field1)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(startMethod(arg) constructorCall testclass1())",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];


let testTreeWithDependencies2 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
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
  let root = jsonToRoot(simpleJsonTree);
  let deps = jsonToDependencies(simpleJsonTree, root.nodeMap);
  root.setDependencies(deps);
  return root;
};

let allDeps2 = [
  "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
  "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() several [...])",
  "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

let testTreeWithOverlappingNodesAndMutualDependencies = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass")
              .callingMethod("com.tngtech.interface1", "startMethod(arg1, arg2)", "targetMethod()")
              .callingMethod("com.tngtech.test.testclass1.InnerTestClass1", "startMethod(arg1, arg2)", "targetMethod()")
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
          .accessingField("com.tngtech.class2.InnerClass2", "startCodeUnit()", "innerTargetField")
          .build())
      .add(testJson.clazz("interface1", "interface")
          .callingMethod("com.tngtech.test.subtest.subtestclass1", "startMethod()", "targetMethod()")
          .build())
      .build();
  let root = jsonToRoot(simpleJsonTree);
  let deps = jsonToDependencies(simpleJsonTree, root.nodeMap);
  root.setDependencies(deps);
  return root;
};

let testTreeWithDependencies3 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
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
  let root = jsonToRoot(simpleJsonTree);
  let deps = jsonToDependencies(simpleJsonTree, root.nodeMap);
  root.setDependencies(deps);
  return root;
};

let allDeps3 = [
  "com.tngtech.main.class1->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
  "com.tngtech.main.class3->com.tngtech.interface1(startMethod(arg1, arg2) implements methodCall targetMethod())",
  "com.tngtech.test.testclass1->com.tngtech.class2(testclass1() extends several [...])",
  "com.tngtech.test.testclass1->com.tngtech.main.class1([...] fieldAccess field1)",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(startMethod1() methodCall targetMethod())",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1([...] constructorCall [...])",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

const treeOf = (root, allDependencies) => ({
  root: root,
  allDependencies: allDependencies
});

const allNodes = root => root.getVisibleDescendants().map(n => n.projectData.fullname);
const getNode = (root, fullname) => root.nodeMap.get(fullname);

module.exports = {
  testTree1: () => treeOf(testTree1(), []),
  testTree2: () => treeOf(testTree2(), []),
  testTree3: () => treeOf(testTree3(), []),
  testTreeWithDependencies1: () => treeOf(testTreeWithDependencies1(), allDeps1),
  testTreeWithDependencies2: () => treeOf(testTreeWithDependencies2(), allDeps2),
  testTreeWithOverlappingNodesAndMutualDependencies: () => treeOf(testTreeWithOverlappingNodesAndMutualDependencies(), []),
  testTreeWithDependencies3: () => treeOf(testTreeWithDependencies3(), allDeps3),
  allNodes: allNodes,
  getNode: getNode
};