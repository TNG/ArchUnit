'use strict';

import {SVG} from "./infrastructure/svg";
import {DivSelection, SvgSelection} from "./infrastructure/svg-selection";
import {RootView} from "./node/root-view";
import {Vector} from "./infrastructure/vectors";
import {ArchUnitDocument} from "./infrastructure/document";
import {ArchUnitWindow} from "./infrastructure/window";

class GraphView {
  private _svgContainerDivSelection: DivSelection;
  private _svgElement: SvgSelection;
  private _translater: SvgSelection;
  private _svgElementForNodes: SvgSelection;
  // private _svgElementForDetailedDependencies: SvgSelection;
  private _transitionDuration: number;
  private _document: ArchUnitDocument;
  private _window: ArchUnitWindow;

  constructor(svgDomElement: Element, svgContainerDivDomElement: Element, transitionDuration: number, svg: SVG, document: ArchUnitDocument, window: ArchUnitWindow) {
    this._svgContainerDivSelection = document.selectDiv(svgContainerDivDomElement);
    this._document = document;
    this._window = window;
    this._svgElement = svg.select(svgDomElement);
    this._svgElement.dimension = {width: 0, height: 0};
    const attributes: Map<string, string> = new Map<string, string>([['id', 'translater']]);
    this._translater = this._svgElement.addGroup(attributes);
    this._transitionDuration = transitionDuration;
    this._svgElementForNodes = this._translater.addGroup();
    // this._svgElementForDetailedDependencies = this._svgElement.addGroup();
  }

  get svgElement() {
    return this._svgElement;
  }

  get translater() {
    return this._translater;
  }
  //
  // get svgElementForDetailedDependencies() {
  //   return this._svgElementForDetailedDependencies;
  // }

  addRootView(rootView: RootView) {
    this._svgElementForNodes.addChild(rootView.svgElement);
  }

  changeScrollPosition(offsetPosition: Vector) {
    this._svgContainerDivSelection.scrollLeft += offsetPosition.x;
    this._svgContainerDivSelection.scrollTop += offsetPosition.y;
  }

  render(halfWidth: number, halfHeight: number) {
    this._renderSizeIfNecessary(halfWidth, halfHeight);
    this._translater.translate(this._toAbsoluteCoordinates(new Vector(halfWidth, halfHeight)));
  }

  renderWithTransition(halfWidth: number, halfHeight: number) {
    this._renderSizeIfNecessary(halfWidth, halfHeight);
    return this._translater.createTransitionWithDuration(this._transitionDuration)
      .step(element => element.translate(this._toAbsoluteCoordinates(
        new Vector(halfWidth, halfHeight))))
      .finish();
  }

  _renderSizeIfNecessary(halfWidth: number, halfHeight: number) {
    const calcRequiredSize = (halfSize: number) => (2 * halfSize + 4);
    const calcExpandedSize = (halfSize: number) => (2 * halfSize + 4);
    const getNewSize = (windowSize: number, requiredSize: number, maxSize: number) => requiredSize < windowSize ? windowSize : maxSize;

    const windowWidth = Math.max(this._document.getClientWidth(), this._window.getInnerWidth() || 0);
    const windowHeight = Math.max(this._document.getClientHeight(), this._window.getInnerHeight() || 0);

    const requiredWidth = calcRequiredSize(halfWidth);
    const expandedWidth = calcExpandedSize(halfWidth);
    const requiredHeight = calcRequiredSize(halfHeight);
    const expandedHeight = calcExpandedSize(halfHeight);

    const minWidth = Math.max(windowWidth, requiredWidth);
    const maxWidth = Math.max(windowWidth, expandedWidth);

    const minHeight = Math.max(windowHeight, requiredHeight);
    const maxHeight = Math.max(windowHeight, expandedHeight);

    if (this._svgElement.width < minWidth || maxWidth < this._svgElement.width) {
      this._svgElement.width = getNewSize(windowWidth, requiredWidth, maxWidth);
    }

    if (this._svgElement.height < minHeight || maxHeight < this._svgElement.height) {
      this._svgElement.height = getNewSize(windowHeight, requiredHeight, maxHeight);
    }
  }

  _toAbsoluteCoordinates(relativeVector: Vector): Vector {
    return new Vector(this._svgElement.width / 2 - relativeVector.x, this._svgElement.height / 2 - relativeVector.y);
  }
}

interface GraphViewFactory {
  getGraphView: (svgDomElement: Element, svgContainerElement: Element) => GraphView
}

const init = (transitionDuration: number, svg: SVG, document: ArchUnitDocument, window: ArchUnitWindow): GraphViewFactory => ({
  getGraphView: (svgDomElement, svgContainerElement) => new GraphView(svgDomElement, svgContainerElement, transitionDuration, svg, document, window)
});

export {init, GraphViewFactory, GraphView};
