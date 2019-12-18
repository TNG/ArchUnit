const expect = require('chai').expect;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const transitionDuration = 5;
const textPadding = 5;
const AppContext = require('../../../../main/app/graph/app-context');
const DependencyView = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDependencyView();
const TestDependencyCreator = require('../testinfrastructure/dependency-test-infrastructure').TestDependencyCreator;
const getDependencyCreator = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDependencyCreator;

describe('DependencyView', () => {

  let groupedDependency;
  let testDependencyCreator;
  let dependencyView;

  beforeEach(() => {
    testDependencyCreator = new TestDependencyCreator(getDependencyCreator());
    groupedDependency = testDependencyCreator.createAndShowDefaultGroupedDependency();
    dependencyView = new DependencyView(groupedDependency)
  });

  describe('public methods of DependencyView', () => {
    describe('refreshViolationCssClass', () => {
      it('should add violation class if dependency is marked as violation', () => {
        expect(Array.from(dependencyView._line._cssClasses)).to.deep.equal(['dependency']);

        dependencyView._dependency.markAsViolation();
        dependencyView.refreshViolationCssClass();

        expect(Array.from(dependencyView._line._cssClasses)).to.deep.equal(['dependency', 'violation']);

        dependencyView._dependency.unMarkAsViolation();
        dependencyView.refreshViolationCssClass();

        expect(Array.from(dependencyView._line._cssClasses)).to.deep.equal(['dependency'])
      });
    });

    describe('show and hide', () => {
      it('should enable and disable pointer events', () => {
        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(true);

        dependencyView.hide();

        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(false);

        dependencyView.show();

        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(true);
      });

      it('should disable pointer events for dependencies which have no detailed description', () => {
        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(true);

        dependencyView._dependency.originNode._isPackage = true;
        dependencyView.hide();

        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(false);

        dependencyView.show();

        expect(dependencyView._hoverArea.pointerEventsEnabled).to.equal(false);
      });
    });
  });
});
