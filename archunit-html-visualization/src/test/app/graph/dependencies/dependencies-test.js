'use strict';

const chai = require('chai');
const expect = chai.expect;

const transitionDuration = 5;
const textPadding = 5;

const MAXIMUM_DELTA = 0.0001;

const Vector = require('../../../../main/app/graph/infrastructure/vectors').Vector;

const guiElementsMock = require('../testinfrastructure/gui-elements-mock');
const AppContext = require('../../../../main/app/graph/app-context');
const Dependencies = AppContext.newInstance({guiElements: guiElementsMock, transitionDuration, textPadding}).getDependencies();
//const {buildFilterCollection} = require('../../../../main/app/graph/filter');
const createDependencies = require('../testinfrastructure/dependencies-test-infrastructure').createDependencies;

const DependenciesUi = require('./testinfrastructure/dependencies-ui').DependenciesUi;


describe('Dependencies', () => {

  describe('#recreateVisible', () => {

    let dependencies;
    let dependenciesUi;

    before(async () => {
      dependencies = createDependencies(Dependencies,
        'my.company.somePkg.FirstClass-my.company.otherPkg.FirstClass',
        'my.company.somePkg.SecondClass-my.company.otherPkg.SecondClass',
        'my.company.somePkg.SecondClass$SomeInnerClass-my.company.otherPkg.SecondClass',
        'my.company.somePkg.SecondClass$SomeInnerClass-my.company.otherPkg.ThirdClass'
      );
      dependencies.recreateVisible();
      await dependencies.createListener().onLayoutChanged();
      dependenciesUi = DependenciesUi.of(dependencies);
    });

    it('displays all dependencies', () => {
      dependenciesUi.expectToShowDependencies(
        'my.company.somePkg.FirstClass-my.company.otherPkg.FirstClass',
        'my.company.somePkg.SecondClass-my.company.otherPkg.SecondClass',
        'my.company.somePkg.SecondClass$SomeInnerClass-my.company.otherPkg.SecondClass',
        'my.company.somePkg.SecondClass$SomeInnerClass-my.company.otherPkg.ThirdClass'
      );
    });

    it('puts all dependencies in front of both end nodes', () => {
      dependenciesUi.visibleDependencyUis.forEach(dependencyUi => {
        dependencyUi.expectToLieInFrontOf(dependencyUi.originNodeSvgElement);
        dependencyUi.expectToLieInFrontOf(dependencyUi.targetNodeSvgElement);
      });
    });

    it('places all dependencies directly between the circles of their end nodes', () => {
      dependenciesUi.visibleDependencyUis.forEach(visibleDependencyUi => {
        const originCircle = visibleDependencyUi._dependency.originNode.absoluteFixableCircle;
        const targetCircle = visibleDependencyUi._dependency.targetNode.absoluteFixableCircle;
        const circleMiddleDistance = Vector.between(originCircle, targetCircle).length();
        const expectedDependencyLength = circleMiddleDistance - (originCircle.r + targetCircle.r);

        expect(visibleDependencyUi.line.lineLength).to.be.closeTo(expectedDependencyLength, MAXIMUM_DELTA);

        visibleDependencyUi.expectToTouchOriginNode();
        visibleDependencyUi.expectToTouchTargetNode();
      });
    });

    it('places mutual dependencies parallel with a small distance to each other', async () => {
      const dependencies = createDependencies(Dependencies,
        'my.company.FirstClass-my.company.SecondClass',
        'my.company.SecondClass-my.company.FirstClass',
      );
      dependencies.recreateVisible();
      await dependencies.createListener().onLayoutChanged();
      const dependenciesUi = DependenciesUi.of(dependencies);

      dependenciesUi.visibleDependencyUis.forEach(dependencyUi => {
        dependencyUi.expectToTouchOriginNode();
        dependencyUi.expectToTouchTargetNode();
      });
    });
  });

  describe('can display violations', () => {
    it('#showViolation()', () => {
      //TODO: nacheinander 2 oder 3 violations einblenden; immer showViolation(...) und danach recreateVisible() aufrufen (im Graph wird zwar danach
      // die Filter geupdated, aber dabei wird auch recreateVisible aufgerufen) --> dann schauen, ob entspr Deps rot (bzw. css-Klasse)
      // --> den Violations-Filter erst im Filter-Abschnitt testen!
    });

    it('#hideViolation()', () => {
      //TODO: die violations erst einblenden und dann nacheinander wieder ausblenden
    });
  });

  describe('Filter', () => {
    describe('by type', () => {
      //TODO: die Infrastruktur muss irgendwie auch verschiedene Typen erlauben...das dann hier testen
    });

    describe('by violations', () => {
      //TODO
    });

    describe('of the nodes', () => {
      it('is also applied to the dependencies', () => {
        //TODO
      });
    });

    describe('by several filters', () => {
      //TODO: combine the filters above
    })
  });

  describe('listens correctly to the nodes', () => {
    //TODO: check listener
  });

  describe('public API-methods', () => {

    describe('#getAllLinks', () => {
      //TODO:
    });

    //TODO ...und übrige public-API Methoden, die nicht zu den Kategorien oben gehören
  });
});