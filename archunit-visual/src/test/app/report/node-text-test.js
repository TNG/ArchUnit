'use strict';

const expect = require('chai').expect;
const visualizationStyles = require('./stubs').visualizationStylesStub();
const NodeText = require('./main-files').get('node-text').init(visualizationStyles);

const withRadius = obj => ({
  withRadius: radius => {
    obj.getRadius = () => radius;
    return obj;
  }
});

const leaf = () => withRadius({isRoot: () => false, isCurrentlyLeaf: () => true});
const root = () => withRadius({isRoot: () => true, isCurrentlyLeaf: () => false});
const nonRootWithChildren = name => withRadius({
  isRoot: () => false,
  isCurrentlyLeaf: () => false,
  getClass: () => 'node',
  getNameWidth: () => name.length * 7
});

describe('NodeText', () => {
  it('should center the text for leafs', () => {
    const text = new NodeText(leaf().withRadius(55));

    expect(text.getY()).to.equal(0)
  });

  it('should position the text on the very top for the root', () => {
    const fontSize = 13;
    visualizationStyles.setNodeFontSize(fontSize);

    const radius = 25;
    const text = new NodeText(root().withRadius(radius));

    const expectedY = -1 * (radius - fontSize);
    expect(text.getY()).to.equal(expectedY);
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