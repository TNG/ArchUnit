'use strict';

const expect = require('chai').expect;
const visualizationStyles = require('./stubs').visualizationStylesStub();
const withCalculateTextWidth = calculateTextWidth => ({
  newNodeText: node => {
    const NodeText = require('./main-files').get('node-text').init(visualizationStyles, calculateTextWidth);
    return new NodeText(node);
  }
});
const newNodeText = (node) => {
  return withCalculateTextWidth(() => 1).newNodeText(node);
};

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
  getName: () => name,
  getClass: () => 'node'
});

describe('NodeText', () => {
  it('should center the text for leafs', () => {
    const text = newNodeText(leaf().withRadius(55));

    expect(text.getY()).to.equal(0)
  });

  it('should position the text on the very top for the root', () => {
    const fontSize = 13;
    visualizationStyles.setNodeFontSize(fontSize);

    const radius = 25;
    const text = newNodeText(root().withRadius(radius));

    const expectedY = -1 * (radius - fontSize);
    expect(text.getY()).to.equal(expectedY);
  });

  it('should position the text on the top rim of the circle for non-roots with children', () => {
    const fontSize = 7;
    visualizationStyles.setNodeFontSize(fontSize);

    const radius = 55;
    const name = 'SomeClass';
    const calculateTextWidth = text => text.length * 2;
    const text = withCalculateTextWidth(calculateTextWidth)
      .newNodeText(nonRootWithChildren(name).withRadius(radius));

    const xOffset = calculateTextWidth(name) / 2;
    const yOffsetTopBorder = -1 * Math.sqrt(radius * radius - xOffset * xOffset);
    const expectedY = yOffsetTopBorder + fontSize;
    expect(text.getY()).to.equal(expectedY);
  });

  it("should pass the node's CSS class to calculate the text width", () => {
    const node = nonRootWithChildren('SomeClass').withRadius(11);
    const expectedCssClass = 'textCssClass';
    node.getClass = () => expectedCssClass;

    let passedCssClass = null;
    const calculateTextWidth = (text, cssClass) => {
      passedCssClass = cssClass;
      return 1;
    };
    withCalculateTextWidth(calculateTextWidth).newNodeText(node).getY();

    expect(passedCssClass).to.equal(expectedCssClass);
  });
});