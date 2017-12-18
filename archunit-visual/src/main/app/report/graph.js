'use strict';

const d3 = require('d3');

const init = (Node, Dependencies, View, visualizationStyles) => {

  const countLinksOfNode = (allLinks, node) => allLinks.filter(d => d.source === node || d.target === node).length;

  const Graph = class {
    constructor(jsonRoot, svg) {
      this._view = new View(svg);
      this.root = new Node(jsonRoot, this._view.svgElementForNodes, rootRadius => this._view.renderWithTransition(rootRadius));
      this.dependencies = new Dependencies(jsonRoot, this.root, this._view.svgElementForDependencies);
      this.root.addListener(this.dependencies.createListener());
      this.root.relayout();
      this.startSimulation = () => this.root.doNext(() => this._startSimulation());
    }

    _startSimulation() {
      //TODO: do not recreate forceSimulation all time, but always recreate allNodes and allLinks (as they might change)

      const allNodes = this.root.createAbsoluteNodes();

      //FIXME: if a node has only one child, drag it in the middle
      const allLinks = this.dependencies.getSimpleDependencies();

      allLinks.forEach(link => console.log(link.source + "->" + link.target));

      const simulation = d3.forceSimulation()
        .alphaDecay(0.007) //.alphaDecay(0.05)
        .force('link', d3.forceLink()
          .id(n => n.fullName)
          .distance(d => d.source.r + d.target.r + 2 * visualizationStyles.getCirclePadding())
          //TODO: maybe make the force of parent nodes weaker
          .strength(link => 1 / Math.min(countLinksOfNode(allLinks, link.source), countLinksOfNode(allLinks, link.target)))
          .iterations(2))
        .stop();

      //TODO: maybe use forceManyBody to prevent "loose" nodes --> no, does not seem to have an affect
      //.force("charge", d3.forceManyBody().strength(-100));

      const ticked = () => {
        //update nodes and deps and re-update allNodes
        allNodes.forEach(node => node.originalNode.visualData.setAbsoluteIntermediatePosition(node.x, node.y, node.originalNode.getParent()));
        allNodes.forEach(node => node.originalNode.updateAbsoluteNode());
      };

      const updateOnEnd = () => {
        //move root to the middle (the root should not be moved by the forceLayout;
        // anyway, this is just to be sure the node is really in the middle)
        this.root.visualData.x = this.root.getRadius();
        this.root.visualData.y = this.root.getRadius();
        allNodes.forEach(node => node.originalNode.visualData.moveToIntermediatePosition());
        this.dependencies.moveAllToTheirPositions();
      };

      simulation.nodes(allNodes).on('tick', ticked);
      simulation.force('link').links(allLinks);

      const allCollisionSimulations = this.root.getSelfAndDescendants().filter(node => !node.isCurrentlyLeaf()).map(node => {
        const collisionSimulation = d3.forceSimulation()
          .alphaDecay(0.005) //.alphaDecay(0.05)
          .force('collide', d3.forceCollide().radius(n => n.r + visualizationStyles.getCirclePadding()).iterations(2))
          .stop();
        collisionSimulation.nodes(node.getCurrentChildren().map(n => n.getAbsoluteNode())).on('tick', ticked);
        return collisionSimulation;
      });


      /**
       * running the simulations synchronized is better than asynchron (using promises):
       * it is faster and achieves better results (as one would assume)
       */
      let k;
      for (let i = 0, n = Math.ceil(Math.log(simulation.alphaMin()) / Math.log(1 - simulation.alphaDecay())); i < n; ++i) {
        simulation.tick();
        //TODO: check whether the condition for the collision-simulations is fullfilled (just to be sure)

        allCollisionSimulations.forEach(s => s.tick());
        ticked();
        k= i;
      }

      //run the remaining simulations of collision --> no, they are run again!!
      for (let j = k, n = Math.ceil(Math.log(allCollisionSimulations[0].alphaMin()) / Math.log(1 - allCollisionSimulations[0].alphaDecay())); j < n; ++j) {
        allCollisionSimulations.forEach(s => s.tick());
        console.log('sheesh');
        ticked();
      }

      updateOnEnd();
    }

    foldAllNodes() {
      this.root.callOnEveryDescendantThenSelf(node => node.foldIfInnerNode());
      this.root.relayout();
    }

    filterNodesByNameContaining(filterString) {
      this.root.filterByName(filterString, false);
      this.dependencies.setNodeFilters(this.root.getFilters());
      this.root.relayout();
    }

    filterNodesByNameNotContaining(filterString) {
      this.root.filterByName(filterString, true);
      this.dependencies.setNodeFilters(this.root.getFilters());
      this.root.relayout();
    }

    filterNodesByType(filter) {
      this.root.filterByType(filter.showInterfaces, filter.showClasses);
      this.dependencies.setNodeFilters(this.root.getFilters());
      this.root.relayout();
    }

    filterDependenciesByType(typeFilterConfig) {
      this.dependencies.filterByType(typeFilterConfig);
    }

    refresh() {
      this.root.relayout();
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

      const appContext = require('./app-context').newInstance();
      const visualizationStyles = appContext.getVisualizationStyles();
      const Node = appContext.getNode(); // FIXME: Correct dependency tree
      const Dependencies = appContext.getDependencies(); // FIXME: Correct dependency tree
      const graphView = appContext.getGraphView();

      const Graph = init(Node, Dependencies, graphView, visualizationStyles).Graph;
      const graph = new Graph(jsonroot, d3.select('#visualization').node());
      //graph.foldAllNodes();

      d3.timeout(() => graph.startSimulation());

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