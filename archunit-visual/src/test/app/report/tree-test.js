'use strict';

const expect = require('chai').expect;
require('./chai/tree-chai-extensions');
require('./chai/tree-visualizer-chai-extensions');
require('./chai/node-chai-extensions');

const testObjects = require("./test-object-creator.js");
const testTree = testObjects.tree;

const stubs = require('./stubs');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(10),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub
});
const circlePadding = appContext.getVisualizationStyles().getCirclePadding();
const Node = appContext.getNode();
const testJson = require("./test-json-creator");

describe('Root', () => {
  it('should have no parent', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit').build();
    const root = new Node(jsonRoot);
    expect(root.getParent()).to.equal(undefined);
  });

  it('should know that it is the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.isRoot()).to.equal(true);
  });

  it('should not fold or change its fold-state', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit').build();
    const root = new Node(jsonRoot);
    root.foldIfInnerNode();
    expect(root.isFolded()).to.equal(false);
    root._changeFoldIfInnerNodeAndRelayout();
    expect(root.isFolded()).to.equal(false);
  });
});

describe('Inner node', () => {
  it('can fold', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');
    innerNode.foldIfInnerNode();
    expect(innerNode.isFolded()).to.equal(true);
    expect(innerNode._originalChildren.map(node => node._view._isVisible)).to.not.include(true);
    expect(listenerStub.foldedNode()).to.equal(innerNode);
  });

  it('can change the fold-state to folded', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');
    innerNode._changeFoldIfInnerNodeAndRelayout();
    expect(innerNode.isFolded()).to.equal(true);
    expect(innerNode._originalChildren.map(node => node._view._isVisible)).to.not.include(true);
    expect(listenerStub.foldedNode()).to.equal(innerNode);
    root.doNext(() => expect(listenerStub.onLayoutChangedWasCalled()).to.equal(true));
  });

  it('can change the fold-state to unfolded', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.package('test')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const listenerStub = stubs.NodeListenerStub();
    root.addListener(listenerStub);
    const innerNode = root.getByName('com.tngtech.archunit.test');
    innerNode._changeFoldIfInnerNodeAndRelayout();
    innerNode._changeFoldIfInnerNodeAndRelayout();
    expect(innerNode.isFolded()).to.equal(false);
    const promises = [];
    promises.push(root.doNext(() => expect(innerNode._originalChildren.map(node => node._view._isVisible)).to.not.include(false)));
    expect(listenerStub.foldedNode()).to.equal(innerNode);
    promises.push(root.doNext(() => expect(listenerStub.onLayoutChangedWasCalled()).to.equal(true)));
    return Promise.all(promises);
  });
});

describe('Leaf', () => {
  it('should not fold or change its fold-state', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    const leaf = root.getByName('com.tngtech.archunit.SomeClass');
    leaf.foldIfInnerNode();
    expect(leaf.isFolded()).to.equal(false);
    leaf._changeFoldIfInnerNodeAndRelayout();
    expect(leaf.isFolded()).to.equal(false);
  });
});

describe('Inner node or leaf', () => {
  it('should know its parent', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.getByName('com.tngtech.archunit.SomeClass').getParent()).to.equal(root);
  });

  it('should know that is not the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    expect(root.getByName('com.tngtech.archunit.SomeClass').isRoot()).to.equal(false);
  });

  it('can be dragged', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.relayout();

    const nodeToDrag = root.getByName('com.tngtech.archunit.visual.SomeClass');
    const dx = -5;
    const dy = 5;
    const expCoordinates = {x: dx, y: dy};

    nodeToDrag._drag(dx, dy);
    return root.doNext(() =>
      expect({x: nodeToDrag.visualData.x, y: nodeToDrag.visualData.y}).to.deep.equal(expCoordinates));
  });

  it('can be dragged anywhere if it is a child of the root', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .build();
    const root = new Node(jsonRoot);
    root.relayout();

    const nodeToDrag = root.getByName('com.tngtech.archunit.SomeClass');
    const dx = -100;
    const dy = 100;
    const expCoordinates = {x: dx, y: dy};
    nodeToDrag._drag(dx, dy);
    return root.doNext(() =>
      expect({x: nodeToDrag.visualData.x, y: nodeToDrag.visualData.y}).to.deep.equal(expCoordinates));
  });

  it('is shifted to the rim of the parent if it dragged out of its parent and the parent is not the root', () => {
    const MAXIMUM_DELTA = 0.0001;
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.relayout();
    const nodeToDrag = root.getByName('com.tngtech.archunit.visual.SomeClass');

    nodeToDrag._drag(-50, 50);
    return root.doNext(() => {
      const expD = Math.trunc(Math.sqrt(Math.pow(nodeToDrag.getParent().getRadius() - nodeToDrag.getRadius(), 2)/2));
      const expCoordinates = {x: -expD, y: expD};

      expect(nodeToDrag.visualData.x).to.closeTo(expCoordinates.x, MAXIMUM_DELTA);
      expect(nodeToDrag.visualData.y).to.closeTo(expCoordinates.y, MAXIMUM_DELTA);
    });
  });
});

