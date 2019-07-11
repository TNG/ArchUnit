'use strict';

const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;

const d3 = require('d3');

const MIN_RADIUS = 5;

const init = () => {

  let currentZIndex = 0;
  const nodeMap = new Map();

  const NodeMock = class {
    /**
     * @param jsonNode same format as when creating a real node
     * @param zIndex higher z-index means a layer more in the front
     * @param svgContainerElement svg-selection, where the svg-element for the dependencies should be added to
     * @param parent
     */
    constructor(jsonNode, svgContainerElement = null, zIndex = 0, parent = null) {
      this._fullName = jsonNode.fullName;
      this._isPackage = jsonNode.type === 'package';
      this._zIndex = zIndex;
      this._circle = {r: 10};
      this._parent = parent;

      if (svgContainerElement) {
        this._svgElement = svgContainerElement.addGroup({'id': this._fullName});
        this._svgElement.translate({x: 0, y: 0});
        this._svgElementForChildren = this._svgElement.addGroup();
        this._svgSelectionForDependencies = this._svgElement.addGroup();
      }

      this.children = Array.from(jsonNode.children || []).map(jsonChild =>
        new NodeMock(jsonChild, this._svgElementForChildren, currentZIndex++, this));

      const childCircles = this.children.map(child => child._circle);
      d3.packSiblings(childCircles);
      this.children.forEach(child => child._updatePosition());
      const enclosingCircle = childCircles.length === 0 ? {r: MIN_RADIUS} : d3.packEnclose(childCircles);
      this._circle.r = enclosingCircle.r;

      if (this._parent === null) {
        this._circle.x = 0;
        this._circle.y = 0;
      }

      this._setOfOverlappingNodes = new Set();

      nodeMap.set(this._fullName, this);
    }

    get _view() {
      return {
        _svgElement: this._svgElement
      }
    }

    getByName(fullName) {
      return nodeMap.get(fullName);
    }

    get absoluteFixableCircle() {
      if (this._parent) {
        const pos = Vector.from(this._parent.absoluteFixableCircle).add(this._circle);
        return {x: pos.x, y: pos.y, r: this._circle.r};
      } else {
        return this._circle;
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

    /**
     * helper methods
     */

    _updatePosition() {
      if (this._svgElement) {
        this._svgElement.translate(this._circle);
      }
    }

    get _maximumZIndexOfSeldAndDescendants() {
      if (this.children.length === 0) {
        return this._zIndex;
      } else {
        return this.children[this.children.length - 1]._maximumZIndexOfSeldAndDescendants;
      }
    }

    addOverlap(otherNodeMock) {
      this._setOfOverlappingNodes.add(otherNodeMock._fullName);
      otherNodeMock._setOfOverlappingNodes.add(this._fullName);
    }

    removeOverlap(otherNodeMock) {
      this._setOfOverlappingNodes.delete(otherNodeMock._fullName);
      otherNodeMock._setOfOverlappingNodes.delete(this._fullName);
    }
  };

  return NodeMock;
};

module.exports = {init};