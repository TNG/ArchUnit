'use strict';

require('./chai-extensions');
const expect = require("chai").expect;
const hierarchy = require("../../../main/resources/report/lib/d3.js").hierarchy;
const pack = require("../../../main/resources/report/lib/d3.js").pack().size([500, 500]).padding(100);
const jsonToRoot = require("../../../main/resources/report/tree.js").jsonToRoot;
const testJson = require("./test-json-creator");

let allNodes = root => root.getVisibleDescendants().map(n => n.projectData.fullname);

let emptyDeps = {
  changeFold: () => {
  },
  recalcEndCoordinatesOf: () => {
  },
  filter: filterfun => {
  },
  resetFilter: () => {
  }
};

let setupSimpleTestTree1 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass").build())
          .build())
      .add(testJson.clazz("class2", "class").build())
      .add(testJson.clazz("class3", "interface").build())
      .build();
  let root = jsonToRoot(simpleJsonTree);
  root.getVisibleDescendants().forEach(n => n.initVisual(0, 0, 0));
  root.setDepsForAll(emptyDeps);
  return root;
};

let setupSimpleTestTree2 = () => {
  let simpleJsonTree = testJson.package("com.tngtech")
      .add(testJson.package("main")
          .add(testJson.clazz("class1", "abstractclass").build())
          .build())
      .add(testJson.package("test")
          .add(testJson.clazz("testclass1").build())
          .add(testJson.package("subtest")
              .add(testJson.clazz("subtestclass1", "class").build())
              .build())
          .build())
      .add(testJson.clazz("class2", "class").build())
      .add(testJson.clazz("class3", "interface").build())
      .build();
  let root = jsonToRoot(simpleJsonTree);
  let d3root = hierarchy(root, d => d.currentChildren)
      .sum(d => d.currentChildren.length === 0 ? 10 : d.currentChildren.length)
      .sort((a, b) => b.value - a.value);
  d3root = pack(d3root);
  d3root.descendants().forEach(d => d.data.initVisual(d.x, d.y, d.r));
  root.setDepsForAll(emptyDeps);
  return root;
};

let getNode = (root, fullname) => root.nodeMap.get(fullname);

