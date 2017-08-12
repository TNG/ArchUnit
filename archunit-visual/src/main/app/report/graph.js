'use strict';

const jsonToRoot = require('./tree.js').jsonToRoot;

const Graph = class {
  constructor(root) {
    this.root = root;
  }

  getVisibleNodes() {
    return this.root.getVisibleDescendants();
  }

  getVisibleDependencies() {
    return this.root.getVisibleDependencies();
  }

  changeFoldStateOfNode(node) {
    return !!node.changeFold();
  }

  //FIXME: fold and do not change state only
  foldAllNodes() {
    this.root.callOnEveryNode(node => {
      if (!node.isRoot()) {
        node.fold();
      }
    });
  }

  getDetailedDependenciesOf(from, to) {
    return this.root.getDetailedDependenciesOf(from, to);
  }

  filterNodesByNameContaining(filterString) {
    this.root.filterByName(filterString, false); // FIXME: Filtering belongs to Graph, not to Node (node._filters only gets filled on root anyway)
  }

  filterNodesByNameNotContaining(filterString) {
    this.root.filterByName(filterString, true); // FIXME: Filtering belongs to Graph, not to Node (node._filters only gets filled on root anyway)
  }

  filterNodesByType(filter) {
    this.root.filterByType(filter.showInterfaces, filter.showClasses);
  }

  resetFilterNodesByType() {
    this.root.resetFilterByType();
  }

  filterDependenciesByKind() {
    return this.root.filterDependenciesByKind();
  }

  resetFilterDependenciesByKind() {
    this.root.resetFilterDependenciesByKind();
  }
};

const jsonToGraph = jsonRoot => {
  const root = jsonToRoot(jsonRoot);
  const graph = new Graph(root);
  require('./graph-visualizer').newInstance().visualizeGraph(graph);
  return graph;
};

