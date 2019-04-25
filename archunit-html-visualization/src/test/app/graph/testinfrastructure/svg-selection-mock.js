'use strict';

//TODO: maybe own directory in infrastructure for mocks

const D3ElementMock = class {
  constructor(svgType, attributes) {
    this._svgType = svgType;
    this._attributes = new Map();
    Object.keys(attributes || {}).forEach(key => this._attributes.set(key, attributes[key]));
  }

  translate({x, y}) {
    this._translation = {x, y};
    return this;
  }

  getTranslation() {
    return this._translation;
  }

  set radius(radius) {
    this._attributes.set('r', radius);
  }

  set offsetX(offset) {
    this._attributes.set('dx', offset);
  }

  set offsetY(offset) {
    this._attributes.set('dy', offset);
  }

  set positionX(x) {
    this._attributes.set('x', x);
  }

  set strokeWidth(strokeWidth) {
    this._strokeWidth = strokeWidth;
  }

  setStartAndEndPosition(startPosition, endPosition) {
    this._attributes.set('x1', startPosition.x);
    this._attributes.set('y1', startPosition.y);
    this._attributes.set('x2', endPosition.x);
    this._attributes.set('y2', endPosition.y);
  }

  /**
   * additional methods for testing
   */

  getAttribute(attribute) {
    return this._attributes.get(attribute);
  }

  get svgType() {
    return this._svgType;
  }
};

const TransitionMock = class extends D3ElementMock {
  constructor(svgSelectionMock) {
    super('transition');
    this._svgSelectionMock = svgSelectionMock;
  }

  step(doWithSelection) {
    doWithSelection(this._svgSelectionMock);
    return this;
  }

  finish() {
    return new Promise(resolve => setTimeout(resolve, 5));
  }
};

