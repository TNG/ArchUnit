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

  const createForceLinkSimulation = (padding, nodes, links) => {
    const countLinksOfNode = node => links.filter(d => d.source === node || d.target === node).length;

    const simulation = d3.forceSimulation(nodes)
      .alphaDecay(0.06)
      .force('link', d3.forceLink()
        .id(n => n.fullName)
        .distance(d => d.source.r + d.target.r + 2 * padding)
        .strength(link => 3 / Math.min(countLinksOfNode(link.source), countLinksOfNode(link.target)))
        .iterations(2))
      .stop();
    simulation.force('link').links(links);
    return simulation;
  };

  const createForceCollideSimulation = (padding, nodes) => {
    return d3.forceSimulation(nodes)
      .alphaDecay(0.02)
      //more iterations promise a better result (that means a higher probability that no nodes are overlapping)
      .force('collide', d3.forceCollide().radius(n => n.r + padding).iterations(3))
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

    createForceLinkSimulation,

    createForceCollideSimulation
  };
};