describe('Node layout', () => {
  it('should put every child node within its parent node considering the padding', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .add(testJson.clazz('SomeClass2', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .add(testJson.clazz('SomeClass3', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.relayout();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          expect(node).to.locatedWithinWithPadding(node.getParent(), circlePadding);
        }
      });
    });
  });

  it('should not make two siblings overlap', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass1', 'class').build())
      .add(testJson.clazz('SomeClass2', 'class').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass1', 'class').build())
        .add(testJson.clazz('SomeClass2', 'class').build())
        .add(testJson.clazz('SomeClass3', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    root.relayout();
    return root.doNext(() => {
      root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          node.getParent().getOriginalChildren().filter(child => child != node).forEach(sibling =>
            expect(node).to.notOverlapWith(sibling, 2 * circlePadding));
        }
      });
    });
  });
});

describe('Node', () => {
  it('creates the correct tree-structure', () => {
    const jsonRoot = testJson.package('com.tngtech.archunit')
      .add(testJson.clazz('SomeClass', 'class').build())
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('visual')
        .add(testJson.clazz('SomeClass', 'class').build())
        .build())
      .add(testJson.package('test')
        .add(testJson.clazz('SomeTestClass', 'class')
          .havingInnerClass(testJson.clazz('SomeInnerClass', 'class').build())
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const exp = ['com.tngtech.archunit(package)', 'com.tngtech.archunit.SomeClass(class)',
      'com.tngtech.archunit.SomeInterface(interface)', 'com.tngtech.archunit.visual(package)',
      'com.tngtech.archunit.visual.SomeClass(class)', 'com.tngtech.archunit.test(package)',
      'com.tngtech.archunit.test.SomeTestClass(class)', 'com.tngtech.archunit.test.SomeTestClass$SomeInnerClass(class)'];
    const act = root.getSelfAndDescendants().map(node => `${node.getFullName()}(${node._description.type})`);
    expect(act).to.deep.equal(exp);
  });

  it("Adds CSS to make the mouse a pointer, if there are children to unfold", () => {
    const jsonRoot = testJson.package("com.tngtech")
      .add(testJson.clazz("Class1", "abstractclass").build())
      .build();

    const root = new Node(jsonRoot);

    expect(root.getClass()).to.contain(' foldable');
    expect(root.getClass()).not.to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).to.contain(' not-foldable');
    expect(root.getCurrentChildren()[0].getClass()).not.to.contain(' foldable');
  });

  //----------------------------------updated until here-------------------------------------

  it("knows if it is currently leaf", () => {
    const tree = testObjects.testTree1();
    const node = tree.getNode('com.tngtech.main');
    expect(node.isCurrentlyLeaf()).to.equal(false);
    node._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => expect(node.isCurrentlyLeaf()).to.equal(true));
  });

  it("can fold single node", () => {
    const tree = testObjects.testTree1();
    const node = tree.getNode('com.tngtech.main');
    node._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => expect(node.getSelfAndDescendants()).to.containOnlyNodes(["com.tngtech.main"]));
  });

  it("can unfold single node", () => {
    const tree = testObjects.testTree1();
    const node = tree.getNode('com.tngtech.main');
    const allNodes1 = ['com.tngtech.main', 'com.tngtech.main.class1'];
    node._changeFoldIfInnerNodeAndRelayout();
    node._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => expect(node.getSelfAndDescendants()).to.containOnlyNodes(allNodes1));
  });

  it("can be folded and unfolded without changing the fold-state of its children", () => {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.test.subtest")._changeFoldIfInnerNodeAndRelayout();
    const toFold = tree.getNode("com.tngtech.test");
    const exp = ["com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest"];
    toFold._changeFoldIfInnerNodeAndRelayout();
    toFold._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => expect(toFold.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });
});

