'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testObjects = require("./test-object-creator.js");

const emptyFun = () => {
};

describe("Node", () => {

  it("knows if it is root", () => {
    let tree = testObjects.testTree1();
    expect(tree.root.isRoot()).to.equal(true);
    expect(tree.getNode("com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is leaf", () => {
    let tree = testObjects.testTree1();
    expect(tree.root.isLeaf()).to.equal(false);

    tree.root.changeFold();
    expect(tree.root.isLeaf()).to.equal(false);
    expect(tree.getNode("com.tngtech.class2").isLeaf()).to.equal(true);
  });

  it("knows if it is current leaf", () => {
    let root = testObjects.testTree1().root;
    expect(root.isCurrentlyLeaf()).to.equal(false);

    root.changeFold();
    expect(root.isCurrentlyLeaf()).to.equal(true);
  });

  it("knows if it is child of an other node", () => {
    let tree = testObjects.testTree1();
    expect(tree.root.isChildOf(tree.getNode("com.tngtech.class2"))).to.equal(false);
    expect(tree.getNode("com.tngtech.class2").isChildOf(tree.root)).to.equal(true);
    expect(tree.root.isChildOf(tree.root)).to.equal(true);
  });

  it("can fold single node", () => {
    let root = testObjects.testTree1().root;
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(["com.tngtech"]);
  });

  it("can unfold single node", () => {
    let root = testObjects.testTree1().root;
    let allNodes1 = testObjects.allNodes(root);
    root.changeFold();
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    let tree = testObjects.testTree2();
    tree.getNode("com.tngtech.test.subtest").changeFold();
    let toFold = tree.getNode("com.tngtech.test");
    toFold.changeFold();
    toFold.changeFold();
    let exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(toFold.getVisibleDescendants()).to.containExactlyNodes(exp);
  });
});

describe("Tree", () => {
  it("returns correct visible nodes", () => {
    let tree = testObjects.testTree1();
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.class2", "com.tngtech.class3"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);

    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("does the initial fold correct (fold each node except the root)", () => {
    let tree = testObjects.testTree2();
    tree.root.foldAllNodes(node => {
    });
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    //check if the hidden packages are also folded
    tree.getNode("com.tngtech.test").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can traverse", () => {
    let root = testObjects.testTree1().root;
    expect(root.traverseTree()).to.equal("com.tngtech(main(class1, ), class2, class3, )");
  });

  it("can inclusively filter classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(true, false).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("main", emptyFun).by().simplename().filterPkgsOrClasses(true, false).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("main", emptyFun).by().fullname().filterPkgsOrClasses(false, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("Subtest", emptyFun).by().fullname().filterPkgsOrClasses(false, true).exclusive().matchCase(false);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, false).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, false).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(true, true).inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().fullname().filterPkgsOrClasses(true, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("tEst", emptyFun).by().fullname().filterPkgsOrClasses(true, true).inclusive().matchCase(false);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("subtest", emptyFun).by().fullname().filterPkgsOrClasses(true, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });


  it("can inclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterClassesAndEliminatePkgs().inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    let res = root.getVisibleDescendants();
    expect(res).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("main", emptyFun).by().fullname().filterClassesAndEliminatePkgs().inclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can reset filter", function () {
    let root = testObjects.testTree2().root;
    root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilterByName();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    let tree = testObjects.testTree2();
    tree.root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    tree.getNode("com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.root.resetFilterByName();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, reset filter and unfold in this order", function () {
    let tree = testObjects.testTree2();
    tree.root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    tree.getNode("com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.root.resetFilterByName();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    let tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main").changeFold();
    tree.root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.root.resetFilterByName();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    let tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main").changeFold();
    tree.root.filterByName("subtest", emptyFun).by().fullname().filterClassesAndEliminatePkgs().exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.root.resetFilterByName();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide interfaces", function () {
    let root = testObjects.testTree2().root;
    root.filterByType(false, true, false);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1", "com.tngtech.class2"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide classes", function () {
    let root = testObjects.testTree2().root;
    root.filterByType(true, false, false);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test", "com.tngtech.test.subtest",
      "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to show only packages", function () {
    let root = testObjects.testTree2().root;
    root.filterByType(false, false, false);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide classes and eliminate packages", function () {
    let root = testObjects.testTree2().root;
    root.filterByType(true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type to hide interfaces and eliminate packages", function () {
    let root = testObjects.testTree3().root;
    root.filterByType(false, true, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can reset the type-filter", function () {
    let root = testObjects.testTree2().root;
    root.filterByType(false, true, false);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    root.resetFilterByType();
    exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2", "com.tngtech.class3"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter by type and then filter by name", function () {
    let root = testObjects.testTree2().root;

    root.filterByType(false, true, false);
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
    let root = testObjects.testTree2().root;
    root.filterByName("test", emptyFun).by().simplename().filterPkgsOrClasses(false, true).exclusive().matchCase(true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);

    root.filterByType(false, true, false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });
});