'use strict';

const init = (jsonToRoot, jsonToDependencies, View) => {
  const Graph = class {
    constructor(root, dependencies) {
      this.root = root;
      this.dependencies = dependencies;
      this.root.setOnDrag(node => this.dependencies.updateOnNodeDragged(node));
      this.root.setOnFold(node => this.dependencies.updateOnNodeFolded(node.getFullName(), node.isFolded()));
      this.root.setOnFiltersChanged(() => this.dependencies.setNodeFilters(this.root.getFilters()));
      this.updatePromise = Promise.resolve();
    }

    initView(svg, initializeDetailedDeps) {
      this._view = new View(svg, this.root.visualData.r);

      this.root.initView(this._view.gTree, () => this._view.renderWithTransition(this.root.visualData.r));

      this.dependencies.initViews(this._view.gEdges, initializeDetailedDeps);
      this.dependencies.refreshViews();
    }

    getVisibleNodes() {
      return this.root.getSelfAndDescendants();
    }

    getVisibleDependencies() {
      return this.dependencies.getVisible();
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
      this.updatePromise = this.updatePromise.then(() => this.dependencies.filterByType(typeFilterConfig));
    }

    refresh() {
      this.updatePromise = this.updatePromise.then(() => {
        const prom = this.root.relayout();
        return Promise.all([prom, this.dependencies.updateViews()]);
      });
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
  const DETAILED_DEPENDENCIES_HIDE_DURATION = 200;
  const DETAILED_DEPENDENCIES_APPEAR_DURATION = 300;

  const d3 = require('d3');
  const isFixed = new Map();

  const svg = d3.select('#visualization');
  let gAllDetailedDeps;

  const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
  const calculateTextWidth = require('./text-width-calculator');
  const appContext = require('./app-context').newInstance();
  const jsonToRoot = appContext.getJsonToRoot(); // FIXME: Correct dependency tree
  const jsonToDependencies = appContext.getJsonToDependencies(); // FIXME: Correct dependency tree
  const graphView = appContext.getGraphView();

  let graph;

  function initializeDetailedDeps(hoverAreas) {
    const shouldBeHidden = new Map();

    const hideDetailedDeps = gDetailedDeps => {
      if (!gDetailedDeps.empty() && !isFixed.get(gDetailedDeps.attr('id'))) {
        gDetailedDeps.select('.frame').style('visibility', 'hidden');
        gDetailedDeps.select('.hoverArea').style('pointer-events', 'none');
        gDetailedDeps.select('text').style('visibility', 'hidden');
      }
    };

    const showDetailedDeps = e => {
      shouldBeHidden.set(`${e.from}-${e.to}`, false);
      const gDetailedDeps = gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`);
      gDetailedDeps.select('.frame').style('visibility', 'visible');
      gDetailedDeps.select('.hoverArea').style('pointer-events', 'all');
      gDetailedDeps.select('text').style('visibility', 'visible');
    };

    const createDetailedDepsIfNecessary = e => {
      if (gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`).empty()) {
        const gDetailedDeps = gAllDetailedDeps.append('g').attr('id', `${e.from}-${e.to}`);
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
                isFixed.set(`${e.from}-${e.to}`, false);
                hideDetailedDeps(gDetailedDeps);
                d3.select(this).remove();
              });
            isFixed.set(`${e.from}-${e.to}`, true);
          }
        };

        gDetailedDeps.append('rect').attr('class', 'hoverArea')
          .on('mouseover', () => showDetailedDeps(e))
          .on('mouseout', () => hideDetailedDeps(gDetailedDeps))
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

    hoverAreas
      .on('mouseover', function (e) {
        if (!isFixed.get(`${e.from}-${e.to}`)) {
          shouldBeHidden.set(`${e.from}-${e.to}`, false);
          const coordinates = d3.mouse(svg.node());
          setTimeout(() => {
            if (!shouldBeHidden.get(`${e.from}-${e.to}`)) {
              gAllDetailedDeps.selectAll('g').each(function () {
                hideDetailedDeps(d3.select(this));
              });
              createDetailedDepsIfNecessary(e);
              updateDetailedDeps(e, coordinates);
              showDetailedDeps(e);
            }
          }, DETAILED_DEPENDENCIES_APPEAR_DURATION);
        }
      });

    hoverAreas
      .on('mouseout', e => {
        shouldBeHidden.set(`${e.from}-${e.to}`, true);
        setTimeout(() => {
          if (shouldBeHidden.get(`${e.from}-${e.to}`)) {
            hideDetailedDeps(gAllDetailedDeps.select(`g[id='${e.from}-${e.to}']`));
          }
        }, DETAILED_DEPENDENCIES_HIDE_DURATION);
      });
  }

  return new Promise((resolve, reject) => {
    d3.json('80/classes.json', function (error, jsonroot) {
      if (error) {
        return reject(error);
      }

      const jsonToGraph = init(jsonToRoot, jsonToDependencies, graphView).jsonToGraph;
      graph = jsonToGraph(jsonroot);
      graph.initView(svg.node(), initializeDetailedDeps);
      gAllDetailedDeps = svg.append('g');

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