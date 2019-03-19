'use strict';

const d3 = require('d3');

const D3Element = class {
  constructor(selection) {
    this._d3Select = selection;
  }

  get() {
    return this._d3Select
  }

  translate({x, y}) {
    return this.get().attr('transform', `translate(${x}, ${y})`);
  }

  set radius(radius) {
    this.get().attr('r', radius);
  }

  set offsetY(offset) {
    this.get().attr('dy', offset);
  }

  set strokeWidth(strokeWidth) {
    this.get().style('stroke-width', strokeWidth);
  }

  setStartAndEndPosition(startPosition, endPosition) {
    this.get().attr('x1', startPosition.x);
    this.get().attr('y1', startPosition.y);
    this.get().attr('x2', endPosition.x);
    this.get().attr('y2', endPosition.y);
  }
};

const Transition = class extends D3Element {
  step(doWithSelection) {
    doWithSelection(this);
    return this;
  }

  finish() {
    return new Promise(resolve => this.get().on('end', () => resolve()));
  }
};

const SvgSelection = class extends D3Element {
  get domElement() {
    return this.get().node();
  }

  get width() {
    return parseInt(this.get().attr('width'));
  }

  set width(newWidth) {
    this.get().attr('width', newWidth)
  }

  get height() {
    return parseInt(this.get().attr('height'));
  }

  set height(newHeight) {
    this.get().attr('height', newHeight);
  }

  set dimension({width, height}) {
    this.width = width;
    this.height = height;
  }

  addGroup(attributes) {
    const group = this.get().append('g');
    Object.keys(attributes || {}).forEach(key => group.attr(key, attributes[key]));
    return new SvgSelection(group);
  }

  addCircle() {
    return new SvgSelection(this.get().append('circle'));
  }

  addText(text) {
    return new SvgSelection(this.get().append('text').text(text));
  }

  addLine() {
    return new SvgSelection(this.get().append('line'));
  }

  addChild(svgSelection) {
    this.domElement.appendChild(svgSelection.domElement)
  }

  detachFromParent() {
    d3.select(this.domElement).remove();
  }

  createTransitionWithDuration(duration) {
    return new Transition(this.get().transition().duration(duration));
  }

  get textWidth() {
    return this.domElement.getComputedTextLength();
  }

  set cssClasses(cssClasses) {
    this.get().attr('class', cssClasses.join(' '));
  }

  addCssClass(cssClass) {
    this.domElement.classList.add(cssClass);
  }

  removeCssClasses(cssClasses) {
    this.domElement.classList.remove(cssClasses);
  }

  show() {
    this.get().style('visibility', 'inherit');
  }

  hide() {
    this.get().style('visibility', 'hidden');
  }

  onClick(clickHandler) {
    this.domElement.onclick = clickHandler;
  }

  onDrag(dragHandler) {
    this.get().call(d3.drag().on('drag', () => dragHandler(d3.event.dx, d3.event.dy)));
  }

  onMouseOver(mouseOverHandler) {
    this.get().on('mouseover', mouseOverHandler);
  }

  onMouseOut(mouseOutHandler) {
    this.get().on('mouseout', mouseOutHandler);
  }

  enablePointerEvents() {
    this.get().style('pointer-events', 'all');
  }

  disablePointerEvents() {
    this.get().style('pointer-events', 'none');
  }

  getMousePosition() {
    return d3.mouse(this.domElement);
  }

  static fromDom(domElement) {
    return new SvgSelection(d3.select(domElement));
  }
};

const DivSelection = class extends D3Element {

  get scrollLeft() {
    return this.get().node().scrollLeft;
  }

  get scrollTop() {
    return this.get().node().scrollTop;
  }

  set scrollLeft(value) {
    this.get().node().scrollLeft = value;
  }

  set scrollTop(value) {
    this.get().node().scrollTop = value;
  }

  static fromDom(domElement) {
    return new DivSelection(d3.select(domElement));
  }
};

module.exports.SvgSelection = SvgSelection;
module.exports.DivSelection = DivSelection;