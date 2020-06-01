const MenuMock = class MenuMock {

  initializeSettings() {
    return this;
  }

  onSettingsChanged(onSettingsChanged) {
    this._onSettingsChanged = onSettingsChanged;
    return this;
  }

  onNodeTypeFilterChanged(onChanged) {
    this._nodeTypeFilterCallback = onChanged;
    return this;
  }

  initializeDependencyFilter() {
    return this;
  }

  onDependencyFilterChanged(onChanged) {
    this._dependencyFilter = onChanged;
    return this;
  }

  onNodeNameFilterChanged(onChanged) {
    this._nodeNameFilter = onChanged;
    return this;
  }

  // this a mock for the public API of the menu
  changeNodeNameFilter(filterString) {
    this._currentFilterString = filterString;
    this._nodeNameFilter(filterString);
  }

  // this is helper function to programmatically change the node filter
  changeNodeFilter(filterString) {
    this._nodeNameFilter(filterString);
  }

  changeDependencyFilter(typeFilterConfig) {
    this._dependencyFilter(typeFilterConfig);
  }

  filterNodesByType({showInterfaces, showClasses}) {
    this._nodeTypeFilterCallback({showInterfaces, showClasses});
  }

  changeMenuSettings(circleFontSize, circlePadding) {
    this._onSettingsChanged(circleFontSize, circlePadding);
  }

  getCurrentFilterString() {
    return this._currentFilterString;
  }
};

module.exports = { MenuMock: MenuMock };
