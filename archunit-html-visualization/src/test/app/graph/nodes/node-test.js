'use strict';

const chai = require('chai');
const expect = chai.expect;
const chaiExtensions = require('../testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);

require('../testinfrastructure/node-chai-extensions');

const rootCreator = require('../testinfrastructure/root-creator');
const {buildFilterCollection} = require("../../../../main/app/graph/filter");
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;
const filterOn = require('../testinfrastructure/node-filter-test-infrastructure').filterOn;

const testWholeLayoutOn = require('../testinfrastructure/node-layout-test-infrastructure').testWholeLayoutOn;
const testGui = require('../testinfrastructure/node-gui-adapter').testGuiFromRoot;

const vectors = require('../../../../main/app/graph/infrastructure/vectors').vectors;

const MAXIMUM_DELTA = 0.0001;

describe('Root', () => {
  it('draws all nodes', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.firstPkg.FirstClass$SomeInnerClass',
      'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass$SomeInnerClass', 'my.company.secondPkg.FirstClass$SomeInnerClass',
      'my.company.secondPkg.SecondClass', 'my.company.secondPkg.ThirdClass$SomeInnerClass', 'my.company.thirdPkg.SomeClass');

    testGui(root).test.that.onlyNodesAre('my.company.firstPkg.FirstClass$SomeInnerClass',
      'my.company.firstPkg.SecondClass', 'my.company.firstPkg.ThirdClass$SomeInnerClass', 'my.company.secondPkg.FirstClass$SomeInnerClass',
      'my.company.secondPkg.SecondClass', 'my.company.secondPkg.ThirdClass$SomeInnerClass', 'my.company.thirdPkg.SomeClass');
  });
});

describe("Node's visual properties are initialized correctly", () => {
  it('Leaves have the css-class "unfoldable"', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.OtherClass$InnerClass');

    testGui(root).test.that.node('my.company.SomeClass').is.markedAs.unfoldable()
      .and.that.node('my.company.OtherClass$InnerClass').is.markedAs.unfoldable();
  });

  it('Inner nodes have the css-class "foldable"', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass');

    testGui(root).test.that.node('my.company').is.markedAs.foldable()
      .and.that.node('my.company.SomeClass').is.markedAs.foldable();
  });
});

describe("Node's geometrical properties", () => {
  it('#getRadius() corresponds to drawn radius', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass');

    const inspectTestGui = testGui(root).inspect;

    expect(root.getByName('my.company.SomeClass').getRadius()).to.equal(inspectTestGui.radiusOf('my.company.SomeClass'));
    expect(root.getByName('my.company').getRadius()).to.equal(inspectTestGui.radiusOf('my.company'));
  });

  it('#absoluteFixableCircle corresponds to drawn circle', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass');

    const inspectTestGui = testGui(root).inspect;

    let absoluteFixAbleCircle = root.getByName('my.company.SomeClass').absoluteFixableCircle;
    expect({x: absoluteFixAbleCircle.x, y: absoluteFixAbleCircle.y}).to.deep.equal(inspectTestGui.positionOf('my.company.SomeClass'));
    expect(absoluteFixAbleCircle.r).to.deep.equal(inspectTestGui.radiusOf('my.company.SomeClass'));

    absoluteFixAbleCircle = root.getByName('my.company').absoluteFixableCircle;
    expect({x: absoluteFixAbleCircle.x, y: absoluteFixAbleCircle.y}).to.deep.equal(inspectTestGui.positionOf('my.company'));
    expect(absoluteFixAbleCircle.r).to.deep.equal(inspectTestGui.radiusOf('my.company'));
  });
});

