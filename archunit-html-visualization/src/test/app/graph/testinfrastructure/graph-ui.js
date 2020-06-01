const Assertion = require('chai').Assertion;
const RootUi = require('../nodes/testinfrastructure/root-ui');
const {DependenciesUi} = require('../dependencies/testinfrastructure/dependencies-ui');
const MenuMock = require('./menu-mock').MenuMock;
const ViolationMenuMock = require('./violation-menu-mock').ViolationMenuMock;

const svgGroupsContainingAVisible = (graph, svgType) => graph._view.svgElement.getAllGroupsContainingAVisibleElementOfType(svgType);
const svgGroupsContainingAVisibleCircle = graph => svgGroupsContainingAVisible(graph, 'circle');
const svgGroupsContainingAVisibleLine = graph => svgGroupsContainingAVisible(graph, 'line');

class GraphUi {
  constructor(graph) {
    this._graph = graph;
    this._rootUi = RootUi.of(graph._root);
    this._dependenciesUi = DependenciesUi.of(graph._dependencies);
    this._menu = new MenuMock();
    this._violationMenu = new ViolationMenuMock();

    this._graph.attachToMenu(this._menu);
    this._graph.attachToViolationMenu(this._violationMenu);
  }

  get rootUi() {
    return this._rootUi;
  }

  async waitForUpdateFinished() {
    await this._graph._root._updatePromise;
  }

  async clickNode(nodeName) {
    const node = this._rootUi.getNodeWithFullName(nodeName);
    node.click();
    return this.waitForUpdateFinished();
  }

  async ctrlClickNode(nodeName) {
    const node = this._rootUi.getNodeWithFullName(nodeName);
    node.ctrlClick();
    return this.waitForUpdateFinished();
  }

  async dragNode(nodeName, dx, dy) {
    const node = this._rootUi.getNodeWithFullName(nodeName);
    node.drag({dx, dy});
    return this.waitForUpdateFinished();
  }

  async changeNodeFilter(filterString) {
    this._menu.changeNodeFilter(filterString);
    return this.waitForUpdateFinished();
  }

  async filterNodesByType({showInterfaces, showClasses}) {
    this._menu.filterNodesByType({showInterfaces, showClasses});
    await this.waitForUpdateFinished();
  }

  getVisibleDependencyWithName(dependencyName) {
    return this._dependenciesUi.getVisibleDependency(dependencyName);
  }

  async selectViolation(violationGroup) {
    this._violationMenu.selectViolationGroup(violationGroup);
    return this.waitForUpdateFinished();
  }

  async deselectViolation(violationGroup) {
    this._violationMenu.hideViolationGroup(violationGroup);
    return this.waitForUpdateFinished();
  }

  async clickUnfoldNodesToShowAllViolations() {
    this._violationMenu.unfoldNodesToShowViolations();
    return this.waitForUpdateFinished();
  }

  async hideNodesWithoutViolationsChanged(hide) {
    this._violationMenu.hideNodesWithoutViolationsChanged(hide);
    return this.waitForUpdateFinished();
  }

  async clickFoldNodesToHideNodesWithoutViolations() {
    this._violationMenu.clickFoldNodesToHideNodesWithoutViolations();
    return this.waitForUpdateFinished();
  }

  async filterDependenciesByType(typeFilterConfig) {
    this._menu.changeDependencyFilter(typeFilterConfig);
    await this.waitForUpdateFinished();
  }

  async changeMenuSettings(circleFontSize, circlePadding) {
    this._menu.changeMenuSettings(circleFontSize, circlePadding);
    await this.waitForUpdateFinished();
  }

  expectOnlyVisibleNodes(...nodes) {
    const allGroupsWithAVisibleCircle = svgGroupsContainingAVisibleCircle(this._graph);
    const expectedNodeNames = Array.isArray(nodes[0]) ? nodes[0] : Array.from(nodes);
    const textElementsOfVisibleCircles = allGroupsWithAVisibleCircle.map(g => g.getVisibleSubElementOfType('text'));
    const actualNodeNames = textElementsOfVisibleCircles.map(textElement => textElement.getAttribute('text'));

    new Assertion(actualNodeNames).to.have.members(expectedNodeNames);
  }

  expectOnlyVisibleDependencies(...dependencies) {
    const expectedVisibleDependencies = Array.isArray(dependencies[0]) ? dependencies[0] : Array.from(dependencies);
    const allGroupsWithAVisibleLine = svgGroupsContainingAVisibleLine(this._graph);

    new Assertion(allGroupsWithAVisibleLine.map(g => g.getAttribute('id'))).to.have.members(expectedVisibleDependencies);
  }

  expectNodeSizeCloseTo(expectedNodeSize, ...nodes) {
    const expectedNodeNames = Array.isArray(nodes[0]) ? nodes[0] : Array.from(nodes);
    const expectedNodes = expectedNodeNames.map(nodeName => this._rootUi.getNodeWithFullName(nodeName));

    expectedNodes.forEach(node => new Assertion(node.radius).to.be.closeTo(expectedNodeSize, 5));
  }

  expectNodeFilter(expectedFilterString) {
    new Assertion(this._menu.getCurrentFilterString()).to.equal(expectedFilterString);
  }
}

module.exports = {of: graph => new GraphUi(graph)};
