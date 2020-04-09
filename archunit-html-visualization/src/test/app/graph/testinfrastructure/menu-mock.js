const MenuMock = class MenuMock {

  initializeSettings() {
    return this;
  }

  onSettingsChanged() {
    return this;
  }

  onNodeTypeFilterChanged(onChanged) {
    this._nodeTypeFilterCallback = onChanged;
    return this;
  }

  initializeDependencyFilter() {
    return this;
  }

  onDependencyFilterChanged() {
    return this;
  }

  onNodeNameFilterChanged(onChanged) {
    this._nodeNameFilter = onChanged;
    return this;
  }

  changeNodeFilter(filterString) {
    this._nodeNameFilter(filterString);
  }

  filterNodesByType({showInterfaces, showClasses}) {
    this._nodeTypeFilterCallback({showInterfaces, showClasses});
  }
};

module.exports = { MenuMock: MenuMock };
