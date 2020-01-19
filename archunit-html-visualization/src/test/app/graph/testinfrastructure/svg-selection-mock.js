'use strict';

//TODO: maybe own directory in infrastructure for mocks

const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;

const D3ElementMock = class {
  constructor(svgType, attributes) {
    this._svgType = svgType;
    this._attributes = new Map();
    Object.keys(attributes || {}).forEach(key => this._attributes.set(key, attributes[key]));
    this.translate({x: 0, y: 0})
  }

  translate({x, y}) {
    this._translation = {x: x || 0, y: y || 0};
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

  get radius() {
    return this.getAttribute('r');
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

    this._pointerEventsEnabled = true;
    this._mousePosition = [0, 0];
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

  /**
   * The position of a rectangle is assumed to define the upper left corner of the rectangle
   */
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

  /**
   * The position of a tspan is assumed to define the lower left corner of the text
   */
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

  createTransitionWithDuration() {
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
    return this._mousePosition;
  }

  setMousePosition(x, y) {
    this._mousePosition = [x, y];
  }

  static fromDom(svgType, attributes = {}) {
    return new SvgSelectionMock(svgType, attributes);
  }

  /**
   * additional methods for testing
   * TODO: maybe move them to own file...ask Peter
   */

  get pointerEventsEnabled() {
    return this._pointerEventsEnabled;
  }

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
    if (this._pointerEventsEnabled) {
      this._onclick(event);
    }
  }

  mouseOut() {
    if (this._pointerEventsEnabled) {
      this._onmouseout();
    }
  }

  mouseOver() {
    if (this._pointerEventsEnabled) {
      this._onmouseover();
    }
  }

  drag(dx, dy) {
    this._ondrag(dx, dy);
  }

  get isVisible() {
    return this._isVisible && this._parent && this._parent.isVisible;
  }

  getAllSubSvgElementsWithId(id) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllSubSvgElementsWithId(id)));
    const self = [];
    if (this.getAttribute('id') === id) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  getSubSvgElementWithId(id) {
    const result = this.getAllSubSvgElementsWithId(id);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one descendant with that id');
    }
    return result[0];
  }

  getAllVisibleDescendantElementsOfType(svgType) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllVisibleDescendantElementsOfType(svgType)));
    const self = [];
    if (this.svgType === svgType) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  getVisibleDescendantElementOfType(svgType) {
    const result = this.getAllVisibleDescendantElementsOfType(svgType);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one descendant of that type');
    }
    return result[0];
  }

  getAllDescendantElementsByTypeAndCssClasses(svgType, ...cssClasses) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllDescendantElementsByTypeAndCssClasses(svgType, ...cssClasses)));
    const self = [];
    if (this.svgType === svgType && cssClasses.every(cssClass => this.cssClasses.has(cssClass))) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  getDescendantElementByTypeAndCssClasses(svgType, ...cssClasses) {
    const result = this.getAllDescendantElementsByTypeAndCssClasses(svgType, ...cssClasses);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one descendant of that type');
    }
    return result[0];
  }

  getAllGroupsContainingAVisibleElementOfType(svgType) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllGroupsContainingAVisibleElementOfType(svgType)));
    const self = [];
    if (this.getAllVisibleSubElementsOfType(svgType).length > 0) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  getAllGroupsContainingAnElementOfType(svgType) {
    const descendants = [].concat.apply([], this._subElements.map(
      subSvgElement => subSvgElement.getAllGroupsContainingAnElementOfType(svgType)));
    const self = [];
    if (this.getAllSubElementsOfType(svgType).length > 0) {
      self.push(this);
    }
    return [...descendants, ...self];
  }

  getGroupContainingAVisibleElementOfType(svgType) {
    const result = this.getAllGroupsContainingAVisibleElementOfType(svgType);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one descendant group with a child of that type')
    }
    return result[0];
  }

  getAllVisibleSubElementsOfType(svgType) {
    return this._subElements.filter(element => element.svgType === svgType && element._isVisible);
  }

  getVisibleSubElementOfType(svgType) {
    const result = this.getAllVisibleSubElementsOfType(svgType);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one sub element of that type')
    }
    return result[0];
  }

  getAllVisibleSubElementsByTypeAndCssClasses(svgType, ...cssClasses) {
    return this._subElements.filter(element => element._isVisible && element.svgType === svgType && cssClasses.every(cssClass => element.cssClasses.has(cssClass)));
  }

  getVisibleSubElementByTypeAndCssClasses(svgType, ...cssClasses) {
    const result = this.getAllVisibleSubElementsByTypeAndCssClasses(svgType, ...cssClasses);
    if (result.length !== 1) {
      throw new Error('the svg element must have exactly one sub element of that type and with that css classes')
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

  get lineLength() {
    return Vector.between(this.absoluteStartPosition, this.absoluteEndPosition).length();
  }

  isInForegroundWithinParent() {
    if (this._parent) {
      return this._parent._subElements[this._parent._subElements.length - 1] === this;
    }
    return true;
  }

  getSelfAndPredecessors() {
    if (this._parent) {
      return [this, ...this._parent.getSelfAndPredecessors()];
    } else {
      return [this];
    }
  }

  /**
   * checks if this svg-element is drawn in front of the given other svg element
   * @return {*}
   */
  isInFrontOf(otherSvgElement) {
    const selfAndPredecessors = this.getSelfAndPredecessors();
    const otherAndPredecessors = otherSvgElement.getSelfAndPredecessors();

    const otherAndPredecessorsSet = new Set(otherAndPredecessors);

    let selfIndex = 0;
    let firstCommonPredecessor = selfAndPredecessors[selfIndex];

    while (!otherAndPredecessorsSet.has(firstCommonPredecessor)) {
      if (selfIndex >= selfAndPredecessors.length - 1) {
        throw new Error('the svg element seem not to be in the same svg hierarchy');
      }

      selfIndex++;
      firstCommonPredecessor = selfAndPredecessors[selfIndex];
    }

    const otherIndex = otherAndPredecessors.indexOf(firstCommonPredecessor);

    if (selfIndex === 0 && otherIndex === 0) {
      throw new Error('the both svg element seem to be the same');
    }

    if (selfIndex === 0) {
      return false;
    }

    if (otherIndex === 0) {
      return true;
    }

    const selfPredecessorIndex = firstCommonPredecessor._subElements.indexOf(selfAndPredecessors[selfIndex - 1]);
    const otherPredecessorIndex = firstCommonPredecessor._subElements.indexOf(otherAndPredecessors[otherIndex - 1]);
    return selfPredecessorIndex > otherPredecessorIndex;
  }
};

const RootSvgMock = class extends SvgSelectionMock {
  constructor() {
    super('svg');
  }

  get isVisible() {
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

module.exports.RootSvgMock = RootSvgMock;
module.exports.SvgSelectionMock = SvgSelectionMock;
module.exports.DivSelectionMock = DivSelectionMock;
