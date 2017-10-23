'use strict';

const init = (jsonToRoot, jsonToDependencies, View) => {
  const Graph = class {
    constructor(root, dependencies) {
      this.root = root;
      this.dependencies = dependencies;
      this.root.setOnDrag(node => this.dependencies.jumpSpecificDependenciesToTheirPositions(node));
      this.root.setOnFold(node => this.dependencies.updateOnNodeFolded(node.getFullName(), node.isFolded()));
      this.root.setOnFiltersChanged(() => this.dependencies.setNodeFilters(this.root.getFilters()));
      this.root.setOnLayoutChanged(() => this.dependencies.moveAllToTheirPositions());
      this.updatePromise = Promise.resolve();
    }

    initView(svg, createDetailedDepsSvg) {
      this._view = new View(svg, this.root.visualData.r);

      this.root.initView(this._view.gTree, () => this._view.renderWithTransition(this.root.getRadius()));

      //FIXME: statt svgElementForDetailed lieber an g der jeweiligen Dependency dranhängen und vllt auch detailed
      // view in dependency-view rein (dann wird es automatisch mitgeschoben, was ja auf jeden Fall erwünscht ist)
      this.dependencies.initViews(this._view.gEdges, createDetailedDepsSvg);
    }

    foldAllNodes() {
      this.root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          node.fold();
        }
      });
    }

    getDetailedDependenciesOf(from, to) {
      return this.dependencies.getDetailedDependenciesOf(from, to);
    }

    filterNodesByNameContaining(filterString) {
      this.updatePromise = this.updatePromise.then(() => this.root.filterByName(filterString, false));
    }

    filterNodesByNameNotContaining(filterString) {
      this.updatePromise = this.updatePromise.then(() => this.root.filterByName(filterString, true));
    }

    filterNodesByType(filter) {
      this.updatePromise = this.updatePromise.then(() => this.root.filterByType(filter.showInterfaces, filter.showClasses));
    }

    filterDependenciesByType(typeFilterConfig) {
      this.updatePromise.then(() => this.dependencies.filterByType(typeFilterConfig));
    }

    refresh() {
      this.updatePromise = this.updatePromise.then(() => this.root.relayout());
    }
  };

  return {
    jsonToGraph: jsonRoot => {
      const root = jsonToRoot(jsonRoot);
      const dependencies = jsonToDependencies(jsonRoot, root);
      const graph = new Graph(root, dependencies);
      return graph;
    }
  };
};

module.exports.init = init; // FIXME: Make create() the only public API

module.exports.create = () => {

  const d3 = require('d3');

  const svg = d3.select('#visualization');

  const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
  const appContext = require('./app-context').newInstance();
  const jsonToRoot = appContext.getJsonToRoot(); // FIXME: Correct dependency tree
  const jsonToDependencies = appContext.getJsonToDependencies(); // FIXME: Correct dependency tree
  const graphView = appContext.getGraphView();

  let graph;

  return new Promise((resolve, reject) => {
    d3.json('80/classes.json', function (error, jsonroot) {
      if (error) {
        return reject(error);
      }

      const jsonToGraph = init(jsonToRoot, jsonToDependencies, graphView).jsonToGraph;
      graph = jsonToGraph(jsonroot);
      const createDetailedDepsParent = () => svg.append('g');
      graph.initView(svg.node(), createDetailedDepsParent);

      //FIXME: Only temporary, we need to decompose this further and separate d3 into something like 'renderer'
      graph.attachToMenu = menu => {
        menu.initializeSettings(
          {
            initialCircleFontSize: visualizationStyles.getNodeFontSize(),
            initialCirclePadding: visualizationStyles.getCirclePadding()
          })
          .onSettingsChanged(
            (circleFontSize, circlePadding) => {
              visualizationStyles.setNodeFontSize(circleFontSize);
              visualizationStyles.setCirclePadding(circlePadding);
              graph.refresh();
            })
          .onNodeTypeFilterChanged(
            filter => {
              graph.filterNodesByType(filter);
            })
          .onDependencyFilterChanged(
            filter => {
              graph.filterDependenciesByType(filter);
            })
          .onNodeNameFilterChanged((filterString, exclude) => {
            if (exclude) {
              graph.filterNodesByNameNotContaining(filterString);
            } else {
              graph.filterNodesByNameContaining(filterString);
            }
          })
          .initializeLegend([
            visualizationStyles.getLineStyle("constructorCall", "constructor call"),
            visualizationStyles.getLineStyle("methodCall", "method call"),
            visualizationStyles.getLineStyle("fieldAccess", "field access"),
            visualizationStyles.getLineStyle("extends", "extends"),
            visualizationStyles.getLineStyle("implements", "implements"),
            visualizationStyles.getLineStyle("implementsAnonymous", "implements anonymous"),
            visualizationStyles.getLineStyle("childrenAccess", "innerclass access"),
            visualizationStyles.getLineStyle("several", "grouped access")
          ]);
      };

      resolve(graph);
    });
  });
};