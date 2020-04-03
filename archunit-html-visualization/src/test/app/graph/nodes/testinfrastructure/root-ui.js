'use strict';

const chai = require('chai');
const chaiExtensions = require('../../testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);
const expect = chai.expect;

const Vector = require('../../../../../main/app/graph/infrastructure/vectors').Vector;
const visualizationStyles = require('../../testinfrastructure/root-creator').getVisualizationStyles();

const MAXIMUM_PADDING_DELTA = 1;

/**
 * Adapter for the UIs of nodes. Only the visible nodes (i.e. whose svg group is visible) are exposed, even if all nodes are handled here.
 * To create the node-uis, the constructor of the root-ui goes through all nodes of the given root using getOriginalChildren() of each node.
 * Then for each node the svg element, where the nodes is drawn in, is accessed over node._view._svgElement. All other svg sub elements are not
 * accessed via the node's view.
 */
class RootUi {
  constructor(root) {
    this._root = root;
    this._nodeUIs = new Map();
    const createNodeUi = (node, parentUi, rootUi) => {
      const nodeUi = new NodeUi(node, parentUi, rootUi);
      nodeUi._childUis = node.getOriginalChildren().map(child => createNodeUi(child, nodeUi, rootUi));
      this._nodeUIs.set(nodeUi._node.getFullName(), nodeUi);
      return nodeUi;
    };
    this._childUis = root.getOriginalChildren().map(child => createNodeUi(child, this, this));
  }

  allNodes() {
    return [...this._nodeUIs.values()].filter(nodeUi => nodeUi._svg.isVisible);
  }

  nodesWithSingleChild() {
    return this.allNodes().filter(nodeUi => nodeUi.childUis.length === 1);
  }

  leafNodes() {
    return this.allNodes().filter(nodeUi => nodeUi.childUis.length === 0);
  }

  nonLeafNodes() {
    return this.allNodes().filter(nodeUi => nodeUi.childUis.length > 0);
  }

  nodeByFullName(nodeFullName) {
    const result = this._nodeUIs.get(nodeFullName);
    if (!result._svg.isVisible) {
      throw new Error('the required node exists but is not visible');
    }
    return result;
  }

  _isRoot() {
    return true;
  }

  contains() {
    return true;
  }

  get childUis() {
    return this._childUis.filter(nodeUi => nodeUi._svg.isVisible);
  }

  isInForeground() {
    return true;
  }

  expectToHaveLeafFullNames(...leafFullNames) {
    expect(this.leafNodes().map(nodeUi => nodeUi._node.getFullName())).to.have.members(leafFullNames);
  }

  checkWholeLayout() {
    this.allNodes().forEach(nodeUi => {
      nodeUi.expectToBeWithin(nodeUi.parent);
      nodeUi.expectNotToOverlapWith(nodeUi.siblings);
      nodeUi.expectToHaveLabelWithinCircle();
    });

    this.nonLeafNodes().forEach(nodeUi => nodeUi.expectToHaveLabelAtTheTop());

    this.leafNodes().forEach(nodeUi => nodeUi.expectToHaveLabelInTheMiddleOfCircle());

    this.nodesWithSingleChild().forEach(nodeUi => nodeUi.expectToHaveLabelAbove(nodeUi.childUis[0]));
  }
}

class NodeUi {
  constructor(node, parentUi, rootUi) {
    this._node = node;
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

  get childUis() {
    return this._childUis.filter(nodeUi => nodeUi._svg.isVisible);
  }

  get siblings() {
    return this._parentUi.childUis.filter(nodeUi => nodeUi !== this);
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

  ctrlClick() {
    this._circleSvg.click({ctrlKey: true});
  }

  async ctrlClickAndAwait() {
    this.ctrlClick();
    await this._rootUi._root._updatePromise;
  }

  click() {
    this._circleSvg.click({ctrlKey: false});
  }

  async clickAndAwait() {
    this.click();
    await this._rootUi._root._updatePromise;
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

  expectNotToOverlapWith(nodeUis) {
    nodeUis.forEach(nodeUi => expect(this.overlapsWith(nodeUi)).to.be.false);
  }

  expectToBeFoldable() {
    expect([...this._svg.cssClasses]).to.include('foldable');
    expect([...this._svg.cssClasses]).not.to.include('unfoldable');
  }

  expectToBeUnfoldable() {
    expect([...this._svg.cssClasses]).to.include('unfoldable');
    expect([...this._svg.cssClasses]).not.to.include('foldable');
  }

  liesInFrontOf(otherNodeFullName) {
    return this._svg.isInFrontOf(this._rootUi._nodeUIs.get(otherNodeFullName)._svg);
  }
}

module.exports = {of: root => new RootUi(root)};
