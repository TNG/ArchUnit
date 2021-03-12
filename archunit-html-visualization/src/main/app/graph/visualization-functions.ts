'use strict';

import {Simulation} from "d3-force";

const d3 = require('d3');
import {PackCircle} from 'd3-hierarchy';
import {InnerNode, Node} from './node/node';
import {Circle} from "./infrastructure/shapes";

const CIRCLE_TEXT_PADDING = 5;
const MIN_NODE_RADIUS = 40;

interface ForceSetup {
  name: string;
  forceFunction: () => any
}

const calculateDefaultRadius = (node: Node): number => {
  const isOriginalLeaf = (node: Node) => node.getOriginalChildren().length === 0;
  const radius = node.getNameWidth() / 2 + CIRCLE_TEXT_PADDING;
  return isOriginalLeaf(node) ? radius : Math.max(radius, MIN_NODE_RADIUS);
};

const calculateDefaultRadiusForNodeWithOneChild = (node: Node, childRadius: number, nodeFontSize: number): number => {
  const halfTextWidth = node.getNameWidth() / 2 + CIRCLE_TEXT_PADDING;
  childRadius = childRadius + nodeFontSize;
  const radius = Math.sqrt(halfTextWidth * halfTextWidth + childRadius * childRadius);
  return Math.max(radius, MIN_NODE_RADIUS);
};

const packCirclesAndReturnEnclosingCircle = (circles: PackCircle[], padding: number = 0): PackCircle => {
  circles.forEach(c => c.r += padding);
  d3.packSiblings(circles);
  const enclosingCircle = d3.packEnclose(circles);
  circles.forEach(c => c.r -= padding);
  return enclosingCircle;
};


const createForceCollideSimulation = (padding: number, nodes: Node[]): Simulation<any, undefined> => {
  return createSimulation(0.03, nodes, {
    name: 'collide',
    forceFunction: d3.forceCollide().radius((n: Circle) => n.r + padding).strength(1)
  });
};

const createSimulation = (alphaDecay: number, nodes: Node[], forceSetup: ForceSetup): Simulation<any, undefined> => {
  return d3.forceSimulation(nodes)
    .alphaDecay(alphaDecay)
    .force(forceSetup.name, forceSetup.forceFunction)
    .stop();
};

const runSimulations = (simulations: Simulation<any, undefined>[], mainSimulation: Simulation<any, undefined>, iterationStart: number, onTick: CallableFunction): number => {
  let i = iterationStart;
  for (let n = Math.ceil(Math.log(mainSimulation.alphaMin()) / Math.log(1 - mainSimulation.alphaDecay())); i < n; ++i) {
    simulations.forEach(s => s.tick());
    onTick();
  }
  return i;
};
// const newInstance = () => {
//
// const createForceLinkSimulation = (padding, nodes, links) => {
//   const count = nodeId => links.filter(link => link.source === nodeId || link.target === nodeId).length;
//   return createSimulation(0.06, nodes, {
//     name: 'link',
//     forceFunction: d3.forceLink(links)
//       .id(n => n.id)
//       .distance(d => d.source.r + d.target.r + 2 * padding)
//
//       // 3: magic number, we don't know exactly how the scale affects the layout ('strength' of attraction)
//       // removing this line causes an infinite-loop in d3
//       .strength(link => 3 / Math.min(count(link.source), count(link.target)))
//   });
// };
//
// /**
//  *
//  * @param alphaDecay controls the granularity of the simulation and hence quality vs performance: higher alphaDecay <=> higher performance
//  * @param nodes the nodes to apply the simulation to
//  * @param forceSetup object containing force name and function, e.g. {name: 'collide', forceFunction: () => ...}
//  */
//
// /**
//  * runs the given simulations, of which mainSimulation is the simulation defining how many steps of the simulations
//  * are executed
//  * @param simulations list with all simulations
//  * @param mainSimulation simulation defining how many steps of the simulations are executed; mainSimulation has
//  * to be in simulations
//  * @param iterationStart number that defines at which number the simulations are started
//  * @param onTick function that is invoked at every tick
//  */
//
// return {
//   /**
//    * Creates a circle packing for the supplied circles (circles are represented as {x: $x, y: $y, r: $radius}).
//    * Only the radius of the supplied circles is relevant,
//    * x- and y-coordinates will be calculated and and overridden.
//    * Returns a circle (represented as {x: $x, y: $y, r: $radius}) enclosing the created circle packing.
//    * @param circles An array representing circles
//    * @param padding The padding between the circles
//    */
//   packCirclesAndReturnEnclosingCircle,
//
//   calculateDefaultRadius,
//
//   calculateDefaultRadiusForNodeWithOneChild,
//
//   createForceLinkSimulation,
//
//   createForceCollideSimulation,
//
//   runSimulations
// };
// };

export {calculateDefaultRadius, calculateDefaultRadiusForNodeWithOneChild, packCirclesAndReturnEnclosingCircle, createForceCollideSimulation, runSimulations};
