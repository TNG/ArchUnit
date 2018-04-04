'use strict';

const d3 = require('d3');

module.exports.newInstance = calculateTextWidth => {
  const CIRCLE_TEXT_PADDING = 5;
  const MIN_NODE_RADIUS = 40;

  const packCirclesAndReturnEnclosingCircle = (circles, padding = 0) => {
    circles.forEach(c => c.r += padding);
    d3.packSiblings(circles);
    const enclosingCircle = d3.packEnclose(circles);
    circles.forEach(c => c.r -= padding);
    return enclosingCircle;
  };

  const calculateDefaultRadius = node => {
    const isOriginalLeaf = node => node.getOriginalChildren().length === 0;
    const radius = calculateTextWidth(node.getName()) / 2 + CIRCLE_TEXT_PADDING;
    return isOriginalLeaf(node) ? radius : Math.max(radius, MIN_NODE_RADIUS);
  };

  const calculateDefaultRadiusForNodeWithOneChild = (node, childRadius, nodeFontSize) => {
    const halfTextWidth = calculateTextWidth(node.getName()) / 2 + CIRCLE_TEXT_PADDING;
    childRadius = childRadius + nodeFontSize;
    const radius = Math.sqrt(halfTextWidth * halfTextWidth + childRadius * childRadius);
    return Math.max(radius, MIN_NODE_RADIUS);
  };

  const createForceLinkSimulation = (padding, nodes, links) => {
    const count = nodeId => links.filter(link => link.source === nodeId || link.target === nodeId).length;
    return createSimulation(0.06, nodes, {
      name: 'link',
      forceFunction: d3.forceLink(links)
        .id(n => n.id)
        .distance(d => d.source.r + d.target.r + 2 * padding)

        // 3: magic number, we don't know exactly how the scale affects the layout ('strength' of attraction)
        // removing this line causes an infinite-loop in d3
        .strength(link => 3 / Math.min(count(link.source), count(link.target)))

        .iterations(2)
    });
  };

  const createForceCollideSimulation = (padding, nodes) => {
    return createSimulation(0.02, nodes, {
      name: 'collide',
      forceFunction: d3.forceCollide().radius(n => n.r + padding).iterations(3)
    });
  };

  /**
   *
   * @param alphaDecay controls the granularity of the simulation and hence quality vs performance: higher alphaDecay <=> higher performance
   * @param nodes the nodes to apply the simulation to
   * @param forceSetup object containing force name and function, e.g. {name: 'collide', forceFunction: () => ...}
   */
  const createSimulation = (alphaDecay, nodes, forceSetup) => {
    return d3.forceSimulation(nodes)
      .alphaDecay(alphaDecay)
      .force(forceSetup.name, forceSetup.forceFunction)
      .stop();
  };

  /**
   * runs the given simulations, of which mainSimulation is the simulation defining how many steps of the simulations
   * are executed
   * @param simulations list with all simulations
   * @param mainSimulation simulation defining how many steps of the simulations are executed; mainSimulation has
   * to be in simulations
   * @param iterationStart number that defines at which number the simulations are started
   * @param onTick function that is invoked at every tick
   */
  const runSimulations = (simulations, mainSimulation, iterationStart, onTick) => {
    let i = iterationStart;
    for (let n = Math.ceil(Math.log(mainSimulation.alphaMin()) / Math.log(1 - mainSimulation.alphaDecay())); i < n; ++i) {
      simulations.forEach(s => s.tick());
      onTick();
    }
    return i;
  };

  return {
    calculateTextWidth,

    /**
     * Creates a circle packing for the supplied circles (circles are represented as {x: $x, y: $y, r: $radius}).
     * Only the radius of the supplied circles is relevant,
     * x- and y-coordinates will be calculated and and overridden.
     * Returns a circle (represented as {x: $x, y: $y, r: $radius}) enclosing the created circle packing.
     * @param circles An array representing circles
     * @param padding The padding between the circles
     */
    packCirclesAndReturnEnclosingCircle,

    calculateDefaultRadius,

    calculateDefaultRadiusForNodeWithOneChild,

    createForceLinkSimulation,

    createForceCollideSimulation,

    runSimulations
  };
};