const ViolationMenuMock = class ViolationMenuMock {
  initialize(violations, showViolationsOfGroupCallback, hideViolationsOfGroupCallback) {
    this._showViolationsOfGroupCallback = showViolationsOfGroupCallback;
    this._hideViolationsOfGroupCallback = hideViolationsOfGroupCallback;
  }

  onHideAllDependenciesChanged(callback) {
    this._hideAllDependenciesChangedCallback = callback;
    this._hideAllDependenciesChangedCallback(true);
  }

  onHideNodesWithoutViolationsChanged(callback) {
    this._hideNodesWithoutViolationsChangedCallback = callback;
    this._hideNodesWithoutViolationsChangedCallback(callback);
  }

  onClickUnfoldNodesToShowAllViolations(callback) {
    this._clickUnfoldNodesToShowAllViolationsCallback = callback;
  }

  onClickFoldNodesToHideNodesWithoutViolations(callback) {
    this._clickFoldNodesToHideNodesWithoutViolationsCallback= callback;
  }

  selectViolationGroup(violationGroup) {
    this._showViolationsOfGroupCallback(violationGroup);
  }

  hideViolationGroup(violationGroup) {
    this._hideViolationsOfGroupCallback(violationGroup);
  }

  hideNodesWithoutViolationsChanged(hide) {
    this._hideNodesWithoutViolationsChangedCallback(hide);
  }

  unfoldNodesToShowViolations() {
    this._clickUnfoldNodesToShowAllViolationsCallback();
  }
};

module.exports = { ViolationMenuMock: ViolationMenuMock };
