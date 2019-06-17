'use strict';

const expect = require('chai').expect;

const svgMock = require('../testinfrastructure/svg-mock');
const fontSize = require('../testinfrastructure/visualization-styles-mock').createVisualizationStylesMock().getDependencyTitleFontSize();

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const transitionDuration = 5;
const textPadding = 5;
const AppContext = require('../../../../main/app/graph/app-context');
const DetailedDependencyView = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDetailedDependencyView();

const checkLayoutOn = require('../testinfrastructure/detailed-dependency-gui-adapter').checkLayoutOn;
const checkThat = require('../testinfrastructure/detailed-dependency-gui-adapter').checkThat;
const interactOn = require('../testinfrastructure/detailed-dependency-gui-adapter').interactOn;
const inspect = require('../testinfrastructure/detailed-dependency-gui-adapter').inspect;

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
    htmlSvgElement = svgMock.select();
    svgGroup = htmlSvgElement.addGroup();
    detailedDependencies = getFirstDetailedDependencies();
  });

  describe('public methods of DetailedDependencyView', () => {
    describe('#fadeIn()', () => {
      it('shows all detailed dependencies', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        await fadeInAndAwait(detailedDependencyView);
        checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
      });

      it('shows the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        detailedDependencyView.fadeIn();
        checkThat(svgGroup).containsNoDetailedDependencies();
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
        checkThat(svgGroup).containsExactlyDetailedDependencies(secondDetailedDependencies);
      });

      it('shows only the latest detailed dependencies, if it is called right after each other', async () => {
        const secondDetailedDependencies = getSecondDetailedDependencies();
        const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
          () => secondDetailedDependencies);

        detailedDependencyViews[0].fadeIn();
        await fadeInAndAwait(detailedDependencyViews[1]);
        checkThat(svgGroup).containsExactlyDetailedDependencies(secondDetailedDependencies);
      });

      it('shows the new detailed dependencies when it is called again and the detailed dependencies have changed', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);

        detailedDependencies.push('Method <my.company.FourthStartClass.startMethod(my.company.SomeParamType)> ' +
          'calls method <my.company.FourthTargetClass> in (my.company.FourthStartClass.java:60)');
        await fadeInAndAwait(detailedDependencyView);
        checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
      });

      describe('the detailed dependencies are put to a position, so that', () => {
        it('their left side is visible if they fit into the width of the svg', async () => {
          htmlSvgElement.dimension = {width: 800, height: 300};
          svgGroup.setMousePosition(10, 40);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          const tspanElements = svgGroup.getVisibleDescendantElementOfType('text').getAllVisibleDescendantElementsOfType('tspan');
          tspanElements.forEach(tspanElement => {
            expect(tspanElement.absolutePosition.x).to.be.at.least(0);
          });
        });

        it('their right side is visible', async () => {
          const width = 400;
          htmlSvgElement.dimension = {width, height: 200};
          svgGroup.setMousePosition(390, 40);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          const tspanElements = svgGroup.getVisibleDescendantElementOfType('text').getAllVisibleDescendantElementsOfType('tspan');
          tspanElements.forEach(tspanElement => {
            expect(tspanElement.absolutePosition.x + tspanElement.textWidth).to.be.at.most(width);
          });
        });
      });

      describe('the detailed dependencies are layouted so that', () => {
        it('they are left aligned', async () => {
          htmlSvgElement.dimension = {width: 400, height: 200};
          svgGroup.setMousePosition(200, 100);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          checkLayoutOn(svgGroup, fontSize, textPadding).that.linesAreLeftAligned();
        });

        it('the detailed dependencies are displayed in front of a rectangle', async () => {
          htmlSvgElement.dimension = {width: 500, height: 200};
          svgGroup.setMousePosition(200, 100);
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          checkLayoutOn(svgGroup, fontSize, textPadding).that.backgroundRectanglesLieExactlyBehindTheLines();
        });
      });
    });

    describe('#fadeOut()', () => {
      it('hides all detailed dependencies again', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        await fadeOutAndAwait(detailedDependencyView);
        checkThat(svgGroup).containsNoDetailedDependencies();
      });

      it('hides the detailed dependencies, if it is called immediately after a #fadeIn()', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        detailedDependencyView.fadeIn();
        await fadeOutAndAwait(detailedDependencyView);
        checkThat(svgGroup).containsNoDetailedDependencies();
      });

      it('hides the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        detailedDependencyView.fadeOut();
        checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
      });
    });
  });

  describe('user interaction with the detailed dependency view:', () => {
    describe('leaving the detailed dependencies view with the mouse', () => {
      it('hides the detailed dependencies', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        await interactOn(svgGroup).withDetailedDependenciesAt(0).moveMouseOutAndWaitFor(transitionDuration);
        checkThat(svgGroup).containsNoDetailedDependencies();
      });

      it('hides the detailed dependencies only after a certain time', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        interactOn(svgGroup).withDetailedDependenciesAt(0).moveMouseOut();
        checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
      });
    });

    describe('clicking on the detailed dependencies shows them permanently:', () => {
      describe('the clicked detailed dependencies view is not hidden through the usual ways', () => {
        it('leaving the detailed dependencies with the mouse does not hide them', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          await interactOn(svgGroup).withDetailedDependenciesAt(0).moveMouseOutAndWaitFor(transitionDuration);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
        });

        it('invoking #fadeOut() does not hide them', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          await fadeOutAndAwait(detailedDependencyView);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
        });

        it('calling #fadeIn() on an other detailed dependency view does not hide them', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies);

          await fadeInAndAwait(detailedDependencyViews[0]);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies, secondDetailedDependencies);
        });

        it('calling #fadeIn() on the same detailed dependency view does not change anything', async () => {
          const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

          await fadeInAndAwait(detailedDependencyView);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          //does not re-create the detailed dependencies
          await fadeInAndAwait(detailedDependencyView);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);

          //does not change the fix-state
          await fadeOutAndAwait(detailedDependencyView);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);

          //does not hide the close button
          interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
          checkThat(svgGroup).containsNoDetailedDependencies();
        });
      });

      it('clicking on the close button hides the fixed detailed dependencies again', async () => {
        const detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);

        await fadeInAndAwait(detailedDependencyView);
        interactOn(svgGroup).withDetailedDependenciesAt(0).click();

        interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
        checkThat(svgGroup).containsNoDetailedDependencies();
      });

      describe('several detailed dependencies views can be shown permanently:', () => {
        it('all the clicked detailed dependencies views are displayed at the same time', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const thirdDetailedDependencies = getThirdDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies, () => thirdDetailedDependencies);

          await fadeInAndAwait(detailedDependencyViews[0]);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          interactOn(svgGroup).withDetailedDependenciesAt(1).click();

          await fadeInAndAwait(detailedDependencyViews[2]);
          interactOn(svgGroup).withDetailedDependenciesAt(2).click();

          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies, secondDetailedDependencies, thirdDetailedDependencies);
        });

        it('all fixed detailed dependencies views can be closed via their close button again', async () => {
          const secondDetailedDependencies = getSecondDetailedDependencies();
          const thirdDetailedDependencies = getThirdDetailedDependencies();
          const detailedDependencyViews = createDetailedDependencyViews(svgGroup, getContainerWidth, () => detailedDependencies,
            () => secondDetailedDependencies, () => thirdDetailedDependencies);

          await fadeInAndAwait(detailedDependencyViews[0]);
          interactOn(svgGroup).withDetailedDependenciesAt(0).click();

          await fadeInAndAwait(detailedDependencyViews[1]);
          interactOn(svgGroup).withDetailedDependenciesAt(1).click();

          await fadeInAndAwait(detailedDependencyViews[2]);
          interactOn(svgGroup).withDetailedDependenciesAt(2).click();

          interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
          checkThat(svgGroup).containsExactlyDetailedDependencies(secondDetailedDependencies, thirdDetailedDependencies);

          interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
          checkThat(svgGroup).containsExactlyDetailedDependencies(thirdDetailedDependencies);

          interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
          checkThat(svgGroup).containsNoDetailedDependencies();
        });
      });
    });

    describe('Dragging the detailed dependencies', () => {
      let detailedDependencyView;
      let positionBefore;
      beforeEach(async () => {
        detailedDependencyView = createSingleDetailedDependencyView(svgGroup, getContainerWidth, () => detailedDependencies);
        await fadeInAndAwait(detailedDependencyView);
        positionBefore = inspect(svgGroup).detailedDependenciesAt(0).absoluteTextPosition();
        interactOn(svgGroup).withDetailedDependenciesAt(0).drag(20, 20);
      });

      it('moves them ', async () => {
        const positionAfterDragging = inspect(svgGroup).detailedDependenciesAt(0).absoluteTextPosition();
        const expectedPosition = {x: positionBefore.x + 20, y: positionBefore.y + 20};
        expect(positionAfterDragging).to.deep.equal(expectedPosition);
      });

      it('keeps the layout correctly', async () => {
        checkLayoutOn(svgGroup, fontSize, textPadding).that.linesAreLeftAligned();
        checkLayoutOn(svgGroup, fontSize, textPadding).that.backgroundRectanglesLieExactlyBehindTheLines();
      });

      describe('shows them permanently', () => {
        it('leaving the detailed dependencies with the mouse does not hide them', async () => {
          await interactOn(svgGroup).withDetailedDependenciesAt(0).moveMouseOutAndWaitFor(transitionDuration);
          checkThat(svgGroup).containsExactlyDetailedDependencies(detailedDependencies);
        });

        it('clicking on the close button hides the detailed dependencies again', () => {
          interactOn(svgGroup).withDetailedDependenciesAt(0).clickOnCloseButton();
          checkThat(svgGroup).containsNoDetailedDependencies();
        });
      });
    });
  })
});