describe("Node", () => {
  it("knows if it is root", () => {
    let root = setupSimpleTestTree1();
    expect(root.isRoot()).to.equal(true);
    expect(getNode(root, "com.tngtech.class2").isRoot()).to.equal(false);
  });

  it("knows if it is current leaf", () => {
    let root = setupSimpleTestTree1();
    expect(root.isCurrentlyLeaf()).to.equal(false);
    expect(getNode(root, "com.tngtech.class2").isCurrentlyLeaf()).to.equal(true);
    root.changeFold();
    expect(root.isCurrentlyLeaf()).to.equal(true);
  });

  it("knows if it is child of an other node", () => {
    let root = setupSimpleTestTree1();
    expect(root.isChildOf(root.nodeMap.get("com.tngtech.class2"))).to.equal(false);
    expect(getNode(root, "com.tngtech.class2").isChildOf(root)).to.equal(true);
    expect(root.isChildOf(root)).to.equal(true);
  });

  it("can fold single node", () => {
    let root = setupSimpleTestTree1();
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(["com.tngtech"]);
  });

  it("can unfold single node", () => {
    let root = setupSimpleTestTree1();
    let allNodes1 = allNodes(root);
    root.changeFold();
    root.changeFold();
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    let root = setupSimpleTestTree2();
    getNode(root, "com.tngtech.test.subtest").changeFold();
    let toFold = getNode(root, "com.tngtech.test");
    toFold.changeFold();
    toFold.changeFold();
    let exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(toFold.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("adapts radius on folding to minimum radius on the same level", () => {
    let root = setupSimpleTestTree2();
    let toFold = getNode(root, "com.tngtech.main");
    let expRadius = Math.min.apply(Math, [getNode(root, "com.tngtech.class2").visualData.r,
      getNode(root, "com.tngtech.class3").visualData.r, getNode(root, "com.tngtech.test").visualData.r]);
    toFold.changeFold();
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("reset radius on unfolding", () => {
    let root = setupSimpleTestTree2();
    let toFold = getNode(root, "com.tngtech.main");
    let expRadius = toFold.visualData.r;
    toFold.changeFold();
    toFold.changeFold();
    expect(toFold.visualData.r).to.equal(expRadius);
  });

  it("can be dragged", () => {
    let root = setupSimpleTestTree2();
    let toDrag = getNode(root, "com.tngtech.class2");
    let dx = 1, dy = -3;
    let newX = toDrag.visualData.x + dx, newY = toDrag.visualData.y + dy;
    toDrag.drag(dx, dy);
    expect(toDrag.visualData.x).to.equal(newX);
    expect(toDrag.visualData.y).to.equal(newY);
  });

  it("drags also its children if it is dragged", () => {
    let root = setupSimpleTestTree2();
    let allNodes2 = allNodes(root);
    let toDrag = getNode(root, "com.tngtech.test");
    let dx = 1, dy = -3;
    let exp = new Map();
    allNodes2.forEach(n => {
      let node = getNode(root, n);
      let xy = [node.visualData.x, node.visualData.y];
      if (node.isChildOf(toDrag)) {
        xy[0] += dx;
        xy[1] += dy;
      }
      exp.set(n, xy);
    });
    toDrag.drag(dx, dy);
    expect(root.getVisibleDescendants()).to.haveExactlyPositions(exp);
  });

  it("cannot be dragged out of its parent", () => {
    let root = setupSimpleTestTree2();
    let toDrag = getNode(root, "com.tngtech.test.subtest.subtestclass1");
    let parent = getNode(root, "com.tngtech.test.subtest");
    let dx = toDrag.visualData.x + parent.visualData.r, dy = 5;
    let newX = toDrag.visualData.x, newY = toDrag.visualData.y;
    toDrag.drag(dx, dy);
    expect(toDrag.visualData.x).to.equal(newX);
    expect(toDrag.visualData.y).to.equal(newY);
  });

  it("is dragged automatically back into its parent on unfolding, so that it is completely within its parent", () => {
    let root = setupSimpleTestTree2();
    let toDrag = getNode(root, "com.tngtech.test.subtest");
    let parent = getNode(root, "com.tngtech.test");
    //move toDrag to the middle of parent
    toDrag.drag(parent.visualData.x - toDrag.visualData.x, parent.visualData.y - toDrag.visualData.y);

    let newDelta = Math.sqrt((parent.visualData.r - toDrag.visualData.r) / 2);
    let newX = toDrag.visualData.x + newDelta, newY = toDrag.visualData.y - newDelta;
    toDrag.changeFold();
    let delta = Math.sqrt((parent.visualData.r - toDrag.visualData.r - 0.5) / 2);
    //move toDrag to the right upper corner of parent
    toDrag.drag(delta, -delta);
    toDrag.changeFold();
    expect(toDrag.visualData.x).to.be.within(newX - 2, newX + 2);
    expect(toDrag.visualData.y).to.be.within(newY - 2, newY + 2);
  });
});

describe("Tree", () => {
  it("creates a correct node map", () => {
    let root = setupSimpleTestTree1();
    let allNodes1 = allNodes(root);
    expect(allNodes1.map(n => root.nodeMap.get(n))).to.containExactlyNodes(allNodes1);
  });

  it("returns correct visible nodes", () => {
    let root = setupSimpleTestTree1();
    let allNodes1 = allNodes(root);
    expect(root.getVisibleDescendants()).to.containExactlyNodes(allNodes1);
    getNode(root, "com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("does the initial fold correct (fold each node except the root)", () => {
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

  it("can traverse", () => {
    let root = setupSimpleTestTree1();
    expect(root.traverseTree()).to.equal("tngtech(main(class1, ), class2, class3, )");
  });

  it("can inclusively filter classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, false, false, true, true, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, false, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, false, true, false, true, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("main", false, false, true, false, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("main", true, false, false, true, true, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("Subtest", true, false, false, true, false, false);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", true, false, true, false, true, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", true, false, true, false, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, false, true, true, true, true);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, false, true, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter packages and classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("tEst", true, false, true, true, true, false);
    let exp = ["com.tngtech", "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter packages and classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("subtest", true, false, true, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });


  it("can inclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, true, false, true, true, true);
    let exp = ["com.tngtech", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    let res = root.getVisibleDescendants();
    expect(res).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by name", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("test", false, true, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can inclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("main", true, true, false, true, true, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can exclusively filter classes and eliminate packages without matching classes by fullname", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("subtest", true, true, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can reset filter", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("subtest", true, true, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilter();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("subtest", true, true, false, true, false, true);
    getNode(root, "com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    getNode(root, "com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilter();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can filter, fold, reset filter and unfold in this order", function () {
    let root = setupSimpleTestTree2();
    root.filterAll("subtest", true, true, false, true, false, true);
    getNode(root, "com.tngtech.main").changeFold();
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilter();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    getNode(root, "com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    let root = setupSimpleTestTree2();
    getNode(root, "com.tngtech.main").changeFold();
    root.filterAll("subtest", true, true, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    getNode(root, "com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilter();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    let root = setupSimpleTestTree2();
    getNode(root, "com.tngtech.main").changeFold();
    root.filterAll("subtest", true, true, false, true, false, true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
      "com.tngtech.test.testclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    root.resetFilter();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
    getNode(root, "com.tngtech.main").changeFold();
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
      "com.tngtech.test.subtest.subtestclass1"];
    expect(root.getVisibleDescendants()).to.containExactlyNodes(exp);
  });
});