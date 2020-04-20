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

  // this a mock for the public API of the menu
  changeNodeNameFilter(filterString) {
    this._nodeNameFilter(filterString);
  }

  // this is helper function to programmatically change the node filter
  changeNodeFilter(filterString) {
    this._nodeNameFilter(filterString);
  }

  filterNodesByType({showInterfaces, showClasses}) {
    this._nodeTypeFilterCallback({showInterfaces, showClasses});
  }
};

module.exports = { MenuMock: MenuMock };