describe("Node's public methods", () => {
  describe('#isPackage()', () => {
    it('returns false for classes and interfaces', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass', 'my.company.somePkg.SomeInterface$SomeInnerClass');

      expect(root.getByName('my.company.SomeClass').isPackage()).to.be.false;
      expect(root.getByName('my.company.somePkg.SomeInterface').isPackage()).to.be.false;
      expect(root.getByName('my.company.somePkg.SomeInterface$SomeInnerClass').isPackage()).to.be.false;
    });

    it('returns true for packages', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass', 'my.company.somePkg.SomeInterface');

      expect(root.getByName('my.company').isPackage()).to.be.true;
      expect(root.getByName('my.company.somePkg').isPackage()).to.be.true;
    });
  });

  it('#getFullName() returns the full qualified name of a node', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass$SomeInnerClass');

    expect(root.getByName('my.company').getFullName()).to.equal('my.company');
    expect(root.getByName('my.company.SomeClass').getFullName()).to.equal('my.company.SomeClass');
    expect(root.getByName('my.company.SomeClass$SomeInnerClass').getFullName()).to.equal('my.company.SomeClass$SomeInnerClass');
  });

  describe('#getParent()', () => {
    it('returns itself for the default root', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');
      expect(root.getByName('default').getParent()).to.equal(root.getByName('default'));
    });

    it('returns the parent node of a node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass$SomeInnerClass');

      expect(root.getByName('my.company').getParent()).to.equal(root.getByName('default'));
      expect(root.getByName('my.company.SomeClass').getParent()).to.equal(root.getByName('my.company'));
      expect(root.getByName('my.company.SomeClass$SomeInnerClass').getParent()).to.equal(root.getByName('my.company.SomeClass'));
    });
  });

  it('#getOriginalChildren returns all children of a node', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass', 'my.company.OtherClass$SomeInnerClass');

    expect(root.getByName('default').getOriginalChildren()).to.onlyContainNodes('my.company');
    expect(root.getByName('my.company').getOriginalChildren()).to.onlyContainNodes('my.company.SomeClass', 'my.company.OtherClass');
    expect(root.getByName('my.company.OtherClass').getOriginalChildren()).to.onlyContainNodes('my.company.OtherClass$SomeInnerClass');
    expect(root.getByName('my.company.OtherClass$SomeInnerClass').getOriginalChildren()).to.be.empty;
  });

  it('#getCurrentChildren() returns all children of a node', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass', 'my.company.OtherClass$SomeInnerClass');

    expect(root.getByName('default').getCurrentChildren()).to.onlyContainNodes('my.company');
    expect(root.getByName('my.company').getCurrentChildren()).to.onlyContainNodes('my.company.SomeClass', 'my.company.OtherClass');
    expect(root.getByName('my.company.OtherClass').getCurrentChildren()).to.onlyContainNodes('my.company.OtherClass$SomeInnerClass');
    expect(root.getByName('my.company.OtherClass$SomeInnerClass').getCurrentChildren()).to.be.empty;
  });

  describe('#isCurrentlyLeaf()', () => {
    it('returns true for leaf', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      expect(root.getByName('my.company.SomeClass').isCurrentlyLeaf()).to.be.true;
    });

    it('returns false for non-leaves', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      expect(root.getByName('default').isCurrentlyLeaf()).to.be.false;
      expect(root.getByName('my.company').isCurrentlyLeaf()).to.be.false;
    });
  });

  describe('#isPredecessorOf()', () => {
    it('returns true if the node is a predecessor of the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');

      expect(root.getByName('default').isPredecessorOf(root.getByName('my.company'))).to.be.true;
      expect(root.getByName('default').isPredecessorOf(root.getByName('my.company.somePkg'))).to.be.true;
      expect(root.getByName('default').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('default').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      expect(root.getByName('my.company').isPredecessorOf(root.getByName('my.company.somePkg'))).to.be.true;
      expect(root.getByName('my.company').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      expect(root.getByName('my.company.somePkg.SomeClass').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;
    });

    it('returns false if the node is not a predecessor of the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');

      expect(root.getByName('my.company.otherPkg').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass'))).to.be.false;
      expect(root.getByName('my.company.otherPkg').isPredecessorOf(root.getByName('my.company.somePkg'))).to.be.false;
    });

    it('returns false if the node equals the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass');

      expect(root.getByName('my.company.somePkg').isPredecessorOf(root.getByName('my.company.somePkg'))).to.be.false;
      expect(root.getByName('my.company.somePkg.SomeClass').isPredecessorOf(root.getByName('my.company.somePkg.SomeClass'))).to.be.false;
    });
  });

  describe('#isPredecessorOfNodeOrItself()', () => {
    it('returns true if the node is a predecessor of the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');

      expect(root.getByName('default').isPredecessorOfNodeOrItself(root.getByName('my.company'))).to.be.true;
      expect(root.getByName('default').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg'))).to.be.true;
      expect(root.getByName('default').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('default').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      expect(root.getByName('my.company').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg'))).to.be.true;
      expect(root.getByName('my.company').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
      expect(root.getByName('my.company').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;

      expect(root.getByName('my.company.somePkg.SomeClass').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass$SomeInnerClass'))).to.be.true;
    });

    it('returns true if the node equals the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass');

      expect(root.getByName('my.company.somePkg').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg'))).to.be.true;
      expect(root.getByName('my.company.somePkg.SomeClass').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass'))).to.be.true;
    });

    it('returns false if the node is not a predecessor of the give node', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');

      expect(root.getByName('my.company.otherPkg').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg.SomeClass'))).to.be.false;
      expect(root.getByName('my.company.otherPkg').isPredecessorOfNodeOrItself(root.getByName('my.company.somePkg'))).to.be.false;
    });
  });

  it('#isFolded() returns false for all nodes', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

    expect(root.getByName('my.company.SomeClass').isFolded()).to.be.false;
    expect(root.getByName('my.company').isFolded()).to.be.false;
    expect(root.getByName('default').isFolded()).to.be.false;
  });

  describe('#overlapsWith()', () => {
    it('returns false if one of the nodes is a predecessor of the other one', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass');

      expect(root.getByName('my.company').overlapsWith(root.getByName('my.company.SomeClass'))).to.be.false;
      expect(root.getByName('my.company').overlapsWith(root.getByName('my.company.SomeClass$SomeInnerClass'))).to.be.false;
      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company.SomeClass$SomeInnerClass'))).to.be.false;

      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company'))).to.be.false;
      expect(root.getByName('my.company.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company'))).to.be.false;
      expect(root.getByName('my.company.SomeClass$SomeInnerClass').overlapsWith(root.getByName('my.company.SomeClass'))).to.be.false;
    });

    it('returns false for not overlapping nodes with the same parent', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.OtherClass', 'my.company.somePkg.SomeClass');

      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company.OtherClass'))).to.be.false;
      expect(root.getByName('my.company.SomeClass').overlapsWith(root.getByName('my.company.somePkg'))).to.be.false;
      expect(root.getByName('my.company.OtherClass').overlapsWith(root.getByName('my.company.somePkg'))).to.be.false;
    });

    it('returns false for not overlapping node not with the same parent', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');

      expect(root.getByName('my.company.somePkg.SomeClass').overlapsWith(root.getByName('my.company.otherPkg.OtherClass'))).to.be.false;
    });
  });

  describe('#getSelfOrFirstPredecessorMatching()', () => {
    it('returns self if it matches the condition', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      const someClass = root.getByName('my.company.SomeClass');
      expect(someClass.getSelfOrFirstPredecessorMatching(node => node.getFullName().includes('Some'))).to.equal(someClass);
    });

    it('returns the first predecessor matching the condition', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.firstPkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.secondPkg.SomeClass', 'my.company.otherPkg.OtherClass');

      const someInnerClass = root.getByName('my.company.somePkg.firstPkg.SomeClass$SomeInnerClass');
      const expectedClass = root.getByName('my.company.somePkg.firstPkg');
      expect(someInnerClass.getSelfOrFirstPredecessorMatching(node => node.getFullName().endsWith('Pkg'))).to.equal(expectedClass);
    });

    it('returns null if no predecessor matches the condition', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      const someClass = root.getByName('my.company.SomeClass');
      expect(someClass.getSelfOrFirstPredecessorMatching(node => node.getFullName().includes('foo'))).to.be.null;
    });
  });

  describe('#getSelfAndPredecessorsUntilExclusively()', () => {
    it('returns an empty array, if the given node equals the one where the method is invoked on', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      const someClass = root.getByName('my.company.SomeClass');
      expect(someClass.getSelfAndPredecessorsUntilExclusively(someClass)).to.be.empty;
    });

    it('returns an array with the node where the method is invoked on, if the given node is the parent', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass$SomeInnerClass');

      const someInnerClass = root.getByName('my.company.SomeClass$SomeInnerClass');
      const someClass = root.getByName('my.company.SomeClass');
      expect(someInnerClass.getSelfAndPredecessorsUntilExclusively(someClass)).to.onlyContainOrderedNodes('my.company.SomeClass$SomeInnerClass');
    });

    it('returns all nodes until the given one exclusively', () => {
      const root = rootCreator.createRootFromClassNames('my.company.somePkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');

      const someInnerClass = root.getByName('my.company.somePkg.SomeClass$SomeInnerClass');
      const company = root.getByName('my.company');
      expect(someInnerClass.getSelfAndPredecessorsUntilExclusively(company)).to.onlyContainOrderedNodes(
        'my.company.somePkg', 'my.company.somePkg.SomeClass', 'my.company.somePkg.SomeClass$SomeInnerClass');
    });

    it('throws an error if the given node does not exist', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      expect(() => root.getByName('my.company.SomeClass').getSelfAndPredecessorsUntilExclusively()).to.throw('the given node does not exist');
    });
  });

  it('#isRoot()', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

    expect(root.getByName('default').isRoot()).to.be.true;
    expect(root.getByName('my.company').isRoot()).to.be.false;
    expect(root.getByName('my.company.SomeClass').isRoot()).to.be.false;
  });

  it('#getSelfAndPredecessors()', () => {
    const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

    expect(root.getByName('default').getSelfAndPredecessors()).to.onlyContainOrderedNodes('default');
    expect(root.getByName('my.company').getSelfAndPredecessors()).to.onlyContainOrderedNodes('my.company', 'default');
    expect(root.getByName('my.company.SomeClass').getSelfAndPredecessors()).to.onlyContainOrderedNodes('my.company.SomeClass', 'my.company', 'default');
  });

  describe("Root's specific public methods", () => {
    it('#getByName() returns the node for a given fullname', () => {
      const root = rootCreator.createRootFromClassNames('my.company.SomeClass');

      expect(root.getByName('my.company.SomeClass').getFullName()).to.equal('my.company.SomeClass');
      expect(root.getByName('my.company').getFullName()).to.equal('my.company');
      expect(root.getByName('default').getFullName()).to.equal('default');
    });
  });

  describe("InnerNode's specific public methods", () => {
    describe('#liesInFrontOf()', () => {
      it('returned value corresponds to the drawing order of the elements', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.FirstClass', 'my.company.somePkg.SecondClass',
          'my.company.otherPkg.OtherClass');

        const inspectTestGui = testGui(root).inspect;

        const firstClass = root.getByName('my.company.somePkg.FirstClass');

        expect(firstClass.liesInFrontOf(root.getByName('my.company.somePkg.SecondClass'))).to.equal(
          inspectTestGui.nodeLiesInFrontOf('my.company.somePkg.FirstClass', 'my.company.somePkg.SecondClass'));
        expect(firstClass.liesInFrontOf(root.getByName('my.company.otherPkg.OtherClass'))).to.equal(
          inspectTestGui.nodeLiesInFrontOf('my.company.somePkg.FirstClass', 'my.company.otherPkg.OtherClass'));
        expect(firstClass.liesInFrontOf(root.getByName('my.company.somePkg'))).to.equal(
          inspectTestGui.nodeLiesInFrontOf('my.company.somePkg.FirstClass', 'my.company.somePkg'));
        expect(firstClass.liesInFrontOf(root.getByName('my.company.otherPkg'))).to.equal(
          inspectTestGui.nodeLiesInFrontOf('my.company.somePkg.FirstClass', 'my.company.otherPkg'));
      });

      it('the returned value is negated, if the elements are switched', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.FirstClass', 'my.company.SecondClass');

        const firstClass = root.getByName('my.company.FirstClass');
        const secondClass = root.getByName('my.company.SecondClass');
        const company = root.getByName('my.company');

        expect(firstClass.liesInFrontOf(secondClass)).to.not.equal(secondClass.liesInFrontOf(firstClass));
        expect(firstClass.liesInFrontOf(company)).to.not.equal(company.liesInFrontOf(firstClass));
      });
    });
  });
});

