const chai = require('chai');
const chaiExtensions = require('../../testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);
const expect = chai.expect;

const Vector = require('../../../../../main/app/graph/infrastructure/vectors').Vector;
const visualizationStyles = require('../../testinfrastructure/root-creator').getVisualizationStyles();

const MAXIMUM_PADDING_DELTA = 1;

class RootUi {
  constructor(root) {
    this._root = root;
    this._nodeUIs = new Map();
    const createNodeUi = (node, parentUi, rootUi) => {
      const nodeUi = new NodeUi(node, parentUi, rootUi);
      nodeUi._childrenUis = node.getOriginalChildren().map(child => createNodeUi(child, nodeUi, rootUi));
      this._nodeUIs.set(node.getFullName(), nodeUi);
      return nodeUi;
    };
    this._childrenUis = root.getOriginalChildren().map(child => createNodeUi(child, this, this));
  }

  allNodes() {
    return [...this._nodeUIs.values()];
  }

  nodesWithSingleChild() {
    return this.allNodes().filter(nodeUi => nodeUi.childrenUis.length === 1);
  }

  leafNodes() {
    return this.allNodes().filter(nodeUi => nodeUi.childrenUis.length === 0);
  }

  nonLeafNodes() {
    return this.allNodes().filter(nodeUi => nodeUi.childrenUis.length > 0);
  }

  nodeByFullName(nodeFullName) {
    return this._nodeUIs.get(nodeFullName);
  }

  _isRoot() {
    return true;
  }

  contains() {
    return true;
  }

  get childrenUis() {
    return new NodeUiCollection(this._childrenUis);
  }

  isInForeground() {
    return true;
  }
}

class NodeUi {
  constructor(node, parentUi, rootUi) {
    this._parentUi = parentUi;
    this._rootUi = rootUi;
    this._svg = node._view._svgElement;
    this._circleSvg = this._svg.getVisibleSubElementOfType('circle');
    this._labelSvg = this._svg.getVisibleSubElementOfType('text');
  }

  get absolutePosition() {
    return Vector.from(this._circleSvg.absolutePosition);
  }

  get parent() {
    return this._parentUi;
  }

  get radius() {
    const radius = this._circleSvg.getAttribute('r');
    if (radius === undefined) {
      throw new Error('NodeUi does not declare a radius even though it is a proper child');
    }
    return radius;
  }

  _isRoot() {
    return false;
  }

  contains(otherNodeUi) {
    if (otherNodeUi._isRoot()) {
      return false;
    }

    const centerDistance = Vector.between(this.absolutePosition, otherNodeUi.absolutePosition).length();
    const maxDistance = centerDistance + otherNodeUi.radius;
    return maxDistance <= this.radius;
  }

  expectToBeWithin(otherNodeUi) {
    expect(otherNodeUi.contains(this)).to.be.true;
  }

  get childrenUis() {
    return new NodeUiCollection(this._childrenUis);
  }

  overlapsWith(otherNodeUi) {
    const centerDistance = Vector.between(this.absolutePosition, otherNodeUi.absolutePosition).length();
    const minDistance = this.radius + otherNodeUi.radius + 2 * visualizationStyles.getCirclePadding();
    return centerDistance + MAXIMUM_PADDING_DELTA < minDistance;
  }

  hasLabelAbove(otherNodeUi) {
    const positionDistance = Vector.between(this._labelSvg.absolutePosition, otherNodeUi.absolutePosition).length();
    return positionDistance + MAXIMUM_PADDING_DELTA >= otherNodeUi.radius;
  }

  hasLabelWithinCircle() {
    const positionDistance = Vector.between(this._labelSvg.absolutePosition, this.absolutePosition).length();
    const circleWidthAtTextPosition = 2 * Math.sqrt(this.radius * this.radius - positionDistance * positionDistance);
    return this._labelSvg.textWidth < circleWidthAtTextPosition;
  }

  hasLabelInTheMiddleOfCircle() {
    const textPosition = this._labelSvg.absolutePosition;
    const circlePosition = this.absolutePosition;
    return textPosition.x === circlePosition.x && textPosition.y === circlePosition.y;
  }

  hasLabelAtTheTop() {
    return this._labelSvg.absolutePosition.y < this.absolutePosition.y;
  }

  isWithinRectangle(width, height) {
    const position = this.absolutePosition;
    const padding = visualizationStyles.getCirclePadding();
    return position.x + this.radius + padding <= width && position.y + this.radius + padding <= height;

  }

  async drag({dx, dy}) {
    this._svg.drag(dx, dy);
    await this._rootUi._root._updatePromise;
  }

  async dragOver(otherNodeFullName) {
    const otherNodeUi = this._rootUi._nodeUIs.get(otherNodeFullName);
    const diffVector = Vector.between(this.absolutePosition, otherNodeUi.absolutePosition);
    const dragVector = diffVector.norm(diffVector.length() - this.radius - otherNodeUi.radius + 1);
    await this.drag({dx: dragVector.x, dy: dragVector.y});
  }

  isInForeground() {
    return this._svg.isInForegroundWithinParent() && this._parentUi.isInForeground();
  }

  expectToHaveLabelAbove(otherNodeUi) {
    expect(this.hasLabelAbove(otherNodeUi)).to.be.true;
  }

  expectToHaveLabelWithinCircle() {
    expect(this.hasLabelWithinCircle()).to.be.true;
  }

  expectToHaveLabelInTheMiddleOfCircle() {
    expect(this.hasLabelInTheMiddleOfCircle()).to.be.true;
  }

  expectToHaveLabelAtTheTop() {
    expect(this.hasLabelAtTheTop()).to.be.true;
  }

  expectToBeWithinRectangle(width, height) {
    expect(this.isWithinRectangle(width, height)).to.be.true;
  }

  expectToBeAtPosition(position) {
    expect(this.absolutePosition).to.deep.closeTo(position);
  }

  expectToBeInForeground() {
    expect(this.isInForeground()).to.be.true;
  }
}

class NodeUiCollection {
  constructor(nodeUis) {
    this._nodeUis = nodeUis;
  }

  get length() {
    return this._nodeUis.length;
  }

  getSingleNodeUi() {
    if (this.length !== 1) {
      throw new Error('the collection does not contain a single node-ui');
    }
    return this._nodeUis[0];
  }

  expectNotToOverlapEachOther() {
    this._nodeUis.forEach((nodeUi, index) => {
      this._nodeUis.slice(index + 1).forEach(otherNodeUi => {
        expect(nodeUi.overlapsWith(otherNodeUi)).to.be.false;
      });
    });
  }
}

module.exports = {of: root => new RootUi(root)};