module.exports.jsonToGraph = jsonToGraph;

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
  const APPEAR_DURATION = 10;

  const d3 = require('d3');
  const isFixed = new Map();

  const svg = d3.select('#visualization'),
    translater = svg.select('#translater'),
    gTree = translater.append('g'),
    gEdges = translater.append('g'),
    gAllDetailedDeps = svg.append('g');

  const visualizationStyles = require('./visualization-styles').fromEmbeddedStyleSheet();
  const calculateTextWidth = require('./text-width-calculator');
  const visualizer = require('./graph-visualizer').newInstance();

  let graph;

  function adaptSVGSize() {
    svg.attr('width', Math.max(parseInt(2 * graph.root.visualData.r + 4),
      d3.select('#container').node().getBoundingClientRect().width));
    svg.attr('height', Math.max(parseInt(2 * graph.root.visualData.r + 4),
      d3.select('#container').node().getBoundingClientRect().height));
    translater.attr('transform',
      `translate(${parseInt(svg.attr('width')) / 2 - graph.root.visualData.r}, ${parseInt(svg.attr('height')) / 2 - graph.root.visualData.r})`);
  }

  function initializeGraph() {
    initializeTree();
    initializeDeps();
  }

  function setVisible(selection, value) {
    selection.each(x => x.visualData.visible = value);
  }

  function initializeTree() {
    const nodes =
      gTree.selectAll()
        .data(graph.getVisibleNodes())
        .enter()
        .append('g')
        .attr('class', d => d.getClass())
        .attr('transform', d => `translate(${d.visualData.x}, ${d.visualData.y})`);

    setVisible(nodes, true);

    const drag = d3.drag().on('drag', d => {
      visualizer.drag(graph, d, d3.event.dx, d3.event.dy, false);
      updateVisualizationAfterDragging(d);
    });

    nodes
      .filter(d => !d.isRoot() && !d.isLeaf())
      .on('click', d => {
        if (graph.changeFoldStateOfNode(d)) {
          updateVisualization();
        }
      });

    nodes
      .filter(d => !d.isRoot())
      .call(drag);

    nodes
      .filter(d => !d.isRoot())
      .append('circle')
      .attr('r', d => d.visualData.r);

    nodes
      .append('text')
      .text(node => node.getName());

    nodes
      .append('title')
      .text(node => node.getName());

    positionTextOfAllNodes(nodes);
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
      .filter(e => e.description.hasDetailedDescription())
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

  function positionTextOfAllNodes(selection) {
    return selection.select('text').attr('dy', getDy);
  }

  function getDy(d) {
    if (d.isRoot()) {
      const fontSize = visualizationStyles.getNodeFontSize();
      return -d.visualData.r + fontSize;
    }
    else if (d.isCurrentlyLeaf()) {
      return 0;
    }
    else {
      const textDom = d3.select(this).node();
      const r = d.visualData.r;
      return computeTextShift(r, textDom.getBBox().width);
    }
  }

  /**
   * computes the greatest possible shift in y-direction of the text, so that the text is still in the circle
   **/
  function computeTextShift(r, width) {
    let shift = Math.sqrt(Math.pow(r, 2) - Math.pow(width / 2, 2));
    const fontSize = visualizationStyles.getNodeFontSize();
    shift = -shift + fontSize;
    return shift;
  }

  function updateVisualizationAfterDragging(node) {
    gTree.selectAll('g').filter(d => d.isChildOf(node)).attr('transform', d => `translate(${d.visualData.x}, ${d.visualData.y})`);
    updateLinePositionWithoutAnimation(gEdges.selectAll('g').filter(d => d.from.startsWith(node.getFullName())
    || d.to.startsWith(node.getFullName())), () => {
    });
  }

  let visualizationIsUpdatedAtTheMoment = false;
  let numberOfWaitingInvokes = 0;

  function updateVisualization() {
    if (visualizationIsUpdatedAtTheMoment) {
      numberOfWaitingInvokes++;
    }
    else {
      visualizationIsUpdatedAtTheMoment = true;
      visualizer.update(graph);
      let numberOfAnimations = 3;
      const countDownLatchOnAnimationEnd = () => {
        numberOfAnimations--;
        if (!numberOfAnimations) {
          visualizationIsUpdatedAtTheMoment = false;
          if (numberOfWaitingInvokes > 0) {
            numberOfWaitingInvokes--;
            updateVisualization();
          }
        }
      };
      adaptSVGSize();
      updateNodes(countDownLatchOnAnimationEnd);
      updateEdgesWithAnimation(countDownLatchOnAnimationEnd);
    }
  }

  function setPositionAndRadius(selection) {
    selection.attr('transform', d => `translate(${d.visualData.x}, ${d.visualData.y})`);
    selection.select('circle').attr('r', d => d.visualData.r);
  }

  function updateNodes(onAnimationEnd) {
    const nodes = gTree.selectAll('g').data(graph.getVisibleNodes(), d => d.getFullName());
    nodes.exit().style('visibility', 'hidden');
    setVisible(nodes.exit(), false);

    const transition = nodes.transition().duration(TRANSITION_DURATION);

    setPositionAndRadius(transition.filter(d => d.visualData.visible));
    setPositionAndRadius(nodes.filter(d => !d.visualData.visible));

    const appearTransition = transition.transition().duration(APPEAR_DURATION);
    runTransitionWithEndCallback(appearTransition, t => t.style('visibility', 'visible'), onAnimationEnd);
    runTransitionWithEndCallback(transition, t => positionTextOfAllNodes(t), onAnimationEnd);

    setVisible(nodes, true);
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

  function updateEdgesWithAnimation(onAnimationEnd) {
    const edges = gEdges.selectAll('g').data(graph.getVisibleDependencies(), e => e.from + "->" + e.to);
    updateEdgesVisibility(edges);
    updateLinePositionWithAnimation(edges, onAnimationEnd);
  }

  function hideEdges(edges) {
    setVisible(edges, false);
    edges.style('visibility', 'hidden');
    edges.select('#area').style('pointer-events', 'none');
  }

  function showEdges(edges) {
    edges.style('visibility', 'visible');
    edges.select('line').attr('class', e => e.getClass());
    edges.select('#area').style('pointer-events', e => e.description.hasDetailedDescription() ? 'all' : 'none');
  }

  function runTransitionWithEndCallback(transition, transitionRunner, callback) {
    if (transition.empty()) {
      callback();
    }
    else {
      let n = 0;
      transition.each(() => n++);
      transitionRunner(transition).on('end', () => {
        n--;
        if (!n) {
          callback();
        }
      });
    }
  }

  function updateLinePositionWithAnimation(edges, onAnimationEnd) {
    const deps = edges.select('#dep').transition().duration(TRANSITION_DURATION);
    runTransitionWithEndCallback(deps, selection => selection
      .attr('x1', e => e.visualData.startPoint.x)
      .attr('y1', e => e.visualData.startPoint.y)
      .attr('x2', e => e.visualData.endPoint.x)
      .attr('y2', e => e.visualData.endPoint.y), () => {
      updateEdges(edges);
      updateClickAreaPosition(edges);
      onAnimationEnd();
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

      graph = jsonToGraph(jsonroot);
      adaptSVGSize();
      initializeGraph();
      graph.foldAllNodes();
      updateVisualization();


      // FIXME: Only temporary, we need to decompose this further and separate d3 into something like 'renderer'
      graph.render = adaptSVGSize;
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
              updateVisualization();
            })
          .onNodeFilterChanged(
            filter => {
              graph.filterNodesByType(filter);
              updateVisualization();
            })
          .onDependencyFilterChanged(
            filter => {
              graph.filterDependenciesByKind()
                .showImplementing(filter.showImplementing)
                .showExtending(filter.showExtending)
                .showConstructorCall(filter.showConstructorCall)
                .showMethodCall(filter.showMethodCall)
                .showFieldAccess(filter.showFieldAccess)
                .showAnonymousImplementing(filter.showAnonymousImplementation)
                .showDepsBetweenChildAndParent(filter.showBetweenClassAndItsInnerClasses);

              updateEdgesWithoutAnimation();
            })
          .onFilterChanged((filterString, exclude) => {
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