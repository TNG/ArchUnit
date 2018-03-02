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
    return createSimulation(0.06, nodes, {
      name: 'link',
      forceFunction: d3.forceLink(links)
        .id(n => n.id)
        .distance(d => d.source.r + d.target.r + 2 * padding)
        .strength(() => 3) // Magic number, we don't know exactly how the scale affects the layout ('strength' of attraction)
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

    createForceCollideSimulation
  };
};