'use strict';

require('./chai/dependencies-chai-extension');
const expect = require('chai').expect;

const testObjects = require("./test-object-creator.js");

const depsOfTree2WithTestFolded = [
  "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
  "com.tngtech.test->com.tngtech.class2()",
  "com.tngtech.test->com.tngtech.main.class1()",
  "com.tngtech.test->com.tngtech.interface1()",
  "com.tngtech.class2->com.tngtech.main.class1(extends)",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

const depsOfTree2WithMainFolded = [
  "com.tngtech.main->com.tngtech.interface1()",
  "com.tngtech.test.testclass1->com.tngtech.class2(several)",
  "com.tngtech.test.testclass1->com.tngtech.main()",
  "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
  "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
  "com.tngtech.class2->com.tngtech.main()",
  "com.tngtech.class2->com.tngtech.interface1(implements)"
];

const initDependency = require('./main-files').get('dependency').init;

const stubs = require('./stubs');

const testJson = require('./test-json-creator');
const appContext = require('./main-files').get('app-context').newInstance({
  visualizationStyles: stubs.visualizationStylesStub(30),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub, //FIXME: really necessary??
  DependencyView: stubs.DependencyViewStub
});
const Node = appContext.getNode();
const Dependencies = appContext.getDependencies();

/*
 * json-root with every kind of dependency of both groups (inheritance and access),
 * several different dependencies from one class to another one,
 * dependencies between a class and its inner class
 * and mutual dependencies (between separated classes and a class and its inner class)
 */
const jsonRoot = testJson.package('com.tngtech')
  .add(testJson.package('pkg1')
    .add(testJson.clazz('SomeClass1', 'class')
      .callingMethod('com.tngtech.pkg1.SomeClass2', 'startMethod(arg1, arg2)', 'targetMethod()')
      .accessingField('com.tngtech.pkg1.SomeClass2', 'startMethod(arg1, arg2)', 'targetField')
      .implementing('com.tngtech.pkg2.SomeInterface1')
      .build())
    .add(testJson.clazz('SomeClass2', 'class')
      .accessingField('com.tngtech.pkg1.SomeClass1', 'startMethod(arg)', 'targetField')
      .build())
    .build())
  .add(testJson.package('pkg2')
    .add(testJson.clazz('SomeInterface1', 'interface').build())
    .add(testJson.package('subpkg1')
      .add(testJson.clazz('SomeClass1', 'class')
        .extending('com.tngtech.pkg1.SomeClass1')
        .callingConstructor('com.tngtech.pkg1.SomeClass1', '<init>()', '<init>()')
        .build())
      .add(testJson.clazz('SomeClassWithInnerInterface', 'class')
        .havingInnerClass(testJson.clazz('SomeInnerInterface', 'interface')
          .callingMethod('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface', 'startMethod(arg)', 'targetMethod(arg1, arg2)')
          .build())
        .implementingAnonymous('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface')
        .build())
      .build())
    .build())
  .add(testJson.clazz('SomeClassWithInnerClass', 'class')
    .implementingAnonymous('com.tngtech.pkg2.SomeInterface1')
    .havingInnerClass(testJson.clazz('SomeInnerClass', 'class')
      .accessingField('com.tngtech.SomeClassWithInnerClass', 'startMethod()', 'targetField')
      .build())
    .build())
  .build();
const root = new Node(jsonRoot);

describe('Dependencies', () => {
  it('creates correct elementary dependencies from json-input', () => {
    const dependencies = new Dependencies(jsonRoot, root);
    const exp = [
      'com.tngtech.pkg1.SomeClass1->com.tngtech.pkg1.SomeClass2(startMethod(arg1, arg2) methodCall targetMethod())',
      'com.tngtech.pkg1.SomeClass1->com.tngtech.pkg1.SomeClass2(startMethod(arg1, arg2) fieldAccess targetField)',
      'com.tngtech.pkg1.SomeClass1->com.tngtech.pkg2.SomeInterface1(implements)',
      'com.tngtech.pkg1.SomeClass2->com.tngtech.pkg1.SomeClass1(startMethod(arg) fieldAccess targetField)',
      'com.tngtech.pkg2.subpkg1.SomeClass1->com.tngtech.pkg1.SomeClass1(extends)',
      'com.tngtech.pkg2.subpkg1.SomeClass1->com.tngtech.pkg1.SomeClass1(<init>() constructorCall <init>())',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface->com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface(startMethod(arg) methodCall targetMethod(arg1, arg2))',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface->com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface(implementsAnonymous)',
      'com.tngtech.SomeClassWithInnerClass->com.tngtech.pkg2.SomeInterface1(implementsAnonymous)',
      'com.tngtech.SomeClassWithInnerClass$SomeInnerClass->com.tngtech.SomeClassWithInnerClass(startMethod() fieldAccess targetField)'
    ];
    expect(dependencies._elementary).to.haveDependencyStrings(exp);
  });

  it('creates correct visible dependencies from the elementary dependencies', () => {
    const dependencies = new Dependencies(jsonRoot, root);
    const exp = [
      'com.tngtech.pkg1.SomeClass1->com.tngtech.pkg1.SomeClass2(several)',
      'com.tngtech.pkg1.SomeClass1->com.tngtech.pkg2.SomeInterface1(implements)',
      'com.tngtech.pkg1.SomeClass2->com.tngtech.pkg1.SomeClass1(fieldAccess)',
      'com.tngtech.pkg2.subpkg1.SomeClass1->com.tngtech.pkg1.SomeClass1(extends constructorCall)',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface->com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface(methodCall)',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface->com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface(implementsAnonymous)',
      'com.tngtech.SomeClassWithInnerClass->com.tngtech.pkg2.SomeInterface1(implementsAnonymous)',
      'com.tngtech.SomeClassWithInnerClass$SomeInnerClass->com.tngtech.SomeClassWithInnerClass(fieldAccess)'
    ];
    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
    expect(dependencies.getVisible().map(dependency => dependency.isVisible())).to.not.include(false);
  });

  it('know if they must share on of the end nodes', () => {
    const dependencies = new Dependencies(jsonRoot, root);
    const hasEndNodes = (node1, node2) => d => (d.from === node1 || d.to === node1) && (d.from === node2 || d.to === node2);
    const filter = d => hasEndNodes('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface')(d)
    || hasEndNodes('com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.SomeClass2')(d);
    const dependenciesSharingNodes = dependencies.getVisible().filter(filter);
    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependenciesSharingNodes)).to.not.include(false);
    expect(mapToMustShareNodes(dependencies.getVisible().filter(d => !filter(d)))).to.not.include(true);
  });

  it('should recreate correctly its visible dependencies after folding a package: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('TargetClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const filterForHiddenDependencies = d => d.from === 'com.tngtech.startPkg.StartClass';
    const hiddenDependencies = dependencies.getVisible().filter(filterForHiddenDependencies);
    const visibleDependencies = dependencies.getVisible().filter(d => !filterForHiddenDependencies(d));

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
  });

  it('should recreate its visible dependencies correctly after folding a class with an inner class: old dependencies ' +
    'are hidden, all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('StartClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('InnerClass', 'class')
          .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
          .build())
        .implementing('com.tngtech.SomeInterface')
        .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
        .build())
      .add(testJson.clazz('TargetClass', 'class')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const filterForHiddenDependencies = d => d.from === 'com.tngtech.StartClassWithInnerClass$InnerClass';
    const hiddenDependencies = dependencies.getVisible().filter(filterForHiddenDependencies);
    const visibleDependencies = dependencies.getVisible().filter(d => !filterForHiddenDependencies(d));

    dependencies.updateOnNodeFolded('com.tngtech.StartClassWithInnerClass', true);

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
  });

  it('should update correctly if the parent-package of the start-node is folded', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('TargetClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.startPkg->com.tngtech.TargetClass()',
      'com.tngtech.startPkg->com.tngtech.SomeInterface()',
      'com.tngtech.TargetClass->com.tngtech.SomeInterface(implements)'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should update correctly if the parent-package of the end-node is folded', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface')
        .callingConstructor('com.tngtech.targetPkg.TargetClass', 'startMethod()', '<init>()').build())
      .add(testJson.package('targetPkg')
        .add(testJson.clazz('TargetClass', 'class').build())
        .build())
      .add(testJson.clazz('StartClass', 'class')
        .callingMethod('com.tngtech.targetPkg.TargetClass', 'startMethod()', 'targetMethod')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.StartClass->com.tngtech.targetPkg()',
      'com.tngtech.SomeInterface->com.tngtech.targetPkg()',
      'com.tngtech.StartClass->com.tngtech.SomeInterface(implements)'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should update correctly if the parent-package of the end-node and the parent-package of the start-node are folded', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.package('targetPkg')
        .add(testJson.clazz('TargetClass', 'class').build())
        .build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.targetPkg.TargetClass', 'startMethod()', 'targetMethod')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.startPkg->com.tngtech.targetPkg()'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should update correctly if the parent-class of the start-node is folded', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.package('targetPkg')
        .add(testJson.clazz('TargetClass', 'class').build())
        .build())
      .add(testJson.clazz('StartClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.targetPkg.TargetClass', 'startMethod()', 'targetMethod')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.StartClassWithInnerClass->com.tngtech.targetPkg.TargetClass(childrenAccess)'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.StartClassWithInnerClass', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should recreate correctly its visible dependencies after unfolding a package: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('TargetClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const visibleDependencies1 = dependencies.getVisible().filter(d => d.from === 'com.tngtech.startPkg.StartClass');

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);

    const filterForHiddenDependencies = d => d.from === 'com.tngtech.startPkg';
    const hiddenDependencies = dependencies.getVisible().filter(filterForHiddenDependencies);
    const visibleDependencies2 = dependencies.getVisible().filter(d => !filterForHiddenDependencies(d));

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies1);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies2);
  });

  it('should update correctly if a package is unfolded again', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.TargetClass', 'startMethod()', 'targetMethod')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('TargetClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.startPkg.StartClass->com.tngtech.TargetClass(methodCall)',
      'com.tngtech.startPkg.StartClass->com.tngtech.SomeInterface(implements)',
      'com.tngtech.TargetClass->com.tngtech.SomeInterface(implements)'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should update correctly if two packages are unfolded again', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.package('targetPkg')
        .add(testJson.clazz('TargetClass', 'class').build())
        .build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.targetPkg.TargetClass', 'startMethod()', 'targetMethod')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.startPkg.StartClass->com.tngtech.targetPkg.TargetClass(methodCall)'];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should update correctly if a package is unfolded again, when another package is folded', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.package('targetPkg')
        .add(testJson.clazz('TargetClass', 'class').build())
        .build())
      .add(testJson.package('startPkg')
        .add(testJson.clazz('StartClass', 'class')
          .callingMethod('com.tngtech.targetPkg.TargetClass', 'startMethod()', 'targetMethod')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.startPkg.StartClass->com.tngtech.targetPkg()'];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can jump the dependencies of a specific node to their positions', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClass1', 'class')
        .accessingField('com.tngtech.SomeClass2', 'startMethod()', 'targetField')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.SomeClass1', 'startMethod()', 'targetMethod()')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const draggedNode = 'com.tngtech.SomeClass1';
    const filter = d => d.from === draggedNode || d.to === draggedNode;
    const jumpedDependencies = dependencies.getVisible().filter(filter);
    const notJumpedDependences = dependencies.getVisible().filter(d => !filter(d));

    dependencies.jumpSpecificDependenciesToTheirPositions(root.getByName(draggedNode));

    const mapDependenciesToHasJumped = dependencies => dependencies.map(d => d._view.hasJumpedToPosition);
    expect(mapDependenciesToHasJumped(jumpedDependencies)).to.not.include(false);
    expect(mapDependenciesToHasJumped(notJumpedDependences)).to.not.include(true);
  });

  it('can move all dependencies to their positions', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClass1', 'class')
        .accessingField('com.tngtech.SomeClass2', 'startMethod()', 'targetField')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.SomeClass1', 'startMethod()', 'targetMethod()')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const promise = dependencies.moveAllToTheirPositions();

    const mapDependenciesToHasMoved = dependencies => dependencies.map(d => d._view.hasMovedToPosition);
    return promise.then(() => expect(mapDependenciesToHasMoved(dependencies.getVisible())).to.not.include(false));
  });

  it('can move all dependencies to their positions twice in a row: the second move does not start before the first is ended', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClass1', 'class')
        .accessingField('com.tngtech.SomeClass2', 'startMethod()', 'targetField')
        .build())
      .add(testJson.clazz('SomeClass2', 'class')
        .callingMethod('com.tngtech.SomeClass1', 'startMethod()', 'targetMethod()')
        .implementing('com.tngtech.SomeInterface')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);
    const exp = [
      'com.tngtech.SomeClass1->com.tngtech.SomeClass2(fieldAccess)',
      'com.tngtech.SomeClass2->com.tngtech.SomeClass1(methodCall)',
      'com.tngtech.SomeClass2->com.tngtech.SomeInterface(implements)',
    ];
    const movedDependenciesFirstTime = [];
    const movedDependenciesSecondTime = [];
    stubs.setMovedDependencies(movedDependenciesFirstTime);

    dependencies.moveAllToTheirPositions().then(() => stubs.setMovedDependencies(movedDependenciesSecondTime));
    const promise = dependencies.moveAllToTheirPositions();

    return promise.then(() => {
      /**
       * when the both invokes of moveAllToTheirPositions above are not executed after each other,
       * then the dependencies are not added to the different array
       */
      expect(movedDependenciesFirstTime).to.haveDependencyStrings(exp);
      expect(movedDependenciesSecondTime).to.haveDependencyStrings(exp);
    });
  });

  it("are uniqued and grouped correctly with complicated dependency structure", () => {
    const graphWrapper = testObjects.testGraph3();
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);

    const node1 = graphWrapper.getNode("com.tngtech.test");
    node1._changeFoldIfInnerNodeAndRelayout();
    let exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
      "com.tngtech.main.class3->com.tngtech.interface1(implements methodCall)",
      "com.tngtech.test->com.tngtech.class2()",
      "com.tngtech.test->com.tngtech.main.class1()",
      "com.tngtech.test->com.tngtech.interface1()",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    return graphWrapper.graph.root.doNext(() => {
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);

      const node2 = graphWrapper.getNode("com.tngtech.test");
      node2._changeFoldIfInnerNodeAndRelayout();
      const node3 = graphWrapper.getNode("com.tngtech.main");
      node3._changeFoldIfInnerNodeAndRelayout();

      return graphWrapper.graph.root.doNext(() => {
        exp = [
          "com.tngtech.main->com.tngtech.interface1()",
          "com.tngtech.test.testclass1->com.tngtech.class2(extends several)",
          "com.tngtech.test.testclass1->com.tngtech.main()",
          "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
          "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
          "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
          "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
          "com.tngtech.class2->com.tngtech.main()",
          "com.tngtech.class2->com.tngtech.interface1(implements)"
        ];
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);
      });
    });
  });

  it("does the filtering of classes (no dependencies of eliminated nodes) and resets them correctly", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByNameNotContaining("subtest");
    const exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
      "com.tngtech.test.testclass1->com.tngtech.class2(several)",
      "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];
    return graphWrapper.graph.dependencies.doNext(() => expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp))
      .then(() => {
        graphWrapper.graph.filterNodesByNameContaining("");
        return graphWrapper.graph.dependencies.doNext(() => expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies));
      });
  });

  it("does the following correctly (in this order): fold, filter, reset filter and unfold", () => {
    const graphWrapper = testObjects.testGraph2();

    const node = graphWrapper.getNode("com.tngtech.test");
    node._changeFoldIfInnerNodeAndRelayout();
    return graphWrapper.graph.root.doNext(() => {
      graphWrapper.graph.filterNodesByNameNotContaining("subtest");
      const exp = [
        "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
        "com.tngtech.test->com.tngtech.class2()",
        "com.tngtech.test->com.tngtech.main.class1()",
        "com.tngtech.test->com.tngtech.interface1()",
        "com.tngtech.class2->com.tngtech.main.class1(extends)",
        "com.tngtech.class2->com.tngtech.interface1(implements)"
      ];
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);

      graphWrapper.graph.filterNodesByNameContaining("");
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(depsOfTree2WithTestFolded);
      node._changeFoldIfInnerNodeAndRelayout();
      return graphWrapper.graph.root.doNext(() => {
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
      });
    });
  });

  it("does the following correctly (in this order): fold, filter, unfold and reset filter", () => {
    const graphWrapper = testObjects.testGraph2();

    const node = graphWrapper.getNode("com.tngtech.test");
    node._changeFoldIfInnerNodeAndRelayout();

    return graphWrapper.graph.root.doNext(() => {
      graphWrapper.graph.filterNodesByNameNotContaining("subtest");
      let exp = [
        "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
        "com.tngtech.test->com.tngtech.class2()",
        "com.tngtech.test->com.tngtech.main.class1()",
        "com.tngtech.test->com.tngtech.interface1()",
        "com.tngtech.class2->com.tngtech.main.class1(extends)",
        "com.tngtech.class2->com.tngtech.interface1(implements)"
      ];
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp)
      node._changeFoldIfInnerNodeAndRelayout();
      exp = [
        "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
        "com.tngtech.test.testclass1->com.tngtech.class2(several)",
        "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
        "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
        "com.tngtech.class2->com.tngtech.main.class1(extends)",
        "com.tngtech.class2->com.tngtech.interface1(implements)"
      ];

      return graphWrapper.graph.root.doNext(() => {
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);
        graphWrapper.graph.filterNodesByNameContaining("");
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
      });
    });
  });

  it("does the following correctly (in this order): filter, fold, unfold and reset the filter", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByNameNotContaining("subtest");
    const node = graphWrapper.getNode("com.tngtech.test");
    node._changeFoldIfInnerNodeAndRelayout();
    return graphWrapper.graph.root.doNext(() => {
      let exp = [
        "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
        "com.tngtech.test->com.tngtech.class2()",
        "com.tngtech.test->com.tngtech.main.class1()",
        "com.tngtech.test->com.tngtech.interface1()",
        "com.tngtech.class2->com.tngtech.main.class1(extends)",
        "com.tngtech.class2->com.tngtech.interface1(implements)"
      ];
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);

      node._changeFoldIfInnerNodeAndRelayout();

      return graphWrapper.graph.root.doNext(() => {
        exp = [
          "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
          "com.tngtech.test.testclass1->com.tngtech.class2(several)",
          "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
          "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
          "com.tngtech.class2->com.tngtech.main.class1(extends)",
          "com.tngtech.class2->com.tngtech.interface1(implements)"
        ];
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);

        graphWrapper.graph.filterNodesByNameContaining("");
        expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
      });
    });

  });

  it("does the following correctly (in this order): filter, fold, reset the filter and unfold", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByNameNotContaining("subtest");
    const node = graphWrapper.getNode("com.tngtech.test");
    node._changeFoldIfInnerNodeAndRelayout();
    return graphWrapper.graph.root.doNext(() => {
      const exp = [
        "com.tngtech.main.class1->com.tngtech.interface1(implements methodCall)",
        "com.tngtech.test->com.tngtech.class2()",
        "com.tngtech.test->com.tngtech.main.class1()",
        "com.tngtech.test->com.tngtech.interface1()",
        "com.tngtech.class2->com.tngtech.main.class1(extends)",
        "com.tngtech.class2->com.tngtech.interface1(implements)"
      ];
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);

      graphWrapper.graph.filterNodesByNameContaining("");
      expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(depsOfTree2WithTestFolded);

      node._changeFoldIfInnerNodeAndRelayout();
      return graphWrapper.graph.root.doNext(() => expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies));
    });
  });

  it("does the filtering by type (hiding interfaces) correctly (no dependencies of eliminated nodes) " +
    "and resets them correctly", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByType({showInterfaces: false, showClasses: true});
    const exp = [
      "com.tngtech.test.testclass1->com.tngtech.class2(several)",
      "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)"
    ];
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);
    graphWrapper.graph.filterNodesByType({showInterfaces: true, showClasses: true});
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the filtering by type (hiding classes) with eliminating packages correctly " +
    "(no dependencies of eliminated nodes) and resets them correctly", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterNodesByType({showInterfaces: true, showClasses: false});
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies([])
    graphWrapper.graph.filterNodesByType({showInterfaces: true, showClasses: true});
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
  })
  ;

  it("does the filtering by type (only show inheritance) correctly and resets it", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.graph.filterDependenciesByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: false,
      showMethodCall: false,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDepsBetweenChildAndParent: true
    });
    const exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(implements)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.interface1(implements)",
      "com.tngtech.class2->com.tngtech.main.class1(extends)",
      "com.tngtech.class2->com.tngtech.interface1(implements)"
    ];

    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);
    graphWrapper.graph.filterDependenciesByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: true,
      showMethodCall: true,
      showFieldAccess: true,
      showAnonymousImplementation: true,
      showDepsBetweenChildAndParent: true
    });
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("does the filtering by type (do not show inheritance) correcty and resets it", () => {
    const graphWrapper = testObjects.testGraph3();
    graphWrapper.graph.filterDependenciesByType({
      showImplementing: false,
      showExtending: false,
      showConstructorCall: true,
      showMethodCall: true,
      showFieldAccess: true,
      showAnonymousImplementation: true,
      showDependenciesBetweenClassAndItsInnerClasses: true
    });
    const exp = [
      "com.tngtech.main.class1->com.tngtech.interface1(methodCall)",
      "com.tngtech.main.class3->com.tngtech.interface1(methodCall)",
      "com.tngtech.test.testclass1->com.tngtech.class2(several)",
      "com.tngtech.test.testclass1->com.tngtech.main.class1(fieldAccess)",
      "com.tngtech.test.testclass1->com.tngtech.interface1(implementsAnonymous)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.class2(methodCall)",
      "com.tngtech.test.subtest.subtestclass1->com.tngtech.test.testclass1(constructorCall)"
    ];
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(exp);
    graphWrapper.graph.filterDependenciesByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: true,
      showMethodCall: true,
      showFieldAccess: true,
      showAnonymousImplementation: true,
      showDependenciesBetweenClassAndItsInnerClasses: true
    });
    expect(graphWrapper.graph.dependencies.getVisible()).to.containExactlyDependencies(graphWrapper.allDependencies);
  });

  it("lists correctly the detailed dependencies of class", () => {
    const graphWrapper = testObjects.testGraph2();

    const exp = [
      "testclass1()->field1",
      "testclass1()->targetMethod()"
    ];
    const act = graphWrapper.graph.dependencies.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
      .map(d => d.description);
    expect(act).to.containExactlyDependencies(exp);
  });

  it("lists correctly the detailed dependencies of class with inner classes depending on the fold-state of the class", () => {
    const graphWrapper = testObjects.testGraphWithOverlappingNodesAndMutualDependencies();

    let act = graphWrapper.graph.dependencies.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
      .map(d => d.description);
    let exp = [
      "testclass1()->field1"
    ];
    expect(act).to.containExactlyDependencies(exp);

    graphWrapper.getNode("com.tngtech.test.testclass1")._changeFoldIfInnerNodeAndRelayout();
    return graphWrapper.graph.root.doNext(() => {
      act = graphWrapper.graph.dependencies.getDetailedDependenciesOf("com.tngtech.test.testclass1", "com.tngtech.class2")
        .map(d => d.description);
      exp = [
        "testclass1()->field1",
        "InnerTestClass1.innertestclass1()->field1"
      ];
      expect(act).to.containExactlyDependencies(exp);
    });
  });

  it("lists correctly the detailed dependencies of folded package", () => {
    const graphWrapper = testObjects.testGraph2();

    graphWrapper.getNode("com.tngtech.test")._changeFoldIfInnerNodeAndRelayout();

    const exp = [
      "testclass1.testclass1()->field1",
      "testclass1.testclass1()->targetMethod()",
      "subtest.subtestclass1.startMethod1()->targetMethod()",
    ];
    const act = graphWrapper.graph.dependencies.getDetailedDependenciesOf("com.tngtech.test", "com.tngtech.class2").map(d => d.description);
    expect(act).to.containExactlyDependencies(exp);
  });
});