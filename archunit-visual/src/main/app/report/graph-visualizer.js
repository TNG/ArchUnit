'use strict';

const setTreeStyles = require("./tree-visualizer.js").setStyles;
const visualizeTree = require("./tree-visualizer.js").visualizeTree;
const dragNode = require("./tree-visualizer.js").dragNode;

const visualizeDependencies = require("./dependencies-visualizer.js").visualizeDependencies;
const refreshVisualDataOf = require("./dependencies-visualizer.js").refreshVisualDataOf;

let visualizeGraph = (graph, packSiblings, packEnclose, circpadding) => {
  visualizeTree(graph.root, packSiblings, packEnclose, circpadding);
  visualizeDependencies(graph.dependencies);
};

let drag = (graph, node, dx, dy, force) => {
  dragNode(node, dx, dy, force);
  refreshVisualDataOf(node.projectData.fullname, graph.getVisibleDependencies());
};

let setStyles = (textWidthFunction, circleTextPadding, relativeTextPosition) => {
  setTreeStyles(textWidthFunction, circleTextPadding, relativeTextPosition);
};

module.exports.visualizer = {
  setStyles: setStyles,
  visualizeGraph: visualizeGraph,
  drag: drag
};