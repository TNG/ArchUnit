'use strict';

const expect = require('chai').expect;
require('./testinfrastructure/general-chai-extensions');
const dependencyVisualizationFunctionsFactory = require('../../../main/app/graph/dependency-visualization-functions');
const {Vector} = require('../../../main/app/graph/infrastructure/vectors');

const visualizationFunctions = dependencyVisualizationFunctionsFactory.newInstance();

describe('Calculates correct start end end point of a single dependency', () => {
  it('if the dependency points to the top', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 100, y: 180});
    expect(results.endPoint).to.deepCloseTo({x: 100, y: 120});
  });

  it('if the dependency points to the bottom', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 100, y: 120});
    expect(results.endPoint).to.deepCloseTo({x: 100, y: 180});
  });

  it('if the dependency points to the left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 180, y: 100});
    expect(results.endPoint).to.deepCloseTo({x: 120, y: 100});
  });

  it('if the dependency points to the right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 120, y: 100});
    expect(results.endPoint).to.deepCloseTo({x: 180, y: 100});
  });

  it('if the dependency points to the top left', () => {
    const startCircle = {x: 200, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 185.857864, y: 185.857864});
    expect(results.endPoint).to.deepCloseTo({x: 114.142136, y: 114.142136});
  });

  it('if the dependency points to the bottom right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 114.142136, y: 114.142136});
    expect(results.endPoint).to.deepCloseTo({x: 185.857864, y: 185.857864});
  });

  it('if the dependency points to the bottom left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 185.857864, y: 114.142136});
    expect(results.endPoint).to.deepCloseTo({x: 114.142136, y: 185.857864});
  });

  it('if the dependency points to the top right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 114.142136, y: 185.857864});
    expect(results.endPoint).to.deepCloseTo({x: 185.857864, y: 114.142136});
  });

  it('if the dependency points between the top right and the right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 150, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 117.888444, y: 191.055728});
    expect(results.endPoint).to.deepCloseTo({x: 182.111456, y: 158.944272});
  });
});

describe('Calculates correct start end end point of a dependency, where another dependency in the opposite direction exists', () => {
  it('if the dependency points to the top', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 91.055728, y: 182.111456});
    expect(results.endPoint).to.deepCloseTo({x: 91.055728, y: 117.888544});
  });

  it('if the dependency points to the bottom', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 108.944272, y: 117.888544});
    expect(results.endPoint).to.deepCloseTo({x: 108.944272, y: 182.111456});
  });

  it('if the dependency points to the left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 182.111456, y: 108.944272});
    expect(results.endPoint).to.deepCloseTo({x: 117.888544, y: 108.944272});
  });

  it('if the dependency points to the right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 117.888544, y: 91.055728});
    expect(results.endPoint).to.deepCloseTo({x: 182.111456, y: 91.055728});
  });

  it('if the dependency points to the top left', () => {
    const startCircle = {x: 200, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 181.026334, y: 193.675445});
    expect(results.endPoint).to.deepCloseTo({x: 106.324555, y: 118.973666});
  });

  it('if the dependency points to the bottom right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 118.973666, y: 106.324555});
    expect(results.endPoint).to.deepCloseTo({x: 193.675445, y: 181.026334});
  });

  it('if the dependency points to the bottom left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 193.675445, y: 118.973666},);
    expect(results.endPoint).to.deepCloseTo({x: 118.973666, y: 193.675445});
  });

  it('if the dependency points to the top right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 106.324555, y: 181.026334});
    expect(results.endPoint).to.deepCloseTo({x: 181.026334, y: 106.324555});
  });

  it('if the dependency points between the top right and the right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 150, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deepCloseTo({x: 112, y: 184});
    expect(results.endPoint).to.deepCloseTo({x: 180, y: 150});
  });
});