const SvgSelectionMock = class extends D3ElementMock {
  constructor(svgType, attributes) {
    super(svgType, attributes);
    this._subElements = [];
    this._parent = null;
    this._isVisible = true;
    this._cssClasses = new Set();
  }

  get domElement() {
    return null;
  }

  get width() {
    return this._attributes.get('width');
  }

  set width(newWidth) {
    this._attributes.set('width', newWidth);
  }

  get height() {
    return this._attributes.get('height');
  }

  set height(newHeight) {
    this._attributes.set('height', newHeight);
  }

  set dimension({width, height}) {
    this.width = width;
    this.height = height;
  }

  addGroup(attributes) {
    const newSvgSelectionMock = new SvgSelectionMock('g', attributes);
    this._subElements.push(newSvgSelectionMock);
    newSvgSelectionMock._parent = this;
    return newSvgSelectionMock;
  }

  addRect() {
    return this._addSvgElementByType('rect');
  }

  addCircle() {
    return this._addSvgElementByType('circle');
  }

  addText(text) {
    const textSvgSelectionMock = this._addSvgElementByType('text');
    textSvgSelectionMock._attributes.set('text', text);
    return textSvgSelectionMock;
  }

  addTSpan(text) {
    const textSvgSelectionMock = this._addSvgElementByType('tspan');
    textSvgSelectionMock._attributes.set('text', text);
    return textSvgSelectionMock;
  }

  addLine() {
    return this._addSvgElementByType('line');
  }

  addChild(svgSelection) {
    this._subElements.push(svgSelection);
    svgSelection._parent = this;
  }

  detachFromParent() {
    this._parent._subElements.splice(this._parent._subElements.indexOf(this), 1);
    this._parent = null;
  }

  createTransitionWithDuration(duration) {
    return new TransitionMock(this);
  }

  get textWidth() {
    return this._attributes.get('text').length * 3;
  }

  set cssClasses(cssClasses) {
    this._cssClasses = new Set(cssClasses);
    this._updateCssClasses();
  }

  addCssClass(cssClass) {
    this._cssClasses.add(cssClass);
    this._updateCssClasses();
  }

  removeCssClasses(...cssClasses) {
    cssClasses.forEach(cssClass => this._cssClasses.delete(cssClass));
    this._updateCssClasses();
  }

  show() {
    this._isVisible = true;
  }

  hide() {
    this._isVisible = false;
  }

  onClick(clickHandler) {
    this._onclick = clickHandler;
  }

  onDrag(dragHandler) {
    this._ondrag = (dx, dy) => dragHandler(dx, dy);
  }

  onMouseOver(mouseOverHandler) {
    this._onmouseover = mouseOverHandler;
  }

  onMouseOut(mouseOutHandler) {
    this._onmouseout = mouseOutHandler;
  }

  enablePointerEvents() {
    this._pointerEventsEnabled = true;
  }

  disablePointerEvents() {
    this._pointerEventsEnabled = false;
  }

  getMousePosition() {
    return [0, 0];
  }

  static fromDom(svgType, attributes = {}) {
    return new SvgSelectionMock(svgType, attributes);
  }

  /**
   * additional methods for testing
   * TODO: maybe move them to own file...ask Peter
   */

  _updateCssClasses() {
    this._attributes.set('class', [...this._cssClasses].join(' '));
  }

  _addSvgElementByType(svgType) {
    const newSvgSelectionMock = new SvgSelectionMock(svgType);
    this._subElements.push(newSvgSelectionMock);
    newSvgSelectionMock._parent = this;
    return newSvgSelectionMock;
  }

  get cssClasses() {
    return this._cssClasses;
  }

  click(event) {
    this._onclick(event);
  }

  drag(dx, dy) {
    this._ondrag(dx, dy);
  }

  get isVisible() {
    return this._isVisible && (!this._parent || this._parent.isVisible);
  }

  getAllGroupsContainingAVisibleElementOfType(svgType) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllGroupsContainingAVisibleElementOfType(svgType)));
    const self = [];
    if (this._getVisibleSubElementsOfType(svgType).length > 0) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  _getVisibleSubElementsOfType(svgType) {
    return this._subElements.filter(element => element.svgType === svgType && element.isVisible);
  }

  getVisibleSubElementOfType(svgType) {
    const result = this._getVisibleSubElementsOfType(svgType);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one sub element of that type')
    }
    return result[0];
  }

  _getPositionOffset() {
    const translation = this.getTranslation() || {x: 0, y: 0};
    const absoluteReferencePosition = this._parent === null ? {x: 0, y: 0} : this._parent.absolutePosition;
    return {
      x: translation.x + absoluteReferencePosition.x,
      y: translation.y + absoluteReferencePosition.y
    }
  }

  _getAbsolutePosition(xPropertyValue, yPropertyValue) {
    const positionOffset = this._getPositionOffset();
    return {
      x: positionOffset.x + xPropertyValue,
      y: positionOffset.y + yPropertyValue
    }
  }

  get absolutePosition() {
    const x = this.getAttribute('x') || 0;
    const y = this.getAttribute('y') || 0;
    const dx = this.getAttribute('dx') || 0;
    const dy = this.getAttribute('dy') || 0;
    return this._getAbsolutePosition(x + dx, y + dy);
  }

  get absoluteStartPosition() {
    const x1 = this.getAttribute('x1');
    const y1 = this.getAttribute('y1');
    return this._getAbsolutePosition(x1, y1);
  }

  get absoluteEndPosition() {
    const x2 = this.getAttribute('x2');
    const y2 = this.getAttribute('y2');
    return this._getAbsolutePosition(x2, y2);
  }

  /**
   * checks if this svg-element, which is assumed to belong to a node, is in the foreground among all svg-element, that belong to nodes
   * @return {*}
   */
  isNodeInForeground() {
    if (this._parent) {
      return this._parent._subElements[this._parent._subElements.length - 1] === this && this._parent._parent.isNodeInForeground(); //skip the
      //svg-element containing the children of the parent node
    }
    return true;
  }
};

const DivSelectionMock = class extends D3ElementMock {

  get scrollLeft() {
    return this._attributes.get('scrollLeft');
  }

  get scrollTop() {
    return this._attributes.get('scrollTop');
  }

  set scrollLeft(value) {
    return this._attributes.set('scrollLeft', value);
  }

  set scrollTop(value) {
    return this._attributes.set('scrollTop', value);
  }

  static fromDom(svgType, attributes = {}) {
    return new DivSelectionMock(svgType, attributes);
  }
};

module.exports.SvgSelectionMock = SvgSelectionMock;
module.exports.DivSelectionMock = DivSelectionMock;