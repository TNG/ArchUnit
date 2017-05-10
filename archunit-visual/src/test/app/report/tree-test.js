'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testTrees = require("./test-tree-creator.js");

const emptyFun = () => {
};

const emptyDeps = {
  getVisible: () => {
  },
  setNodeFilters: () => {
  },
  changeFold: () => {
  }
};

describe("Node", () => {

  it("knows if it is root", () => {
    let root = testTrees.testTree1().root;
    expect(root.isRoot()).to.equal(true);
    expect(testTrees.getNode(root, "com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is leaf", () => {
    let root = testTrees.testTree1().root;
    expect(root.isLeaf()).to.equal(false);

    root.changeFold(emptyFun);
    expect(root.isLeaf()).to.equal(false);
    expect(testTrees.getNode(root, "com.tngtech.class2").isLeaf()).to.equal(true);
  });

  it("knows if it is current leaf", () => {
    let root = testTrees.testTree1().root;
    expect(root.isCurrentlyLeaf()).to.equal(false);

    root.changeFold(emptyFun);
    expect(root.isCurrentlyLeaf()).to.equal(true);
  });

  it("knows if it is child of an other node", () => {
    let root = testTrees.testTree1().root;
    expect(root.isChildOf(root.nodeMap.get("com.tngtech.class2"))).to.equal(false);
    expect(testTrees.getNode(root, "com.tngtech.class2").isChildOf(root)).to.equal(true);
    expect(root.isChildOf(root)).to.equal(true);
  });

  it("can fold single node", () => {
    let root = testTrees.testTree1().root;
    root.changeFold(emptyFun);
    expect(root.getVisibleDescendants()).to.containExactlyNodes(["com.tngtech"]);
  });

  it("can unfold single node", () => {
    let root = testTrees.testTree1().root;
    let allNodes1 = testTrees.allNodes(root);
    root.changeFold(emptyFun);
    root.changeFold(emptyFun);
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    let root = testTrees.testTree2().root;
    testTrees.getNode(root, "com.tngtech.test.subtest").changeFold(emptyFun);
    let toFold = testTrees.getNode(root, "com.tngtech.test");
    toFold.changeFold(emptyFun);
    toFold.changeFold(emptyFun);
    let exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(toFold.getVisibleDescendants()).to.containExactlyNodes(exp);
  });
});

describe("Tree", () => {
  it("creates a correct node map", () => {
    let root = testTrees.testTree1().root;
    let allNodes1 = testTrees.allNodes(root);
    expect(allNodes1.map(n => root.nodeMap.get(n))).to.containExactlyNodes(allNodes1);
  });

  it("returns correct visible nodes", () => {
    let root = testTrees.testTree1().root;
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.class2", "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("does the initial fold correct (fold each node except the root)", () => {
    let root = testTrees.testTree2().root;
    root.foldAllExceptRoot(emptyFun);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    //check if the hidden packages are also folded
    testTrees.getNode(root, "com.tngtech.test").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can traverse", () => {
    let root = testTrees.testTree1().root;
    expect(root.traverseTree()).to.equal("com.tngtech(main(class1, ), class2, class3, )");
  });

  it("can inclusively filter classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(true, false).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("main", emptyFun).by().simplename().filterPkgsOrClasses(true, false).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("main", emptyFun).by().fullname().filterPkgsOrClasses(false, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("Subtest", emptyFun).by().fullname().filterPkgsOrClasses(false, true).exclusive().matchCase(false);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, false).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, false).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(true, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("tEst", emptyFun).by().fullname().filterPkgsOrClasses(true, true).inclusive().matchCase(false);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("subtest", emptyFun).by().fullname().filterPkgsOrClasses(true, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });


  it("can inclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterClassesAndEliminatePkgs().inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    let res = root.getVisibleDescendants();
    expect(res).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("main", emptyFun).by().fullname().filterClassesAndEliminatePkgs().inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can reset filter", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, reset filter and unfold in this order", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    testTrees.getNode(root, "com.tngtech.main").changeFold(emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide interfaces", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByType(false, true, false, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1", "com.tngtech.class2"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide classes", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByType(true, false, false, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test", "com.tngtech.test.subtest",
      "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to show only packages", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByType(false, false, false, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide classes and eliminate packages", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByType(true, false, true, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide interfaces and eliminate packages", function () {
    let root = testTrees.testTree3().root;
    root.setDependencies(emptyDeps);
    root.filterByType(false, true, true, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can reset the type-filter", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByType(false, true, false, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    root.resetFilterByType(emptyFun);
    exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2", "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type and then filter by name", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);

    root.filterByType(false, true, false, emptyFun);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).exclusive().matchCase(true);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by name and then filter by type", function () {
    let root = testTrees.testTree2().root;
    root.setDependencies(emptyDeps);
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    root.filterByType(false, true, false, emptyFun);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });
});