describe("Tree", () => {
  it("returns correct visible nodes", () => {
    const tree = testObjects.testTree1();
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.class2", "com.tngtech.class3"];
    expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
    exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main"];
    tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can inclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("main", false);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can exclusively filter classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    const exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can reset filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

      root.filterByName("", false);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
        "com.tngtech.test.subtest.subtestclass1"];
      return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
    });
  });

  it("can filter, fold, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => {
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
        "com.tngtech.test", "com.tngtech.test.testclass1"];
      tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
      return tree.root.doNext(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        tree.root.filterByName("", false);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
          "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
          "com.tngtech.test.subtest.subtestclass1"];
        return tree.root.doNext(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
      });
    });
  });

  //FIXME: make test less ugly
  it("can filter, fold, reset filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.root.filterByName("subtest", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
      "com.tngtech.test", "com.tngtech.test.testclass1"];
    tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => {
      expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      tree.root.filterByName("", false);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1"];
      return tree.root.doNext(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
          "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
          "com.tngtech.test.subtest.subtestclass1"];
        tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
        return tree.root.doNext(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
      });
    });
  });

  it("can fold, filter, unfold and reset filter in this order", function () {
    const tree = testObjects.testTree2();
    //FIXME: at the end filtering should consider the promise in node by itsef
    tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
    return tree.root.doNext(() => {
      tree.root.filterByName("subtest", true);
      let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1"];
      return tree.root.doNext(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
          "com.tngtech.test", "com.tngtech.test.testclass1"];
        tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout();
        return tree.root.doNext(() => {
          expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
          tree.root.filterByName("", false);
          exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
            "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
            "com.tngtech.test.subtest.subtestclass1"];
          return tree.root.doNext(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
        });
      });
    });
  });

  it("can fold, filter, reset the filter and unfold in this order", function () {
    const tree = testObjects.testTree2();
    tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout()
    return tree.root.doNext(() => {
      tree.root.filterByName("subtest", true);
      let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.test",
        "com.tngtech.test.testclass1"];
      return tree.root.doNext(() => {
        expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
        tree.root.filterByName("", false);
        exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main",
          "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
          "com.tngtech.test.subtest.subtestclass1"];
        return tree.root.doNext(() => {
          expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp);
          exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1",
            "com.tngtech.test", "com.tngtech.test.testclass1", "com.tngtech.test.subtest",
            "com.tngtech.test.subtest.subtestclass1"];
          tree.getNode("com.tngtech.main")._changeFoldIfInnerNodeAndRelayout()
          return tree.root.doNext(() => expect(tree.root.getSelfAndDescendants()).to.containOnlyNodes(exp));
        });
      });
    });
  });

  it("can filter by type to hide interfaces", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1", "com.tngtech.class2"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can filter by type to hide classes", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can filter out everything by type except the root node", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, false);
    const exp = ["com.tngtech"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can filter by type to hide classes and eliminate packages", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(true, false);
    const exp = ["com.tngtech", "com.tngtech.class3"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can filter by type to hide interfaces and eliminate packages", function () {
    const root = testObjects.testTree3().root;
    root.filterByType(false, true);
    const exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1"];
    return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
  });

  it("can reset the type-filter", function () {
    const root = testObjects.testTree2().root;
    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);
      root.filterByType(true, true);
      exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
        "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
        "com.tngtech.class2", "com.tngtech.class3"];
      return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
    });
  });

  it("can filter by type and then filter by name", function () {
    const root = testObjects.testTree2().root;

    root.filterByType(false, true);
    let exp = ["com.tngtech", "com.tngtech.main", "com.tngtech.main.class1", "com.tngtech.test",
      "com.tngtech.test.testclass1", "com.tngtech.test.subtest", "com.tngtech.test.subtest.subtestclass1",
      "com.tngtech.class2"];
    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp)

      root.filterByName("test", true);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
      return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
    });
  });

  it("can filter by name and then filter by type", function () {
    const root = testObjects.testTree2().root;
    root.filterByName("test", true);
    let exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.class3", "com.tngtech.main", "com.tngtech.main.class1"];
    return root.doNext(() => {
      expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp);

      root.filterByType(false, true);
      exp = ["com.tngtech", "com.tngtech.class2", "com.tngtech.main", "com.tngtech.main.class1"];
      return root.doNext(() => expect(root.getSelfAndDescendants()).to.containOnlyNodes(exp));
    });
  });

  //FIXME: more tests, especially for different cases of node filter input
});


