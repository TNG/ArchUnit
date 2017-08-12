'use strict';

const expect = require("chai").expect;
require('./chai/tree-chai-extensions');

const testJson = require("./test-json-creator");
const jsonToRoot = require('./main-files').get('tree').jsonToRoot;
const testObjects = require("./test-object-creator.js");

describe("Node", () => {
  it("knows if it is root", () => {
    const tree = testObjects.testTree1();
    expect(tree.root.isRoot()).to.equal(true);
    expect(tree.getNode("com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is current leaf", () => {
    const root = testObjects.testTree1().root;
    expect(root.isCurrentlyLeaf()).to.equal(false);

    root.changeFold();
    expect(root.isCurrentlyLeaf()).to.equal(true);
  });

  it("knows if it is child of an other node", () => {
    const tree = testObjects.testTree1();
    expect(tree.root.isChildOf(tree.getNode("com.tngtech.class2"))).to.equal(false);
    expect(tree.getNode("com.tngtech.class2").isChildOf(tree.root)).to.equal(true);
    expect(tree.root.isChildOf(tree.root)).to.equal(true);
  });

  it("can fold single node", () => {
    const root = testObjects.testTree1().root;
    root.changeFold();
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(["com.tngtech"]);
  });

  it("can unfold single node", () => {
    const root = testObjects.testTree1().root;
    const allNodes1 = testObjects.allNodes(root);
    root.changeFold();
    root.changeFold();
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(allNodes1);
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.test.subtest").changeFold();
    const toFold = tree.getNode("com.tngtech.test");
    toFold.changeFold();
    toFold.changeFold();
    const exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(toFold.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("Adds CSS to make the mouse a pointer, if there are children to unfold", () => {
    const tree = testJson.package("com.tngtech")
      .add(testJson.clazz("Class1", "abstractclass").build())
      .build();

    const root = jsonToRoot(tree);

    expect(root.getClass()).to.contain(' foldable');
    expect(root.getClass()).not.to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).not.to.contain(' foldable');
  });
});

describe("Tree", () => {
  it("returns correct visible nodes", () => {
    const tree = testObjects.testTree1();
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.class2", "com.tngtech.class3"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("does the initial fold correct (fold each node except the root)", () => {
    const tree = testObjects.testTree2();
    tree.root.foldAllNodes(() => {
    });
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    //check if the hidden packages are also folded
    tree.getNode("com.tngtech.test").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can inclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("main", false);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can exclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    const exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can reset filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    tree.getNode("com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter, fold, reset filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    tree.getNode("com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main").changeFold();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main").changeFold();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.root.filterByName("", false);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    tree.getNode("com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide interfaces", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1", "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter out everything by type except the root node", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, false);
    const exp = ["com.tngtech"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide classes and eliminate packages", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type to hide interfaces and eliminate packages", function () {
    const root = testObjects.testTree3().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can reset the type-filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.resetFilterByType();
    exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2", "com.tngtech.class3"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by type and then filter by name", function () {
    const root = testObjects.testTree2().root;

    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.filterByName("test", true);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  it("can filter by name and then filter by type", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("test", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

    root.filterByType(false, true);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
  });

  //FIXME: more tests, especially for different cases of node filter input
});