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

      this._svgElement = svg.createGroup(fullNodeName.replace(/\\$/g, '.-'));

      this._circle = this._svgElement.addCircle();

      this._text = this._svgElement.addText(nodeName);

      this._svgElementForChildren = this._svgElement.addGroup();
      this._svgElementForDependencies = this._svgElement.addGroup();

      this._onDrag(onDrag);
      this._onClick(onClick, onCtrlClick);
    }

    addChildView(childView) {
      this._svgElementForChildren.addChild(childView._svgElement)
    }

    get svgElementForDependencies() {
      return this._svgElementForDependencies.domElement;
    }

    detachFromParent() {
      this._svgElement.detachFromParent();
    }

    getTextWidth() {
      return this._text.textWidth;
    }

    updateNodeType(cssClasses) {
      this._svgElement.cssClasses = cssClasses;
    }

    hide() {
      this._svgElement.hide();
    }

    show() {
      this._svgElement.show();
    }

    jumpToPosition(position) {
      this._svgElement.translate(position);
    }

    changeRadius(r, textOffset) {
      const radiusPromise = this._circle.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.radius = r)
        .finish();
      const textPromise = this._text.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.offsetY = textOffset)
        .finish();
      return Promise.all([radiusPromise, textPromise]);
    }

    setRadius(r, textOffset) {
      this._circle.radius = r;
      this._text.offsetY = textOffset;
    }

    startMoveToPosition(position) {
      return this._svgElement.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.translate(position))
        .finish();
    }

    moveToPosition(position) {
      return this._svgElement.createTransitionWithDuration(transitionDuration)
        .step(svgSelection => svgSelection.translate(position))
        .finish();
    }

    _onClick(handler, ctrlHandler) {
      const clickHandler = event => {
        if (event.ctrlKey || event.altKey) {
          ctrlHandler();
        } else {
          handler();
        }
        return false;
      };
      this._circle.onClick(clickHandler);
      this._text.onClick(clickHandler);
    }

    _onDrag(handler) {
      this._svgElement.onDrag(handler);
    }
  }

  return View;
};


module.exports = {init};