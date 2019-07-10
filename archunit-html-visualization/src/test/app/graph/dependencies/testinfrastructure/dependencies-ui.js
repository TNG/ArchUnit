'use strict';

const expect = require('chai').expect;

const DetailedDependencyUi = require('./detailed-dependency-ui').DetailedDependencyUi;

const sleep = (timeInMs) => {
  return new Promise(resolve => {
    setTimeout(resolve, timeInMs);
  });
};

class DependencyUi {
  constructor(groupedDependency) {
    this._dependency = groupedDependency;
    this._svg = groupedDependency._view._svgElement;
    this._detailedDependencyUi = DetailedDependencyUi.of(groupedDependency._detailedView);
  }

  get detailedDependencyUi() {
    return this._detailedDependencyUi;
  }

  get line() {
    return this._svg.getDescendantElementByTypeAndCssClasses('line', 'dependency');
  }

  get hoverArea() {
    return this._svg.getDescendantElementByTypeAndCssClasses('line', 'area');
  }

  get originNodeSvgElement() {
    return this._dependency.originNode._view._svgElement;
  }

  get targetNodeSvgElement() {
    return this._dependency.targetNode._view._svgElement;
  }

  equals(otherDependencyUi) {
    return this._svg === otherDependencyUi._svg;
  }

  isInFrontOf(svgElement) {
    return this._svg.isInFrontOf(svgElement);
  }

  isVisible() {
    return this._svg.isVisible;
  }

  get id() {
    return this._svg.getAttribute('id');
  }

  expectToLieInFrontOf(svgElement) {
    expect(this.isInFrontOf(svgElement)).to.be.true;
  }

  expectToLieBetween(svgElement1, svgElement2) {
    expect(this.isInFrontOf(svgElement1) ^ this.isInFrontOf(svgElement2)).to.equal(1);
  }

  expectToBeMarkedAsViolation() {
    expect(this.line.cssClasses.has('violation')).to.be.true;
  }

  expectToNotBeMarkedAsViolation() {
    expect(this.line.cssClasses.has('violation')).to.be.false;
  }

  expectToEqual(otherDependencyUi) {
    expect(this.equals(otherDependencyUi)).to.be.true;
  }

  async hoverOverAndWaitFor(timeInMs) {
    this.hoverArea.mouseOver();
    await sleep(timeInMs);
  }

  async leaveWithMouseAndWaitFor(timeInMs) {
    this.hoverArea.mouseOut();
    await sleep(timeInMs);
  }
}

class DependenciesUi {
  constructor(dependencies) {
    this._dependencies = dependencies;
  }

  get visibleDependencyUis() {
    return this._dependencies._getVisibleDependencies().map(dependency => new DependencyUi(dependency));
  }

  expectToShowDependencies(...dependencyStrings) {
    const visibleDependencyUis = this.visibleDependencyUis;

    visibleDependencyUis.forEach(dependencyUi => {
      expect(dependencyUi.isVisible()).to.be.true;
    });

    expect(visibleDependencyUis.map(dependencyUi => dependencyUi.id)).to.have.members(dependencyStrings);
  }
}

module.exports.DependencyUi = {of: groupedDependency => new DependencyUi(groupedDependency)};
module.exports.DependenciesUi = {of: dependencies => new DependenciesUi(dependencies)};