describe('Filter and fold nodes in combination:', () => {
  it('A folded class stays folded when it is hidden and shown again by a filter', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass',
      'my.company.otherPkg.SomeInterface');
    const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

    await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
    await filterOn(root, filterCollection).typeFilter(true, false).await();
    testGui(root).test.that.onlyNodesAre('my.company.otherPkg.SomeInterface');

    await filterOn(root, filterCollection).typeFilter(true, true).await();
    testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.SomeInterface');
  });

  it('A folded class that does not match the filter but has matching children, is not hidden', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass', 'my.company.OtherClass');
    const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

    await testGui(root).interact.clickNodeAndAwait('my.company.SomeClass');
    await filterOn(root, filterCollection).nameFilter('*$SomeInnerClass').await();
    testGui(root).test.that.onlyNodesAre('my.company.SomeClass');

    await testGui(root).interact.clickNodeAndAwait('my.company.SomeClass');
    testGui(root).test.that.onlyNodesAre('my.company.SomeClass$SomeInnerClass');
  });

  describe('Fold and filter after each other', () => {
    it('Fold, filter, unfold, reset the filter and unfold nodes that were hidden by the filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.somePkg.SomeClass', 'my.company.somePkg.SomeInterface',
        'my.company.otherPkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg.SomeClass');
      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg');
      await filterOn(root, filterCollection).typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~my.company.otherPkg.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg');

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.otherPkg');

      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.SomeInterface',
        'my.company.otherPkg.OtherClass');

      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.SomeInterface',
        'my.company.otherPkg.SomeClass', 'my.company.otherPkg.OtherClass');

      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg.SomeClass');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.SomeInterface',
        'my.company.otherPkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.OtherClass');
    });

    it('Filter, fold, reset the filter and unfold', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.SomeClass$SomeInnerInterface', 'my.company.somePkg.OtherClass', 'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.OtherClass').await();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg.SomeClass');
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg.SomeClass');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.SomeClass$SomeInnerInterface', 'my.company.somePkg.OtherClass', 'my.company.otherPkg.OtherClass');
    });

    it('Fold, filter, reset the filter, unfold', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.somePkg.SomeInterface', 'my.company.somePkg.OtherInterface', 'my.company.otherPkg.SomeClass$SomeInnerClass',
        'my.company.otherPkg.SomeClass$OtherInnerClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg.SomeClass');
      await filterOn(root, filterCollection).typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~*.SomeInnerClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.otherPkg.SomeClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.otherPkg.SomeClass', 'my.company.somePkg');

      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.otherPkg.SomeClass', 'my.company.somePkg');

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeInterface', 'my.company.somePkg.OtherInterface',
        'my.company.otherPkg.SomeClass');

      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg.SomeClass');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeInterface', 'my.company.somePkg.OtherInterface',
        'my.company.otherPkg.SomeClass$SomeInnerClass', 'my.company.otherPkg.SomeClass$OtherInnerClass');
    });

    it('Filter, fold, unfold and reset the filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.SomeClass$SomeInnerInterface', 'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~my.company.otherPkg.OtherClass').await();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg.SomeClass');
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg');

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass');

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg.SomeClass');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass$SomeInnerClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.SomeClass$SomeInnerInterface');

      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass$SomeInnerClass',
        'my.company.somePkg.SomeClass$SomeInnerInterface', 'my.company.otherPkg.OtherClass');
    });
  });

  describe('Fold and filter directly after each other, so that the relayout in between may not be finished', () => {
    it('Fold and then filter, reset the filter and then unfold', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      testGui(root).interact.clickNode('my.company.somePkg');
      testGui(root).interact.clickNode('my.company.otherPkg');
      await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.OtherClass|~my.company.otherPkg.OtherClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg');

      filterOn(root, filterCollection).nameFilter('').goOn();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg');

      await testGui(root).interact.clickNodeAndAwait('my.company.otherPkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
    });

    it('Fold and then reset the filter, filter and then unfold', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').await();

      testGui(root).interact.clickNode('my.company.somePkg');
      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').goOn();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.OtherClass', 'my.company.otherPkg.OtherClass');
    });

    it('Reset the filter and then fold, unfold and then filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').await();

      filterOn(root, filterCollection).nameFilter('').goOn();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      testGui(root).interact.clickNode('my.company.somePkg');
      await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.OtherClass', 'my.company.otherPkg.OtherClass');
    });

    it('Filter and then fold, unfold and then reset the filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').goOn();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');

      testGui(root).interact.clickNode('my.company.somePkg');
      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
    });

    it('Filter, fold, unfold, reset the filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').goOn();
      testGui(root).interact.clickNode('my.company.somePkg');
      testGui(root).interact.clickNode('my.company.somePkg');
      await filterOn(root, filterCollection).nameFilter('').await();

      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass')
    });

    it('Fold, filter, reset the filter, unfold', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      testGui(root).interact.clickNode('my.company.somePkg');
      filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').goOn();
      filterOn(root, filterCollection).nameFilter('').goOn();
      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');

      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass')
    });

    it('Fold, filter, unfold, reset the filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass');
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      testGui(root).interact.clickNode('my.company.somePkg');
      filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').goOn();
      testGui(root).interact.clickNode('my.company.somePkg');
      await filterOn(root, filterCollection).nameFilter('').await();

      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.somePkg.OtherClass',
        'my.company.otherPkg.OtherClass')
    });
  });
});

