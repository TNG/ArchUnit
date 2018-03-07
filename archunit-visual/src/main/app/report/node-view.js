'use strict';

const d3 = require('d3');

const init = (transitionDuration) => {
  const createPromiseOnEndOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('end', resolve));

  const createPromiseOnEndAndInterruptOfTransition = (transition, transitionRunner) =>
    new Promise(resolve => transitionRunner(transition).on('interrupt', resolve).on('end', resolve));

  const View = class {
    constructor(parentSvgElement, node, onClick, onDrag) {
      this._svgElement = d3.select(parentSvgElement)
        .append('g')
        .data([node])
        .attr('id', node.getFullName().replace(/\\$/g, '.-'))
        .node();

      this._circle = d3.select(this._svgElement)
        .append('circle')
        .node();

      if (node.isRoot()) {
        d3.select(this._svgElement)
          .select('circle')
          .style('visibility', 'hidden')
      }

      this._text = d3.select(this._svgElement)
        .append('text')
        .text(node.getName())
        .node();

      this._onDrag(onDrag);
      this._onClick(onClick);
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

    startMoveToPosition(position) {
      return createPromiseOnEndAndInterruptOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    moveToPosition(position) {
      return createPromiseOnEndOfTransition(d3.select(this._svgElement).transition().duration(transitionDuration), t => t.attr('transform', `translate(${position.x}, ${position.y})`));
    }

    _onClick(handler) {
      d3.select(this._svgElement).select('circle').on('click', handler);
      d3.select(this._svgElement).select('text').on('click', handler);
    }

    _onDrag(handler) {
      const drag = d3.drag().on('drag', () => handler(d3.event.dx, d3.event.dy));
      d3.select(this._svgElement).call(drag);
    }
  };

  return View;
};

module.exports.init = (transitionDuration) => ({
  View: init(transitionDuration)
});