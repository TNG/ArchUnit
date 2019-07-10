'use strict';

const filterOn = (root, filterCollection) => {
  let applyNameFilter = false;
  let applyTypeFilter = false;

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
      return and;
    },
    nameFilter: (filterString) => {
      root.nameFilterString = filterString;
      applyNameFilter = true;
      return and;
    }
  };

  const goOn = () => {
    applyFilters();
  };

  const waitFor = async () => {
    applyFilters();
    await root._updatePromise;
  };

  const and = {
    and: filter,
    goOn, await: waitFor
  };

  return filter;
};

module.exports.filterOn = filterOn;