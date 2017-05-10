'use strict';

const setTreeStyles = require("./tree-visualdata.js").setStyles;
const visualizeTree = require("./tree-visualdata.js").visualizeTree;
const dragNode = require("./tree-visualdata.js").dragNode;
const adaptNodeVisualDataToFoldState = require("./tree-visualdata.js").adaptToFoldState;

const refreshVisualDataOfDependencies = require("./dependency-visualdata.js").refreshVisualDataOfDependencies;
const refreshVisualDataOf = require("./dependency-visualdata.js").refreshVisualDataOf;

//TODO: set an update function in the tree and the dependencies instead of giving these functions every time of calling changeFold
let visualize = (root, packSiblings, packEnclose, circpadding) => {
  visualizeTree(root, packSiblings, packEnclose, circpadding);
  refreshVisualDataOfDependencies(root.getVisibleEdges());
};

let adaptToFoldState = node => {
  adaptNodeVisualDataToFoldState(node);
  //refreshVisualDataOf(node.projectData.fullname, node.getVisibleEdges());

  //Warum alle und nicht nur diejenigen, deren Start/Ziel gefoldet wurde?? -> beim Folden werden alle Deps neu berechnet
  //(ueber die Transformer) -> die schon existierenden haben dann keine VisualData mehr
  refreshVisualDataOfDependencies(node.getVisibleEdges());
};

let drag = (node, dx, dy, force) => {
  dragNode(node, dx, dy, force);
  refreshVisualDataOf(node.projectData.fullname, node.getVisibleEdges());
};

let setStyles = (textWidthFunction, circleTextPadding, relativeTextPosition) => {
  setTreeStyles(textWidthFunction, circleTextPadding, relativeTextPosition);
};

module.exports.visualizer = {
  setStyles: setStyles,
  visualize: visualize,
  adaptToFoldState: adaptToFoldState,
  refreshOnFiltering: refreshVisualDataOfDependencies,
  drag: drag
};