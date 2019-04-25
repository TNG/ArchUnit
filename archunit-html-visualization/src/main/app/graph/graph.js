'use strict';

const {buildFilterCollection} = require('./filter');

const init = (Root, Dependencies, View, visualizationStyles) => {

  const Graph = class {
    constructor(jsonGraph, violations, svg, svgContainerDivDomElement) {
      this._view = new View(svg, svgContainerDivDomElement);
      this._root = new Root(jsonGraph.root,
        (halfWidth, halfHeight) => this._view.renderWithTransition(halfWidth, halfHeight),
        (halfWidth, halfHeight) => this._view.render(halfWidth, halfHeight),
        (offsetPosition) => this._view.changeScrollPosition(offsetPosition),
        newNodeFilterString => this._onNodeFilterStringChanged(newNodeFilterString));

      this._view.addRootView(this._root.view);

      this._dependencies = new Dependencies(jsonGraph.dependencies, this._root,
        {
          svgDetailedDependenciesContainer: this._view.svgElementForDetailedDependencies, svg: this._view.svgElement,
          svgCenterTranslater: this._view.translater
        });

      this._root.addListener(this._dependencies.createListener());
      this._root.getLinks = () => this._dependencies.getAllLinks();
      this._root.getVisibleViolationsFilter = () => this._dependencies.getVisibleViolationsNodeFilter();
      this._root.getDependenciesDirectlyWithinNode = node => this._dependencies.getDependenciesDirectlyWithinNode(node);

      this._createFilters();

      this._root.foldAllNodes();
      this._dependencies.recreateVisible();

      this._root.relayoutCompletely();
      this._violations = violations;
    }

    _updateFilterAndRelayout(filterKey) {
      this._root.doNextAndWaitFor(() => this._filterCollection.updateFilter(filterKey));
      //FIXME: the initialization of the filters causes a delayed load of the html page --> reduce initial calls to one
      this._root.enforceCompleteRelayout();
    }

    _createFilters() {
      this._filterCollection = buildFilterCollection()
        .addFilterGroup(this._root.filterGroup)
        .addFilterGroup(this._dependencies.filterGroup)
        .build();

      this._root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
      this._root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');
      this._dependencies.filterGroup.getFilter('type').addDependentFilterKey('nodes.visibleViolations');
      this._dependencies.filterGroup.getFilter('nodeTypeAndName').addDependentFilterKey('nodes.visibleViolations');
      this._dependencies.filterGroup.getFilter('violations').addDependentFilterKey('nodes.visibleViolations');
    }

    _filterNodesByName(filterString) {
      this._root.nameFilterString = filterString;
      this._updateFilterAndRelayout('nodes.name');
    }

    _filterNodesByType(filter) {
      this._root.changeTypeFilter(filter.showInterfaces, filter.showClasses);
      this._updateFilterAndRelayout('nodes.type');
    }

    _filterDependenciesByType(typeFilterConfig) {
      this._dependencies.changeTypeFilter(typeFilterConfig);
      this._updateFilterAndRelayout('dependencies.type');
    }

    _unfoldNodesToShowAllViolations() {
      const nodesContainingViolations = this._dependencies.getNodesContainingViolations();
      nodesContainingViolations.forEach(node => node.callOnEveryPredecessorThenSelf(node => node.unfold()));
      this._dependencies.recreateVisible();
      this._root.relayoutCompletely();
    }

    _foldNodesWithMinimumDepthWithoutViolations() {
      this._root.foldNodesWithMinimumDepthThatHaveNotDescendants(this._dependencies.getNodesInvolvedInVisibleViolations());
      this._dependencies.recreateVisible();
      this._root.relayoutCompletely();
    }

    _onNodeFilterStringChanged(newNodeFilterString) {
      this._menu.changeNodeNameFilter(newNodeFilterString);
      this._root.doNextAndWaitFor(() => this._filterCollection.updateFilter('nodes.name'));
    }

    _onHideNodesWithoutViolationsChanged(hide) {
      this._filterCollection.getFilter('nodes.visibleViolations').filterPrecondition.filterIsEnabled = hide;
      this._updateFilterAndRelayout('nodes.visibleViolations');
    }

    _showViolations(violationsGroup) {
      this._dependencies.showViolations(violationsGroup);
      this._updateFilterAndRelayout('dependencies.violations');
    }

    _hideViolations(violationsGroup) {
      this._dependencies.hideViolations(violationsGroup);
      this._updateFilterAndRelayout('dependencies.violations');
    }

    attachToMenu(menu) {
      this._menu = menu;
      this._menu.initializeSettings(
        {
          initialCircleFontSize: visualizationStyles.getNodeFontSize(),
          initialCirclePadding: visualizationStyles.getCirclePadding()
        })
        .onSettingsChanged(
          (circleFontSize, circlePadding) => {
            visualizationStyles.setNodeFontSize(circleFontSize);
            visualizationStyles.setCirclePadding(circlePadding);
            this._root.relayoutCompletely();
          })
        .onNodeTypeFilterChanged(filter => this._filterNodesByType(filter))
        .initializeDependencyFilter(this._dependencies.dependencyTypes)
        .onDependencyFilterChanged(filter => this._filterDependenciesByType(filter))
        .onNodeNameFilterChanged((filterString) => this._filterNodesByName(filterString));
    }

    attachToViolationMenu(violationMenu) {
      violationMenu.initialize(this._violations,
        violationsGroup => this._showViolations(violationsGroup),
        violationsGroup => this._hideViolations(violationsGroup)
      );

      violationMenu.onHideAllDependenciesChanged(
        hide => {
          this._filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = hide;
          this._updateFilterAndRelayout('dependencies.violations');
        });

      violationMenu.onHideNodesWithoutViolationsChanged(hide => this._onHideNodesWithoutViolationsChanged(hide));

      violationMenu.onClickUnfoldNodesToShowAllViolations(() => this._unfoldNodesToShowAllViolations());
      violationMenu.onClickFoldNodesToHideNodesWithoutViolations(() => this._foldNodesWithMinimumDepthWithoutViolations());
    }
  };

  return {
    Graph
  };
};

module.exports = {
  init: (appContext) => ({
    create: (svgElement, svgContainerDivElement) => {
      const Graph = init(appContext.getRoot(), appContext.getDependencies(),
        appContext.getGraphView(), appContext.getVisualizationStyles()).Graph;

      const visualizationData = appContext.getVisualizationData();
      return new Graph(visualizationData.jsonGraph, visualizationData.jsonViolations, svgElement, svgContainerDivElement);
    }
  })
};