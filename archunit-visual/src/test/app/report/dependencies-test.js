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
      .accessingField('com.tngtech.SomeClassWithInnerClass', 'startMethod1()', 'targetField')
      .accessingField('com.tngtech.SomeClassWithInnerClass', 'startMethod2()', 'targetField')
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
      'com.tngtech.SomeClassWithInnerClass$SomeInnerClass->com.tngtech.SomeClassWithInnerClass(startMethod1() fieldAccess targetField)',
      'com.tngtech.SomeClassWithInnerClass$SomeInnerClass->com.tngtech.SomeClassWithInnerClass(startMethod2() fieldAccess targetField)'
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

  it('know if they must share one of the end nodes', () => {
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
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);

    //ensure that the dependencies are not recreated
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
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
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
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies1);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies2);
  });

  it('should update whether they must share one of the end nodes after folding', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('InnerClass', 'class')
          .callingConstructor('com.tngtech.SomeClass', '<init>()', '<init>()')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.ClassWithInnerClass', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(false);
  });

  it('should update whether they must share one of the end nodes after unfolding ', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('InnerClass', 'class')
          .callingConstructor('com.tngtech.SomeClass', '<init>()', '<init>()')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.ClassWithInnerClass', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', false);

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(true);
  });

  it('should be transformed correctly if the parent-package of the start-node is folded', () => {
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

  it('should be transformed correctly if the parent-package of the end-node is folded', () => {
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

  it('should be transformed correctly if the parent-package of the end-node and the parent-package of the start-node are folded', () => {
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

  it('should be transformed correctly if the parent-class of the start-node is folded', () => {
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

  it('should be transformed correctly if a package is unfolded again', () => {
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

  it('should be transformed correctly if two packages are unfolded again', () => {
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

  it('should be transformed correctly if a package is unfolded again, when another package is folded', () => {
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
    stubs.saveMovedDependenciesTo(movedDependenciesFirstTime);

    dependencies.moveAllToTheirPositions().then(() => stubs.saveMovedDependenciesTo(movedDependenciesSecondTime));
    const promise = dependencies.moveAllToTheirPositions();

    return promise.then(() => {
      /**
       * when the both invokes of moveAllToTheirPositions above are not executed after each other,
       * then the dependencies are not added to the second array
       */
      expect(movedDependenciesFirstTime).to.haveDependencyStrings(exp);
      expect(movedDependenciesSecondTime).to.haveDependencyStrings(exp);
    });
  });

  it('sets and applies the node filter correctly', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('MatchingClass1', 'class')
        .implementing('com.tngtech.SomeInterface')
        .callingConstructor('com.tngtech.MatchingClass2', 'startMethod()', '<init>()')
        .build())
      .add(testJson.clazz('MatchingClass2', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeInterface', 'interface')
        .accessingField('com.tngtech.SomeClass1', 'startMethod()', 'targetField')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.MatchingClass1->com.tngtech.MatchingClass2(constructorCall)',
      'com.tngtech.MatchingClass2->com.tngtech.MatchingClass1(methodCall)'
    ];
    root.filterByName('Matching', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('resets the node filter correctly', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('MatchingClass1', 'class')
        .implementing('com.tngtech.SomeInterface')
        .callingConstructor('com.tngtech.MatchingClass2', 'startMethod()', '<init>()')
        .build())
      .add(testJson.clazz('MatchingClass2', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeInterface', 'interface')
        .accessingField('com.tngtech.SomeClass1', 'startMethod()', 'targetField')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = [
      'com.tngtech.SomeClass1->com.tngtech.MatchingClass1(methodCall)',
      'com.tngtech.MatchingClass1->com.tngtech.SomeInterface(implements)',
      'com.tngtech.MatchingClass1->com.tngtech.MatchingClass2(constructorCall)',
      'com.tngtech.MatchingClass2->com.tngtech.MatchingClass1(methodCall)',
      'com.tngtech.SomeInterface->com.tngtech.SomeClass1(fieldAccess)'
    ];
    root.filterByName('Matching', false);
    dependencies.setNodeFilters(root.getFilters());
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should recreate correctly its visible dependencies after setting the node filter: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeClass1', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('MatchingClass1', 'class')
        .implementing('com.tngtech.SomeInterface')
        .callingConstructor('com.tngtech.MatchingClass2', 'startMethod()', '<init>()')
        .build())
      .add(testJson.clazz('MatchingClass2', 'class')
        .callingMethod('com.tngtech.MatchingClass1', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.clazz('SomeInterface', 'interface')
        .accessingField('com.tngtech.SomeClass1', 'startMethod()', 'targetField')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const filterForVisibleDependencies = d => d.from.startsWith('com.tngtech.MatchingClass') && d.to.startsWith('com.tngtech.MatchingClass');
    const hiddenDependencies = dependencies.getVisible().filter(d => !filterForVisibleDependencies(d));
    const visibleDependencies = dependencies.getVisible().filter(filterForVisibleDependencies);

    root.filterByName('Matching', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
  });

  it('updates on node filtering whether they must share one of the end nodes', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('InnerClass', 'class')
          .callingConstructor('com.tngtech.SomeClass', '<init>()', '<init>()')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.ClassWithInnerClass', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    //fold the class with the inner class, so that the two dependencies must share their nodes
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    root.filterByName('InnerClass', true);
    dependencies.setNodeFilters(root.getFilters());

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(true);
  });

  it('updates on resetting the node filter whether they must share one of the end nodes', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testJson.clazz('InnerClass', 'class')
          .callingConstructor('com.tngtech.SomeClass', '<init>()', '<init>()')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.ClassWithInnerClass', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    //fold the class with the inner class, so that the two dependencies must share their nodes
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    root.filterByName('InnerClass', true);
    dependencies.setNodeFilters(root.getFilters());
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(false);
  });

  it('can do this: fold pkg -> node filter, so that a dependency of the folded package is removed when the ' +
    'original end node of the dependency (which is hidden because of folding) is hidden through the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .callingMethod('com.tngtech.SomeInterface', 'startMethod()', 'targetMethod')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.pkgToFold.MatchingClassX', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.pkgToFold->com.tngtech.SomeInterface()'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: fold class -> node filter, so that a dependency of the folded class is changed when the ' +
    'dependency of its inner class (which is hidden through the filter) was merged with its own dependency', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements)'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: fold pkg -> node filter -> reset node filter, so that a dependency of the folded package is ' +
    'shown again when the original end node of the dependency (which is hidden because of folding) is shown again ' +
    'through resetting the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .callingMethod('com.tngtech.SomeInterface', 'startMethod()', 'targetMethod')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .add(testJson.clazz('SomeClass', 'class')
        .callingMethod('com.tngtech.pkgToFold.MatchingClassX', 'startMethod()', 'targetMethod()')
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.pkgToFold->com.tngtech.SomeInterface()',
      'com.tngtech.SomeClass->com.tngtech.pkgToFold()'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.filterByName('X', true);
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: fold class -> node filter -> reset node filter, so that can a dependency of the folded class ' +
    'is changed when the dependency of its inner class (which is shown again through resetting the filter) ' +
    'was merged with its own dependency', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements childrenAccess)'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.filterByName('X', true);
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: fold pkg -> node filter -> unfold pkg, so that the unfolding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface')
        .callingMethod('com.tngtech.pkgToFold.NotMatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeInterface->com.tngtech.pkgToFold.NotMatchingClass(methodCall)'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: fold class -> node filter -> unfold class, so that the unfolding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements)'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: filter -> fold pkg, so that folding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface')
        .callingMethod('com.tngtech.pkgToFold.NotMatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeInterface->com.tngtech.pkgToFold()'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: filter -> fold class, so that folding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements)'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: filter -> fold pkg -> unfold pkg, so that unfolding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface')
        .callingMethod('com.tngtech.pkgToFold.NotMatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeInterface->com.tngtech.pkgToFold.NotMatchingClass(methodCall)'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: filter -> fold class -> unfolding class, so that unfolding does not affect the filter', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements)'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });


  it('can do this: node filter -> fold pkg -> reset node filter, so that the fold state is not changed', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface')
        .callingMethod('com.tngtech.pkgToFold.NotMatchingClass', 'startMethod()', 'targetMethod()')
        .build())
      .add(testJson.package('pkgToFold')
        .add(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .add(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeInterface->com.tngtech.pkgToFold()',
      'com.tngtech.pkgToFold->com.tngtech.SomeInterface()'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can do this: node filter -> fold class -> reset node filter, so that the fold state is not changed', () => {
    const jsonRoot = testJson.package('com.tngtech')
      .add(testJson.clazz('SomeInterface', 'interface').build())
      .add(testJson.clazz('SomeClassWithInnerClass', 'class')
        .implementing('com.tngtech.SomeInterface')
        .havingInnerClass(testJson.clazz('MatchingClassX', 'class')
          .implementing('com.tngtech.SomeInterface')
          .build())
        .havingInnerClass(testJson.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const root = new Node(jsonRoot);
    const dependencies = new Dependencies(jsonRoot, root);

    const exp = ['com.tngtech.SomeClassWithInnerClass->com.tngtech.SomeInterface(implements childrenAccess)'];

    root.filterByName('X', true);
    dependencies.setNodeFilters(root.getFilters());
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.filterByName('', false);
    dependencies.setNodeFilters(root.getFilters());

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  const jsonRootWithAllDependencies = testJson.package('com.tngtech')
    .add(testJson.clazz('SomeInterface', 'interface').build())
    .add(testJson.clazz('SomeClass1', 'class')
      .extending('com.tngtech.SomeClass2')
      .callingConstructor('com.tngtech.SomeClass2', '<init>()', '<init>()')
      .callingMethod('com.tngtech.SomeClass2', 'startMethod()', 'targetMethod()')
      .build())
    .add(testJson.clazz('SomeClass2', 'class')
      .implementing('com.tngtech.SomeInterface')
      .accessingField('com.tngtech.SomeInterface', 'startMethod()', 'targetField')
      .implementingAnonymous('com.tngtech.SomeInterface')
      .havingInnerClass(testJson.clazz('SomeInnerClass', 'class')
        .callingMethod('com.tngtech.SomeClass2', 'startMethod()', 'targetMethod()')
        .build())
      .build())
    .build();

  it('should recreate correctly its visible dependencies after filtering by type (only show implementing an interface):' +
    ' old dependencies are hidden, all new ones are visible but they are not re-instantiated', () => {
    const root = new Node(jsonRootWithAllDependencies);
    const dependencies = new Dependencies(jsonRootWithAllDependencies, root);

    const filter = d1 => dependencies._elementary.filter(
      d2 =>
      d1.from === d2.from &&
      d1.to === d2.to &&
      d2.description.typeName === 'implements').length > 0;
    const visibleDependencies = dependencies.getVisible().filter(filter);
    const hiddenDependencies = dependencies.getVisible().filter(d => !filter(d));

    dependencies.filterByType({
      showImplementing: true,
      showExtending: false,
      showConstructorCall: false,
      showMethodCall: false,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDepsBetweenChildAndParent: true
    });


    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(dependencies.getVisible().map(d => d._view.isVisible)).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
  });

  it('updates the position of the dependencies after filtering by type', () => {
    const root = new Node(jsonRootWithAllDependencies);
    const dependencies = new Dependencies(jsonRootWithAllDependencies, root);

    dependencies.filterByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: false,
      showMethodCall: false,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDepsBetweenChildAndParent: true
    });

    return dependencies._updatePromise.then(() =>
      expect(dependencies.getVisible().map(d => d._view.hasJumpedToPosition)).to.not.include(false));
  });

  it('can filter by type: only show inheritance-dependencies', () => {
    const root = new Node(jsonRootWithAllDependencies);
    const dependencies = new Dependencies(jsonRootWithAllDependencies, root);

    const exp = [
      'com.tngtech.SomeClass1->com.tngtech.SomeClass2(extends)',
      'com.tngtech.SomeClass2->com.tngtech.SomeInterface(implements)'
    ];

    dependencies.filterByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: false,
      showMethodCall: false,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDepsBetweenChildAndParent: true
    });

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can reset the filter by type: show all dependencies again', () => {
    const root = new Node(jsonRootWithAllDependencies);
    const dependencies = new Dependencies(jsonRootWithAllDependencies, root);
    const exp = dependencies.getVisible().map(d => d.toString());

    dependencies.filterByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: false,
      showMethodCall: false,
      showFieldAccess: false,
      showAnonymousImplementation: false,
      showDependenciesBetweenClassAndItsInnerClasses: true
    });
    dependencies.filterByType({
      showImplementing: true,
      showExtending: true,
      showConstructorCall: true,
      showMethodCall: true,
      showFieldAccess: true,
      showAnonymousImplementation: true,
      showDependenciesBetweenClassAndItsInnerClasses: true
    });

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
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