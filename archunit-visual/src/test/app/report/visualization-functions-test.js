'use strict';

const expect = require("chai").expect;

const visualizationFunctionsFactory = require('./main-files').get('visualization-functions');
const visualizationFunctions = visualizationFunctionsFactory.newInstance(() => () => 1);
const packCirclesAndReturnEnclosingCircle = visualizationFunctions.packCirclesAndReturnEnclosingCircle;
const Vector = require('./main-files').get('vectors').Vector;

const twoElementSubSets = arr => {
  if (arr.length < 2) {
    return [];
  }
  const first = arr.shift();
  const result = twoElementSubSets(Array.from(arr));
  arr.map(elem => [first, elem]).forEach(subset => result.push(subset));
  return result;
};

const expectNoOverlapBetween = circles => {
  const circlePairs = twoElementSubSets(circles);
  circlePairs.forEach(([firstCircle, secondCircle]) =>
    expect(Vector.between(firstCircle, secondCircle).length()).to.at.least(firstCircle.r + secondCircle.r));
};

const maxDistanceBetweenEnclosedAndEnclosingCircleCenter =
  (enclosed, enclosingCircle) => Vector.between(enclosed, enclosingCircle).length() + enclosed.r;

describe('Circle packing', () => {
  it('should add non overlapping x- and y- coordinates to the supplied circles', () => {
    const circles = [{x: 0, y: 0, r: 3}, {x: 0, y: 0, r: 4}, {x: 0, y: 0, r: 5}];

    packCirclesAndReturnEnclosingCircle(circles, 1);

    expectNoOverlapBetween(circles);
  });

  it('should return a circle enclosing all circles', () => {
    const circles = [{x: 0, y: 0, r: 3}, {x: 0, y: 0, r: 4}, {x: 0, y: 0, r: 5}];

    const enclosingCircle = packCirclesAndReturnEnclosingCircle(circles, 1);

    circles.forEach(enclosed => {
      expect(maxDistanceBetweenEnclosedAndEnclosingCircleCenter(enclosed, enclosingCircle)).to.be.at.most(enclosingCircle.r);
    });
  });

  it('should add the passed padding', () => {
    const circles = [{x: 0, y: 0, r: 3}, {x: 0, y: 0, r: 4}];

    let padding = 0;
    let enclosingCircle = packCirclesAndReturnEnclosingCircle(circles, padding);
    expect(Vector.between(circles[0], circles[1]).length()).to.equal(circles[0].r + circles[1].r);
    const oldEnclosingCircleRadius = enclosingCircle.r;

    padding = 10;
    enclosingCircle = packCirclesAndReturnEnclosingCircle(circles, padding);
    const expectedDistance = circles[0].r + circles[1].r + 2 * padding;
    expect(Vector.between(circles[0], circles[1]).length()).to.equal(expectedDistance);

    expect(enclosingCircle.r).to.be.at.least(oldEnclosingCircleRadius + padding);
  });
});

const nodeWithoutChildren = nodeName => newNode(nodeName, 0);
const nodeWithChildren = nodeName => newNode(nodeName, 1);
const newNode = (nodeName, numberOfChildren) => {
  return {
    getName: () => nodeName,
    getOriginalChildren: () => new Array(numberOfChildren).fill().map(() => ({}))
  };
};

const expectedDefaultRadius = 40;
const expectedTextPadding = 5;

describe('Calculate default radius', () => {
  it(`should simply adjust the size to the node text for original leafs and add ${expectedTextPadding}px of padding`, () => {
    const nodeName = 'Short';
    const calculateTextWidth = text => text.length * 3;
    const calculateDefaultRadius = visualizationFunctionsFactory.newInstance(calculateTextWidth).calculateDefaultRadius;

    const radius = calculateDefaultRadius(nodeWithoutChildren(nodeName));

    const radiusForText = calculateTextWidth(nodeName) / 2;
    expect(radius).to.be.lessThan(expectedDefaultRadius);
    expect(radius).to.equal(radiusForText + expectedTextPadding);
  });

  it(`should adjust the size same as for leafs, but enforce a minimum of ${expectedDefaultRadius}px`, () => {
    const calculateTextWidth = text => text.length * 3;
    const calculateDefaultRadius = visualizationFunctionsFactory.newInstance(calculateTextWidth).calculateDefaultRadius;

    let radius = calculateDefaultRadius(nodeWithChildren('Short'));

    expect(radius).to.equal(expectedDefaultRadius);

    const nodeName = 'TwentyThreeCharsFillThis';
    radius = calculateDefaultRadius(nodeWithChildren(nodeName));

    const radiusForText = calculateTextWidth(nodeName) / 2;
    const expectedWidth = radiusForText + expectedTextPadding;
    expect(expectedWidth).to.equal(expectedDefaultRadius + 1);
    expect(radius).to.equal(expectedWidth);
  });
});