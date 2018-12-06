'use strict';

import chai from 'chai';
import './chai/dependencies-chai-extension';
import './chai/node-chai-extensions';
import stubs from './stubs';
import {createTestDependencies, testRoot} from './test-json-creator';
import AppContext from '../../../main/app/report/app-context';
import {buildFilterCollection} from "../../../main/app/report/filter";

const expect = chai.expect;

const appContext = AppContext.newInstance({
  visualizationStyles: stubs.visualizationStylesStub(30),
  calculateTextWidth: stubs.calculateTextWidthStub,
  NodeView: stubs.NodeViewStub,
  DependencyView: stubs.DependencyViewStub
});
const Root = appContext.getRoot();
const Dependencies = appContext.getDependencies();

const updateFilterAndRelayout = (root, filterCollection, filterKey) => {
  root.doNextAndWaitFor(() => filterCollection.updateFilter(filterKey));
  root.relayoutCompletely();
};

/*
 * json-root with every kind of dependency of both groups (inheritance and access),
 * several different dependencies from one class to another one,
 * dependencies between a class and its inner class
 * and mutual dependencies (between separated classes and a class and its inner class)
 */
const jsonRoot = testRoot.package('com.tngtech')
  .add(testRoot.package('pkg1')
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('SomeClass2', 'class').build())
    .build())
  .add(testRoot.package('pkg2')
    .add(testRoot.clazz('SomeInterface1', 'interface').build())
    .add(testRoot.package('subpkg1')
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClassWithInnerInterface', 'class')
        .havingInnerClass(testRoot.clazz('SomeInnerInterface', 'interface').build())
        .havingInnerClass(testRoot.clazz('1', 'class').build())
        .build())
      .build())
    .build())
  .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
    .havingInnerClass(testRoot.clazz('1', 'class').build())
    .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
    .build())
  .build();
const jsonDependencies = createTestDependencies()
  .addMethodCall().from('com.tngtech.pkg1.SomeClass1', 'startMethod(arg1, arg2)')
  .to('com.tngtech.pkg1.SomeClass2', 'targetMethod()')
  .addFieldAccess().from('com.tngtech.pkg1.SomeClass1', 'startMethod(arg1, arg2)')
  .to('com.tngtech.pkg1.SomeClass2', 'targetField')
  .addInheritance().from('com.tngtech.pkg1.SomeClass1')
  .to('com.tngtech.pkg2.SomeInterface1')
  .addFieldAccess().from('com.tngtech.pkg1.SomeClass2', 'startMethod(arg)')
  .to('com.tngtech.pkg1.SomeClass1', 'targetField')
  .addInheritance().from('com.tngtech.pkg2.subpkg1.SomeClass1')
  .to('com.tngtech.pkg1.SomeClass1')
  .addConstructorCall().from('com.tngtech.pkg2.subpkg1.SomeClass1', '<init>()')
  .to('com.tngtech.pkg1.SomeClass1', '<init>()')
  .addMethodCall().from('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface', 'startMethod(arg)')
  .to('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface', 'targetMethod(arg1, arg2)')
  .addInheritance().from('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$1')
  .to('com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface')
  .addInheritance().from('com.tngtech.SomeClassWithInnerClass$1')
  .to('com.tngtech.pkg2.SomeInterface1')
  .addFieldAccess().from('com.tngtech.SomeClassWithInnerClass$SomeInnerClass', 'startMethod1()')
  .to('com.tngtech.SomeClassWithInnerClass', 'targetField')
  .addFieldAccess().from('com.tngtech.SomeClassWithInnerClass$SomeInnerClass', 'startMethod2()')
  .to('com.tngtech.SomeClassWithInnerClass', 'targetField')
  .build();

const root = new Root(jsonRoot, null, () => Promise.resolve());

const jsonRootWithTwoClassesAndTwoDeps = testRoot.package('com.tngtech')
  .add(testRoot.clazz('SomeClass1', 'class').build())
  .add(testRoot.clazz('SomeClass2', 'class').build())
  .build();

const jsonDependenciesWithTwo = createTestDependencies()
  .addFieldAccess().from('com.tngtech.SomeClass1', 'startMethod()')
  .to('com.tngtech.SomeClass2', 'targetField')
  .addFieldAccess().from('com.tngtech.SomeClass2', 'startMethod()')
  .to('com.tngtech.SomeClass1', 'targetField')
  .build();

const rootWithTwoClassesAndTwoDeps = new Root(jsonRootWithTwoClassesAndTwoDeps, null, () => Promise.resolve());

