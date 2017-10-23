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

    initView(svg, createDetailedDepsSvg, create) {
      this._view = new View(svg, this.root.visualData.r);

      this.root.initView(this._view.gTree, () => this._view.renderWithTransition(this.root.getRadius()));

      //FIXME: statt svgElementForDetailed lieber an g der jeweiligen Dependency dranhängen und vllt auch detailed
      // view in dependency-view rein
      this.dependencies.initViews(this._view.gEdges, createDetailedDepsSvg, create);
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
  /*
   * padding between a line and its title
   */
  const TEXT_PADDING = 5;

  const d3 = require('d3');

  const svg = d3.select('#visualization');
  let gAllDetailedDeps;

  const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
  const calculateTextWidth = require('./text-width-calculator');
  const appContext = require('./app-context').newInstance();
  const jsonToRoot = appContext.getJsonToRoot(); // FIXME: Correct dependency tree
  const jsonToDependencies = appContext.getJsonToDependencies(); // FIXME: Correct dependency tree
  const graphView = appContext.getGraphView();

  let graph;

  const showDetailedDeps = e => {
    e._detailedView._shouldBeHidden = false;
    const gDetailedDeps = gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`);
    gDetailedDeps.style('visibility', 'visible');
    gDetailedDeps.select('.hoverArea').style('pointer-events', 'all');
  };

  const createDetailedDepsIfNecessary = e => {
    if (gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`).empty()) {
      const gDetailedDeps = gAllDetailedDeps.append('g').attr('id', `${e.from}-${e.to}`);

      //FIXME: use d3 data (of course with an array of the element!)
      gDetailedDeps.node()._data = e;
      gDetailedDeps.append('rect').attr('class', 'frame');
      gDetailedDeps.append('text').attr('class', 'access');

      const fixDetailedDeps = () => {
        if (gDetailedDeps.select('.closeButton').empty()) {
          const fontSize = visualizationStyles.getDependencyTitleFontSize();
          gDetailedDeps.append('text')
            .attr('class', 'closeButton')
            .text('x')
            .attr('dx', gDetailedDeps.select('.hoverArea').attr('width') / 2 - fontSize / 2)
            .attr('dy', fontSize)
            .on('click', function () {
              e._detailedView._isFixed = false;
              e._detailedView.hideIfNotFixed();
              d3.select(this).remove();
            });
          e._detailedView._isFixed = true;
        }
      };

      gDetailedDeps.append('rect').attr('class', 'hoverArea')
        .on('mouseover', () => showDetailedDeps(e))
        .on('mouseout', () => e._detailedView.fadeOut())
        .on('click', () => {
          fixDetailedDeps();
        });

      const drag = d3.drag().on('drag', () => {
        fixDetailedDeps();
        gDetailedDeps.attr('transform', () => {
          const transform = gDetailedDeps.attr('transform');
          const translateBefore = transform.substring(transform.indexOf("(") + 1, transform.indexOf(")")).split(",").map(s => parseInt(s));
          return `translate(${translateBefore[0] + d3.event.dx}, ${translateBefore[1] + d3.event.dy})`
        });
      });
      gDetailedDeps.call(drag);
    }
  };

  const updateDetailedDeps = (e, coordinates) => {
    const detailedDeps = graph.getDetailedDependenciesOf(e.from, e.to);
    if (detailedDeps.length > 0) {

      const gDetailedDeps = gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`);
      const maxWidth = Math.max.apply(null, detailedDeps.map(d => calculateTextWidth(d.description, 'access'))) + 2 * TEXT_PADDING + 10;

      gDetailedDeps.attr('transform', () => {
        //ensure that the rect is visible on the left side
        let x = Math.max(maxWidth / 2, coordinates[0]);
        //ensure that the rect is visible on the right side
        x = Math.min(x, svg.attr('width') - maxWidth / 2);
        return `translate(${x}, ${coordinates[1]})`;
      });

      const tspans = gDetailedDeps.select('text.access')
        .selectAll('tspan')
        .data(detailedDeps);

      const fontSize = visualizationStyles.getDependencyTitleFontSize();
      tspans.exit().remove();

      tspans.enter()
        .append('tspan');

      gDetailedDeps.select('text')
        .selectAll('tspan')
        .text(d => d.description)
        .attr('class', d => d.cssClass)
        .attr("x", -maxWidth / 2)
        .attr("dy", () => fontSize + TEXT_PADDING);

      gDetailedDeps.selectAll('rect')
        .attr('x', -maxWidth / 2 - TEXT_PADDING)
        .attr('height', detailedDeps.length * (fontSize + TEXT_PADDING) + 2 * TEXT_PADDING)
        .attr('width', maxWidth + fontSize);
    }
  };

  const create = (e, coordinates) => {
    createDetailedDepsIfNecessary(e);
    updateDetailedDeps(e, coordinates);
    showDetailedDeps(e);
  };

  return new Promise((resolve, reject) => {
    d3.json('80/classes.json', function (error, jsonroot) {
      if (error) {
        return reject(error);
      }

      const jsonToGraph = init(jsonToRoot, jsonToDependencies, graphView).jsonToGraph;
      graph = jsonToGraph(jsonroot);
      const createDetailedDepsParent = () => gAllDetailedDeps = svg.append('g');
      graph.initView(svg.node(), createDetailedDepsParent, create);

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