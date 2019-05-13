'use strict';

const chai = require('chai');
const expect = chai.expect;
const chaiExtensions = require('./testinfrastructure/general-chai-extensions');
chai.use(chaiExtensions);

const dependencyVisualizationFunctionsFactory = require('../../../main/app/graph/dependency-visualization-functions');
const {Vector} = require('../../../main/app/graph/infrastructure/vectors');

const visualizationFunctions = dependencyVisualizationFunctionsFactory.newInstance();

describe('Calculates correct start end end point of a single dependency', () => {
  it('if the dependency points to the top', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 100, y: 180});
    expect(results.endPoint).to.deep.closeTo({x: 100, y: 120});
  });

  it('if the dependency points to the bottom', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 100, y: 120});
    expect(results.endPoint).to.deep.closeTo({x: 100, y: 180});
  });

  it('if the dependency points to the left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 180, y: 100});
    expect(results.endPoint).to.deep.closeTo({x: 120, y: 100});
  });

  it('if the dependency points to the right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 120, y: 100});
    expect(results.endPoint).to.deep.closeTo({x: 180, y: 100});
  });

  it('if the dependency points to the top left', () => {
    const startCircle = {x: 200, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 185.857864, y: 185.857864});
    expect(results.endPoint).to.deep.closeTo({x: 114.142136, y: 114.142136});
  });

  it('if the dependency points to the bottom right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 114.142136, y: 114.142136});
    expect(results.endPoint).to.deep.closeTo({x: 185.857864, y: 185.857864});
  });

  it('if the dependency points to the bottom left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 185.857864, y: 114.142136});
    expect(results.endPoint).to.deep.closeTo({x: 114.142136, y: 185.857864});
  });

  it('if the dependency points to the top right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 114.142136, y: 185.857864});
    expect(results.endPoint).to.deep.closeTo({x: 185.857864, y: 114.142136});
  });

  it('if the dependency points between the top right and the right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 150, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 117.888444, y: 191.055728});
    expect(results.endPoint).to.deep.closeTo({x: 182.111456, y: 158.944272});
  });

  it('if the start circle of the dependency is within the end circle', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 110, y: 130, r: 70};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 93.675445, y: 81.026334});
    expect(results.endPoint).to.deep.closeTo({x: 87.864056, y: 63.592169});
  });

  it('if the end circle of the dependency is within the start circle', () => {
    const startCircle = {x: 110, y: 130, r: 70};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 87.864056, y: 63.592169});
    expect(results.endPoint).to.deep.closeTo({x: 93.675445, y: 81.026334});
  });

  it('if the start circle of the dependency is exactly in the middle of the end circle: then the dependency points to the bottom left', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 40};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 114.142136, y: 114.142136});
    expect(results.endPoint).to.deep.closeTo({x: 128.284271, y: 128.284271});
  });
});

describe('Calculates correct start end end point of a dependency, where another dependency in the opposite direction exists', () => {
  it('if the dependency points to the top', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 91.055728, y: 182.111456});
    expect(results.endPoint).to.deep.closeTo({x: 91.055728, y: 117.888544});
  });

  it('if the dependency points to the bottom', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 108.944272, y: 117.888544});
    expect(results.endPoint).to.deep.closeTo({x: 108.944272, y: 182.111456});
  });

  it('if the dependency points to the left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 182.111456, y: 108.944272});
    expect(results.endPoint).to.deep.closeTo({x: 117.888544, y: 108.944272});
  });

  it('if the dependency points to the right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 117.888544, y: 91.055728});
    expect(results.endPoint).to.deep.closeTo({x: 182.111456, y: 91.055728});
  });

  it('if the dependency points to the top left', () => {
    const startCircle = {x: 200, y: 200, r: 20};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 181.026334, y: 193.675445});
    expect(results.endPoint).to.deep.closeTo({x: 106.324555, y: 118.973666});
  });

  it('if the dependency points to the bottom right', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 200, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 118.973666, y: 106.324555});
    expect(results.endPoint).to.deep.closeTo({x: 193.675445, y: 181.026334});
  });

  it('if the dependency points to the bottom left', () => {
    const startCircle = {x: 200, y: 100, r: 20};
    const endCircle = {x: 100, y: 200, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 193.675445, y: 118.973666},);
    expect(results.endPoint).to.deep.closeTo({x: 118.973666, y: 193.675445});
  });

  it('if the dependency points to the top right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 106.324555, y: 181.026334});
    expect(results.endPoint).to.deep.closeTo({x: 181.026334, y: 106.324555});
  });

  it('if the dependency points between the top right and the right', () => {
    const startCircle = {x: 100, y: 200, r: 20};
    const endCircle = {x: 200, y: 150, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(true, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 112, y: 184});
    expect(results.endPoint).to.deep.closeTo({x: 180, y: 150});
  });

  it('if the start circle of the dependency is within the end circle', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 110, y: 130, r: 70};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 93.675445, y: 81.026334});
    expect(results.endPoint).to.deep.closeTo({x: 87.864056, y: 63.592169});
  });

  it('if the end circle of the dependency is within the start circle', () => {
    const startCircle = {x: 110, y: 130, r: 70};
    const endCircle = {x: 100, y: 100, r: 20};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 87.864056, y: 63.592169});
    expect(results.endPoint).to.deep.closeTo({x: 93.675445, y: 81.026334});
  });

  it('if the start circle of the dependency is exactly in the middle of the end circle: then the dependency points to the bottom left', () => {
    const startCircle = {x: 100, y: 100, r: 20};
    const endCircle = {x: 100, y: 100, r: 40};

    const results = visualizationFunctions.calculateStartAndEndPositionOfDependency(false, startCircle, endCircle);
    expect(results.startPoint).to.deep.closeTo({x: 114.142136, y: 114.142136});
    expect(results.endPoint).to.deep.closeTo({x: 128.284271, y: 128.284271});
  });
});