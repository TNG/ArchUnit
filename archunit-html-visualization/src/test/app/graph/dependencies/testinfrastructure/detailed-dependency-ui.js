'use strict';

const expect = require('chai').expect;

const fontSize = require('../../testinfrastructure/visualization-styles-mock').createVisualizationStylesMock().getDependencyTitleFontSize();

const sleep = (timeInMs) => {
  return new Promise(resolve => {
    setTimeout(resolve, timeInMs);
  });
};

class DetailedDependencyUi {
  constructor(detailedDependencyView) {
    this._detailedDependencyView = detailedDependencyView;
  }

  get svgElement() {
    return this._detailedDependencyView._svgElement;
  }

  get rectangles() {
    return this.svgElement.getAllVisibleSubElementsOfType('rect');
  }

  get hoverAreaElement() {
    return this.svgElement.getVisibleSubElementByTypeAndCssClasses('rect', 'hoverArea');
  }

  get textElement() {
    return this.svgElement.getVisibleSubElementByTypeAndCssClasses('text', 'access');
  }

  get closeButton() {
    return this.svgElement.getVisibleSubElementByTypeAndCssClasses('text', 'closeButton');
  }

  allLineElements() {
    return this.textElement.getAllVisibleSubElementsOfType('tspan');
  }

  isVisible() {
    return this.svgElement && this.svgElement.isVisible;
  }

  expectToShowDetailedDependencies(detailedDependencies) {
    expect(this.isVisible()).to.be.true;

    const allLines = this.allLineElements().map(lineElement => lineElement.getAttribute('text'));
    expect(allLines).to.deep.equal(detailedDependencies);
  }

  expectToBeHidden() {
    expect(this.isVisible()).to.not.be.true;
  }

  expectLinesToBeLeftAligned() {
    const allLineElements = this.allLineElements();
    const xPositionOfFirstLineElement = allLineElements[0].absolutePosition.x;
    allLineElements.forEach(lineElement => {
      expect(lineElement.absolutePosition.x).to.equal(xPositionOfFirstLineElement);
    });
  }

  expectRectangleToLieBehindTheLines(rect) {
    const lineElements = this.allLineElements();
    const rectPosition = rect.absolutePosition;

    lineElements.forEach(lineElement => {
      const linePosition = lineElement.absolutePosition;
      expect(rectPosition.x).to.be.at.most(linePosition.x);
      expect(rectPosition.x + rect.width).to.be.at.least(linePosition.x + lineElement.textWidth);

      expect(rectPosition.y).to.be.at.most(linePosition.y - fontSize);
      expect(rectPosition.y + rect.getAttribute('height')).to.be.at.least(linePosition.y);
    });
  }

  async moveMouseOutAndWaitFor(timeInMs) {
    this.moveMouseOut();
    await sleep(timeInMs);
  }

  moveMouseOut() {
    this.hoverAreaElement.mouseOut();
  }

  click() {
    this.hoverAreaElement.click();
  }

  drag(dx, dy) {
    this.svgElement.drag(dx, dy);
  }
}

module.exports.DetailedDependencyUi = {of: detailedDependencyView => new DetailedDependencyUi(detailedDependencyView)};