'use strict';

const testOn = require('./node-test-infrastructure').testOnRoot;

const filterOn = (root, filterCollection) => {
  let applyNameFilter = false;
  let applyTypeFilter = false;
  const testFunctions = [];

  const applyFilters = () => {
    if (applyTypeFilter) {
      root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.type'));
    }
    if (applyNameFilter) {
      root.doNextAndWaitFor(() => filterCollection.updateFilter('nodes.name'));
    }
    root.enforceCompleteRelayout();
  };

  const filter = {
    typeFilter: (showInterfaces, showClasses) => {
      root.changeTypeFilter(showInterfaces, showClasses);
      applyTypeFilter = true;
      return testOrAddFilter;
    },
    nameFilter: (filterString) => {
      root.nameFilterString = filterString;
      applyNameFilter = true;
      return testOrAddFilter;
    }
  };

  const test = {
    that: {
      nodesAre: (...classNames) => {
        testFunctions.push(() => testOn(root).that.it.hasOnlyVisibleLeaves(...classNames));
        return and;
      },
      node: (fqn) => ({
        isFoldable: () => {
          testFunctions.push(() => testOn(root).that.nodeWithFullName(fqn).is.foldable());
          return and;
        },
        isUnfoldable: () => {
          testFunctions.push(() => testOn(root).that.nodeWithFullName(fqn).is.unfoldable());
          return and;
        }
      })
    }
  };

  const finish = () => {
    applyFilters();
    testFunctions.forEach(testFunction => testFunction());
  };

  const await = async () => {
    applyFilters();
    await root._updatePromise;
    testFunctions.forEach(testFunction => testFunction());
  };

  const and = {
    and: test,
    finish, await
  };

  const testOrAddFilter = {
    and: filter,
    thenTest: test,
    finish, await
  };

  return filter;
};

module.exports.filterOn = filterOn;