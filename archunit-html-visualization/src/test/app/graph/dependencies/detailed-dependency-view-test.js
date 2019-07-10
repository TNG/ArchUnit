'use strict';

const expect = require('chai').expect;

const svgMock = require('../testinfrastructure/svg-mock');

const fontSize = require('../testinfrastructure/visualization-styles-mock').createVisualizationStylesMock().getDependencyTitleFontSize();

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const transitionDuration = 5;
const textPadding = 5;
const AppContext = require('../../../../main/app/graph/app-context');
const DetailedDependencyView = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDetailedDependencyView();

const DetailedDependencyUi = require('./testinfrastructure/detailed-dependency-ui').DetailedDependencyUi;

const createDetailedDependencyViews = (svgContainer, getContainerWidth, ...getAllDetailedDependencies) => {
  const detailedDependencyViews = getAllDetailedDependencies.map(getDetailedDependencies =>
    new DetailedDependencyView(svgContainer, getContainerWidth, callback => detailedDependencyViews.forEach(callback), getDetailedDependencies));
  return detailedDependencyViews;
};

const createSingleDetailedDependencyView = (svgContainer, getContainerWidth, getDetailedDependency) => {
  return createDetailedDependencyViews(svgContainer, getContainerWidth, getDetailedDependency)[0];
};

const sleep = (timeInMs) => {
  return new Promise(resolve => {
    setTimeout(resolve, timeInMs);
  });
};

const fadeInAndAwait = async (detailedDependencyView) => {
  detailedDependencyView.fadeIn();
  await sleep(transitionDuration);
};

const fadeOutAndAwait = async (detailedDependencyView) => {
  detailedDependencyView.fadeOut();
  await sleep(transitionDuration);
};

const getFirstDetailedDependencies = () => ([
  'Method <my.company.FirstStartClass.startMethod(my.company.SomeParamType)> ' +
  'calls method <my.company.FirstTargetClass.someTargetMetod(my.company.OtherParamType)> in (my.company.FirstStartClass.java:30)',
  'Method <my.company.SecondStartClass.startMethod(my.company.SomeParamType)> ' +
  'gets field <my.company.SecondTargetClass.targetField> in (my.company.SecondStartClass.java:40)',
  'Method <my.company.ThirdStartClass.startMethod(my.company.SomeParamType)> ' +
  'has parameter of type <my.company.ThirdTargetClass> in (my.company.ThirdStartClass.java:50)'
]);

const getSecondDetailedDependencies = () => [
  'Method <my.company.SomeClass.someMethod()> ' +
  'calls method <my.company.TargetClass.someMethod(my.company.SomeParamType)> in (my.company.SomeClass.java:30)'
];

const getThirdDetailedDependencies = () => [
  'Method <my.company.OtherClass.otherMethod()> ' +
  'calls method <my.company.TargetClass.otherMethod(my.company.OtherParamType)> in (my.company.OtherClass.java:30)'
];

