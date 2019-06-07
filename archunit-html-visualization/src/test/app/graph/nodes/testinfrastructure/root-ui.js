const expect = require('chai').expect;
const Vector = require('../../../../../main/app/graph/infrastructure/vectors').Vector;

class RootUi {
  constructor(root) {
    this._nodeUIs = [];
    const createNodeUi = (node, parentUi) => {
      const nodeUi = new NodeUi(node, parentUi);
      node.getOriginalChildren().forEach(child => createNodeUi(child, nodeUi));
      this._nodeUIs.push(nodeUi);
    };
    root.getOriginalChildren().forEach(child => createNodeUi(child, this));
  }

  allNodes() {
    return this._nodeUIs;
  }

  isRoot() {
    return true;
  }

  contains() {
    return true;
  }
}

class NodeUi {
  constructor(node, parentUi) {
    this._parentUi = parentUi;
    this._svg = node._view._svgElement;
    this._circleSvg = this._svg.getVisibleSubElementOfType('circle');
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

  isRoot() {
    return false;
  }

  contains(otherNodeUi) {
    if (otherNodeUi.isRoot()) {
      return false;
    }

    const centerDistance = Vector.between(this.absolutePosition, otherNodeUi.absolutePosition).length();
    const maxDistance = centerDistance + otherNodeUi.radius;
    return maxDistance <= this.radius;
  }

  expectToBeWithin(otherNodeUi) {
    expect(otherNodeUi.contains(this)).to.be.true;
  }
}

module.exports = {of: root => new RootUi(root)};