describe('Dependencies', () => {
  it('creates correct elementary dependencies from json-input', () => {
    const dependencies = new Dependencies(jsonDependencies, root);
    const exp = [
      '<com.tngtech.pkg1.SomeClass1.startMethod(arg1, arg2)> METHOD_CALL to <com.tngtech.pkg1.SomeClass2.targetMethod()>',
      '<com.tngtech.pkg1.SomeClass1.startMethod(arg1, arg2)> FIELD_ACCESS to <com.tngtech.pkg1.SomeClass2.targetField>',
      '<com.tngtech.pkg1.SomeClass1> INHERITANCE to <com.tngtech.pkg2.SomeInterface1>',
      '<com.tngtech.pkg1.SomeClass2.startMethod(arg)> FIELD_ACCESS to <com.tngtech.pkg1.SomeClass1.targetField>',
      '<com.tngtech.pkg2.subpkg1.SomeClass1> INHERITANCE to <com.tngtech.pkg1.SomeClass1>',
      '<com.tngtech.pkg2.subpkg1.SomeClass1.<init>()> CONSTRUCTOR_CALL to <com.tngtech.pkg1.SomeClass1.<init>()>',
      '<com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface.startMethod(arg)> METHOD_CALL to <com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface.targetMethod(arg1, arg2)>',
      '<com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$1> INHERITANCE to <com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface>',
      '<com.tngtech.SomeClassWithInnerClass$1> INHERITANCE to <com.tngtech.pkg2.SomeInterface1>',
      '<com.tngtech.SomeClassWithInnerClass$SomeInnerClass.startMethod1()> FIELD_ACCESS to <com.tngtech.SomeClassWithInnerClass.targetField>',
      '<com.tngtech.SomeClassWithInnerClass$SomeInnerClass.startMethod2()> FIELD_ACCESS to <com.tngtech.SomeClassWithInnerClass.targetField>'
    ];
    expect(dependencies._elementary).to.haveDependencyStrings(exp);
  });

  it('creates correct visible dependencies from the elementary dependencies', () => {
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();
    const exp = [
      'com.tngtech.pkg1.SomeClass1-com.tngtech.pkg1.SomeClass2',
      'com.tngtech.pkg1.SomeClass1-com.tngtech.pkg2.SomeInterface1',
      'com.tngtech.pkg1.SomeClass2-com.tngtech.pkg1.SomeClass1',
      'com.tngtech.pkg2.subpkg1.SomeClass1-com.tngtech.pkg1.SomeClass1',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface-com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface',
      'com.tngtech.SomeClassWithInnerClass$SomeInnerClass-com.tngtech.SomeClassWithInnerClass',
      'com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$1-com.tngtech.pkg2.subpkg1.SomeClassWithInnerInterface$SomeInnerInterface',
      'com.tngtech.SomeClassWithInnerClass$1-com.tngtech.pkg2.SomeInterface1',
    ];
    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
    expect(dependencies.getVisible().map(dependency => dependency.isVisible())).to.not.include(false);
  });

  it('know if they must share one of the end nodes', () => {
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();
    const hasEndNodes = (node1, node2) => d => (d.from === node1 || d.to === node1) && (d.from === node2 || d.to === node2);
    const filter = d => hasEndNodes('com.tngtech.pkg1.SomeClass1', 'com.tngtech.pkg1.SomeClass2')(d);
    const dependenciesSharingNodes = dependencies.getVisible().filter(filter);
    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependenciesSharingNodes)).to.not.include(false);
    expect(mapToMustShareNodes(dependencies.getVisible().filter(d => !filter(d)))).to.not.include(true);
  });

  it('should recreate correctly its visible dependencies after folding a package: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .add(testRoot.clazz('TargetClass', 'class')
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.startPkg.StartClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.TargetClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();

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

  it('should recreate correctly its visible dependencies after folding several nodes: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated, dependencies are correctly transformed', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkg1')
        .add(testRoot.clazz('SomeClass', 'class').build())
        .build())
      .add(testRoot.package('pkg2')
        .add(testRoot.clazz('SomeClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.pkg1.SomeClass', 'startMethod()')
      .to('com.tngtech.pkg2.SomeClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkg1.SomeClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.pkg2.SomeClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();

    const filterForHiddenDependencies = d => d.from === 'com.tngtech.pkg1.SomeClass' ||
      d.from === 'com.tngtech.pkg2.SomeClass' || d.to === 'com.tngtech.pkg2.SomeClass';
    const hiddenDependencies = dependencies.getVisible().filter(filterForHiddenDependencies);
    const visibleDependencies = dependencies.getVisible().filter(d => !filterForHiddenDependencies(d));

    dependencies.noteThatNodeFolded('com.tngtech.pkg1', true);
    dependencies.noteThatNodeFolded('com.tngtech.pkg2', true);
    dependencies.recreateVisible();

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);

    //ensure that the dependencies are not recreated
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);

    const exp = [
      'com.tngtech.pkg1-com.tngtech.pkg2',
      'com.tngtech.pkg1-com.tngtech.SomeInterface',
      'com.tngtech.pkg2-com.tngtech.SomeInterface',
      'com.tngtech.SomeClass-com.tngtech.SomeInterface'
    ];

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should recreate its visible dependencies correctly after folding a class with an inner class: old dependencies ' +
    'are hidden, all new ones are visible but they are not re-instantiated', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('StartClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('InnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('TargetClass', 'class').build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.StartClassWithInnerClass$InnerClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addMethodCall().from('com.tngtech.StartClassWithInnerClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.StartClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();

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
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .add(testRoot.clazz('TargetClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.startPkg.StartClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.TargetClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();

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

  const jsonRootSharingNodes = testRoot.package('com.tngtech')
    .add(testRoot.clazz('ClassWithInnerClass', 'class')
      .havingInnerClass(testRoot.clazz('InnerClass', 'class').build())
      .build())
    .add(testRoot.clazz('SomeClass', 'class').build())
    .build();
  const jsonDependenciesSharingNodes = createTestDependencies()
    .addConstructorCall().from('com.tngtech.ClassWithInnerClass$InnerClass', '<init>()')
    .to('com.tngtech.SomeClass', '<init>()')
    .addMethodCall().from('com.tngtech.SomeClass', 'startMethod()')
    .to('com.tngtech.ClassWithInnerClass', 'targetMethod()')
    .build();

  it('should update whether they must share one of the end nodes after folding', () => {
    const root = new Root(jsonRootSharingNodes, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesSharingNodes, root);

    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(false);
  });

  it('should update whether they must share one of the end nodes after unfolding ', () => {
    const root = new Root(jsonRootSharingNodes, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesSharingNodes, root);

    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', false);

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(true);
  });

  it('should be transformed correctly if the parent-package of the start-node is folded', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .add(testRoot.clazz('TargetClass', 'class').build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.startPkg.StartClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.TargetClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      'com.tngtech.startPkg-com.tngtech.TargetClass',
      'com.tngtech.startPkg-com.tngtech.SomeInterface',
      'com.tngtech.TargetClass-com.tngtech.SomeInterface'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if the parent-package of the end-node is folded', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('targetPkg')
        .add(testRoot.clazz('TargetClass', 'class').build())
        .build())
      .add(testRoot.clazz('StartClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addConstructorCall().from('com.tngtech.SomeInterface', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', '<init>()')
      .addMethodCall().from('com.tngtech.StartClass', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.StartClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      'com.tngtech.StartClass-com.tngtech.targetPkg',
      'com.tngtech.SomeInterface-com.tngtech.targetPkg',
      'com.tngtech.StartClass-com.tngtech.SomeInterface'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if the parent-package of the end-node and the parent-package of the start-node are folded', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.package('targetPkg')
        .add(testRoot.clazz('TargetClass', 'class').build())
        .build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', 'targetMethod()')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      'com.tngtech.startPkg-com.tngtech.targetPkg'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if the parent-class of the start-node is folded', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.package('targetPkg')
        .add(testRoot.clazz('TargetClass', 'class').build())
        .build())
      .add(testRoot.clazz('StartClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('StartClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.StartClassWithInnerClass$StartClass', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', 'targetMethod()')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      'com.tngtech.StartClassWithInnerClass-com.tngtech.targetPkg.TargetClass'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.StartClassWithInnerClass', true);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if a package is unfolded again', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .add(testRoot.clazz('TargetClass', 'class').build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.TargetClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.startPkg.StartClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.TargetClass')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      'com.tngtech.startPkg.StartClass-com.tngtech.TargetClass',
      'com.tngtech.startPkg.StartClass-com.tngtech.SomeInterface',
      'com.tngtech.TargetClass-com.tngtech.SomeInterface'
    ];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if two packages are unfolded again', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.package('targetPkg')
        .add(testRoot.clazz('TargetClass', 'class').build())
        .build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', 'targetMethod()')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = ['com.tngtech.startPkg.StartClass-com.tngtech.targetPkg.TargetClass'];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('should be transformed correctly if a package is unfolded again, when another package is folded', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.package('targetPkg')
        .add(testRoot.clazz('TargetClass', 'class').build())
        .build())
      .add(testRoot.package('startPkg')
        .add(testRoot.clazz('StartClass', 'class').build())
        .build())
      .build();

    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.startPkg.StartClass', 'startMethod()')
      .to('com.tngtech.targetPkg.TargetClass', 'targetMethod()')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = ['com.tngtech.startPkg.StartClass-com.tngtech.targetPkg'];

    dependencies.updateOnNodeFolded('com.tngtech.startPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.targetPkg', true);
    dependencies.updateOnNodeFolded('com.tngtech.startPkg', false);

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  const jsonRootForMoveTest = testRoot.package('com.tngtech')
    .add(testRoot.clazz('SomeInterface', 'interface').build())
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('SomeClass2', 'class').build())
    .build();

  const jsonDependenciesForMoveTest = createTestDependencies()
    .addFieldAccess().from('com.tngtech.SomeClass1', 'startMethod()')
    .to('com.tngtech.SomeClass2', 'targetField')
    .addMethodCall().from('com.tngtech.SomeClass2', 'startMethod()')
    .to('com.tngtech.SomeClass1', 'targetField')
    .addInheritance().from('com.tngtech.SomeClass2')
    .to('com.tngtech.SomeInterface')
    .build();

  it('can jump the dependencies of a specific node to their positions', () => {
    const root = new Root(jsonRootForMoveTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForMoveTest, root);
    dependencies.recreateVisible();

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
    const root = new Root(jsonRootForMoveTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForMoveTest, root);
    dependencies.recreateVisible();

    const promise = dependencies.moveAllToTheirPositions();

    const mapDependenciesToHasMoved = dependencies => dependencies.map(d => d._view.hasMovedToPosition);
    return promise.then(() => expect(mapDependenciesToHasMoved(dependencies.getVisible())).to.not.include(false));
  });

  it('can move all dependencies to their positions twice in a row: the second move does not start before the first is ended', () => {
    const root = new Root(jsonRootForMoveTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForMoveTest, root);
    dependencies.recreateVisible();
    const exp = [
      'com.tngtech.SomeClass1-com.tngtech.SomeClass2',
      'com.tngtech.SomeClass2-com.tngtech.SomeClass1',
      'com.tngtech.SomeClass2-com.tngtech.SomeInterface',
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

  const jsonRootForFilterTest = testRoot.package('com.tngtech')
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('MatchingClass1', 'class').build())
    .add(testRoot.clazz('MatchingClass2', 'class').build())
    .add(testRoot.clazz('SomeInterface', 'interface').build())
    .build();

  const jsonDependenciesForFilterTest = createTestDependencies()
    .addMethodCall().from('com.tngtech.SomeClass1', 'startMethod()')
    .to('com.tngtech.MatchingClass1', 'targetMethod()')
    .addConstructorCall().from('com.tngtech.MatchingClass1', 'startMethod()')
    .to('com.tngtech.MatchingClass2', '<init>()')
    .addInheritance().from('com.tngtech.MatchingClass1')
    .to('com.tngtech.SomeInterface')
    .addMethodCall().from('com.tngtech.MatchingClass2', 'startMethod()')
    .to('com.tngtech.MatchingClass1', 'targetMethod()')
    .addFieldAccess().from('com.tngtech.SomeInterface', 'startMethod()')
    .to('com.tngtech.SomeClass1', 'targetField')
    .build();

  it('sets and applies the node filter correctly', () => {
    const root = new Root(jsonRootForFilterTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForFilterTest, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = [
      'com.tngtech.MatchingClass2-com.tngtech.MatchingClass1',
      'com.tngtech.MatchingClass1-com.tngtech.MatchingClass2'
    ];

    root.nameFilterString = '*Matching*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('resets the node filter correctly', () => {
    const root = new Root(jsonRootForFilterTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForFilterTest, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = [
      'com.tngtech.SomeClass1-com.tngtech.MatchingClass1',
      'com.tngtech.MatchingClass1-com.tngtech.SomeInterface',
      'com.tngtech.MatchingClass1-com.tngtech.MatchingClass2',
      'com.tngtech.MatchingClass2-com.tngtech.MatchingClass1',
      'com.tngtech.SomeInterface-com.tngtech.SomeClass1'
    ];

    root.nameFilterString = '*Matching*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('should recreate correctly its visible dependencies after setting the node filter: old dependencies are hidden, ' +
    'all new ones are visible but they are not re-instantiated', () => {
    const root = new Root(jsonRootForFilterTest, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesForFilterTest, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();
    dependencies.recreateVisible();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const filterForVisibleDependencies = d => d.from.startsWith('com.tngtech.MatchingClass') && d.to.startsWith('com.tngtech.MatchingClass');
    const hiddenDependencies = dependencies.getVisible().filter(d => !filterForVisibleDependencies(d));
    const visibleDependencies = dependencies.getVisible().filter(filterForVisibleDependencies);

    root.nameFilterString = '*Matching*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() => {
      expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
      expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
      expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
      expect(dependencies.getVisible()).to.include.members(visibleDependencies);
    });
  });

  it('updates on node filtering whether they must share one of the end nodes', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('InnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addConstructorCall().from('com.tngtech.ClassWithInnerClass$InnerClass', '<init>()')
      .to('com.tngtech.SomeClass', '<init>()')
      .addMethodCall().from('com.tngtech.SomeClass', 'startMethod()')
      .to('com.tngtech.ClassWithInnerClass', 'targetMethod()')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    //fold the class with the inner class, so that the two dependencies must share their nodes
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    root.nameFilterString = '~*InnerClass*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);

    return root._updatePromise.then(() =>
      expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(true));
  });

  it('updates on resetting the node filter whether they must share one of the end nodes', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('ClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('InnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addConstructorCall().from('com.tngtech.ClassWithInnerClass$InnerClass', '<init>()')
      .to('com.tngtech.SomeClass', '<init>()')
      .addMethodCall().from('com.tngtech.SomeClass', 'startMethod()')
      .to('com.tngtech.ClassWithInnerClass', 'targetMethod()')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    //fold the class with the inner class, so that the two dependencies must share their nodes
    dependencies.updateOnNodeFolded('com.tngtech.ClassWithInnerClass', true);

    root.nameFilterString = '~*InnerClass*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    const mapToMustShareNodes = dependencies => dependencies.map(d => d.visualData.mustShareNodes);
    return root._updatePromise.then(() =>
      expect(mapToMustShareNodes(dependencies.getVisible())).to.not.include(false));
  });

  it('can do this: fold pkg -> node filter, so that a dependency of the folded package is removed when the ' +
    'original end node of the dependency (which is hidden because of folding) is hidden through the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.pkgToFold.MatchingClassX', 'startMethod()')
      .to('com.tngtech.SomeInterface', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkgToFold.NotMatchingClass')
      .to('com.tngtech.SomeInterface')
      .addMethodCall().from('com.tngtech.SomeClass', 'startMethod()')
      .to('com.tngtech.pkgToFold.MatchingClassX', 'targetMethod()')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.pkgToFold-com.tngtech.SomeInterface'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: fold class -> node filter, so that a dependency of the folded class is changed when the ' +
    'dependency of its inner class (which is hidden through the filter) was merged with its own dependency', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeInterface2', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface2')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();
    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: fold pkg -> node filter -> reset node filter, so that a dependency of the folded package is ' +
    'shown again when the original end node of the dependency (which is hidden because of folding) is shown again ' +
    'through resetting the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.pkgToFold.MatchingClassX', 'startMethod()')
      .to('com.tngtech.SomeInterface', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkgToFold.NotMatchingClass')
      .to('com.tngtech.SomeInterface')
      .addMethodCall().from('com.tngtech.SomeClass', 'startMethod()')
      .to('com.tngtech.pkgToFold.MatchingClassX', 'targetMethod()')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();
    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.pkgToFold-com.tngtech.SomeInterface',
      'com.tngtech.SomeClass-com.tngtech.pkgToFold'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: fold class -> node filter -> reset node filter, so that can a dependency of the folded class ' +
    'is changed when the dependency of its inner class (which is shown again through resetting the filter) ' +
    'was merged with its own dependency', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeInterface2', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface2')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();
    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface',
      'com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface2'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: fold pkg -> node filter -> unfold pkg, so that the unfolding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeInterface', 'startMethod()')
      .to('com.tngtech.pkgToFold.NotMatchingClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkgToFold.MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeInterface-com.tngtech.pkgToFold.NotMatchingClass'];

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', false);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: fold class -> node filter -> unfold class, so that the unfolding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .havingInnerClass(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface'];

    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', false);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: filter -> fold pkg, so that folding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeInterface', 'startMethod()')
      .to('com.tngtech.pkgToFold.NotMatchingClass', 'targetMehtod()')
      .addInheritance().from('com.tngtech.pkgToFold.MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeInterface-com.tngtech.pkgToFold'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: filter -> fold class, so that folding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .havingInnerClass(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: filter -> fold pkg -> unfold pkg, so that unfolding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeInterface', 'startMethod()')
      .to('com.tngtech.pkgToFold.NotMatchingClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkgToFold.MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeInterface-com.tngtech.pkgToFold.NotMatchingClass'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', false);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: filter -> fold class -> unfolding class, so that unfolding does not affect the filter', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .havingInnerClass(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', false);

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });


  it('can do this: node filter -> fold pkg -> reset node filter, so that the fold state is not changed', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.package('pkgToFold')
        .add(testRoot.clazz('MatchingClassX', 'class').build())
        .add(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeInterface', 'startMethod()')
      .to('com.tngtech.pkgToFold.NotMatchingClass', 'targetMethod()')
      .addInheritance().from('com.tngtech.pkgToFold.MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeInterface-com.tngtech.pkgToFold',
      'com.tngtech.pkgToFold-com.tngtech.SomeInterface'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.pkgToFold', true);
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  it('can do this: node filter -> fold class -> reset node filter, so that the fold state is not changed', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeClassWithInnerClass', 'class')
        .havingInnerClass(testRoot.clazz('MatchingClassX', 'class').build())
        .havingInnerClass(testRoot.clazz('NotMatchingClass', 'class').build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass')
      .to('com.tngtech.SomeInterface')
      .addInheritance().from('com.tngtech.SomeClassWithInnerClass$MatchingClassX')
      .to('com.tngtech.SomeInterface')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    root.addListener(dependencies.createListener());
    root.getLinks = () => dependencies.getAllLinks();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(root.filterGroup)
      .addFilterGroup(dependencies.filterGroup)
      .build();
    root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
    root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');

    const exp = ['com.tngtech.SomeClassWithInnerClass-com.tngtech.SomeInterface'];

    root.nameFilterString = '~*X*';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');
    dependencies.updateOnNodeFolded('com.tngtech.SomeClassWithInnerClass', true);
    root.nameFilterString = '';
    updateFilterAndRelayout(root, filterCollection, 'nodes.name');

    return root._updatePromise.then(() =>
      expect(dependencies.getVisible()).to.haveDependencyStrings(exp));
  });

  const jsonRootWithAllDependencies = testRoot.package('com.tngtech')
    .add(testRoot.clazz('SomeInterface', 'interface').build())
    .add(testRoot.clazz('SomeClass1', 'class').build())
    .add(testRoot.clazz('SomeClass2', 'class')
      .havingInnerClass(testRoot.clazz('1', 'class').build())
      .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
      .build())
    .build();
  const jsonDependenciesAll = createTestDependencies()
    .addInheritance().from('com.tngtech.SomeClass1').to('com.tngtech.SomeClass2')
    .addConstructorCall().from('com.tngtech.SomeClass1', '<init>()')
    .to('com.tngtech.SomeClass2', '<init>()')
    .addMethodCall().from('com.tngtech.SomeClass1', 'startMethod()')
    .to('com.tngtech.SomeClass2', 'targetMethod()')
    .addInheritance().from('com.tngtech.SomeClass2').to('com.tngtech.SomeInterface')
    .addFieldAccess().from('com.tngtech.SomeClass2', 'startMethod()')
    .to('com.tngtech.SomeInterface', 'targetField')
    .addInheritance().from('com.tngtech.SomeClass2$1')
    .to('com.tngtech.SomeInterface')
    .addMethodCall().from('com.tngtech.SomeClass2$SomeInnerClass', 'startMethod()')
    .to('com.tngtech.SomeClass2', 'targetField')
    .build();

  it('should recreate correctly its visible dependencies after filtering by type (only show inheritance):' +
    ' old dependencies are hidden, all new ones are visible but they are not re-instantiated', () => {
    const root = new Root(jsonRootWithAllDependencies, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesAll, root);
    dependencies.recreateVisible();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    const filter = d1 => dependencies._elementary.filter(
      d2 =>
        d1.from === d2.from &&
        d1.to === d2.to &&
        d2.type === 'INHERITANCE').length > 0;
    const visibleDependencies = dependencies.getVisible().filter(filter);
    const hiddenDependencies = dependencies.getVisible().filter(d => !filter(d));

    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: false,
      FIELD_ACCESS: false
    });

    filterCollection.updateFilter('dependencies.type');

    expect(dependencies.getVisible().map(d => d.isVisible())).to.not.include(false);
    expect(dependencies.getVisible().map(d => d._view.isVisible)).to.not.include(false);
    expect(hiddenDependencies.map(d => d.isVisible())).to.not.include(true);
    expect(hiddenDependencies.map(d => d._view.isVisible)).to.not.include(true);
    expect(dependencies.getVisible()).to.include.members(visibleDependencies);
  });

  it('can filter by type: only show inheritance-dependencies', () => {
    const root = new Root(jsonRootWithAllDependencies, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesAll, root);

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    const exp = [
      'com.tngtech.SomeClass1-com.tngtech.SomeClass2',
      'com.tngtech.SomeClass2-com.tngtech.SomeInterface',
      'com.tngtech.SomeClass2$1-com.tngtech.SomeInterface'
    ];

    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: false,
      FIELD_ACCESS: false
    });
    filterCollection.updateFilter('dependencies.type');

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can filter by type: hide dependencies between a class and its inner classes', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeClass', 'class')
        .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeOtherClass', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.SomeClass').to('com.tngtech.SomeClass$SomeInnerClass')
      .addFieldAccess().from('com.tngtech.SomeClass$SomeInnerClass').to('com.tngtech.SomeClass')
      .addMethodCall().from('com.tngtech.SomeClass').to('com.tngtech.SomeOtherClass')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    const exp = [
      'com.tngtech.SomeClass-com.tngtech.SomeOtherClass'
    ];

    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: true,
      METHOD_CALL: true,
      FIELD_ACCESS: true,
      INNERCLASS_DEPENDENCY: false
    });
    filterCollection.updateFilter('dependencies.type');

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can reset the filter by type: show all dependencies again', () => {
    const root = new Root(jsonRootWithAllDependencies, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependenciesAll, root);
    dependencies.recreateVisible();

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    const exp = dependencies.getVisible().map(d => d.toString());

    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: false,
      METHOD_CALL: false,
      FIELD_ACCESS: false
    });
    filterCollection.updateFilter('dependencies.type');
    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: true,
      METHOD_CALL: true,
      FIELD_ACCESS: true,
      INNERCLASS_DEPENDENCY: true
    });
    filterCollection.updateFilter('dependencies.type');

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('creates the correct detailed dependencies of a class without children to another class: all grouped elementary ' +
    'dependencies are listed', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeInterface', 'interface').build())
      .add(testRoot.clazz('SomeClass1', 'class').build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeClass1', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetMethod(arg)')
      .addFieldAccess().from('com.tngtech.SomeClass1', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetField')
      .addConstructorCall().from('com.tngtech.SomeClass1', '<init>()')
      .to('com.tngtech.SomeClass2', '<init>()')
      .addInheritance().from('com.tngtech.SomeClass1')
      .to('com.tngtech.SomeClass2')
      .addInheritance().from('com.tngtech.SomeClass1')
      .to('com.tngtech.SomeInterface')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      '<com.tngtech.SomeClass1.startMethod(arg)> METHOD_CALL to <com.tngtech.SomeClass2.targetMethod(arg)>',
      '<com.tngtech.SomeClass1.startMethod(arg)> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>',
      '<com.tngtech.SomeClass1.<init>()> CONSTRUCTOR_CALL to <com.tngtech.SomeClass2.<init>()>',
      '<com.tngtech.SomeClass1> INHERITANCE to <com.tngtech.SomeClass2>'
    ];

    const act = dependencies.getDetailedDependenciesOf('com.tngtech.SomeClass1', 'com.tngtech.SomeClass2');
    expect(act).to.deep.equal(exp);
  });

  it('creates the correct detailed dependencies of a class with an inner class to another class: ' +
    'all grouped elementary dependencies are listed, but the dependencies of the inner classes are ignored', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeClass1', 'class')
        .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeClass1', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetMethod(arg)')
      .addMethodCall().from('com.tngtech.SomeClass1$SomeInnerClass', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetMethod(arg)')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      '<com.tngtech.SomeClass1.startMethod(arg)> METHOD_CALL to <com.tngtech.SomeClass2.targetMethod(arg)>'
    ];

    const act = dependencies.getDetailedDependenciesOf('com.tngtech.SomeClass1', 'com.tngtech.SomeClass2');
    expect(act).to.deep.equal(exp);
  });

  it('creates the correct detailed dependencies of a folded class with an inner class to another class: ' +
    'all grouped elementary dependencies, included the ones of the inner class, are listed', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeClass1', 'class')
        .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
        .build())
      .add(testRoot.clazz('SomeClass2', 'class').build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.SomeClass1', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetMethod(arg)')
      .addMethodCall().from('com.tngtech.SomeClass1$SomeInnerClass', 'startMethod(arg)')
      .to('com.tngtech.SomeClass2', 'targetMethod(arg)')
      .build();
    const root = new Root(jsonRoot, null, () => Promise.resolve());
    root.getLinks = () => [];
    const dependencies = new Dependencies(jsonDependencies, root);

    const exp = [
      '<com.tngtech.SomeClass1.startMethod(arg)> METHOD_CALL to <com.tngtech.SomeClass2.targetMethod(arg)>',
      '<com.tngtech.SomeClass1$SomeInnerClass.startMethod(arg)> METHOD_CALL to <com.tngtech.SomeClass2.targetMethod(arg)>'
    ];

    root.getByName('com.tngtech.SomeClass1')._changeFoldIfInnerNodeAndRelayout();
    dependencies.updateOnNodeFolded('com.tngtech.SomeClass1', true);

    const act = dependencies.getDetailedDependenciesOf('com.tngtech.SomeClass1', 'com.tngtech.SomeClass2');
    expect(act).to.deep.equal(exp);
  });

  it('create correct links, which are used for the layout of the nodes', () => {
    const jsonRoot = testRoot.package('com.tngtech')
      .add(testRoot.clazz('SomeClass', 'class').build())
      .add(testRoot.package('pkg1')
        .add(testRoot.package('subpkg')
          .add(testRoot.clazz('SomeClass', 'class').build())
          .build())
        .build())
      .add(testRoot.package('pkg2')
        .add(testRoot.package('subpkg')
          .add(testRoot.clazz('SomeClass', 'class')
            .havingInnerClass(testRoot.clazz('SomeInnerClass', 'class').build())
            .build())
          .build())
        .build())
      .build();
    const jsonDependencies = createTestDependencies()
      .addMethodCall().from('com.tngtech.pkg1.subpkg.SomeClass', 'startMethod()')
      .to('com.tngtech.pkg2.subpkg.SomeClass', 'targetMethod()')
      .addMethodCall().from('com.tngtech.pkg2.subpkg.SomeClass', 'startMethod()')
      .to('com.tngtech.SomeClass', 'targetMethod()')
      .addMethodCall().from('com.tngtech.pkg2.subpkg.SomeClass$SomeInnerClass', 'startMethod()')
      .to('com.tngtech.pkg2.subpkg.SomeClass', 'targetMethod()')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());
    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.recreateVisible();

    const exp = [
      {
        'source': 'com.tngtech.pkg1',
        'target': 'com.tngtech.pkg2'
      },
      {
        'source': 'com.tngtech.pkg1.subpkg',
        'target': 'com.tngtech.pkg2.subpkg'
      },
      {
        'source': 'com.tngtech.pkg1.subpkg.SomeClass',
        'target': 'com.tngtech.pkg2.subpkg.SomeClass'
      },
      {
        'source': 'com.tngtech.SomeClass',
        'target': 'com.tngtech.pkg2'
      },
      {
        'source': 'com.tngtech.SomeClass',
        'target': 'com.tngtech.pkg2.subpkg'
      },
      {
        'source': 'com.tngtech.SomeClass',
        'target': 'com.tngtech.pkg2.subpkg.SomeClass'
      },
      {
        'source': 'com.tngtech.pkg2.subpkg.SomeClass$SomeInnerClass',
        'target': 'com.tngtech.pkg2.subpkg.SomeClass',
      }
    ];
    const act = dependencies.getAllLinks();

    expect(act).to.deep.equal(exp);
  });

  it('can show a violation: all dependencies of the violation are marked', () => {
    const rule = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);
    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    expect(dependencies.getVisible().filter(d => d.from === 'com.tngtech.SomeClass1')[0].isViolation).to.be.true;
    expect(dependencies.getVisible().filter(d => d.from === 'com.tngtech.SomeClass2')[0].isViolation).to.be.false;
  });

  it('can hide a violation again: the corresponding dependencies are unmarked', () => {
    const rule = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    dependencies.hideViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    expect(dependencies.getVisible().map(d => d.isViolation)).to.not.include(true);
  });

  it('does not unmark a dependency on hiding a violation if this dependency is part of another violation ' +
    '(which is not hidden)', () => {
    const rule1 = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const rule2 = {
      rule: 'rule2',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule1);
    filterCollection.updateFilter('dependencies.violations');

    dependencies.showViolations(rule2);
    filterCollection.updateFilter('dependencies.violations');

    dependencies.hideViolations(rule1);
    filterCollection.updateFilter('dependencies.violations');

    expect(dependencies.getVisible().filter(d => d.from === 'com.tngtech.SomeClass1')[0].isViolation).to.be.true;
    expect(dependencies.getVisible().filter(d => d.from === 'com.tngtech.SomeClass2')[0].isViolation).to.be.false;
  });

  it('shows all dependencies again when the last violation-rule is hidden again', () => {
    const rule = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);

    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = true;
    filterCollection.updateFilter('dependencies.violations');

    dependencies.hideViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    const exp = ['com.tngtech.SomeClass1-com.tngtech.SomeClass2',
      'com.tngtech.SomeClass2-com.tngtech.SomeClass1'];

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can hide all dependencies that are not part of a violation when a violation is shown', () => {
    const rule = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);
    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);
    filterCollection.updateFilter('dependencies.violations');
    filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = true;
    filterCollection.updateFilter('dependencies.violations');

    const exp = ['com.tngtech.SomeClass1-com.tngtech.SomeClass2'];

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can show all dependencies, also those that are not part of a violation, again', () => {
    const rule = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };
    const dependencies = new Dependencies(jsonDependenciesWithTwo, rootWithTwoClassesAndTwoDeps);
    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);
    filterCollection.updateFilter('dependencies.violations');

    filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = true;
    filterCollection.updateFilter('dependencies.violations');

    filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = false;
    filterCollection.updateFilter('dependencies.violations');

    const exp = ['com.tngtech.SomeClass1-com.tngtech.SomeClass2',
      'com.tngtech.SomeClass2-com.tngtech.SomeClass1'];

    expect(dependencies.getVisible()).to.haveDependencyStrings(exp);
  });

  it('can return all node-fullnames containing violations', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.package('pkg1')
          .add(testRoot.package('pkg2')
            .add(testRoot.package('pkg3')
              .add(testRoot.clazz('SomeClass2', 'class').build())
              .build())
            .add(testRoot.clazz('SomeClass1', 'class').build())
            .build())
          .build())
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .add(testRoot.clazz('SomeClass2', 'class').build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.pkg3.SomeClass2')
      .to('com.tngtech.pkg1.pkg2.SomeClass1')
      .addFieldAccess().from('com.tngtech.SomeClass1', 'startMethod()')
      .to('com.tngtech.SomeClass2', 'targetField')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());

    const rule1 = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };

    const rule2 = {
      rule: 'rule2',
      violations: [
        '<com.tngtech.pkg1.pkg2.pkg3.SomeClass2> INHERITANCE to <com.tngtech.pkg1.pkg2.SomeClass1>'
      ]
    };

    const dependencies = new Dependencies(jsonDependencies, root);
    dependencies.showViolations(rule1);
    dependencies.showViolations(rule2);

    const exp = ['com.tngtech', 'com.tngtech.pkg1.pkg2'];

    expect(dependencies.getNodesContainingViolations()).to.containExactlyNodes(exp);
  });

  it('can return a set of all nodes that are involved in violations when these nodes are classes only', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.package('pkg1')
          .add(testRoot.package('pkg2')
            .add(testRoot.clazz('SomeClass2', 'class').build())
            .add(testRoot.clazz('SomeClass1', 'class').build())
            .build())
          .build())
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .add(testRoot.clazz('SomeClass2', 'class').build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.pkg1.pkg2.SomeClass2')
      .to('com.tngtech.pkg1.pkg2.SomeClass1')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());

    const rule = {
      rule: 'rule',
      violations: ['<com.tngtech.pkg1.pkg2.SomeClass2> INHERITANCE to <com.tngtech.pkg1.pkg2.SomeClass1>']
    };

    const dependencies = new Dependencies(jsonDependencies, root);
    buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);

    const exp = ['com.tngtech.pkg1.pkg2.SomeClass2', 'com.tngtech.pkg1.pkg2.SomeClass1'];

    expect([...dependencies.getNodesInvolvedInVisibleViolations()]).to.containExactlyNodes(exp);
  });

  it('can return a set of all nodes that a involved in violations when these nodes contain packages', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.package('pkg')
          .add(testRoot.clazz('SomeClass', 'class').build())
          .build())
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .add(testRoot.clazz('SomeClass2', 'class').build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addInheritance().from('com.tngtech.pkg.SomeClass')
      .to('com.tngtech.SomeClass1')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());

    const rule = {
      rule: 'rule',
      violations: ['<com.tngtech.pkg.SomeClass> INHERITANCE to <com.tngtech.SomeClass1>']
    };

    const dependencies = new Dependencies(jsonDependencies, root);
    buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule);

    root.getByName('com.tngtech.pkg').fold();

    const exp = ['com.tngtech.pkg.SomeClass', 'com.tngtech.SomeClass1'];

    expect([...dependencies.getNodesInvolvedInVisibleViolations()]).to.containExactlyNodes(exp);
  });

  it('does not return node-fullnames of violations that are hidden by a filter', () => {
    const jsonRoot =
      testRoot.package('com.tngtech')
        .add(testRoot.clazz('SomeClass1', 'class').build())
        .add(testRoot.clazz('SomeClass2', 'class').build())
        .build();
    const jsonDependencies = createTestDependencies()
      .addFieldAccess().from('com.tngtech.SomeClass1', 'startMethod()')
      .to('com.tngtech.SomeClass2', 'targetField')
      .build();

    const root = new Root(jsonRoot, null, () => Promise.resolve());

    const rule1 = {
      rule: 'rule1',
      violations: ['<com.tngtech.SomeClass1.startMethod()> FIELD_ACCESS to <com.tngtech.SomeClass2.targetField>']
    };

    const dependencies = new Dependencies(jsonDependencies, root);
    const filterCollection = buildFilterCollection()
      .addFilterGroup(dependencies.filterGroup)
      .build();

    dependencies.showViolations(rule1);

    dependencies.changeTypeFilter({
      INHERITANCE: true,
      CONSTRUCTOR_CALL: true,
      METHOD_CALL: true,
      FIELD_ACCESS: false
    });
    filterCollection.updateFilter('dependencies.type');

    expect(dependencies.getNodesContainingViolations()).to.containExactlyNodes([]);
  });
});