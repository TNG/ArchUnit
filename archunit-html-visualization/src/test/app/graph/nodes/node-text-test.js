'use strict';

const chai = require('chai');
const expect = chai.expect;
const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const nodeTextFactory = require('../../../../main/app/graph/nodes/node-text');

const visualizationStyles = guiElementsMock.getEmbeddedVisualizationStyles();
const NodeText = nodeTextFactory.init(visualizationStyles);

const withRadius = obj => ({
  withRadius: radius => {
    obj.getRadius = () => radius;
    return obj;
  }
});

const leaf = () => withRadius({isRoot: () => false, isCurrentlyLeaf: () => true});
const nonRootWithChildren = name => withRadius({
  isRoot: () => false,
  isCurrentlyLeaf: () => false,
  getNameWidth: () => name.length * 7
});

describe('NodeText', () => {
  it('should center the text for leafs', () => {
    const text = new NodeText(leaf().withRadius(55));

    expect(text.getY()).to.equal(0)
  });

  it('should position the text on the top rim of the circle for non-roots with children', () => {
    const fontSize = 7;
    visualizationStyles.setNodeFontSize(fontSize);

    const radius = 55;
    const name = 'SomeClass';
    const text = new NodeText(nonRootWithChildren(name).withRadius(radius));

    const xOffset = text._node.getNameWidth() / 2;
    const yOffsetTopBorder = -1 * Math.sqrt(radius * radius - xOffset * xOffset);
    const expectedY = yOffsetTopBorder + fontSize;
    expect(text.getY()).to.equal(expectedY);
  });
});