describe('Dragging node in combination with folding and filtering:', () => {
  it('A folded node can be dragged', async () => {
    let _offsetPosition = {x: 0, y: 0};
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass', {
      onJumpedToPosition: offsetPosition => _offsetPosition = offsetPosition
    });
    await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');

    const nodePositionBefore = testGui(root).inspect.positionOf('my.company.somePkg');

    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg', {dx: 50, dy: 50});

    const expectedPosition = vectors.add(_offsetPosition, {x: nodePositionBefore.x + 50, y: nodePositionBefore.y + 50});
    testGui(root).test.that.node('my.company.somePkg').is.atPosition(expectedPosition);
  });

  describe('Clicking on a node after dragging it:', () => {
    it('folds the node if it was unfolded before and leads to a correct layout', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');
      await testGui(root).interact.dragNodeAndAwait('my.company.somePkg', {dx: 50, dy: 50});

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');

      testGui(root).test.that.onlyNodesAre('my.company.somePkg', 'my.company.otherPkg.OtherClass');
      testWholeLayoutOn(root);
    });

    it('unfolds the node if it was folded before and leads to a correct layout', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');
      testGui(root).interact.clickNodeAndAwait('my.company.somePkg');
      await testGui(root).interact.dragNodeAndAwait('my.company.somePkg', {dx: 50, dy: 50});

      await testGui(root).interact.clickNodeAndAwait('my.company.somePkg');

      testGui(root).test.that.onlyNodesAre('my.company.somePkg.SomeClass', 'my.company.otherPkg.OtherClass');
      testWholeLayoutOn(root);
    });
  });

  it('the nodes can be filtered after dragging one, which leads to a correct layout', async () => {
    const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.somePkg.SomeClass', 'my.company.otherPkg.SomeClass',
      'my.company.otherPkg.OtherClass');
    const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
    await testGui(root).interact.dragNodeAndAwait('my.company.somePkg.SomeClass', {dx: 50, dy: 50});

    await filterOn(root, filterCollection).nameFilter('~my.company.somePkg.SomeClass').await();

    testGui(root).test.that.onlyNodesAre('my.company.otherPkg.SomeClass', 'my.company.otherPkg.OtherClass');
    testWholeLayoutOn(root);
  });
});