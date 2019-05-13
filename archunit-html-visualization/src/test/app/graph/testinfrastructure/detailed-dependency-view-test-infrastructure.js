'use strict';

const expect = require('chai').expect;

const sleep = (timeInMs) => {
  return new Promise(resolve => {
    setTimeout(resolve, timeInMs);
  });
};

const checkThat = (svgGroup) => ({
  containsExactlyDetailedDependencies: (...detailedDependencies) => {
    const svgTextElements = svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('text', 'access');

    expect(svgTextElements.length).to.equal(detailedDependencies.length);
    svgTextElements.forEach((svgTextElement, i) => {
      const tspanElements = svgTextElement.getAllVisibleSubElementsOfType('tspan');
      const allLines = tspanElements.map(tspan => tspan.getAttribute('text'));
      expect(allLines).to.deep.equal(detailedDependencies[i]);
    });
  },
  containsNoDetailedDependencies: () => {
    const svgTextElements = svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('text', 'access');
    expect(svgTextElements).to.be.empty;
  }
});

const checkLayoutOn = (svgGroup, fontSize, textPadding) => ({
  that: {
    linesAreLeftAligned: () => {
      const tspanElements = svgGroup.getVisibleDescendantElementByTypeAndCssClasses('text', 'access')
        .getAllVisibleDescendantElementsOfType('tspan');
      const xPositionOfFirstTSpan = tspanElements[0].absolutePosition.x;
      tspanElements.forEach(tspanElement => {
        expect(tspanElement.absolutePosition.x).to.equal(xPositionOfFirstTSpan);
      });
    },
    backgroundRectanglesLieExactlyBehindTheLines: () => {
      const tspanElements = svgGroup.getVisibleDescendantElementByTypeAndCssClasses('text', 'access')
        .getAllVisibleDescendantElementsOfType('tspan');
      const positionOfTSpan = tspanElements[0].absolutePosition;
      const heightOfTSpans = (fontSize + textPadding) * tspanElements.length;
      const rects = svgGroup.getAllVisibleDescendantElementsOfType('rect');

      rects.forEach(rect => {
        expect(rect.getAttribute('height')).to.equal(heightOfTSpans + 2 * textPadding);
        expect(rect.absolutePosition.x).to.equal(positionOfTSpan.x - textPadding);
        expect(rect.absolutePosition.y).to.be.at.most(positionOfTSpan.y - textPadding - fontSize);

        tspanElements.forEach(tspanElement => {
          expect(rect.width).to.be.at.least(tspanElement.textWidth + 2 * textPadding + fontSize);
        });
      });
    }
  }
});

const interactOn = (svgGroup) => ({
  withDetailedDependenciesAt: (index) => ({
    click: () => {
      svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('rect', 'hoverArea')[index].click();
    },
    clickOnCloseButton: () => {
      svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('text', 'closeButton')[index].click();
    },
    moveMouseOut: () => {
      svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('rect', 'hoverArea')[index].mouseOut();
    },
    moveMouseOutAndWaitFor: async (timeInMs) => {
      svgGroup.getAllVisibleDescendantElementsByTypeAndCssClasses('rect', 'hoverArea')[index].mouseOut();
      await sleep(timeInMs);
    },
    drag: (dx, dy) => {
      svgGroup.getAllGroupsContainingAVisibleElementOfType('rect')[index].drag(dx, dy);
    }
  })
});

module.exports = {
  checkThat,
  interactOn,
  checkLayoutOn
};