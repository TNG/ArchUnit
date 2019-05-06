'use strict';

const expect = require('chai').expect;
require('../testinfrastructure/node-chai-extensions');

const rootCreator = require('../testinfrastructure/root-creator');
const createListenerMock = require('../testinfrastructure/listener-mock').createListenerMock;
const document = require('../testinfrastructure/gui-elements-mock').document;
const {buildFilterCollection} = require("../../../../main/app/graph/filter");

const filterOn = require('../testinfrastructure/node-filter-test-infrastructure').filterOn;
const testGui = require('../testinfrastructure/node-gui-adapter').testGuiFromRoot;
const testWholeLayoutOn = require('../testinfrastructure/node-layout-test-infrastructure').testWholeLayoutOn;

describe('Filters', () => {
  describe('by type', () => {
    it('can hide interfaces', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface1',
        'my.company.SomeInterface2', 'my.company.OtherClass$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeClass', 'my.company.OtherClass');
    });

    it('can hide classes', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeInterface', 'my.company.SomeClass1',
        'my.company.SomeClass2', 'my.company.OtherInterface$SomeInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeInterface', 'my.company.OtherInterface');
    });

    it('can hide interfaces and classes, so that nothing remains', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, false).await();
      testGui(root).test.that.onlyNodesAre();
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeClass');
      testWholeLayoutOn(root);
    });

    it('can be applied successively with a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeInterface');

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeClass');
      testWholeLayoutOn(root);
    });

    it('can be applied directly successively, so that the relayout of the filtering before is not finished yet', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      filterOn(root, filterCollection).typeFilter(true, false).goOn();
      filterOn(root, filterCollection).typeFilter(false, false).goOn();
      await filterOn(root, filterCollection).typeFilter(false, true).await();

      testGui(root).test.that.onlyNodesAre('my.company.SomeClass');
      testWholeLayoutOn(root);
    });

    it('can be reset successively, so that all classes are shown again', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass', 'my.company.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, false).await();
      testGui(root).test.that.onlyNodesAre();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeClass');

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.SomeClass', 'my.company.SomeInterface');
    });

    it('does not filter out a class with a descendant interface, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInterface',
        'my.company.first.OtherClass', 'my.company.second.SomeClass$SomeInnerClass$SomeInterface', 'my.company.second.OtherInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass$SomeInterface',
        'my.company.second.SomeClass$SomeInnerClass$SomeInterface', 'my.company.second.OtherInterface');
    });

    it('does not filter out an interface with a descendant class, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface$SomeClass',
        'my.company.first.OtherInterface', 'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeInterface$SomeClass',
        'my.company.second.SomeInterface$SomeInnerInterface$SomeClass', 'my.company.second.OtherClass');
    });

    it('does filter out packages without descendant interfaces, if only interfaces are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$SomeInnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass',
        'my.company.third.SomeInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(true, false).await();
      testGui(root).test.that.onlyNodesAre('my.company.third.SomeInterface');
    });

    it('does filter out packages without descendant classes, if only classes are shown', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeInterface', 'my.company.first.OtherInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeInterface$SomeInnerInterface',
        'my.company.second.somePkg.someOtherPkg.SomeInterface',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.third.SomeClass');
    });
  });

  describe('by name:', () => {
    it('can filter nodes by simple string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.OtherClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass');
    });

    it('can filter nodes by strings separated by "|"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|my.company.first.OtherClass|my.company.second.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
    });

    it('does not filter out a node that is the child of one of the option strings', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.third.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.third.SomeClass$InnerClass');
    });

    it('does filter out a node that is not the child of one of the option strings but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|my.company.second.Some').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass');
    });

    it('ignores leading and closing whitespaces at each option string', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.company.third.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first ').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('   my.company.first   ').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('   my.company.first   | my.company.second.SomeClass   ').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
      await filterOn(root, filterCollection).nameFilter('    my.company.first   | my.company.second.SomeClass  | my.company.third').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.company.second.SomeClass', 'my.company.third.SomeClass');
    });

    it('can filter nodes by string containing * as wildcard', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('*SomeClass*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.second.SomeClass',
        'my.secondCompany.first.SomeClass$InnerClass', 'my.secondCompany.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.*.*.Some*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.second.SomeClass',
        'my.secondCompany.first.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.*|*SomeClass$*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass',
        'my.secondCompany.first.SomeClass$InnerClass');
    });

    it('can exclude nodes by beginning an option string with "$"', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.second.OtherClass');
      await filterOn(root, filterCollection).nameFilter('~my.company.first.SomeClass|~my.company.second.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass', 'my.company.second.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first|~my.company.first.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass');
      await filterOn(root, filterCollection).nameFilter('~*.first.Some*|~*.first.Other*|~*.second.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.second.OtherClass');
    });

    it('filters out a node that is the child of one of the excluded options', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass', 'my.company.third.SomeClass$InnerClass');
      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass|~my.company.first').await();
      testGui(root).test.that.onlyNodesAre();
      await filterOn(root, filterCollection).nameFilter('~my.company.third.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.OtherClass', 'my.company.second.SomeClass');
    });

    it('does not filter out a node that is not the child of one of the excluded options but has one of them as prefix', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.firstOther.OtherClass', 'my.company.second.SomeClass', 'my.company.second.SomeClassOther');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~my.company.first').await();
      testGui(root).test.that.onlyNodesAre('my.company.firstOther.OtherClass', 'my.company.second.SomeClass',
        'my.company.second.SomeClassOther');
      await filterOn(root, filterCollection).nameFilter('~my.company.first|~my.company.second.SomeClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.firstOther.OtherClass', 'my.company.second.SomeClassOther');
    });

    it('creates a correct layout at the end', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.first.OtherClass', 'my.firstCompany.second.SomeClass',
        'my.firstCompany.second.OtherClass', 'my.secondCompany.first.SomeClass', 'my.secondCompany.first.OtherClass',
        'my.thirdCompany.first.SomeClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.first*.*|my.second*.*|~*OtherClass').await();
      testGui(root).test.that.onlyNodesAre('my.firstCompany.first.SomeClass$InnerClass', 'my.firstCompany.second.SomeClass',
        'my.secondCompany.first.SomeClass');
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

      testGui(root).test.that.onlyNodesAre('my.firstCompany.first.OtherClass', 'my.firstCompany.second.OtherClass',
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

      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass|~my.otherCompany').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.somePkg.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*|~*OtherClass').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.SomeClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|~*otherPkg*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass');
      await filterOn(root, filterCollection).nameFilter('my.*.first.*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
        'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass');
      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.somePkg.SomeClass', 'my.company.first.somePkg.OtherClass',
        'my.company.first.otherPkg.SomeClass', 'my.company.first.otherPkg.OtherClass',
        'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.OtherClass',
        'my.company.second.otherPkg.SomeClass', 'my.company.second.otherPkg.OtherClass',
        'my.otherCompany.first.somePkg.SomeClass', 'my.otherCompany.first.somePkg.OtherClass',
        'my.otherCompany.first.otherPkg.SomeClass', 'my.otherCompany.first.otherPkg.OtherClass',
        'my.otherCompany.second.somePkg.SomeClass', 'my.otherCompany.second.somePkg.OtherClass',
        'my.otherCompany.second.otherPkg.SomeClass', 'my.otherCompany.second.otherPkg.OtherClass');

      testWholeLayoutOn(root);
    });

    it('does not filter out a node, that does not match the filter itself but has a matching descendant', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass$SomeInnerClass',
        'my.company.first.SomeClass$OtherInnerClass', 'my.company.second.SomeClass$SomeMatchingInnerClass',
        'my.company.second.SomeClass$SomeInnerClass', 'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('my.company.first.SomeClass$SomeInnerClass|*$*Matching*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass$SomeInnerClass', 'my.company.second.SomeClass$SomeMatchingInnerClass',
        'my.company.second.OtherClass$SomeInnerClass$SomeMatchingInnerClass');
    });

    it('does filter out packages, that have no matching descendants', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.SomeMoreClass',
        'my.company.second.SomeClass', 'my.company.second.somePkg.SomeClass$InnerClass',
        'my.company.second.somePkg.someOtherPkg.SomeClass$InnerClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*.Some*').await();
      testGui(root).test.that.onlyNodesAre('my.company.third.OtherClass');
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

          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          testGui(root).interact.ctrlClickNode('my.company.second');
          await root._updatePromise;
          testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass');
        });

        it('when the name filter already contains something', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass',
            'my.company.third.SomeClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          await filterOn(root, filterCollection).nameFilter('*first*|*second*').await();

          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass');

          testGui(root).interact.ctrlClickNode('my.company.second');
          await root._updatePromise;
          testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass');
        });

        it('right after the name filter was changed, i.e. before the last relayout was finished', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

          filterOn(root, filterCollection).nameFilter('*first*').goOn();

          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          testGui(root).test.that.onlyNodesAre('my.company.first.OtherClass');
        });

        it('when several nodes are clicked directly successively', async () => {
          const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.first.SomeClass', 'my.company.first.OtherClass',
            'my.company.second.SomeClass', 'my.company.second.OtherClass', 'my.otherCompany.first.SomeClass', 'my.otherCompany.first.OtherClass', {
              onNodeFilterStringChanged: () => root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'))
            });

          const nodeListenerMock = createListenerMock('onLayoutChanged');
          root.addListener(nodeListenerMock.listener);

          const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();
          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          testGui(root).interact.ctrlClickNode('my.company.first.OtherClass');
          testGui(root).interact.ctrlClickNode('my.company.second.SomeClass');
          testGui(root).interact.ctrlClickNode('my.otherCompany');
          document.ctrlKeyup();
          await root._updatePromise;

          testGui(root).test.that.onlyNodesAre('my.company.second.OtherClass');
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
        testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
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
        testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
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

          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('~my.company.first.SomeClass');

          testGui(root).interact.ctrlClickNode('my.company.second');
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

          testGui(root).interact.ctrlClickNode('my.company.first.SomeClass');
          await root._updatePromise;
          expect(actualNameFilterString).to.equal('*first*|*second*|~my.company.first.SomeClass');

          testGui(root).interact.ctrlClickNode('my.company.second');
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
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
        'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.otherCompany.first.SomeClass',
        'my.company.second.SomeClass', 'my.otherCompany.second.SomeClass');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('my.*.second.*').await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*').await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass$OtherInnerClass',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);

      testGui(root).interact.ctrlClickNode('my.company.second.SomeClass$OtherInnerClass');
      document.ctrlKeyup();
      await root._updatePromise;
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);

      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass');
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
      await filterOn(root, filterCollection).nameFilter('my.*.first.*|my.*second.*|~*Other*').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeInterface', 'my.otherCompany.first.SomeClass$SomeInnerInterface',
        'my.otherCompany.first.SomeInterface', 'my.company.second.SomeInterface', 'my.otherCompany.second.SomeInterface');
      testWholeLayoutOn(root);

      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      await filterOn(root, filterCollection).nameFilter('my.*.second.*').await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass$OtherInnerClass', 'my.otherCompany.second.SomeClass');
      testWholeLayoutOn(root);

      filterOn(root, filterCollection).nameFilter('my.*.second.*|*third*Other*').goOn();
      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.onlyNodesAre('my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);

      testGui(root).interact.ctrlClickNode('my.company.second.SomeClass$OtherInnerClass');
      document.ctrlKeyup();
      filterOn(root, filterCollection).nameFilter('my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(false, true).goOn();
      filterOn(root, filterCollection).nameFilter('~my.*.second.*').goOn();
      filterOn(root, filterCollection).typeFilter(true, true).goOn();
      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.onlyNodesAre('my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.otherCompany.first.SomeClass$SomeInnerInterface', 'my.otherCompany.first.SomeInterface',
        'my.company.second.SomeClass$OtherInnerClass', 'my.company.second.SomeInterface',
        'my.otherCompany.second.SomeClass', 'my.otherCompany.second.SomeInterface',
        'my.company.third.SomeClass', 'my.otherCompany.third.OtherClass');
      testWholeLayoutOn(root);
    });

    it('filters out packages that have no matching descendant only because of both filters', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout(
        'my.company.first.SomeClass', 'my.company.first.SomeInterface',
        'my.company.second.SomeInterface', 'my.company.second.somePkg.SomeClass', 'my.company.second.somePkg.SomeInterface$SomeClass',
        'my.company.third.OtherClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).and.nameFilter('~*SomeClass*').await();
      testGui(root).test.that.onlyNodesAre('my.company.third.OtherClass');
    });
  });

  //TODO: also test in node-test, that such a node cannot be unfolded anymore (not only has the correct css-class)
  describe('changes css-class of those nodes from "foldable" to "unfoldable", that loose their children, and resets it again', () => {
    it('because of the type-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInterface',
        'my.company.OtherClass$SomeInnerClass', 'my.company.SomeInterface$SomeClass', 'my.company.OtherInterface$SomeInnerInterface');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).typeFilter(false, true).await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.unfoldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
      await filterOn(root, filterCollection).typeFilter(true, false).await();
      testGui(root).test.that.node('my.company.SomeInterface').is.markedAs.unfoldable()
        .and.that.node('my.company.OtherInterface').is.markedAs.foldable();
      await filterOn(root, filterCollection).typeFilter(true, true).await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.foldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
    });

    it('because of the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.unfoldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
      await filterOn(root, filterCollection).nameFilter('').await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.foldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
    });

    //TODO: test this also for nodes that become unfoldable only because of the violations-filter (somewhere in graph-tests)
    it('only because of the type- and the name-filter', async () => {
      const root = await rootCreator.createRootFromClassNamesAndLayout('my.company.SomeClass$SomeInnerClass',
        'my.company.SomeClass$InnerInterface', 'my.company.OtherClass$OtherInnerClass');

      const filterCollection = buildFilterCollection().addFilterGroup(root.filterGroup).build();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(false, true).await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.unfoldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
      await filterOn(root, filterCollection).nameFilter('').and.typeFilter(false, true).await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.foldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();

      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(false, true).await();
      await filterOn(root, filterCollection).nameFilter('~*$SomeInner*').and.typeFilter(true, true).await();
      testGui(root).test.that.node('my.company.SomeClass').is.markedAs.foldable()
        .and.that.node('my.company.OtherClass').is.markedAs.foldable();
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