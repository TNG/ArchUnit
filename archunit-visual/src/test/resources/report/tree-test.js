'use strict';

require('./chai-extensions');
const expect = require("chai").expect;
const hierarchy = require("../../../main/resources/report/lib/d3.js").hierarchy;
const pack = require("../../../main/resources/report/lib/d3.js").pack().size([500, 500]).padding(100);
const jsonToRoot = require("../../../main/resources/report/tree.js").jsonToRoot;
const testJson = require("./test-json-creator");

let allNodes1 = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
  "com.tngtech.main.class1"];
let setupSimpleTestTree1 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass"))
          .build())
      .add(testJson.clazz("class2", "class"))
      .add(testJson.clazz("class3", "interface"))
      .build();
  let root = jsonToRoot(simpleJsonTree, []);
  root.getVisibleDescendants().forEach(n => n.initVisual(0, 0, 0));
  return root;
};

let allNodes2 = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
  "com.tngtech.main.class1", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
  "com.tngtech.test.subtest.subtestclass1"];
let setupSimpleTestTree2 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass"))
          .build())
      .add(testJson.package("test")
          .add(testJson.clazz("testclass1"))
          .add(testJson.package("subtest")
              .add(testJson.clazz("subtestclass1", "class"))
              .build())
          .build())
      .add(testJson.clazz("class2", "class"))
      .add(testJson.clazz("class3", "interface"))
      .build();
  let root = jsonToRoot(simpleJsonTree, []);
  let d3root = hierarchy(root, d => d.currentChildren)
      .sum(d => d.currentChildren.length === 0 ? 10 : d.currentChildren.length)
      .sort((a, b) => b.value - a.value);
  d3root = pack(d3root);
  d3root.descendants().forEach(d => d.data.initVisual(d.x, d.y, d.r));
  return root;
};

let getNode = (root, fullname) => root.nodeMap.get(fullname);

describe("Node", function () {
  it("knows if it is root", function () {
    let root = setupSimpleTestTree1();
    expect(root.isRoot()).to.equal(true);
    expect(getNode(root, "com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is current leaf", function () {
    let root = setupSimpleTestTree1();
    expect(root.isCurrentLeaf()).to.equal(false);
    expect(getNode(root, "com.tngtech.class2").isCurrentLeaf()).to.equal(true);
    root.changeFold();
    expect(root.isCurrentLeaf()).to.equal(true);
  });

  it("knows if it is child of an other node", function () {
    let root = setupSimpleTestTree1();
    expect(root.isChildOf(root.nodeMap.get("com.tngtech.class2"))).to.equal(false);
    expect(getNode(root, "com.tngtech.class2").isChildOf(root)).to.equal(true);
    expect(root.isChildOf(root)).to.equal(true);
  });

  it("can fold single node", function () {
    let root = setupSimpleTestTree1();
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(["com.tngtech"]);
  });

  it("can unfold single node", function () {
    let root = setupSimpleTestTree1();
    root.changeFold();
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
  });

  it("can be folded and unfolded without changing the fold-state of its children", function () {
    let root = setupSimpleTestTree2();
    getNode(root, "com.tngtech.test.subtest").changeFold();
    getNode(root, "com.tngtech.test").changeFold();
    getNode(root, "com.tngtech.test").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.main.class1", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("adapts radius on folding to minimum radius on the same level", function () {
    let root = setupSimpleTestTree2();
    let toFold = getNode(root, "com.tngtech.main");
    let expRadius = Math.min.apply(Math, [getNode(root, "com.tngtech.class2").visualData.r,
      getNode(root, "com.tngtech.class3").visualData.r, getNode(root, "com.tngtech.test").visualData.r]);
    toFold.changeFold();
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("reset radius on unfolding", function () {
    let root = setupSimpleTestTree2();
    let toFold = getNode(root, "com.tngtech.main");
    let expRadius = toFold.visualData.r;
    toFold.changeFold();
    toFold.changeFold();
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("can be dragged", function () {
    let root = setupSimpleTestTree2();
  });
});

describe("Tree", function () {
  it("creates a correct node map", function () {
    let root = setupSimpleTestTree1();
    expect(allNodes1.map(n => root.nodeMap.get(n))).to.containExactlyNodes(allNodes1);
  });

  it("returns correct visible nodes", function () {
    let root = setupSimpleTestTree1();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
    getNode(root, "com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("does the initial fold correct (fold each node except the root)", function () {
    let root = setupSimpleTestTree2();
    root.foldAllExceptRoot();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    //check if the hidden packages are also folded
    getNode(root, "com.tngtech.test").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can traverse", function () {
    let root = setupSimpleTestTree1();
    expect(root.traverseTree()).to.equal("tngtech(main(class1, ), class2, class3, )");
  });
});

describe("Dependencies", function () {

});