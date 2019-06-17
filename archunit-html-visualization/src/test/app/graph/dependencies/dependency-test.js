'use strict';

const chai = require('chai');
const expect = chai.expect;
const chaiExtensions = require('../testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);

const fontSize = require('../testinfrastructure/visualization-styles-mock').createVisualizationStylesMock().getDependencyTitleFontSize();
const transitionDuration = 5;
const textPadding = 5;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const getDependencyCreator = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDependencyCreator;
const TestDependencyCreator = require('../testinfrastructure/dependency-test-infrastructure').TestDependencyCreator;
const checkThat = require('../testinfrastructure/dependency-gui-adapter').checkThat;
const interactOn = require('../testinfrastructure/dependency-gui-adapter').interactOn;
const inspect = require('../testinfrastructure/dependency-gui-adapter').inspect;
const detailedDepsGuiAdapter = require('../testinfrastructure/detailed-dependency-gui-adapter');

const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;

describe('ElementaryDependency', () => {
  describe('exposes end nodes and their fullnames', () => {
    let testDependencyCreator;
    let dependency;
    before(() => {
      testDependencyCreator = new TestDependencyCreator(getDependencyCreator());
      dependency = testDependencyCreator.createElementaryDependency();
    });

    it('#originNode', () => {
      expect(dependency.originNode).to.equal(testDependencyCreator.originNode);
    });

    it('#targetNode', () => {
      expect(dependency.targetNode).to.equal(testDependencyCreator.targetNode);
    });

    it('#from returns the origin fullname', () => {
      expect(dependency.from).to.equal(testDependencyCreator.from);
    });

    it('#to returns the target fullname', () => {
      expect(dependency.to).to.equal(testDependencyCreator.to);
    });
  });

  it('the matching concerning specific filters can be set and changed', () => {
    const testDependencyCreator = new TestDependencyCreator(getDependencyCreator());
    const dependency = testDependencyCreator.createElementaryDependency();

    dependency.setMatchesFilter('someFilter', true);
    dependency.setMatchesFilter('otherFilter', false);

    expect(dependency.matchesAllFilters()).to.be.false;
    expect(dependency.matchesFilter('someFilter')).to.be.true;
    expect(dependency.matchesFilter('otherFilter')).to.be.false;

    dependency.setMatchesFilter('someFilter', false);
    dependency.setMatchesFilter('otherFilter', true);

    expect(dependency.matchesAllFilters()).to.be.false;
    expect(dependency.matchesFilter('someFilter')).to.be.false;
    expect(dependency.matchesFilter('otherFilter')).to.be.true;

    dependency.setMatchesFilter('someFilter', true);
    dependency.setMatchesFilter('otherFilter', true);

    expect(dependency.matchesAllFilters()).to.be.true;
  });

  it('can be marked and unmarked as violation', () => {
    const testDependencyCreator = new TestDependencyCreator(getDependencyCreator());
    const dependency = testDependencyCreator.createElementaryDependency();

    expect(dependency.isViolation).to.be.false;

    dependency.markAsViolation();
    expect(dependency.isViolation).to.be.true;

    dependency.unMarkAsViolation();
    expect(dependency.isViolation).to.be.false;
  });

  describe('can be shifted to end nodes on a lower depth (i.e. higher in the tree), so that', () => {
    let testDependencyCreator;
    let dependency;
    beforeEach(() => {
      testDependencyCreator = new TestDependencyCreator(getDependencyCreator());
      dependency = testDependencyCreator.createElementaryDependency();
    });

    it('the returned ElementaryDependency has the new end nodes and their fullnames', () => {
      const shiftedDependency = testDependencyCreator.shiftElementaryDependencyAtBothEnds(dependency);
      expect(shiftedDependency.originNode).to.equal(testDependencyCreator.parentOriginNode);
      expect(shiftedDependency.targetNode).to.equal(testDependencyCreator.parentTargetNode);
      expect(shiftedDependency.from).to.equal(testDependencyCreator.parentFrom);
      expect(shiftedDependency.to).to.equal(testDependencyCreator.parentTo);
    });

    it('the returned ElementaryDependency retains the violation-property', () => {
      dependency.markAsViolation();
      const shiftedDependency = testDependencyCreator.shiftElementaryDependencyAtBothEnds(dependency);
      expect(shiftedDependency.isViolation).to.be.true;
    });

    it('the returned ElementaryDependency loosed its type and description', () => {
      const shiftedDependency = testDependencyCreator.shiftElementaryDependencyAtBothEnds(dependency);
      expect(shiftedDependency.type).to.be.empty;
      expect(shiftedDependency.description).to.be.empty;
    });
  });
});

describe('GroupedDependency', () => {
  describe('creation of a GroupedDependency', () => {
    describe('the created GroupedDependency has correct end nodes and their fullnames', () => {
      let testDepCreator;
      let groupedDependency;

      before(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());

        const elementaryDependency1 = testDepCreator.createElementaryDependency();
        const elementaryDependency2 = testDepCreator.createElementaryDependency();
        const elementaryDependency3 = testDepCreator.createElementaryDependency();
        groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2, elementaryDependency3);
      });

      it('#originNode', () => {
        expect(groupedDependency.originNode).to.equal(testDepCreator.originNode);
      });

      it('#targetNode', () => {
        expect(groupedDependency.targetNode).to.equal(testDepCreator.targetNode);
      });

      it('#from', () => {
        expect(groupedDependency.from).to.equal(testDepCreator.from);
      });

      it('#to', () => {
        expect(groupedDependency.to).to.equal(testDepCreator.to);
      });
    });

    describe('its svg-element is correctly created', () => {
      let testDepCreator;

      before(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());
        const elementaryDependency = testDepCreator.createElementaryDependency();
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency);
      });

      it('it is added to the svg-container-element', () => {
        checkThat(testDepCreator.svgContainer).containsExactlyDependencies(`${testDepCreator.from}-${testDepCreator.to}`);
      });

      it("it lies in front of both end nodes", () => {
        checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.inFrontOf.bothEndNodes();
      });
    });

    describe('The "violation"-css-class of a GroupedDependency are initialized correctly:', () => {
      let testDepCreator;
      let elementaryDependency1;
      let elementaryDependency2;

      beforeEach(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());
        elementaryDependency1 = testDepCreator.createElementaryDependency();
        elementaryDependency2 = testDepCreator.createElementaryDependency();
      });

      it('the visible line has the css class "violation", if one of the elementary dependencies was a violation', () => {
        elementaryDependency1.markAsViolation();
        const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2);
        checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.markedAs.violation();
      });

      it('the visible line does not have the css class "violation", if none of the elementary dependencies was a violation', () => {
        const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2);
        checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.not.markedAs.violation();
      });
    });

    describe('when a dependency with the same start and end node already exists', () => {
      let testDepCreator;
      let elementaryDependency1;
      let elementaryDependency2;
      let elementaryDependency3;

      beforeEach(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());

        elementaryDependency1 = testDepCreator.createElementaryDependency();
        elementaryDependency2 = testDepCreator.createElementaryDependency();
        elementaryDependency3 = testDepCreator.createElementaryDependency();
      });

      it('it is not drawn again and the object is not recreated', () => {
        const originGroupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1);
        const secondGroupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency2, elementaryDependency3);

        checkThat(testDepCreator.svgContainer).containsExactlyDependencies(`${testDepCreator.from}-${testDepCreator.to}`);
        expect(secondGroupedDependency).to.equal(originGroupedDependency);
      });

      it('its violation property is updated', () => {
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1);
        elementaryDependency1.markAsViolation();
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2);

        checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.markedAs.violation();

        elementaryDependency1.unMarkAsViolation();
        elementaryDependency2.markAsViolation();
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency3);

        checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.not.markedAs.violation();
      });
    });
  });

  describe('User interaction', () => {
    let testDepCreator;
    let elementaryDep1;
    let elementaryDep2;

    beforeEach(() => {
      testDepCreator = new TestDependencyCreator(getDependencyCreator());

      elementaryDep1 = testDepCreator.createElementaryDependency();
      elementaryDep2 = testDepCreator.createElementaryDependency();
    });

    it("hovering over the dependency's line shows the detailed dependency", async () => {
      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer)
        .containsExactlyDetailedDependencies([elementaryDep1.description, elementaryDep2.description]);
    });

    it("hovering over the dependency's line does not show the detailed dependency if one of the dependency end nodes is a package", async () => {
      const shiftedElementaryDependency = testDepCreator.shiftElementaryDependencyAtStart(elementaryDep1);
      testDepCreator.createAndShowGroupedDependencyFrom(shiftedElementaryDependency);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.parentFrom}-${testDepCreator.to}`, transitionDuration);
      detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer).containsNoDetailedDependencies();
    });

    it("leaving the dependency's line with the mouse hides the detailed dependency again", async () => {
      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      await interactOn(testDepCreator.svgContainer)
        .leaveDependencyWithMouseAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer).containsNoDetailedDependencies();
    });

    it("if the dependency has changed, then re-hovering over the dependency's line shows the new detailed dependency", async () => {
      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      await interactOn(testDepCreator.svgContainer).leaveDependencyWithMouseAndWaitFor(
        `${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);

      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1);
      await interactOn(testDepCreator.svgContainer).hoverOverDependencyAndWaitFor(
        `${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer)
        .containsExactlyDetailedDependencies([elementaryDep1.description]);
    });

    it('layouts the detailed dependency correctly', async () => {
      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      detailedDepsGuiAdapter
        .checkLayoutOn(testDepCreator.svgDetailedDepsContainer, fontSize, textPadding).that.backgroundRectanglesLieExactlyBehindTheLines();
      detailedDepsGuiAdapter.checkLayoutOn(testDepCreator.svgDetailedDepsContainer, fontSize, textPadding).that.linesAreLeftAligned();
    });

    it('the detailed dependency can be dragged', async () => {
      testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      await interactOn(testDepCreator.svgContainer)
        .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
      const positionBefore = detailedDepsGuiAdapter
        .inspect(testDepCreator.svgDetailedDepsContainer).detailedDependenciesAt(0).absoluteTextPosition();
      const expectedPosition = {x: positionBefore.x + 20, y: positionBefore.y + 20};
      detailedDepsGuiAdapter.interactOn(testDepCreator.svgDetailedDepsContainer).withDetailedDependenciesAt(0).drag(20, 20);
      const positionAfterDragging = detailedDepsGuiAdapter
        .inspect(testDepCreator.svgDetailedDepsContainer).detailedDependenciesAt(0).absoluteTextPosition();
      expect(positionAfterDragging).to.deep.equal(expectedPosition);
    });

    describe('clicking on the detailed dependency shows it permanently:', () => {
      beforeEach(async () => {
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
        await interactOn(testDepCreator.svgContainer)
          .hoverOverDependencyAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
        detailedDepsGuiAdapter.interactOn(testDepCreator.svgDetailedDepsContainer).withDetailedDependenciesAt(0).click();
      });

      it("leaving the dependency's line with the mouse does not hide the detailed dependency", async () => {
        await interactOn(testDepCreator.svgContainer)
          .leaveDependencyWithMouseAndWaitFor(`${testDepCreator.from}-${testDepCreator.to}`, transitionDuration);
        detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer)
          .containsExactlyDetailedDependencies([elementaryDep1.description, elementaryDep2.description]);
      });

      it('clicking on the close-button of the fixed detailed dependency hides it again', () => {
        detailedDepsGuiAdapter.interactOn(testDepCreator.svgDetailedDepsContainer).withDetailedDependenciesAt(0).clickOnCloseButton();
        detailedDepsGuiAdapter.checkThat(testDepCreator.svgDetailedDepsContainer).containsNoDetailedDependencies();
      });
    });
  });

  describe('public API methods', () => {
    describe("methods for changing the layer of the dependency's svg-element", () => {
      let testDepCreator;
      let groupedDependency;

      beforeEach(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());
        groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();
      });

      describe('#setContainerEndNodeToEndNodeInForeground()', () => {
        beforeEach(() => {
          //as the default layer of a dependency is the foreground, change it to background before testing
          groupedDependency.setContainerEndNodeToEndNodeInBackground();
        });

        it("makes the dependency's svg-element lie in front of both end nodes", () => {
          groupedDependency.setContainerEndNodeToEndNodeInForeground();

          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.inFrontOf.bothEndNodes();
        });

        it("does not change the position of the dependency's svg-element", () => {
          const linePositionsBefore = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);

          groupedDependency.setContainerEndNodeToEndNodeInForeground();

          const linePositionsAfterwards = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);
          expect(linePositionsAfterwards.startPosition).to.deep.equal(linePositionsBefore.startPosition);
          expect(linePositionsAfterwards.endPosition).to.deep.equal(linePositionsBefore.endPosition);
        });
      });

      describe('#setContainerEndNodeToEndNodeInBackground() with followed #onNodesFocused()', () => {
        it("makes the dependency's svg-element lie in front of one end node and behind the other", () => {
          groupedDependency.setContainerEndNodeToEndNodeInBackground();

          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.between.bothEndNodes();
        });

        it("does not change the position of the dependency's svg-element", () => {
          const linePositionsBefore = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);

          groupedDependency.setContainerEndNodeToEndNodeInBackground();

          const linePositionsAfterwards = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);
          expect(linePositionsAfterwards.startPosition).to.deep.equal(linePositionsBefore.startPosition);
          expect(linePositionsAfterwards.endPosition).to.deep.equal(linePositionsBefore.endPosition);
        });
      });
    });

    describe('#onNodeRimChanged()', () => {
      describe('updates the position of the dependency', () => {
        let testDepCreator;
        let groupedDependency;

        before(() => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

          groupedDependency.onNodeRimChanged();
        });

        it('leads to correct end positions of the svg-lines', () => {
          const expectedDependencyLength =
            Vector.between(testDepCreator.originNode.absoluteFixableCircle, testDepCreator.targetNode.absoluteFixableCircle).length()
            - (testDepCreator.originNode.absoluteFixableCircle.r + testDepCreator.targetNode.absoluteFixableCircle.r);

          const actualPositions = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);

          expect(Vector.between(testDepCreator.originNode.absoluteFixableCircle, actualPositions.startPosition).length())
            .to.equal(testDepCreator.originNode.absoluteFixableCircle.r);
          expect(Vector.between(testDepCreator.targetNode.absoluteFixableCircle, actualPositions.endPosition).length())
            .to.equal(testDepCreator.targetNode.absoluteFixableCircle.r);
          expect(Vector.between(actualPositions.startPosition, actualPositions.endPosition).length()).to.equal(expectedDependencyLength);
        });

        it('#startPoint and #endPoint equal the drawn positions', () => {
          const positions = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);
          expect(groupedDependency.startPoint.equals(positions.startPosition)).to.be.true;
          expect(groupedDependency.endPoint.equals(positions.endPosition)).to.be.true;
        });
      });

      describe('updates the visibility of the dependency:', () => {

        let testDepCreator;
        let groupedDependency;

        beforeEach(() => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

          testDepCreator.originNode.addOverlap(testDepCreator.targetNode);
          groupedDependency.onNodeRimChanged();
        });

        it('it is hidden, if the end nodes are overlapping', () => {
          checkThat(testDepCreator.svgContainer).containsExactlyDependencies();
          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.not.hoverable();
        });

        it('it is shown again, if the end nodes are not overlapping anymore', () => {
          testDepCreator.originNode.removeOverlap(testDepCreator.targetNode);
          groupedDependency.onNodeRimChanged();

          checkThat(testDepCreator.svgContainer).containsExactlyDependencies(`${testDepCreator.from}-${testDepCreator.to}`);
          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.hoverable();
        });
      });
    });

    describe('#moveToPosition()', () => {
      describe('updates the position of the dependency', () => {
        let testDepCreator;
        let groupedDependency;

        before(async () => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

          await groupedDependency.moveToPosition();
        });

        it('leads to correct end positions of the svg-lines', () => {
          const expectedDependencyLength =
            Vector.between(testDepCreator.originNode.absoluteFixableCircle, testDepCreator.targetNode.absoluteFixableCircle).length()
            - (testDepCreator.originNode.absoluteFixableCircle.r + testDepCreator.targetNode.absoluteFixableCircle.r);

          const actualPositions = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);

          expect(Vector.between(testDepCreator.originNode.absoluteFixableCircle, actualPositions.startPosition).length())
            .to.equal(testDepCreator.originNode.absoluteFixableCircle.r);
          expect(Vector.between(testDepCreator.targetNode.absoluteFixableCircle, actualPositions.endPosition).length())
            .to.equal(testDepCreator.targetNode.absoluteFixableCircle.r);
          expect(Vector.between(actualPositions.startPosition, actualPositions.endPosition).length()).to.equal(expectedDependencyLength);
        });

        it('#startPoint and #endPoint equal the drawn positions', () => {
          const positions = inspect(testDepCreator.svgContainer).linePositionOf(`${testDepCreator.from}-${testDepCreator.to}`);
          expect(groupedDependency.startPoint.equals(positions.startPosition)).to.be.true;
          expect(groupedDependency.endPoint.equals(positions.endPosition)).to.be.true;
        });
      });

      describe('updates the visibility of the dependency:', () => {

        let testDepCreator;
        let groupedDependency;

        beforeEach(async () => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

          testDepCreator.originNode.addOverlap(testDepCreator.targetNode);
          await groupedDependency.moveToPosition();
        });

        it('it is hidden, if the end nodes are overlapping', () => {
          checkThat(testDepCreator.svgContainer).containsExactlyDependencies();
          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.not.hoverable();
        });

        it('it is shown again, if the end nodes are not overlapping anymore', async () => {
          testDepCreator.originNode.removeOverlap(testDepCreator.targetNode);
          await groupedDependency.moveToPosition();

          checkThat(testDepCreator.svgContainer).containsExactlyDependencies(`${testDepCreator.from}-${testDepCreator.to}`);
          checkThat(testDepCreator.svgContainer).dependency(`${testDepCreator.from}-${testDepCreator.to}`).is.hoverable();
        });
      });
    });

    it('#hide()', () => {
      const testDepCreator = new TestDependencyCreator(getDependencyCreator());
      const groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

      groupedDependency.hide();

      checkThat(testDepCreator.svgContainer).containsExactlyDependencies();
    });

    it('#toString()', () => {
      const testDepCreator = new TestDependencyCreator(getDependencyCreator());
      const groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

      expect(groupedDependency.toString()).to.equal(`${testDepCreator.from}-${testDepCreator.to}`);
    });
  });
});