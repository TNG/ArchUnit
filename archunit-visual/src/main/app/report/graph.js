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
      this.root._callOnSelfThenEveryDescendant(node => node.callbackOnFold = () => this.startSimulation());
    }

    _startSimulation() {
      //TODO: do not recreate forceSimulation all time, but always recreate allNodes and allLinks (as they might change)

      this.root.createAbsoluteNodes();
      //FIXME: if a node has only one child, drag it in the middle
      const allLinks = this.dependencies.getSimpleDependencies();
      //allLinks.forEach(link => console.log(link.source + "->" + link.target));

      const allNodesSoFar = new Map();
      let currentNodes = new Map();
      currentNodes.set(this.root.getFullName(), this.root);

      while (currentNodes.size > 0) {

        //console.log('new round');

        const newNodesArray = [].concat.apply([], Array.from(currentNodes.values()).map(node => node.getCurrentChildren()));
        const newNodes = new Map();
        //add to newNodes and allNodesSoFar
        newNodesArray.forEach(node => newNodes.set(node.getFullName(), node));
        newNodesArray.forEach(node => allNodesSoFar.set(node.getFullName(), node));
        //take only links having at least one new end node and having both end nodes in allNodesSoFar
        const currentLinks = allLinks.filter(link => (newNodes.has(link.source) || newNodes.has(link.target)) && (allNodesSoFar.has(link.source) && allNodesSoFar.has(link.target)));

        if (newNodes.size === 0) {
          break;
        }

        const simulation = d3.forceSimulation()
          .alphaDecay(0.06) //.alphaDecay(0.05)
          .force('link', d3.forceLink()
            .id(n => n.fullName)
            .distance(d => d.source.r + d.target.r + 2 * visualizationStyles.getCirclePadding())
            .strength(link => 3 / Math.min(countLinksOfNode(currentLinks, link.source), countLinksOfNode(currentLinks, link.target)))
            .iterations(2))
          .stop();

        const ticked = () => {
          //update nodes and deps and re-update allNodes
          Array.from(newNodes.values()).forEach(node => node.visualData.setAbsoluteIntermediatePosition(node.getAbsoluteNode().x, node.getAbsoluteNode().y, node.getParent()));
          Array.from(newNodes.values()).forEach(node => node.updateAbsoluteNode());
        };

        const updateOnEnd = () => {
          //move root to the middle (the root should not be moved by the forceLayout;
          // anyway, this is just to be sure the node is really in the middle)
          this.root.visualData.x = this.root.getRadius();
          this.root.visualData.y = this.root.getRadius();

          Array.from(newNodes.values()).forEach(node => {
            node.getAbsoluteNode().fx = node.getAbsoluteNode().x;
            node.getAbsoluteNode().fy = node.getAbsoluteNode().y;
          });
        };

        simulation.nodes(Array.from(allNodesSoFar.values()).map(node => node.getAbsoluteNode()));
        simulation.force('link').links(currentLinks);

        const allCollisionSimulations = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf()).map(node => {
          const collisionSimulation = d3.forceSimulation()
            .alphaDecay(0.03)
            .force('collide', d3.forceCollide().radius(n => n.r + visualizationStyles.getCirclePadding()).iterations(1));
          collisionSimulation.nodes(node.getCurrentChildren().map(n => n.getAbsoluteNode()))
            .stop();
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
          k = i;
        }
        //run the remaining simulations of collision
        for (let j = k, n = Math.ceil(Math.log(allCollisionSimulations[0].alphaMin()) / Math.log(1 - allCollisionSimulations[0].alphaDecay())); j < n; ++j) {
          allCollisionSimulations.forEach(s => s.tick());
          ticked();
        }

        updateOnEnd();

        currentNodes = newNodes;
      }

      const updateOnEnd = () => {
        //move root to the middle (the root should not be moved by the forceLayout;
        // anyway, this is just to be sure the node is really in the middle)
        this.root.visualData.x = this.root.getRadius();
        this.root.visualData.y = this.root.getRadius();

        //move only children to middle of parent
        Array.from(allNodesSoFar.values()).forEach(node => {
          if (node.getParent() && node.getParent().getCurrentChildren().length === 1) {
            node.visualData.x = 0;
            node.visualData.y = 0;
          }
          node.visualData.moveToIntermediatePosition();
        });
        this.dependencies.moveAllToTheirPositions();
      };
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
      graph.foldAllNodes();

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