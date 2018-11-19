'use strict';

import {buildFilterCollection} from './filter';

const init = (Root, Dependencies, View, visualizationStyles) => {

  const Graph = class {
    constructor(jsonRoot, violations, svg, foldAllNodes) {
      this._view = new View(svg);
      this.root = new Root(jsonRoot, this._view.svgElementForNodes, rootRadius => this._view.renderWithTransition(rootRadius),
        newNodeFilterString => this.onNodeFilterStringChanged(newNodeFilterString));
      this.dependencies = new Dependencies(jsonRoot, this.root, this._view.svgElementForDependencies);

      this.root.addListener(this.dependencies.createListener());
      this.root.getLinks = () => this.dependencies.getAllLinks();
      this.root.getNodesWithDependencies = () => this.dependencies.getDistinctNodesHavingDependencies();
      this.root.getNodesInvolvedInVisibleViolations = () => this.dependencies.getNodesInvolvedInVisibleViolations();
      this.root.getHasNodeVisibleViolation = () => this.dependencies.getHasNodeVisibleViolation();

      this._createFilters();

      if (foldAllNodes) {
        this.root.foldAllNodes();
      }
      this.dependencies.recreateVisible();

      this.root.relayoutCompletely();
      this._violations = violations;
    }

    _updateFilterAndRelayout(filterKey) {
      this.root.doNextAndWaitFor(() => this._filterCollection.updateFilter(filterKey));
      this.root.relayoutCompletely();
    }

    _createFilters() {
      this._filterCollection = buildFilterCollection()
        .addFilterGroup(this.root.filterGroup)
        .addFilterGroup(this.dependencies.filterGroup)
        .build();

      this.root.filterGroup.getFilter('typeAndName').addDependentFilterKey('dependencies.nodeTypeAndName');
      this.root.filterGroup.getFilter('combinedFilter').addDependentFilterKey('dependencies.visibleNodes');
      this.dependencies.filterGroup.getFilter('type').addDependentFilterKey('nodes.visibleViolations');
      this.dependencies.filterGroup.getFilter('nodeTypeAndName').addDependentFilterKey('nodes.visibleViolations');
      this.dependencies.filterGroup.getFilter('violations').addDependentFilterKey('nodes.visibleViolations');

    }

    filterNodesByName(filterString) {
      this.root.nameFilterString = filterString;
      this._updateFilterAndRelayout('nodes.name');
    }

    filterNodesByType(filter) {
      this.root.changeTypeFilter(filter.showInterfaces, filter.showClasses);
      this._updateFilterAndRelayout('nodes.type');
    }

    filterDependenciesByType(typeFilterConfig) {
      this.dependencies.changeTypeFilter(typeFilterConfig);
      this._updateFilterAndRelayout('dependencies.type');
    }

    unfoldNodesToShowAllViolations() {
      const nodesContainingViolations = this.dependencies.getNodesContainingViolations();
      nodesContainingViolations.forEach(node => node.callOnEveryPredecessorThenSelf(node => node.unfold()));
      this.dependencies.recreateVisible();
      this.root.relayoutCompletely();
    }

    foldNodesWithMinimumDepthWithoutViolations() {
      this.root.foldNodesWithMinimumDepthThatHaveNoViolations();
      this.dependencies.recreateVisible();
      this.root.relayoutCompletely();
    }

    onNodeFilterStringChanged(newNodeFilterString) {
      this._menu.changeNodeNameFilter(newNodeFilterString);
      this.root.doNextAndWaitFor(() => this._filterCollection.updateFilter('nodes.name'));
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
            this.root.relayoutCompletely();
          })
        .onNodeTypeFilterChanged(filter => this.filterNodesByType(filter))
        .onDependencyFilterChanged(filter => this.filterDependenciesByType(filter))
        .onNodeNameFilterChanged((filterString) => this.filterNodesByName(filterString))
        .initializeLegend([
          visualizationStyles.getLineStyle('constructorCall', 'constructor call'),
          visualizationStyles.getLineStyle('methodCall', 'method call'),
          visualizationStyles.getLineStyle('fieldAccess', 'field access'),
          visualizationStyles.getLineStyle('extends', 'extends'),
          visualizationStyles.getLineStyle('implements', 'implements'),
          visualizationStyles.getLineStyle('implementsAnonymous', 'implements anonymous'),
          visualizationStyles.getLineStyle('childrenAccess', 'innerclass access'),
          visualizationStyles.getLineStyle('several', 'grouped access')
        ]);
    }

    onHideNodesWithoutViolationsChanged(hide) {
      this._filterCollection.getFilter('nodes.visibleViolations').filterPrecondition.filterIsEnabled = hide;
      this._updateFilterAndRelayout('nodes.visibleViolations');
    }

    showViolations(violationsGroup) {
      this.dependencies.showViolations(violationsGroup);
      this._updateFilterAndRelayout('dependencies.violations');
    }

    hideViolations(violationsGroup) {
      this.dependencies.hideViolations(violationsGroup);
      this._updateFilterAndRelayout('dependencies.violations');
    }

    attachToViolationMenu(violationMenu) {
      violationMenu.initialize(this._violations,
        violationsGroup => this.showViolations(violationsGroup),
        violationsGroup => this.hideViolations(violationsGroup)
      );

      violationMenu.onHideAllDependenciesChanged(
        hide => {
          this._filterCollection.getFilter('dependencies.violations').filterPrecondition.filterIsEnabled = hide;
          this._updateFilterAndRelayout('dependencies.violations');
        });

      violationMenu.onHideNodesWithoutViolationsChanged(hide => this.onHideNodesWithoutViolationsChanged(hide));

      violationMenu.onClickUnfoldNodesToShowAllViolations(() => this.unfoldNodesToShowAllViolations());
      violationMenu.onClickFoldNodesToHideNodesWithoutViolations(() => this.foldNodesWithMinimumDepthWithoutViolations());
    }
  };

  return {
    Graph
  };
};

export default (appContext, resources, svgElement, foldAllNodes) => {
  const Graph = init(appContext.getRoot(), appContext.getDependencies(),
    appContext.getGraphView(), appContext.getVisualizationStyles()).Graph;

  const {root, violations} = resources.getResources();
  return new Graph(root, violations, svgElement, foldAllNodes);
};