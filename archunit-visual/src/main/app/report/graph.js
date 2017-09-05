'use strict';

const init = (jsonToRoot, visualizer) => {
  const Graph = class {
    constructor(root) {
      this.root = root;
    }

    getVisibleNodes() {
      return this.root.getSelfAndDescendants();
    }

    getVisibleDependencies() {
      return this.root.getVisibleDependencies();
    }

    foldAllNodes() {
      this.root.callOnEveryDescendantThenSelf(node => {
        if (!node.isRoot()) {
          node.fold();
        }
      });
      this.refresh();
    }

    getDetailedDependenciesOf(from, to) {
      return this.root.getDetailedDependenciesOf(from, to);
    }

    filterNodesByNameContaining(filterString) {
      this.root.filterByName(filterString, false); // FIXME: Filtering belongs to Graph, not to Node (node._filters only gets filled on root anyway)
      this.refresh();
    }

    filterNodesByNameNotContaining(filterString) {
      this.root.filterByName(filterString, true); // FIXME: Filtering belongs to Graph, not to Node (node._filters only gets filled on root anyway)
      this.refresh();
    }

    filterNodesByType(filter) {
      this.root.filterByType(filter.showInterfaces, filter.showClasses); // FIXME: Filtering belongs to Graph, not to Node (node._filters only gets filled on root anyway)
      this.refresh();
    }

    resetFilterNodesByType() {
      this.root.resetFilterByType();
    }

    filterDependenciesByType(typeFilterConfig) {
      this.root.filterDependenciesByType(typeFilterConfig);
    }

    resetFilterDependenciesByType() {
      this.root.resetFilterDependenciesByType();
    }

    refresh() {
      this.root.relayout();
    }
  };

  return {
    jsonToGraph: jsonRoot => {
      const root = jsonToRoot(jsonRoot);
      const graph = new Graph(root);
      visualizer.visualizeGraph(graph);
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
  /*
   * the width of the click area of the lines
   */
  const clickAreaWidth = 10;

  const DETAILED_DEPENDENCIES_HIDE_DURATION = 200;
  const DETAILED_DEPENDENCIES_APPEAR_DURATION = 300;
  const TRANSITION_DURATION = 300;

  const d3 = require('d3');
  const isFixed = new Map();

  const svg = d3.select('#visualization'),
    translater = svg.select('#translater'),
    gTree = translater.append('g'),
    gEdges = translater.append('g'),
    gAllDetailedDeps = svg.append('g');

  const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
  const calculateTextWidth = require('./text-width-calculator');
  const appContext = require('./app-context').newInstance();
  const jsonToRoot = appContext.getJsonToRoot(); // FIXME: Correct dependency tree
  const visualizer = require('./graph-visualizer').newInstance(require('./dependencies-visualizer'));

  let graph;

  function adaptSVGSizeAndPosition() {
    adaptSVGSize();
    adaptSVGPosition(translater);
  }

  function adaptSVGSize() {
    svg.attr('width', Math.max(parseInt(2 * graph.root.visualData.r + 4),
      d3.select('#container').node().getBoundingClientRect().width));
    svg.attr('height', Math.max(parseInt(2 * graph.root.visualData.r + 4),
      d3.select('#container').node().getBoundingClientRect().height));
  }

  function adaptSVGPosition(svgSelection) {
    svgSelection.attr('transform',
      `translate(${parseInt(svg.attr('width')) / 2 - graph.root.visualData.r}, ${parseInt(svg.attr('height')) / 2 - graph.root.visualData.r})`);
  }

  function adaptSVGSizeAndPositionWithTransition(transition) {
    adaptSVGSize();
    adaptSVGPosition(transition);
  }

  function initializeGraph() {
    initializeTree();
    initializeDeps();
  }

  function setVisible(selection, value) {
    selection.each(x => x.visualData.visible = value);
  }

  function initializeTree() {
    const onMoved = node => {
      visualizer.drag(graph, node);
      updateVisualizationAfterDragging(node);
    };
    graph.root.initView(gTree.node(), updateVisualization, onMoved);
  }

  function initializeDeps() {
    const edges = gEdges.selectAll().data(graph.getVisibleDependencies()).enter();
    createNewEdges(edges);
  }

  function createNewEdges(selection) {
    const newEdges = selection.append('g');

    setVisible(newEdges, true);

    newEdges
      .append('line')
      .attr('id', 'dep')
      .attr('class', e => e.getClass())
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y);

    const hoverAreas = newEdges
      .filter(e => e.hasDetailedDescription())
      .append('line')
      .attr('id', 'area')
      .attr('class', 'area')
      .style('visibility', 'hidden')
      .style('pointer-events', 'all')
      .style('stroke-width', clickAreaWidth)
      .style('stroke', 'yellow')
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y);

    initializeDetailedDeps(hoverAreas);
  }

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

  function updateVisualizationAfterDragging(node) {
    gTree.selectAll('g').filter(d => d.isChildOf(node)).attr('transform', d => `translate(${d.visualData.x}, ${d.visualData.y})`);
    updateLinePositionWithoutAnimation(gEdges.selectAll('g').filter(d => d.from.startsWith(node.getFullName())
    || d.to.startsWith(node.getFullName())), () => {
    });
  }

  let updatePromise = Promise.all([]);

  function updateVisualization() {
    updatePromise = updatePromise.then(() => {
      visualizer.update(graph);
      updateNodes();
      return Promise.all([updateEdgesWithAnimation()]);
    });
  }

  function updateNodes() {
    adaptSVGSizeAndPositionWithTransition(translater.transition().duration(TRANSITION_DURATION));
    return graph.root.updateView(TRANSITION_DURATION);
  }

  function updateEdges(edges) {
    showEdges(edges);
    createNewEdges(edges.enter());
  }

  function updateEdgesVisibility(edges) {
    hideEdges(edges.exit());
    setVisible(edges.filter(function () {
      return d3.select(this).style('visibility') === 'visible';
    }), true);
  }

  function updateEdgesWithoutAnimation() {
    const edges = gEdges.selectAll('g').data(graph.getVisibleDependencies(), e => e.from + "->" + e.to);
    updateEdgesVisibility(edges);
    updateLinePositionWithoutAnimation(edges, updateEdges);
  }

  function updateEdgesWithAnimation() {
    const edges = gEdges.selectAll('g').data(graph.getVisibleDependencies(), e => e.from + "->" + e.to);
    updateEdgesVisibility(edges);
    return updateLinePositionWithAnimation(edges);
  }

  function hideEdges(edges) {
    setVisible(edges, false);
    edges.style('visibility', 'hidden');
    edges.select('#area').style('pointer-events', 'none');
  }

  function showEdges(edges) {
    edges.style('visibility', 'visible');
    edges.select('line').attr('class', e => e.getClass());
    edges.select('#area').style('pointer-events', e => e.hasDetailedDescription() ? 'all' : 'none');
  }

  function runTransition(transition, transitionRunner) {
    return new Promise(resolve => {
      if (transition.empty()) {
        return resolve();
      }

      let wasTriggered = false;
      transitionRunner(transition).on('end', () => {
        if (!wasTriggered) {
          wasTriggered = true;
          resolve();
        }
      });
    });
  }

  function updateLinePositionWithAnimation(edges) {
    const dependencyTransition = edges.select('#dep').transition().duration(TRANSITION_DURATION);

    const adaptStartAndEnd = selection => selection
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y);

    return runTransition(dependencyTransition, adaptStartAndEnd).then(() => {
      updateEdges(edges);
      updateClickAreaPosition(edges);
    });
  }

  function updateLinePositionWithoutAnimation(edges, callback) {
    edges.select('#dep')
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y);
    callback(edges);
    updateClickAreaPosition(edges);
  }

  function updateClickAreaPosition(edges) {
    edges
      .select('#area')
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y);
  }

  return new Promise((resolve, reject) => {
    d3.json('classes.json', function (error, jsonroot) {
      if (error) {
        return reject(error);
      }

      const jsonToGraph = init(jsonToRoot, visualizer).jsonToGraph;
      graph = jsonToGraph(jsonroot);
      adaptSVGSizeAndPosition();
      initializeGraph();
      graph.foldAllNodes();
      updateVisualization();


      // FIXME: Only temporary, we need to decompose this further and separate d3 into something like 'renderer'
      graph.render = adaptSVGSizeAndPosition;
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
              updateVisualization();
            })
          .onNodeTypeFilterChanged(
            filter => {
              graph.filterNodesByType(filter);
              updateVisualization();
            })
          .onDependencyFilterChanged(
            filter => {
              graph.filterDependenciesByType(filter);
              updateEdgesWithoutAnimation();
            })
          .onNodeNameFilterChanged((filterString, exclude) => {
            if (exclude) {
              graph.filterNodesByNameNotContaining(filterString);
            } else {
              graph.filterNodesByNameContaining(filterString);
            }
            updateVisualization();
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