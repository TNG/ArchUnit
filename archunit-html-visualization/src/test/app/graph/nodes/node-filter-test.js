'use strict';

const expect = require('chai').expect;
require('../testinfrastructure/node-chai-extensions');

const rootCreator = require('../testinfrastructure/root-creator');
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;
const document = require('../testinfrastructure/gui-elements-mock').document;
const {buildFilterCollection} = require("../../../../main/app/graph/filter");

const testOn = require('../testinfrastructure/node-test-infrastructure').testOnRoot;
const testWholeLayoutOn = require('../testinfrastructure/node-layout-test-infrastructure').testWholeLayoutOn;
const testGuiFromRoot = require('../testinfrastructure/node-userinteraction-adapter').testGuiFromRoot;
const filterOn = require('../testinfrastructure/node-filter-test-infrastructure').filterOn;

describe('Filters', () => {
  describe('by type', () => {
    it('can hide interfaces', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface1',
        'my.company.SomeInterface2', 'my.company.OtherClass$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.SomeClass', 'my.company.OtherClass').await();
    });

    it('can hide classes', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeInterface', 'my.company.SomeClass1',
        'my.company.SomeClass2', 'my.company.OtherInterface$SomeInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false)
        .thenTest.that.classesAre('my.company.SomeInterface',
          'my.company.OtherInterface').await();
    });

    it('can hide interfaces and classes, so that nothing remains', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, false)
        .thenTest.that.classesAre().await();
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.SomeClass').await();
      testWholeLayoutOn(root);
    });

    it('can be applied successively with a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false)
        .thenTest.that.classesAre('my.company.SomeInterface').await();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.SomeClass').await();
      testWholeLayoutOn(root);
    });

    it('can be applied directly successively, so that the relayout of the filtering before is not finished yet', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).typeFilter(true, false).goOn();
      filterOn(root, filterCollection).typeFilter(false, false).goOn();
      await filterOn(root, filterCollection).typeFilter(false, true).await();

      testOn(root).that.it.hasClasses('my.company.SomeClass');
      testWholeLayoutOn(root);
    });

    it('can be reset successively, so that all classes are shown again', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, false).thenTest.that.classesAre().await();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.SomeClass').await();

      await filterOn(root, filterCollection).typeFilter(true, true)
        .thenTest.that.classesAre('my.company.SomeClass', 'my.company.SomeInterface').await();
    });

    it('does not filter out a class with a descendant interface, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInterface',
        'my.company.first.OtherClass', 'my.company.second.SomeClass$SomeInnerClass$SomeInterface', 'my.company.second.OtherInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false)
        .thenTest.that.classesAre('my.company.first.SomeClass$SomeInterface', 'my.company.second.SomeClass$SomeInnerClass$SomeInterface',
          'my.company.second.OtherInterface').await();
    });

    it('does not filter out an interface with a descendant class, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface$SomeClass',
        'my.company.first.OtherInterface', 'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.first.SomeInterface$SomeClass',
          'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass').await();
    });

    it('does filter out packages without descendant interfaces, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$SomeInnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass',
        'my.company.third.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false)
        .thenTest.that.classesAre('my.company.third.SomeInterface').await();
    });

    it('does filter out packages without descendant classes, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface', 'my.company.first.OtherInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeInterface$SomeInnerInterface',
        'my.company.second.somePkg.someOtherPkg.SomeInterface',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.third.SomeClass').await();
    });
  });

  describe('by name:', () => {
    it('can filter nodes by simple string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass')
        .thenTest.that.classesAre('my.company.first.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first.OtherClass')
        .thenTest.that.classesAre('my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first')
        .thenTest.that.classesAre('my.company.first.SomeClass',
          'my.company.first.OtherClass').await();
    });

    it('can filter nodes by strings separated by "|"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass|my.company.second.SomeClass')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass').await();
    });

    it('does not filter out a node that is the child of one of the option strings', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.third.SomeClass')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
          'my.company.third.SomeClass$InnerClass').await();
    });

    it('does filter out a node that is not the child of one of the option strings but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first')
        .thenTest.that.classesAre('my.company.first.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.second.Some')
        .thenTest.that.classesAre('my.company.first.SomeClass').await();
    });

    it('ignores leading and closing whitespaces at each option string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first ')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('   my.company.first   ')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('   my.company.first   | my.company.second.SomeClass   ')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('    my.company.first   | my.company.second.SomeClass  | my.company.third')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
          'my.company.second.SomeClass', 'my.company.third.SomeClass').await();
    });

    it('can filter nodes by string containing * as wildcard', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('*SomeClass*')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.second.SomeClass',
          'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('my.*.first')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
          'my.secondCompany.first.SomeClass$InnerClass').await();
      await filterOn(root, filterCollection).nameFilter('my.*.*.Some*')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.second.SomeClass',
          'my.secondCompany.first.SomeClass$InnerClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first.*|*SomeClass$*')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
          'my.secondCompany.first.SomeClass$InnerClass').await();
    });

    it('can exclude nodes by beginning an option string with "$"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass')
        .thenTest.that.classesAre('my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass|~my.company.second.SomeClass')
        .thenTest.that.classesAre('my.company.first.OtherClass', 'my.company.second.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first|~my.company.first.SomeClass')
        .thenTest.that.classesAre('my.company.first.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('~*.first.Some*|~*.first.Other*|~*.second.SomeClass')
        .thenTest.that.classesAre('my.company.second.OtherClass').await();
    });

    it('filters out a node that is the child of one of the excluded options', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first')
        .thenTest.that.classesAre('my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass').await();
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|~my.company.first').thenTest.that.classesAre().await();
      await filterOn(root, filterCollection).nameFilter('~my.company.third.SomeClass')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass').await();
    });

    it('does not filter out a node that is not the child of one of the excluded options but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass', 'my.company.second.SomeClassOther');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first')
        .thenTest.that.classesAre('my.company.firstOther.OtherClass', 'my.company.second.SomeClass',
          'my.company.second.SomeClassOther').await();
      await filterOn(root, filterCollection).nameFilter('~my.company.first|~my.company.second.SomeClass')
        .thenTest.that.classesAre('my.company.firstOther.OtherClass', 'my.company.second.SomeClassOther').await();
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.first.OtherClass', 'my.firstCompany.second.SomeClass',
        'my.firstCompany.second.OtherClass', 'my.secondCompany.first.SomeClass', 'my.secondCompany.first.OtherClass',
        'my.thirdCompany.first.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.first*.*|my.second*.*|~*OtherClass')
        .thenTest.that.classesAre('my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.second.SomeClass',
          'my.secondCompany.first.SomeClass').await();
      testWholeLayoutOn(root);
    });

    it('can be applied directly successively, so that the relayout of the filtering before is not finished yet', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.firstCompany.first.SomeClass', 'my.firstCompany.first.OtherClass',
        'my.firstCompany.second.SomeClass', 'my.firstCompany.second.OtherClass', 'my.secondCompany.first.SomeClass',
        'my.secondCompany.first.OtherClass', 'my.thirdCompany.first.SomeClass');

      let filterString = '';
      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
      filterString += '~my.firstCompany.*.SomeClass';
      filterOn(root, filterCollection).nameFilter(filterString).goOn();
      filterString += '|~*.secondCompany.*.Other*';
      filterOn(root, filterCollection).nameFilter(filterString).goOn();
      filterString += '|*.first*.*.*';
      filterOn(root, filterCollection).nameFilter(filterString).goOn();
      filterString += '|*.second*.*.*';
      await filterOn(root, filterCollection).nameFilter(filterString).await();

      testOn(root).that.it.hasClasses('my.firstCompany.first.OtherClass', 'my.firstCompany.second.OtherClass',
        'my.secondCompany.first.SomeClass');
      testWholeLayoutOn(root);
    });

    it('can be reset successively', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
        'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.OtherClass',
        'my.company.second.otherPkg.SomeClass', 'my.company.second.otherPkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
        'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass',
        'my.otherCompany.second.somePkg.SomeClass', 'my.otherCompany.second.somePkg.OtherClass',
        'my.otherCompany.second.otherPkg.SomeClass', 'my.otherCompany.second.otherPkg.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass|~my.otherCompany')
        .thenTest.that.classesAre('my.company.first.somePkg.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass')
        .thenTest.that.classesAre('my.company.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.SomeClass').await();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*')
        .thenTest.that.classesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
          'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*')
        .thenTest.that.classesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
          'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
          'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
          'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass').await();
      await filterOn(root, filterCollection).nameFilter('')
        .thenTest.that.classesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
          'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
          'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.OtherClass',
          'my.company.second.otherPkg.SomeClass', 'my.company.second.otherPkg.OtherClass',
          'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
          'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass',
          'my.otherCompany.second.somePkg.SomeClass', 'my.otherCompany.second.somePkg.OtherClass',
          'my.otherCompany.second.otherPkg.SomeClass', 'my.otherCompany.second.otherPkg.OtherClass').await();

      testWholeLayoutOn(root);
    });

    it('does not filter out a node, that does not match the filter itself but has a matching descendant', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInnerClass',
        'my.company.first.SomeClass$OtherInnerClass', 'my.company.second.SomeClass$SomeMatchingInnerClass',
        'my.company.second.SomeClass$SomeInnerClass', 'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass$SomeInnerClass|*$*Matching*')
        .thenTest.that.classesAre('my.company.first.SomeClass$SomeInnerClass', 'my.company.second.SomeClass$SomeMatchingInnerClass',
          'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass').await();
    });

    it('does filter out packages, that have no matching descendants', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.SomeMoreClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$InnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass$InnerClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*.Some*')
        .thenTest.that.classesAre('my.company.third.OtherClass').await();
    });

    describe('provides a ctrl-click filter,', () => {
      //TODO: test on graph if the fullname is added to the filter bar

      describe('which extends the name filter by the fullnames of the clicked nodes', () => {
        it('when the name filter is empty at the beginning', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testOn(root).that.it.hasClasses('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          testGuiFromRoot(root).ctrlClickNode('my.company.second');
          await root._updatePromise;
          testOn(root).that.it.hasClasses('my.company.first.OtherClass');
        });

        it('when the name filter already contains something', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass',
            'my.company.third.SomeClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          await filterOn(root, filterCollection).nameFilter('*first*|*second*').await();

          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testOn(root).that.it.hasClasses('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          testGuiFromRoot(root).ctrlClickNode('my.company.second');
          await root._updatePromise;
          testOn(root).that.it.hasClasses('my.company.first.OtherClass');
        });

        it('right after the name filter was changed, i.e. before the last relayout was finished', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          filterOn(root, filterCollection).nameFilter('*first*').goOn();

          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testOn(root).that.it.hasClasses('my.company.first.OtherClass');
        });

        it('when several nodes are clicked directly successively', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', 'my.otherCompany.first.SomeClass', 'my.otherCompany.first.OtherClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const nodeListenerMock = createListenerMock('onLayoutChanged');
          root.addListener(nodeListenerMock.listener);

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          testGuiFromRoot(root).ctrlClickNode('my.company.first.OtherClass');
          testGuiFromRoot(root).ctrlClickNode('my.company.second.SomeClass');
          testGuiFromRoot(root).ctrlClickNode('my.otherCompany');
          document.ctrlKeyup();
          await root._updatePromise;

          testOn(root).that.it.hasClasses('my.company.second.OtherClass');
          testWholeLayoutOn(root);
          nodeListenerMock.test.that.listenerFunction('onLayoutChanged').was.called.once();
        });
      });

      it('which does no relayout when the ctrl-key is kept pressed', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass', {
          onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
        });

        const nodeListenerMock = createListenerMock('onLayoutChanged');
        root.addListener(nodeListenerMock.listener);

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
        testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
        await root._updatePromise;

        nodeListenerMock.test.that.listenerFunction('onLayoutChanged').was.not.called();
      });

      it('which does a single relayout when the ctrl-key is left again', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass', {
          onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
        });

        const nodeListenerMock = createListenerMock('onLayoutChanged');
        root.addListener(nodeListenerMock.listener);

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
        testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
        document.ctrlKeyup();
        await root._updatePromise;

        testWholeLayoutOn(root);
        nodeListenerMock.test.that.listenerFunction('onLayoutChanged').was.called.once();
      });

      describe('calls the listener with the updated name filter string', async () => {
        it('when the name filter was empty before', async () => {
          let actualNameFilterString;
          const root = await rootCreator.createRootFromClassNamesAndLayout(
            'my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: (nameFilterString) => actualNameFilterString = nameFilterString
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('~my.company.first.SomeClass');

          testGuiFromRoot(root).ctrlClickNode('my.company.second');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('~my.company.first.SomeClass|~my.company.second');
        });

        it('when the name filter already contains something', async () => {
          let actualNameFilterString;
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass',
            'my.company.third.SomeClass', {
              onNodeFilterStringChanged: (nameFilterString) => actualNameFilterString = nameFilterString
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          await filterOn(root, filterCollection).nameFilter('*first*|*second*').await();

          testGuiFromRoot(root).ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('*first*|*second*|~my.company.first.SomeClass');

          testGuiFromRoot(root).ctrlClickNode('my.company.second');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('*first*|*second*|~my.company.first.SomeClass|~my.company.second');
        });
      });
    });
  });

  describe('by name and by type', () => {
    it('successively, i.e. the relayout of the filtering before was finished', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass', {
          onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
        });

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*')
        .thenTest.that.classesAre('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
          'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface').await();
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.otherCompany.first.SomeClass',
          'my.company.second.SomeClass', 'my.otherCompany.second.SomeClass').await();
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('my.*.second.*')
        .thenTest.that.classesAre('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass').await();
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*')
        .thenTest.that.classesAre('my.company.second.SomeClass$OtherInnerClass',
          'my.otherCompany.second.SomeClass', 'my.otherCompany.third.OtherClass').await();
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).typeFilter(true, true)
        .thenTest.that.classesAre('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
          'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass').await();
      testWholeLayoutOn(root);

      testGuiFromRoot(root).ctrlClickNode('my.company.second.SomeClass$OtherInnerClass');
      document.ctrlKeyup();
      await root._updatePromise;
      testOn(root).that.it.hasClasses('my.company.second.SomeClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.SomeInterface',
          'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
          'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
          'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
          'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass').await();
      testWholeLayoutOn(root);
    });

    it('directly successively, i.e. the relayout of the filtering before was not finished', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass', {
          onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
        });

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).typeFilter(true, false).goOn();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*')
        .thenTest.that.classesAre('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
          'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface').await();
      testWholeLayoutOn(root);

      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      await filterOn(root, filterCollection).nameFilter('my.*.second.*')
        .thenTest.that.classesAre('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass').await();
      testWholeLayoutOn(root);

      filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*').goOn();
      await filterOn(root, filterCollection).typeFilter(true, true)
        .thenTest.that.classesAre('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
          'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass').await();
      testWholeLayoutOn(root);

      testGuiFromRoot(root).ctrlClickNode('my.company.second.SomeClass$OtherInnerClass');
      document.ctrlKeyup();
      filterOn(root, filterCollection).nameFilter('my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      filterOn(root, filterCollection).nameFilter('~my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(true, true).goOn();
      await filterOn(root, filterCollection).nameFilter('')
        .thenTest.that.classesAre('my.company.first.SomeClass', 'my.company.first.SomeInterface',
          'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
          'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
          'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
          'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass').await();
      testWholeLayoutOn(root);
    });

    it('filters out packages that have no matching descendant only because of both filters', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.SomeInterface$SomeClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection)
        .typeFilter(false, true).and.nameFilter('~*SomeClass*')
        .thenTest.that.classesAre('my.company.third.OtherClass').await();
    });
  });

  //TODO: also test in node-test, that such a node cannot be unfolded anymore (not only has the correct css-class)
  describe('changes css-class of those nodes from "foldable" to "unfoldable", that loose their children, and resets it again', () => {
    it('because of the type-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInterface',
        'my.company.OtherClass$SomeInnerClass', 'my.company.SomeInterface$SomeClass', 'my.company.OtherInterface$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true)
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.unfoldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
      await filterOn(root, filterCollection).typeFilter(true, false)
        .thenTest.that.node('my.company.SomeInterface').isMarkedAs.unfoldable()
        .and.that.node('my.company.OtherInterface').isMarkedAs.foldable().await();
      await filterOn(root, filterCollection).typeFilter(true, true)
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.foldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
    });

    it('because of the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*')
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.unfoldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
      await filterOn(root, filterCollection).nameFilter('')
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.foldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
    });

    //TODO: test this also for nodes that become unfoldable only because of the violations-filter (somewhere in graph-tests)
    it('only because of the type- and the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.SomeClass$InnerInterface', 'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection)
        .nameFilter('~*$SomeInner*').and.typeFilter(false, true)
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.unfoldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
      await filterOn(root, filterCollection)
        .nameFilter('').and.typeFilter(false, true)
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.foldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(false, true).await();
      await filterOn(root, filterCollection)
        .nameFilter('~*$SomeInner*').and.typeFilter(true, true)
        .thenTest.that.node('my.company.SomeClass').isMarkedAs.foldable()
        .and.that.node('my.company.OtherClass').isMarkedAs.foldable().await();
    });
  });

  describe('public methods', () => {
    describe('#isCurrentlyLeaf()', () => {
      it('returns true after filtering out all descendants of a class and false after resetting the filter again', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
          'my.company.SomeClass$SomeInnerInterface');

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

        await filterOn(root, filterCollection).nameFilter('~*InnerClass*').and.typeFilter(false, true).await();
        expect(root.getByName('my.company.SomeClass').isCurrentlyLeaf()).to.be.true;

        await filterOn(root, filterCollection).nameFilter('').await();
        expect(root.getByName('my.company.SomeClass').isCurrentlyLeaf()).to.be.false;
      });
    });

    describe('#getCurrentChildren()', () => {
      it('returns only the children matching the filter', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface',
          'my.company.OtherInterface');

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

        await filterOn(root, filterCollection).nameFilter('~*Other*').and.typeFilter(true, false).await();
        expect(root.getByName('my.company').getCurrentChildren()).to.onlyContainNodes('my.company.SomeInterface');

        await filterOn(root, filterCollection).nameFilter('').await();
        expect(root.getByName('my.company').getCurrentChildren()).to.onlyContainNodes('my.company.SomeInterface', 'my.company.OtherInterface');

        await filterOn(root, filterCollection).typeFilter(true, true).await();
        expect(root.getByName('my.company').getCurrentChildren()).to.onlyContainNodes('my.company.SomeClass', 'my.company.SomeInterface',
          'my.company.OtherInterface');
      });
    });
  });
});