describe('DetailedDependencyView', () => {

  let htmlSvgElement;
  let svgGroup;
  let detailedDependencies;
  const getContainerWidth = () => htmlSvgElement.width;

  beforeEach(() => {
    htmlSvgElement = svgMock.createEmptyElement();
    svgMock.createSvgRoot().addChild(htmlSvgElement);
    svgGroup = htmlSvgElement.addGroup();
    detailedDependencies = getFirstDetailedDependencies();
  });

  describe('public methods of DetailedDependencyView', () => {
    describe('#fadeIn()', () => {
      it('shows all detailed dependencies', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        await fadeInAndAwait(detailedDependencyView);
        DetailedDependencyUi.of(detailedDependencyView).expectToShowDetailedDependencies(detailedDependencies);
      });

      it('shows the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        detailedDependencyView.fadeIn();
        DetailedDependencyUi.of(detailedDependencyView).expectToBeHidden();
      });

      it('hides other detailed dependencies, that are currently displayed', async () => {
        const secondDetailedDependencies = [
          'Method <my.company.SomeClass.someMethod()> ' +
          'calls method <my.company.TargetClass.otherMethod(my.company.OtherParamType)> in (my.company.SomeClass.java:30)'
        ];
        const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
          () => secondDetailedDependencies);

        await fadeInAndAwait(detailedDependencyViews[0]);
        await fadeInAndAwait(detailedDependencyViews[1]);

        DetailedDependencyUi.of(detailedDependencyViews[0]).expectToBeHidden();
        DetailedDependencyUi.of(detailedDependencyViews[1]).expectToShowDetailedDependencies(secondDetailedDependencies);
      });

      it('shows only the latest detailed dependencies, if it is called right after each other', async () => {
        const secondDetailedDependencies = getSecondDetailedDependencies();
        const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
          () => secondDetailedDependencies);

        detailedDependencyViews[0].fadeIn();
        await fadeInAndAwait(detailedDependencyViews[1]);

        DetailedDependencyUi.of(detailedDependencyViews[0]).expectToBeHidden();
        DetailedDependencyUi.of(detailedDependencyViews[1]).expectToShowDetailedDependencies(secondDetailedDependencies);
      });

      it('shows the new detailed dependencies when it is called again and the detailed dependencies have changed', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);

        detailedDependencies.push('Method <my.company.FourthStartClass.startMethod(my.company.SomeParamType)> ' +
          'calls method <my.company.FourthTargetClass> in (my.company.FourthStartClass.java:60)');
        await fadeInAndAwait(detailedDependencyView);

        DetailedDependencyUi.of(detailedDependencyView).expectToShowDetailedDependencies(detailedDependencies);
      });

      describe('the detailed dependencies are put to a position, so that', () => {
        it('their left side is visible if they fit into the width of the svg', async () => {
          htmlSvgElement.dimension = {width: 800, height: 300};
          svgGroup.setMousePosition(10, 40);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);

          DetailedDependencyUi.of(detailedDependencyView).allLineElements().forEach(lineElement => {
            expect(lineElement.absolutePosition.x).to.be.at.least(0);
          });
        });

        it('their right side is visible', async () => {
          const width = 400;
          htmlSvgElement.dimension = {width, height: 200};
          svgGroup.setMousePosition(390, 40);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          DetailedDependencyUi.of(detailedDependencyView).allLineElements().forEach(lineElement => {
            expect(lineElement.absolutePosition.x + lineElement.textWidth).to.be.at.most(width);
          });
        });
      });

      describe('the detailed dependencies are layouted so that', () => {
        it('they are left aligned', async () => {
          htmlSvgElement.dimension = {width: 400, height: 200};
          svgGroup.setMousePosition(200, 100);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          DetailedDependencyUi.of(detailedDependencyView).expectLinesToBeLeftAligned();
        });

        it('the detailed dependencies are displayed in front of a rectangle', async () => {
          htmlSvgElement.dimension = {width: 500, height: 200};
          svgGroup.setMousePosition(200, 100);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
          detailedDependencyUi.rectangles.forEach(rect => detailedDependencyUi.expectRectangleToLieBehindTheLines(rect));
        });
      });
    });

    describe('#fadeOut()', () => {
      it('hides all detailed dependencies again', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        await fadeOutAndAwait(detailedDependencyView);

        DetailedDependencyUi.of(detailedDependencyView).expectToBeHidden();
      });

      it('hides the detailed dependencies, if it is called immediately after a #fadeIn()', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        detailedDependencyView.fadeIn();
        await fadeOutAndAwait(detailedDependencyView);
        DetailedDependencyUi.of(detailedDependencyView).expectToBeHidden();
      });

      it('hides the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        detailedDependencyView.fadeOut();
        DetailedDependencyUi.of(detailedDependencyView).expectToShowDetailedDependencies(detailedDependencies);
      });
    });
  });

  describe('user interaction with the detailed dependency view:', () => {
    describe('leaving the detailed dependencies view with the mouse', () => {
      it('hides the detailed dependencies', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);

        const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
        await detailedDependencyUi.moveMouseOutAndWaitFor(transitionDuration);
        detailedDependencyUi.expectToBeHidden();
      });

      it('hides the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);

        const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
        detailedDependencyUi.moveMouseOut();
        detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);
      });
    });

    describe('clicking on the detailed dependencies shows them permanently:', () => {
      describe('the clicked detailed dependencies view is not hidden through the usual ways', () => {
        it('leaving the detailed dependencies with the mouse does not hide them', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);

          const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
          detailedDependencyUi.click();
          await detailedDependencyUi.moveMouseOutAndWaitFor(transitionDuration);
          detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);
        });

        it('invoking #fadeOut() does not hide them', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
          detailedDependencyUi.click();

          await fadeOutAndAwait(detailedDependencyView);
          detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);
        });

        it('calling #fadeIn() on an other detailed dependency view does not hide them', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies);
          const detailedDependencyUis = detailedDependencyViews.map(detailedDependencyView => DetailedDependencyUi.of(detailedDependencyView));

          await fadeInAndAwait(detailedDependencyViews[0]);
          detailedDependencyUis[0].click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          detailedDependencyUis[0].expectToShowDetailedDependencies(detailedDependencies);
          detailedDependencyUis[1].expectToShowDetailedDependencies(secondDetailedDependencies);
        });

        it('calling #fadeIn() on the same detailed dependency view does not change anything', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
          const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);

          await fadeInAndAwait(detailedDependencyView);
          detailedDependencyUi.click();

          //does not re-create the detailed dependencies
          await fadeInAndAwait(detailedDependencyView);
          detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);

          //does not change the fix-state
          await fadeOutAndAwait(detailedDependencyView);
          detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);

          //does not hide the close button
          detailedDependencyUi.closeButton.click();
          detailedDependencyUi.expectToBeHidden();
        });
      });

      it('a close button is shown in the upper right corner', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);

        const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
        detailedDependencyUi.click();

        const closeButton = detailedDependencyUi.closeButton;
        const closeButtonPosition = closeButton.absolutePosition;

        detailedDependencyUi.rectangles.forEach(rect => {
          const rectPosition = rect.absolutePosition;
          expect(closeButtonPosition.x).to.equal(rectPosition.x + rect.width - closeButton.textWidth / 2);
          expect(closeButtonPosition.y).to.equal(rectPosition.y + fontSize);
        });
      });

      it('clicking on the close button hides the fixed detailed dependencies again', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        const detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);

        await fadeInAndAwait(detailedDependencyView);
        detailedDependencyUi.click();

        detailedDependencyUi.closeButton.click();
        detailedDependencyUi.expectToBeHidden();
      });

      describe('several detailed dependencies views can be shown permanently:', () => {
        it('all the clicked detailed dependencies views are displayed at the same time', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const thirdDetailedDependencies = getThirdDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies, () => thirdDetailedDependencies);
          const detailedDependencyUis = detailedDependencyViews.map(detailedDependencyView => DetailedDependencyUi.of(detailedDependencyView));

          await fadeInAndAwait(detailedDependencyViews[0]);
          detailedDependencyUis[0].click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          detailedDependencyUis[1].click();

          await fadeInAndAwait(detailedDependencyViews[2]);
          detailedDependencyUis[2].click();

          detailedDependencyUis[0].expectToShowDetailedDependencies(detailedDependencies);
          detailedDependencyUis[1].expectToShowDetailedDependencies(secondDetailedDependencies);
          detailedDependencyUis[2].expectToShowDetailedDependencies(thirdDetailedDependencies);
        });

        it('all fixed detailed dependencies views can be closed via their close button again', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const thirdDetailedDependencies = getThirdDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies, () => thirdDetailedDependencies);
          const detailedDependencyUis = detailedDependencyViews.map(detailedDependencyView => DetailedDependencyUi.of(detailedDependencyView));

          await fadeInAndAwait(detailedDependencyViews[0]);
          detailedDependencyUis[0].click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          detailedDependencyUis[1].click();

          await fadeInAndAwait(detailedDependencyViews[2]);
          detailedDependencyUis[2].click();

          detailedDependencyUis[0].closeButton.click();
          detailedDependencyUis[0].expectToBeHidden();
          detailedDependencyUis[1].expectToShowDetailedDependencies(secondDetailedDependencies);
          detailedDependencyUis[2].expectToShowDetailedDependencies(thirdDetailedDependencies);

          detailedDependencyUis[1].closeButton.click();
          detailedDependencyUis[0].expectToBeHidden();
          detailedDependencyUis[1].expectToBeHidden();
          detailedDependencyUis[2].expectToShowDetailedDependencies(thirdDetailedDependencies);

          detailedDependencyUis[2].closeButton.click();
          detailedDependencyUis[0].expectToBeHidden();
          detailedDependencyUis[1].expectToBeHidden();
          detailedDependencyUis[2].expectToBeHidden();
        });
      });
    });

    describe('Dragging the detailed dependencies', () => {
      let detailedDependencyView;
      let detailedDependencyUi;
      let positionBefore;
      beforeEach(async () => {
        detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        detailedDependencyUi = DetailedDependencyUi.of(detailedDependencyView);
        await fadeInAndAwait(detailedDependencyView);
        positionBefore = detailedDependencyUi.textElement.absolutePosition;
        detailedDependencyUi.drag(20, 20);
      });

      it('moves them ', async () => {
        const positionAfterDragging = detailedDependencyUi.textElement.absolutePosition;
        const expectedPosition = {x: positionBefore.x + 20, y: positionBefore.y + 20};
        expect(positionAfterDragging).to.deep.equal(expectedPosition);
      });

      it('keeps the layout correctly', async () => {
        detailedDependencyUi.expectLinesToBeLeftAligned();
        detailedDependencyUi.rectangles.forEach(rect => detailedDependencyUi.expectRectangleToLieBehindTheLines(rect));
      });

      describe('shows them permanently', () => {
        it('leaving the detailed dependencies with the mouse does not hide them', async () => {
          await detailedDependencyUi.moveMouseOutAndWaitFor(transitionDuration);
          detailedDependencyUi.expectToShowDetailedDependencies(detailedDependencies);
        });

        it('a close button is shown in the upper right corner', async () => {
          const closeButton = detailedDependencyUi.closeButton;
          const closeButtonPosition = closeButton.absolutePosition;

          detailedDependencyUi.rectangles.forEach(rect => {
            const rectPosition = rect.absolutePosition;
            expect(closeButtonPosition.x).to.equal(rectPosition.x + rect.width - closeButton.textWidth / 2);
            expect(closeButtonPosition.y).to.equal(rectPosition.y + fontSize);
          });
        });

        it('clicking on the close button hides the detailed dependencies again', () => {
          detailedDependencyUi.closeButton.click();
          detailedDependencyUi.expectToBeHidden();
        });
      });
    });
  })
});