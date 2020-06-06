'use strict';

const expect = require('chai').expect;
require('../testinfrastructure/node-chai-extensions');

const rootCreator = require('../testinfrastructure/root-creator');
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;
const document = require('../testinfrastructure/gui-elements-mock').document;
const {buildFilterCollection} = require("../../../../main/app/graph/filter");

const filterOn = require('../testinfrastructure/node-filter-test-infrastructure').filterOn;

const RootUi = require('./testinfrastructure/root-ui');

describe('Filters', () => {
  describe('by type', () => {
    it('can hide interfaces', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface1',
        'my.company.SomeInterface2', 'my.company.OtherClass$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.SomeClass', 'my.company.OtherClass');
    });

    it('can hide classes', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeInterface', 'my.company.SomeClass1',
        'my.company.SomeClass2', 'my.company.OtherInterface$SomeInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.SomeInterface', 'my.company.OtherInterface');
    });

    it('can hide interfaces and classes, so that nothing remains', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, false).await();
      RootUi.of(root).expectToHaveLeafFullNames();
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();

      const rootUi = RootUi.of(root);
      rootUi.expectToHaveLeafFullNames('my.company.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('can be applied successively with a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.SomeInterface');

      await filterOn(root, filterCollection).typeFilter(false, true).await();

      const rootUi = RootUi.of(root);
      rootUi.expectToHaveLeafFullNames('my.company.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('can be applied directly successively, so that the relayout of the filtering before is not finished yet', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).typeFilter(true, false).goOn();
      filterOn(root, filterCollection).typeFilter(false, false).goOn();
      await filterOn(root, filterCollection).typeFilter(false, true).await();

      const rootUi = RootUi.of(root);
      rootUi.expectToHaveLeafFullNames('my.company.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('can be reset successively, so that all classes are shown again', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).typeFilter(false, false).await();
      rootUi.expectToHaveLeafFullNames();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      rootUi.expectToHaveLeafFullNames('my.company.SomeClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      rootUi.expectToHaveLeafFullNames('my.company.SomeClass', 'my.company.SomeInterface');
    });

    it('does not filter out a class with a descendant interface, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInterface',
        'my.company.first.OtherClass', 'my.company.second.SomeClass$SomeInnerClass$SomeInterface', 'my.company.second.OtherInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.first.SomeClass$SomeInterface',
        'my.company.second.SomeClass$SomeInnerClass$SomeInterface', 'my.company.second.OtherInterface');
    });

    it('does not filter out an interface with a descendant class, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface$SomeClass',
        'my.company.first.OtherInterface', 'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.first.SomeInterface$SomeClass',
        'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass');
    });

    it('does filter out packages without descendant interfaces, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$SomeInnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass',
        'my.company.third.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.third.SomeInterface');
    });

    it('does filter out packages without descendant classes, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface', 'my.company.first.OtherInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeInterface$SomeInnerInterface',
        'my.company.second.somePkg.someOtherPkg.SomeInterface',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.third.SomeClass');
    });
  });

  describe('by name:', () => {
    it('can filter nodes by simple string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.OtherClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass');
    });

    it('can filter nodes by strings separated by "|"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass|my.company.second.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
    });

    it('does not filter out a node that is the child of one of the option strings', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.third.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.third.SomeClass$InnerClass');
    });

    it('does filter out a node that is not the child of one of the option strings but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.second.Some').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass');
    });

    it('ignores leading and closing whitespaces at each option string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.company.first ').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('   my.company.first   ').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('   my.company.first   | my.company.second.SomeClass   ').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
      await filterOn(root, filterCollection).nameFilter('    my.company.first   | my.company.second.SomeClass  | my.company.third').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.second.SomeClass', 'my.company.third.SomeClass');
    });

    it('can filter nodes by string containing * as wildcard', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('*SomeClass*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.second.SomeClass',
        'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.*.*.Some*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.second.SomeClass',
        'my.secondCompany.first.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.*|*SomeClass$*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass');
    });

    it('can exclude nodes by beginning an option string with "$"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');
      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass|~my.company.second.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass', 'my.company.second.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|~my.company.first.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('~*.first.Some*|~*.first.Other*|~*.second.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.second.OtherClass');
    });

    it('filters out a node that is the child of one of the excluded options', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('~my.company.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|~my.company.first').await();
      rootUi.expectToHaveLeafFullNames();
      await filterOn(root, filterCollection).nameFilter('~my.company.third.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
    });

    it('does not filter out a node that is not the child of one of the excluded options but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass', 'my.company.second.SomeClassOther');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('~my.company.first').await();
      rootUi.expectToHaveLeafFullNames('my.company.firstOther.OtherClass', 'my.company.second.SomeClass',
        'my.company.second.SomeClassOther');
      await filterOn(root, filterCollection).nameFilter('~my.company.first|~my.company.second.SomeClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.firstOther.OtherClass', 'my.company.second.SomeClassOther');
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.first.OtherClass', 'my.firstCompany.second.SomeClass',
        'my.firstCompany.second.OtherClass', 'my.secondCompany.first.SomeClass', 'my.secondCompany.first.OtherClass',
        'my.thirdCompany.first.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.first*.*|my.second*.*|~*OtherClass').await();
      rootUi.expectToHaveLeafFullNames('my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.second.SomeClass',
        'my.secondCompany.first.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
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

      const rootUi = RootUi.of(root);

      rootUi.expectToHaveLeafFullNames('my.firstCompany.first.OtherClass', 'my.firstCompany.second.OtherClass',
        'my.secondCompany.first.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
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

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass|~my.otherCompany').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.somePkg.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
        'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass');
      await filterOn(root, filterCollection).nameFilter('').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
        'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.OtherClass',
        'my.company.second.otherPkg.SomeClass', 'my.company.second.otherPkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
        'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass',
        'my.otherCompany.second.somePkg.SomeClass', 'my.otherCompany.second.somePkg.OtherClass',
        'my.otherCompany.second.otherPkg.SomeClass', 'my.otherCompany.second.otherPkg.OtherClass');

      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('does not filter out a node, that does not match the filter itself but has a matching descendant', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInnerClass',
        'my.company.first.SomeClass$OtherInnerClass', 'my.company.second.SomeClass$SomeMatchingInnerClass',
        'my.company.second.SomeClass$SomeInnerClass', 'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass$SomeInnerClass|*$*Matching*').await();
      RootUi.of(root).expectToHaveLeafFullNames(
        'my.company.first.SomeClass$SomeInnerClass',
        'my.company.second.SomeClass$SomeMatchingInnerClass',
        'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass');
    });

    it('does filter out packages, that have no matching descendants', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.SomeMoreClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$InnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass$InnerClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*.Some*').await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.third.OtherClass');
    });

    describe('provides a ctrl-click filter,', () => {

      describe('which extends the name filter by the fullnames of the clicked nodes', () => {
        it('when the name filter is empty at the beginning', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          const rootUi = RootUi.of(root);

          await rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();
          rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          await rootUi.getNodeWithFullName('my.company.second').ctrlClickAndAwait();
          rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass');
        });

        it('when the name filter already contains something', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass',
            'my.company.third.SomeClass', {
              onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          await filterOn(root, filterCollection).nameFilter('*first*|*second*').await();

          const rootUi = RootUi.of(root);

          await rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();
          rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          await rootUi.getNodeWithFullName('my.company.second').ctrlClickAndAwait();
          rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass');
        });

        it('right after the name filter was changed, i.e. before the last relayout was finished', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          filterOn(root, filterCollection).nameFilter('*first*').goOn();

          const rootUi = RootUi.of(root);

          await rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();
          rootUi.expectToHaveLeafFullNames('my.company.first.OtherClass');
        });

        it('when several nodes are clicked directly successively', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', 'my.otherCompany.first.SomeClass', 'my.otherCompany.first.OtherClass', {
              onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
            });

          const nodeListenerMock = createListenerMock('onLayoutChanged');
          root.addListener(nodeListenerMock.listener);

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          const rootUi = RootUi.of(root);

          rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClick();
          rootUi.getNodeWithFullName('my.company.first.OtherClass').ctrlClick();
          rootUi.getNodeWithFullName('my.company.second.SomeClass').ctrlClick();
          rootUi.getNodeWithFullName('my.otherCompany').ctrlClick();
          document.ctrlKeyup();
          await root._updatePromise;

          rootUi.expectToHaveLeafFullNames('my.company.second.OtherClass');
          rootUi.expectLayoutInvariantsToBeSatisfied();
          nodeListenerMock.test.that.listenerFunction('onLayoutChanged').was.called.once();
        });
      });

      it('which does no relayout when the ctrl-key is kept pressed', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass', {
          onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
        });

        const nodeListenerMock = createListenerMock('onLayoutChanged');
        root.addListener(nodeListenerMock.listener);

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

        await RootUi.of(root).getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();

        nodeListenerMock.test.that.listenerFunction('onLayoutChanged').was.not.called();
      });

      it('which does a single relayout when the ctrl-key is left again', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass', {
          onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
        });

        const nodeListenerMock = createListenerMock('onLayoutChanged');
        root.addListener(nodeListenerMock.listener);

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
        const rootUi = RootUi.of(root);

        rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClick();
        document.ctrlKeyup();
        await root._updatePromise;

        rootUi.expectLayoutInvariantsToBeSatisfied();
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

          const rootUi = RootUi.of(root);

          await rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();
          expect(actualNameFilterString).to.equal('~my.company.first.SomeClass');

          await rootUi.getNodeWithFullName('my.company.second').ctrlClickAndAwait();
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

          const rootUi = RootUi.of(root);

          await filterOn(root, filterCollection).nameFilter('*first*|*second*').await();

          await rootUi.getNodeWithFullName('my.company.first.SomeClass').ctrlClickAndAwait();
          expect(actualNameFilterString).to.equal('*first*|*second*|~my.company.first.SomeClass');

          await rootUi.getNodeWithFullName('my.company.second').ctrlClickAndAwait();
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
          onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
        });

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
        'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.otherCompany.first.SomeClass',
        'my.company.second.SomeClass', 'my.otherCompany.second.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      await filterOn(root, filterCollection).nameFilter('my.*.second.*').await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      await filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*').await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass$OtherInnerClass',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      rootUi.getNodeWithFullName('my.company.second.SomeClass$OtherInnerClass').ctrlClick();
      document.ctrlKeyup();
      await root._updatePromise;
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      await filterOn(root, filterCollection).nameFilter('').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('directly successively, i.e. the relayout of the filtering before was not finished', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass', {
          onNodeFilterStringChanged: () => root.scheduleAction(() => filterCollection.updateFilter('nodes.name'))
        });

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      filterOn(root, filterCollection).typeFilter(true, false).goOn();
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
        'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      await filterOn(root, filterCollection).nameFilter('my.*.second.*').await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*').goOn();
      await filterOn(root, filterCollection).typeFilter(true, true).await();
      rootUi.expectToHaveLeafFullNames('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();

      rootUi.getNodeWithFullName('my.company.second.SomeClass$OtherInnerClass').ctrlClick();
      document.ctrlKeyup();
      filterOn(root, filterCollection).nameFilter('my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      filterOn(root, filterCollection).nameFilter('~my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(true, true).goOn();
      await filterOn(root, filterCollection).nameFilter('').await();
      rootUi.expectToHaveLeafFullNames('my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass');
      rootUi.expectLayoutInvariantsToBeSatisfied();
    });

    it('filters out packages that have no matching descendant only because of both filters', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.SomeInterface$SomeClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).and.nameFilter('~*SomeClass*').await();
      RootUi.of(root).expectToHaveLeafFullNames('my.company.third.OtherClass');
    });
  });

  describe('changes css-class of those nodes from "foldable" to "unfoldable", that loose their children, and resets it again', () => {
    it('because of the type-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInterface',
        'my.company.OtherClass$SomeInnerClass', 'my.company.SomeInterface$SomeClass', 'my.company.OtherInterface$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).typeFilter(false, true).await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeUnfoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();

      await filterOn(root, filterCollection).typeFilter(true, false).await();

      rootUi.getNodeWithFullName('my.company.SomeInterface').expectToBeUnfoldable();
      rootUi.getNodeWithFullName('my.company.OtherInterface').expectToBeFoldable();

      await filterOn(root, filterCollection).typeFilter(true, true).await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeFoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();
    });

    it('because of the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeUnfoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();

      await filterOn(root, filterCollection).nameFilter('').await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeFoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();
    });

    it('only because of the type- and the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.SomeClass$InnerInterface', 'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      const rootUi = RootUi.of(root);

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(false, true).await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeUnfoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();

      await filterOn(root, filterCollection).nameFilter('').and.typeFilter(false, true).await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeFoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(true, true).await();

      rootUi.getNodeWithFullName('my.company.SomeClass').expectToBeFoldable();
      rootUi.getNodeWithFullName('my.company.OtherClass').expectToBeFoldable();
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

    describe('#getOriginalChildren()', () => {
      it('returns still all children of a node after filtering some out', async () => {
        const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass', 'my.company.SomeClass$OtherInnerClass');

        const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

        await filterOn(root, filterCollection).nameFilter('~my.company.SomeClass$OtherInnerClass').await();
        expect(root.getByName('my.company.SomeClass').getOriginalChildren()).to.onlyContainNodes('my.company.SomeClass$SomeInnerClass', 'my.company.SomeClass$OtherInnerClass');
      });
    });
  });
});
