'use strict';

const testOn = require('./node-test-infrastructure').testOnRoot;

const filterOn = (root, filterCollection) => {
  let applyNameFilter = false;
  let applyTypeFilter = false;
  const testFunctions = [];

  let testOnRoot;

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
      classesAre: (...classNames) => {
        testFunctions.push(() => testOnRoot.that.it.hasClasses(...classNames));
        return and;
      },
      node: (fqn) => ({
        isMarkedAs: {
          foldable: () => {
            testFunctions.push(() => testOnRoot.that.nodeWithFullName(fqn).is.markedAs.foldable());
            return and;
          },
          unfoldable: () => {
            testFunctions.push(() => testOnRoot.that.nodeWithFullName(fqn).is.markedAs.unfoldable());
            return and;
          }
        }
      })
    }
  };

  const goOn = () => {
    applyFilters();
    testOnRoot = testOn(root);
    testFunctions.forEach(testFunction => testFunction());
  };

  const await = async () => {
    applyFilters();
    await root._updatePromise;
    testOnRoot = testOn(root);
    testFunctions.forEach(testFunction => testFunction());
  };

  const and = {
    and: test,
    goOn, await
  };

  const testOrAddFilter = {
    and: filter,
    thenTest: test,
    goOn, await
  };

  return filter;
};

module.exports.filterOn = filterOn;