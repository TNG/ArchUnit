'use strict';

import {Selection} from 'd3-selection'
import {Vector} from "./vectors";

const d3 = require('d3');

class D3Element {
  private _d3Select: Selection<SVGElement, any, HTMLElement, any>

  constructor(selection: Selection<SVGElement, any, HTMLElement, any>) {
    this._d3Select = selection;
  }

  get() {
    return this._d3Select
  }

  translate(vector: Vector) {
    return this.get().attr('transform', `translate(${vector.x}, ${vector.y})`);
  }

  getTranslation() {
    const transform = this.get().attr('transform');
    const indexOfTranslate = transform.indexOf('translate');
    const translationsString = transform.substring(transform.indexOf('(', indexOfTranslate) + 1, transform.indexOf(')', indexOfTranslate));
    const translation = translationsString.split(',').map((s: string) => parseInt(s));
    return {
      x: translation[0] || 0,
      y: translation[1] || 0
    }
  }

  set radius(radius: number) {
    this.get().attr('r', radius);
  }

  set offsetX(offset: number) {
    this.get().attr('dx', offset);
  }

  set offsetY(offset: number) {
    this.get().attr('dy', offset);
  }

  set positionX(x: number) {
    this.get().attr('x', x);
  }

  set strokeWidth(strokeWidth: number) {
    this.get().style('stroke-width', strokeWidth);
  }

  setStartAndEndPosition(startPosition: Vector, endPosition: Vector) {
    this.get().attr('x1', startPosition.x);
    this.get().attr('y1', startPosition.y);
    this.get().attr('x2', endPosition.x);
    this.get().attr('y2', endPosition.y);
  }
}

class Transition extends d3.Transition {
  constructor(transition: d3.Transition<SVGElement, any, HTMLElement, any>) {
    super(transition);
  }

  step(doWithSelection: (transition: Transition) => Selection<SVGElement, any, HTMLElement, any>) {
    doWithSelection(this);
    return this;
  }

  finish() {
    return new Promise<void>(resolve => this.get().on('end', () => resolve()));
  }
}

class SvgSelection extends D3Element {
  get domElement(): SVGElement {
    return this.get().node();
  }

  get width(): number {
    return parseInt(this.get().attr('width'));
  }

  set width(newWidth: number) {
    this.get().attr('width', newWidth)
  }

  get height(): number {
    return parseInt(this.get().attr('height'));
  }

  set height(newHeight: number) {
    this.get().attr('height', newHeight);
  }

  set dimension({width, height}: {width: number, height: number}) {
    this.width = width;
    this.height = height;
  }

  addGroup(attributes: Map<string, string> | null = null): SvgSelection {
    const group = this.get().append('g');
    Object.keys(attributes || {}).forEach(key => group.attr(key, attributes.get(key)));
    return new SvgSelection(group);
  }

  addRect(): SvgSelection {
    return new SvgSelection(this.get().append('rect'));
  }

  addCircle(): SvgSelection {
    return new SvgSelection(this.get().append('circle'));
  }

  addText(text: string): SvgSelection {
    return new SvgSelection(this.get().append('text').text(text));
  }

  addTSpan(text: string): SvgSelection {
    return new SvgSelection(this.get().append('tspan').text(text));
  }

  addLine(): SvgSelection {
    return new SvgSelection(this.get().append('line'));
  }

  addChild(svgSelection: SvgSelection): void {
    this.domElement.appendChild(svgSelection.domElement)
  }

  detachFromParent(): void {
    d3.select(this.domElement).remove();
  }

  createTransitionWithDuration(duration: number): Transition {
    return new Transition(this.get().transition().duration(duration));
  }

  get textWidth(): number {
    return (this.domElement as SVGTextContentElement).getComputedTextLength();
  }

  set cssClasses(cssClasses: string[]) {
    this.get().attr('class', cssClasses.join(' '));
  }

  addCssClass(cssClass: string): void {
    this.domElement.classList.add(cssClass);
  }

  removeCssClasses(...cssClasses: string[]): void {
    this.domElement.classList.remove(...cssClasses);
  }

  show(): void {
    this.get().style('visibility', 'inherit');
  }

  hide(): void {
    this.get().style('visibility', 'hidden');
  }

  onClick(clickHandler: (this: GlobalEventHandlers, ev: MouseEvent) => any): void {
    this.domElement.onclick = clickHandler;
  }

  onDrag(dragHandler: (x: number, y: number) => void): void {
    this.get().call(d3.drag().on('drag', () => dragHandler(d3.event.dx, d3.event.dy)));
  }

  onMouseOver(mouseOverHandler: (this: GlobalEventHandlers, ev: MouseEvent) => any): void {
    this.get().on('mouseover', mouseOverHandler);
  }

  onMouseOut(mouseOutHandler: (this: GlobalEventHandlers, ev: MouseEvent) => any): void {
    this.get().on('mouseout', mouseOutHandler);
  }

  enablePointerEvents(): void {
    this.get().style('pointer-events', 'all');
  }

  disablePointerEvents(): void {
    this.get().style('pointer-events', 'none');
  }

  getMousePosition() {
    return d3.mouse(this.domElement);
  }

  static fromDom(domElement: Element): SvgSelection {
    return new SvgSelection(d3.select(domElement));
  }
}

class DivSelection extends D3Element {

  get scrollLeft(): number {
    return this.get().node().scrollLeft;
  }

  get scrollTop(): number {
    return this.get().node().scrollTop;
  }

  set scrollLeft(value: number) {
    this.get().node().scrollLeft = value;
  }

  set scrollTop(value: number) {
    this.get().node().scrollTop = value;
  }

  static fromDom(domElement: Element): DivSelection {
    return new DivSelection(d3.select(domElement));
  }
}

export {SvgSelection, DivSelection}
// module.exports.SvgSelection = SvgSelection;
// module.exports.DivSelection = DivSelection;
