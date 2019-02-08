'use strict';

const d3 = require('d3');
const svg = require('../infrastructure/gui-elements').svg;

const init = (transitionDuration) => {
  const createPromiseOnEndOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('end', resolve));

  const createPromiseOnEndAndInterruptOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('interrupt', () => resolve()).on('end', resolve));

  class View {
    constructor(
      {nodeName, fullNodeName},
      {onClick, onDrag, onCtrlClick}) {

      this._realSvgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));
      this._svgElement = this._realSvgElement.domElement;

      this._circle = this._realSvgElement.addCircle().domElement;

      this._text = this._realSvgElement.addText(nodeName).domElement;

      this._svgElementForChildren = this._realSvgElement.addGroup().domElement;
      this._svgElementForDependencies = this._realSvgElement.addGroup().domElement;

      this._onDrag(onDrag);
      this._onClick(onClick, onCtrlClick);
    }

    addChildView(childView) {
      this._svgElementForChildren.appendChild(childView._svgElement)
    }

    get svgElementForDependencies() {
      return this._svgElementForDependencies;
    }

    get svgElementForChildren() {
      return this._svgElementForChildren;
    }

    detachFromParent() {
      d3.select(this._svgElement).remove();
    }

    getTextWidth() {
      return this._text.getComputedTextLength();
    }

    updateNodeType(nodeType) {
      d3.select(this._svgElement).attr('class', nodeType);
    }

    hide() {
      d3.select(this._svgElement).style('visibility', 'hidden');
    }

    show() {
      d3.select(this._svgElement).style('visibility', 'inherit');
    }

    showIfVisible(node) {
      if (node.isVisible()) {
        this.show();
      }
    }

    jumpToPosition(position) {
      d3.select(this._svgElement).attr('transform', `translate(${position.x}, ${position.y})`);
    }

    changeRadius(r, textOffset) {
      const radiusPromise = createPromiseOnEndOfTransition(d3.select(this._circle).transition().duration(transitionDuration), t => t.attr('r', r));
      const textPromise = createPromiseOnEndOfTransition(d3.select(this._text).transition().duration(transitionDuration), t => t.attr('dy', textOffset));
      return Promise.all([radiusPromise, textPromise]);
    }

    setRadius(r, textOffset) {
      d3.select(this._circle).attr('r', r);
      d3.select(this._text).attr('dy', textOffset);
    }

    startMoveToPosition(position) {
      return createPromiseOnEndAndInterruptOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    moveToPosition(position) {
      return createPromiseOnEndOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    _onClick(handler, ctrlHandler) {
      const onClick = event => {
        if (event.ctrlKey || event.altKey) {
          ctrlHandler();
        } else {
          handler();
        }
        return false;
      };
      d3.select(this._svgElement).select('circle').node().onclick = onClick;
      d3.select(this._svgElement).select('text').node().onclick = onClick;
    }

    _onDrag(handler) {
      const drag = d3.drag().on('drag', () => handler(d3.event.dx, d3.event.dy));
      d3.select(this._svgElement).call(drag);
    }
  }

  return View;
};


module.exports = {init};