// FIXME: Define these constants, that need to match production code, but can't be accessed from tests, in a central spot
const CIRCLE_TEXT_PADDING = 5;
const CIRCLE_PADDING = testObjects.visualizationStyles.getCirclePadding();

// FIXME: These tests should really better communicate what they're actually testing, and what the preconditions are
describe("Layout of nodes", () => {
  it("draws text within node circles", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkText = node => {
      expect(node).to.haveTextWithinCircle(stubs.calculateTextWidthStub, CIRCLE_TEXT_PADDING, 0);
      node.getOriginalChildren().forEach(c => checkText(c));
    };
    return graphWrapper.graph.root.doNext(() => checkText(graphWrapper.graph.root));
  });

  it("draws children within parent", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkLayout = node => {
      expect(node).to.haveChildrenWithinCircle(CIRCLE_PADDING);
      node.getOriginalChildren().forEach(c => checkLayout(c));
    };
    return graphWrapper.graph.root.doNext(() => checkLayout(graphWrapper.graph.root));
  });

  it("draws children without overlap", () => {
    const graphWrapper = testObjects.testGraph2();
    const checkLayout = node => {
      expect(node.getOriginalChildren()).to.doNotOverlap(CIRCLE_PADDING);
      node.getOriginalChildren().forEach(c => checkLayout(c));
    };
    return graphWrapper.graph.root.doNext(() => checkLayout(graphWrapper.graph.root));
  });
});

describe("Dragging nodes", () => {
  it("can be dragged", () => {
    const tree = testTree('root.SomeClass1', 'root.SomeClass2');

    const toDrag = tree.getByName('root.SomeClass1');
    const dx = 1;
    const dy = -3;
    const expected = {x: toDrag.visualData.x + dx, y: toDrag.visualData.y + dy};

    toDrag._drag(dx, dy);

    return tree.doNext(() => expect({
      x: toDrag.visualData.x,
      y: toDrag.visualData.y
    }).to.deep.equal(expected));
  });

  const expectedCoordsAfterDrag = (root, toDrag, dx, dy) => {
    const result = new Map(root.getSelfAndDescendants().map(node => [node, node.getAbsoluteCoords()]));
    toDrag.getSelfAndDescendants().forEach(node => {
      const coords = result.get(node);
      coords.x += dx;
      coords.y += dy;
    });
    return result;
  };

  it("drags along its children, if it is dragged", () => {
    const root = testTree(
      'root.SomeClass',
      'root.sub.SubClass1',
      'root.sub.SubClass2');

    const toDrag = root.getByName('root.sub');
    const dx = -5, dy = 4;

    const expectedCoordsByNode = expectedCoordsAfterDrag(root, toDrag, dx, dy);

    toDrag._drag(dx, dy);

    return root.doNext(() => {
      root.getSelfAndDescendants().forEach(node => {
        expect(node.getAbsoluteCoords()).to.deep.equal(expectedCoordsByNode.get(node))
      });
    });
  });

  it("can't be dragged out of its parent", () => {
    const tree = testTree(
      'root.other',
      'root.parent.SomeClass1',
      'root.parent.SomeClass2');

    const toDrag = tree.getByName('root.parent.SomeClass1');
    const parent = toDrag.getParent();
    expect(parent).not.to.be.root();

    const deltaOutsideOfParent = 2 * parent.getRadius() + 1;

    toDrag._drag(deltaOutsideOfParent, 0);
    expect(toDrag).to.be.locatedWithin(parent);

    toDrag._drag(0, deltaOutsideOfParent);
    expect(toDrag).to.be.locatedWithin(parent);
  });

  it("can be dragged anywhere, if parent is root", () => {
    const tree = testTree(
      'root.SomeClass1',
      'root.SomeClass2');

    const toDrag = tree.getByName('root.SomeClass1');
    const parent = toDrag.getParent();
    expect(parent).to.be.root();

    const oldX = toDrag.visualData.x;
    const oldY = toDrag.visualData.y;
    const deltaOutsideOfParent = 2 * parent.getRadius() + 1;

    toDrag._drag(deltaOutsideOfParent, 0);

    return tree.doNext(() => {
      expect(toDrag.visualData.x).to.equal(oldX + deltaOutsideOfParent);

      toDrag._drag(0, deltaOutsideOfParent);
      return tree.doNext(() => {
        expect(toDrag.visualData.y).to.equal(oldY + deltaOutsideOfParent);
      });
    });
  });
});