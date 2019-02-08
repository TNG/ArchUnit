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

  static fromDom(domElement) {
    return new SvgSelection(d3.select(domElement));
  }
};

module.exports = SvgSelection;