'use strict';

const chai = require('chai');
const expect = chai.expect;
const chaiExtensions = require('../testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);

const transitionDuration = 5;
const textPadding = 5;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const getDependencyCreator = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDependencyCreator;
const TestDependencyCreator = require('../testinfrastructure/dependency-test-infrastructure').TestDependencyCreator;

const DependencyUi = require('./testinfrastructure/dependencies-ui').DependencyUi;

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
      let dependencyUi;

      before(() => {
        testDepCreator = new TestDependencyCreator(getDependencyCreator());
        const elementaryDependency = testDepCreator.createElementaryDependency();
        const dependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency);
        dependencyUi = DependencyUi.of(dependency);
      });

      it("it lies in front of both end nodes", () => {
        dependencyUi.expectToLieInFrontOf(dependencyUi.originNodeSvgElement);
        dependencyUi.expectToLieInFrontOf(dependencyUi.targetNodeSvgElement);
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
        DependencyUi.of(groupedDependency).expectToBeMarkedAsViolation();
      });

      it('the visible line does not have the css class "violation", if none of the elementary dependencies was a violation', () => {
        const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2);
        DependencyUi.of(groupedDependency).expectToNotBeMarkedAsViolation();
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
        const originDependencyUi = DependencyUi.of(originGroupedDependency);
        const secondGroupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency2, elementaryDependency3);

        originDependencyUi.expectToEqual(DependencyUi.of(secondGroupedDependency));
        expect(secondGroupedDependency).to.equal(originGroupedDependency);
      });

      it('its violation property is updated', () => {
        testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1);
        elementaryDependency1.markAsViolation();
        let groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency2);
        DependencyUi.of(groupedDependency).expectToBeMarkedAsViolation();

        elementaryDependency1.unMarkAsViolation();
        elementaryDependency2.markAsViolation();
        groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDependency1, elementaryDependency3);

        DependencyUi.of(groupedDependency).expectToNotBeMarkedAsViolation();
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
      const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      const dependencyUi = DependencyUi.of(groupedDependency);
      await dependencyUi.hoverOverAndWaitFor(transitionDuration);
      dependencyUi.detailedDependencyUi.expectToShowDetailedDependencies([elementaryDep1.description, elementaryDep2.description]);
    });

    it("hovering over the dependency's line does not show the detailed dependency if one of the dependency end nodes is a package", async () => {
      const shiftedElementaryDependency = testDepCreator.shiftElementaryDependencyAtStart(elementaryDep1);
      const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(shiftedElementaryDependency);
      const dependencyUi = DependencyUi.of(groupedDependency);
      await dependencyUi.hoverOverAndWaitFor(transitionDuration);
      dependencyUi.detailedDependencyUi.expectToBeHidden();
    });

    it("leaving the dependency's line with the mouse hides the detailed dependency again", async () => {
      const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      const dependencyUi = DependencyUi.of(groupedDependency);
      await dependencyUi.hoverOverAndWaitFor(transitionDuration);
      await dependencyUi.leaveWithMouseAndWaitFor(transitionDuration);
      dependencyUi.detailedDependencyUi.expectToBeHidden();
    });

    it("if the dependency has changed, then re-hovering over the dependency's line shows the new detailed dependency", async () => {
      const groupedDependency1 = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      const dependencyUi1 = DependencyUi.of(groupedDependency1);
      await dependencyUi1.hoverOverAndWaitFor(transitionDuration);
      await dependencyUi1.leaveWithMouseAndWaitFor(transitionDuration);

      const groupedDependency2 = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1);
      const dependencyUi2 = DependencyUi.of(groupedDependency2);
      await dependencyUi2.hoverOverAndWaitFor(transitionDuration);
      dependencyUi2.detailedDependencyUi.expectToShowDetailedDependencies([elementaryDep1.description]);
    });

    it('layouts the detailed dependency correctly', async () => {
      const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      const dependencyUi = DependencyUi.of(groupedDependency);
      await dependencyUi.hoverOverAndWaitFor(transitionDuration);
      dependencyUi.detailedDependencyUi.rectangles.forEach(rect => dependencyUi.detailedDependencyUi.expectRectangleToLieBehindTheLines(rect));
      dependencyUi.detailedDependencyUi.expectLinesToBeLeftAligned();
    });

    it('the detailed dependency can be dragged', async () => {
      const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
      const dependencyUi = DependencyUi.of(groupedDependency);
      await dependencyUi.hoverOverAndWaitFor(transitionDuration);
      const positionBefore = dependencyUi.detailedDependencyUi.textElement.absolutePosition;
      const expectedPosition = {x: positionBefore.x + 20, y: positionBefore.y + 20};
      dependencyUi.detailedDependencyUi.drag(20, 20);
      const positionAfterDragging = dependencyUi.detailedDependencyUi.textElement.absolutePosition;
      expect(positionAfterDragging).to.deep.equal(expectedPosition);
    });

    describe('clicking on the detailed dependency shows it permanently:', () => {
      let dependencyUi;
      beforeEach(async () => {
        const groupedDependency = testDepCreator.createAndShowGroupedDependencyFrom(elementaryDep1, elementaryDep2);
        dependencyUi = DependencyUi.of(groupedDependency);
        await dependencyUi.hoverOverAndWaitFor(transitionDuration);
        dependencyUi.detailedDependencyUi.click();
      });

      it("leaving the dependency's line with the mouse does not hide the detailed dependency", async () => {
        await dependencyUi.leaveWithMouseAndWaitFor(transitionDuration);
        dependencyUi.detailedDependencyUi.expectToShowDetailedDependencies([elementaryDep1.description, elementaryDep2.description]);
      });

      it('clicking on the close-button of the fixed detailed dependency hides it again', () => {
        dependencyUi.detailedDependencyUi.closeButton.click();
        dependencyUi.detailedDependencyUi.expectToBeHidden();
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
          const dependencyUi = DependencyUi.of(groupedDependency);
          dependencyUi.expectToLieInFrontOf(dependencyUi.originNodeSvgElement);
          dependencyUi.expectToLieInFrontOf(dependencyUi.targetNodeSvgElement);
        });

        it("does not change the position of the dependency's svg-element", () => {
          const dependencyUi = DependencyUi.of(groupedDependency);
          const startPositionBefore = dependencyUi.line.absoluteStartPosition;
          const endPositionBefore = dependencyUi.line.absoluteEndPosition;

          groupedDependency.setContainerEndNodeToEndNodeInForeground();

          const startPositionAfterwards = dependencyUi.line.absoluteStartPosition;
          const endPositionAfterwards = dependencyUi.line.absoluteEndPosition;
          expect(startPositionAfterwards).to.deep.equal(startPositionBefore);
          expect(endPositionAfterwards).to.deep.equal(endPositionBefore);
        });
      });

      describe('#setContainerEndNodeToEndNodeInBackground() with followed #onNodesFocused()', () => {
        it("makes the dependency's svg-element lie in front of one end node and behind the other", () => {
          groupedDependency.setContainerEndNodeToEndNodeInBackground();
          const dependencyUi = DependencyUi.of(groupedDependency);

          dependencyUi.expectToLieBetween(dependencyUi.originNodeSvgElement, dependencyUi.targetNodeSvgElement);
        });

        it("does not change the position of the dependency's svg-element", () => {
          const dependencyUi = DependencyUi.of(groupedDependency);
          const startPositionBefore = dependencyUi.line.absoluteStartPosition;
          const endPositionBefore = dependencyUi.line.absoluteEndPosition;

          groupedDependency.setContainerEndNodeToEndNodeInBackground();

          const startPositionAfterwards = dependencyUi.line.absoluteStartPosition;
          const endPositionAfterwards = dependencyUi.line.absoluteEndPosition;
          expect(startPositionAfterwards).to.deep.equal(startPositionBefore);
          expect(endPositionAfterwards).to.deep.equal(endPositionBefore);
        });
      });
    });

    describe('#onNodeRimChanged()', () => {
      describe('updates the position of the dependency', () => {
        let testDepCreator;
        let groupedDependency;
        let dependencyUi;

        before(() => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();
          groupedDependency.onNodeRimChanged();
          dependencyUi = DependencyUi.of(groupedDependency);
        });

        it('leads to correct end positions of the svg-lines', () => {
          const expectedDependencyLength =
            Vector.between(testDepCreator.originNode.absoluteFixableCircle, testDepCreator.targetNode.absoluteFixableCircle).length()
            - (testDepCreator.originNode.absoluteFixableCircle.r + testDepCreator.targetNode.absoluteFixableCircle.r);

          const startPosition = dependencyUi.line.absoluteStartPosition;
          const endPosition = dependencyUi.line.absoluteEndPosition;

          expect(Vector.between(testDepCreator.originNode.absoluteFixableCircle, startPosition).length())
            .to.equal(testDepCreator.originNode.absoluteFixableCircle.r);
          expect(Vector.between(testDepCreator.targetNode.absoluteFixableCircle, endPosition).length())
            .to.equal(testDepCreator.targetNode.absoluteFixableCircle.r);
          expect(Vector.between(startPosition, endPosition).length()).to.equal(expectedDependencyLength);
        });

        it('#startPoint and #endPoint equal the drawn positions', () => {
          const startPosition = dependencyUi.line.absoluteStartPosition;
          const endPosition = dependencyUi.line.absoluteEndPosition;
          expect(groupedDependency.startPoint.equals(startPosition)).to.be.true;
          expect(groupedDependency.endPoint.equals(endPosition)).to.be.true;
        });
      });

      describe('updates the visibility of the dependency:', () => {

        let testDepCreator;
        let groupedDependency;
        let dependencyUi;

        beforeEach(() => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

          testDepCreator.originNode.addOverlap(testDepCreator.targetNode);
          groupedDependency.onNodeRimChanged();
          dependencyUi = DependencyUi.of(groupedDependency);
        });

        it('it is hidden, if the end nodes are overlapping', () => {
          expect(dependencyUi.isVisible()).to.be.false;
          expect(dependencyUi.hoverArea.pointerEventsEnabled).to.be.false;
        });

        it('it is shown again, if the end nodes are not overlapping anymore', () => {
          testDepCreator.originNode.removeOverlap(testDepCreator.targetNode);
          groupedDependency.onNodeRimChanged();

          expect(dependencyUi.isVisible()).to.be.true;
          expect(dependencyUi.hoverArea.pointerEventsEnabled).to.be.true;
        });
      });
    });

    describe('#moveToPosition()', () => {
      describe('updates the position of the dependency', () => {
        let testDepCreator;
        let groupedDependency;
        let dependencyUi;

        before(async () => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();
          dependencyUi = DependencyUi.of(groupedDependency);

          await groupedDependency.moveToPosition();
        });

        it('leads to correct end positions of the svg-lines', () => {
          const expectedDependencyLength =
            Vector.between(testDepCreator.originNode.absoluteFixableCircle, testDepCreator.targetNode.absoluteFixableCircle).length()
            - (testDepCreator.originNode.absoluteFixableCircle.r + testDepCreator.targetNode.absoluteFixableCircle.r);

          const startPosition = dependencyUi.line.absoluteStartPosition;
          const endPosition = dependencyUi.line.absoluteEndPosition;

          expect(Vector.between(testDepCreator.originNode.absoluteFixableCircle, startPosition).length())
            .to.equal(testDepCreator.originNode.absoluteFixableCircle.r);
          expect(Vector.between(testDepCreator.targetNode.absoluteFixableCircle, endPosition).length())
            .to.equal(testDepCreator.targetNode.absoluteFixableCircle.r);
          expect(Vector.between(startPosition, endPosition).length()).to.equal(expectedDependencyLength);
        });

        it('#startPoint and #endPoint equal the drawn positions', () => {
          const startPosition = dependencyUi.line.absoluteStartPosition;
          const endPosition = dependencyUi.line.absoluteEndPosition;

          expect(groupedDependency.startPoint.equals(startPosition)).to.be.true;
          expect(groupedDependency.endPoint.equals(endPosition)).to.be.true;
        });
      });

      describe('updates the visibility of the dependency:', () => {

        let testDepCreator;
        let groupedDependency;
        let dependencyUi;

        beforeEach(async () => {
          testDepCreator = new TestDependencyCreator(getDependencyCreator());
          groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();
          dependencyUi = DependencyUi.of(groupedDependency);

          testDepCreator.originNode.addOverlap(testDepCreator.targetNode);
          await groupedDependency.moveToPosition();
        });

        it('it is hidden, if the end nodes are overlapping', () => {
          expect(dependencyUi.isVisible()).to.be.false;
          expect(dependencyUi.hoverArea.pointerEventsEnabled).to.be.false;
        });

        it('it is shown again, if the end nodes are not overlapping anymore', async () => {
          testDepCreator.originNode.removeOverlap(testDepCreator.targetNode);
          await groupedDependency.moveToPosition();

          expect(dependencyUi.isVisible()).to.be.true;
          expect(dependencyUi.hoverArea.pointerEventsEnabled).to.be.true;
        });
      });
    });

    it('#hide()', () => {
      const testDepCreator = new TestDependencyCreator(getDependencyCreator());
      const groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();
      const dependencyUi = DependencyUi.of(groupedDependency);

      groupedDependency.hide();

      expect(dependencyUi.isVisible()).to.be.false;
    });

    it('#toString()', () => {
      const testDepCreator = new TestDependencyCreator(getDependencyCreator());
      const groupedDependency = testDepCreator.createAndShowDefaultGroupedDependency();

      expect(groupedDependency.toString()).to.equal(`${testDepCreator.from}-${testDepCreator.to}`);
    });
  });
});