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

const calculateTextWidthStub = text => text.length * 7;

const NodeViewStub = class {
  constructor() {
    this.cssClass = '';
    this.isVisible = true;
    this.hasMovedToPosition = false;
    this.hasMovedToRadius = false;

    this.show = () => this.isVisible = true;
    this.hide = () => this.isVisible = false;
    this.jumpToPosition = () => this.hasJumpedToPosition = true;
    this.moveToPosition = () => {
      this.hasMovedToPosition = true;
      return Promise.resolve();
    };
    this.moveToRadius = (r, textOffset) => {
      this.hasMovedToRadius = true;
      this.textOffset = textOffset;
      return Promise.resolve();
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
const setMovedDependencies = arr => movedDependencies = arr;

const DependencyViewStub = class {
  constructor(parentSvgElement, dependency) {
    this.isVisible = true;
    this.hasJumpedToPosition = false;
    this.hasMovedToPosition = false;

    this.show = () => this.isVisible = true;
    this.hide = () => this.isVisible = false;

    this.jumpToPositionAndShowIfVisible = dependency => {
      this.hasJumpedToPosition = true;
      this.isVisible = dependency.isVisible();
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

const createNodeListenerStub = () => {
  let _onDragWasCalled = false;
  let _foldedNode;
  let _onLayoutChangedWasCalled = false;
  return {
    onDrag: () => _onDragWasCalled = true,
    onFold: node => _foldedNode = node,
    onLayoutChanged: () => _onLayoutChangedWasCalled = true,
    onDragWasCalled: () => _onDragWasCalled,
    foldedNode: () => _foldedNode,
    onLayoutChangedWasCalled: () => _onLayoutChangedWasCalled
  }
};

module.exports = {
  visualizationStylesStub: createVisualizationStylesStub,
  calculateTextWidthStub: calculateTextWidthStub,
  NodeViewStub: NodeViewStub,
  DependencyViewStub: DependencyViewStub,
  NodeListenerStub: createNodeListenerStub,
  setMovedDependencies: setMovedDependencies
};