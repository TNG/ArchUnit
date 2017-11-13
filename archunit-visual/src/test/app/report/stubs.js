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
    this._cssClass = '';
    this._isVisible = true;
    this.show = () => this._isVisible = true;
    this.hide = () => this._isVisible = false;
    this.jumpToPosition = () => {
    };
    this.moveToPosition = () => Promise.resolve();
    this.moveToRadius = () => Promise.resolve();
    this.updateNodeType = cssClass => this._cssClass = cssClass;
    this.showIfVisible = node => {
      if (node.isVisible()) {
        this._isVisible = true;
      }
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
  NodeListenerStub: createNodeListenerStub
};