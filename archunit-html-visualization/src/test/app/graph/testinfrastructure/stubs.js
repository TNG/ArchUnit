'use strict';

const createVisualizationStylesStub = (circlePadding = 1, nodeFontSize = 10) => {
  let _circlePadding = circlePadding;
  let _nodeFontSize = nodeFontSize;
  return {
    getCirclePadding: () => _circlePadding,
    setCirclePadding: padding => _circlePadding = padding,
    getNodeFontSize: () => _nodeFontSize,
    setNodeFontSize: fontSize => _nodeFontSize = fontSize
  };
};

//all nodes are added to this list when they are moved to their position to be able to track the process
let movedNodes = [];
let nodesWhoseRadiusWasChanged = [];
const saveMovedNodesTo = arr => movedNodes = arr;
const saveNodesWhoseRadiusWasChangedTo = arr => nodesWhoseRadiusWasChanged = arr;

const NodeViewStub = class {
  constructor(parentSvgElement, node) {
    this.cssClass = '';
    this.isVisible = true;
    this.hasMovedToPosition = false;
    this.hasMovedToRadius = false;

    this.getTextWidth = () => node.getName().length * 7;

    this.show = () => this.isVisible = true;
    this.hide = () => this.isVisible = false;
    this.jumpToPosition = () => this.hasJumpedToPosition = true;
    this.moveToPosition = () => {
      this.hasMovedToPosition = true;
      return Promise.resolve();
    };
    this.startMoveToPosition = () => Promise.resolve();
    this.changeRadius = (r, textOffset) => {
      this.hasMovedToRadius = true;
      this.textOffset = textOffset;
      return new Promise(resolve => {
        movedNodes.push(node.getFullName());
        setTimeout(resolve, 10);
      });
    };
    this.setRadius = () => {
      nodesWhoseRadiusWasChanged.push(node.getFullName());
    };
    this.updateNodeType = cssClass => this.cssClass = cssClass;
    this.showIfVisible = node => {
      if (node.isVisible()) {
        this.isVisible = true;
      }
    };
  }
};

//all dependencies are added to this list when they are moved to their position to be able to track the process
let movedDependencies = [];
const saveMovedDependenciesTo = arr => movedDependencies = arr;

const DependencyViewStub = class {
  constructor() {
    this.refreshWasCalled = false;
    this.hasJumpedToPosition = false;
    this.hasMovedToPosition = false;

    this.show = () => this.isVisible = true;
    this.hide = () => this.isVisible = false;

    this.jumpToPositionAndShowIfVisible = dependency => {
      this.hasJumpedToPosition = true;
      this.isVisible = dependency.isVisible();
    };
    this.refresh = () => {
      this.refreshWasCalled = true;
    };
    this.moveToPositionAndShowIfVisible = dependency => {
      this.hasMovedToPosition = true;
      this.isVisible = dependency.isVisible();
      return new Promise(resolve => {
        movedDependencies.push(dependency);
        setTimeout(resolve, 10);
      });
    };
  }
};

const GraphViewStub = class {
  constructor() {
    this.renderWithTransition = () => {
    };
  }
};

const createNodeListenerStub = () => {
  let _onDragWasCalled = false;
  const _draggedNodes = [];
  let _foldedNode;
  let _initialFoldedNode = null;
  let _onLayoutChangedWasCalled = false;

  const overlappedNodesAndPosition = [];

  return {
    onDrag: (node) => {
      _onDragWasCalled = true;
      _draggedNodes.push(node.getFullName());
    },
    onFold: node => _foldedNode = node,
    onLayoutChanged: () => _onLayoutChangedWasCalled = true,
    onInitialFold: node => _initialFoldedNode = node,

    onDragWasCalled: () => _onDragWasCalled,
    draggedNodes: () => _draggedNodes,
    foldedNode: () => _foldedNode,
    initialFoldedNode: () => _initialFoldedNode,
    onLayoutChangedWasCalled: () => _onLayoutChangedWasCalled,

    onNodesOverlapping: (fullNameOfOverlappedNode, positionOfOverlappingNode) =>
      overlappedNodesAndPosition.push({overlappedNode: fullNameOfOverlappedNode, position: positionOfOverlappingNode}),
    resetNodesOverlapping: () => {
    },
    finishOnNodesOverlapping: () => {
    },

    overlappedNodesAndPosition: () => overlappedNodesAndPosition,

    resetInitialFoldedNode: () => _initialFoldedNode = null
  }
};

module.exports = {
  visualizationStylesStub: createVisualizationStylesStub,
  NodeViewStub: NodeViewStub,
  DependencyViewStub: DependencyViewStub,
  GraphViewStub: GraphViewStub,
  NodeListenerStub: createNodeListenerStub,
  saveMovedDependenciesTo: saveMovedDependenciesTo,
  saveMovedNodesTo: saveMovedNodesTo,
  saveNodesWhoseRadiusWasChangedTo: saveNodesWhoseRadiusWasChangedTo
};