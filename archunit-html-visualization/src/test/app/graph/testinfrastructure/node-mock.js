'use strict';

const NodeMock = class {
  /**
   * @param fullName
   * @param isPackage
   * @param centerPosition
   * @param radius
   * @param zIndex higher z-index means a layer more in the front
   * @param svgContainerElement svg-selection, where the svg-element for the dependencies should be added to
   */
  constructor(fullName, isPackage, centerPosition, radius, zIndex, svgContainerElement = null) {
    this._fullName = fullName;
    this._isPackage = isPackage;
    this._centerPosition = centerPosition;
    this._radius = radius;
    this._zIndex = zIndex;

    if (svgContainerElement) {
      this._svgElement = svgContainerElement.addGroup({'id': fullName});
      this._svgElement.translate(this._centerPosition);
      this._svgSelectionForDependencies = this._svgElement.addGroup();
    }

    this._setOfOverlappingNodes = new Set();
  }

  get absoluteFixableCircle() {
    return {
      x: this._centerPosition.x,
      y: this._centerPosition.y,
      r: this._radius
    }
  }

  getFullName() {
    return this._fullName;
  }

  liesInFrontOf(otherNodeMock) {
    return this._zIndex > otherNodeMock._zIndex;
  }

  isPackage() {
    return this._isPackage;
  }

  overlapsWith(otherNodeMock) {
    return this._setOfOverlappingNodes.has(otherNodeMock._fullName);
  }

  get svgSelectionForDependencies() {
    return this._svgSelectionForDependencies;
  }

  //helper methods
  addOverlap(otherNodeMock) {
    this._setOfOverlappingNodes.add(otherNodeMock._fullName);
    otherNodeMock._setOfOverlappingNodes.add(this._fullName);
  }

  removeOverlap(otherNodeMock) {
    this._setOfOverlappingNodes.delete(otherNodeMock._fullName);
    otherNodeMock._setOfOverlappingNodes.delete(this._fullName);
  }
};

module.exports = NodeMock;