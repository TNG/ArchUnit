'use strict';

const init = (Root, Dependencies, View, visualizationStyles) => {

  const Graph = class {
    constructor(jsonRoot, violations, svg, foldAllNodes) {
      this._view = new View(svg);
      this.root = new Root(jsonRoot, this._view.svgElementForNodes, rootRadius => this._view.renderWithTransition(rootRadius), newNodeFilterString => this.onNodeFilterStringChanged(newNodeFilterString));
      this.dependencies = new Dependencies(jsonRoot, this.root, this._view.svgElementForDependencies);
      this.root.addListener(this.dependencies.createListener());
      this.root.getLinks = () => this.dependencies.getAllLinks();
      this.root.getNodesWithDependencies = () => this.dependencies.getDistinctNodesHavingDependencies();
      if (foldAllNodes) {
        this.root.foldAllNodes();
      }
      this.dependencies.recreateVisible();
      this.root.relayoutCompletely();
      this._violations = violations;
    }

    filterNodesByName(filterString) {
      this.root.filterByName(filterString);
    }

    filterNodesByType(filter) {
      this.root.filterByType(filter.showInterfaces, filter.showClasses);
    }

    filterDependenciesByType(typeFilterConfig) {
      this.dependencies.filterByType(typeFilterConfig);
    }

    unfoldNodesToShowAllViolations() {
      const nodesContainingViolations = this.dependencies.getNodesContainingViolations();
      nodesContainingViolations.forEach(node => node.callOnEveryPredecessorThenSelf(node => node.unfold()));
      this.dependencies.recreateVisible();
      this.root.relayoutCompletely();
    }

    foldNodesWithMinimumDepthWithoutViolations() {
      const nodesWithoutViolations = this.dependencies.getNodesInvolvedInViolations();
      this.root.foldNodesWithMinimumDepthThatHaveNotDescendants(nodesWithoutViolations);
      this.dependencies.recreateVisible();
      this.root.relayoutCompletely();
    }

    onNodeFilterStringChanged(newNodeFilterString) {
      this._menu.changeNodeNameFilter(newNodeFilterString);
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
        .onNodeTypeFilterChanged(
          filter => {
            this.filterNodesByType(filter);
          })
        .onDependencyFilterChanged(
          filter => {
            this.filterDependenciesByType(filter);
          })
        .onNodeNameFilterChanged((filterString) => {
          this.filterNodesByName(filterString);
        })
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

    attachToViolationMenu(violationMenu) {
      violationMenu.initialize(this._violations,
        violationsGroup => this.dependencies.showViolations(violationsGroup),
        violationsGroup => this.dependencies.hideViolations(violationsGroup));
      violationMenu.onHideAllDependenciesChanged(
        hide => this.dependencies.onHideAllOtherDependenciesWhenViolationExists(hide));
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