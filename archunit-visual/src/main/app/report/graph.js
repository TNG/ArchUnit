'use strict';

const init = (Node, Dependencies, View) => {
  const Graph = class {
    constructor(jsonRoot, svg) {
      this._view = new View(svg);
      this.root = new Node(jsonRoot, this._view.svgElementForNodes, rootRadius => this._view.renderWithTransition(rootRadius));
      this.dependencies = new Dependencies(jsonRoot, this.root, this._view.svgElementForDependencies);
      this.root.addListener(this.dependencies.createListener());
      this.updatePromise = this.root.relayout();
    }

    foldAllNodes() {
      this.updatePromise = this.updatePromise.then(() => {
        this.root.callOnEveryDescendantThenSelf(node => node.foldIfInnerNode());
        return this.root.relayout();
      });
    }

    filterNodesByNameContaining(filterString) {
      this.updatePromise = this.updatePromise.then(() => {
        this.root.filterByName(filterString, false);
        this.dependencies.setNodeFilters(this.root.getFilters());
        return this.root.relayout();
      });
    }

    filterNodesByNameNotContaining(filterString) {
      this.updatePromise = this.updatePromise.then(() => {
        this.root.filterByName(filterString, true);
        this.dependencies.setNodeFilters(this.root.getFilters());
        return this.root.relayout();
      });
    }

    filterNodesByType(filter) {
      this.updatePromise = this.updatePromise.then(() => {
        this.root.filterByType(filter.showInterfaces, filter.showClasses);
        this.dependencies.setNodeFilters(this.root.getFilters());
        return this.root.relayout();
      });
    }

    filterDependenciesByType(typeFilterConfig) {
      this.updatePromise.then(() => this.dependencies.filterByType(typeFilterConfig));
    }

    refresh() {
      this.updatePromise = this.updatePromise.then(() => this.root.relayout());
    }
  };

  return {
    Graph
  };
};

module.exports.init = init; // FIXME: Make create() the only public API

module.exports.create = () => {

  return new Promise((resolve, reject) => {
    const d3 = require('d3');

    d3.json('80/classes.json', function (error, jsonroot) {
      if (error) {
        return reject(error);
      }

      const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
      const appContext = require('./app-context').newInstance();
      const Node = appContext.getNode(); // FIXME: Correct dependency tree
      const Dependencies = appContext.getDependencies(); // FIXME: Correct dependency tree
      const graphView = appContext.getGraphView();

      const Graph = init(Node, Dependencies, graphView).Graph;
      const graph = new Graph(jsonroot, d3.select('#visualization').node());
      graph.foldAllNodes();

      //FIXME AU-24: Move this into graph
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