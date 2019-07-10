'use strict';

const svg = require('../testinfrastructure/svg-mock');
const createMockRootFromClassNames = require('./mock-root-creator').createMockRootFromClassNames;

const defaultType = 'METHOD_CALL';
const containerWidth = 300;

const dependencyStringsToJson = (...dependencyStrings) => {
  return dependencyStrings.map((dependencyString, i) => {
    const fromAndTo = dependencyString.split('-');
    return {
      type: defaultType,
      originClass: fromAndTo[0],
      targetClass: fromAndTo[1],
      description:
        `Method <${fromAndTo[0]}.startMethod()> calls method <${fromAndTo[1]}.targetMethod()> in (SomeClass.java:${i})`
    };
  });
};

const createDependencies = (Dependencies, ...dependencyStrings) => {
  const svgContainer = svg.createSvgRoot().addGroup();
  const svgNodesContainer = svgContainer.addGroup();
  const svgDetailedDepsContainer = svgContainer.addGroup();

  const allClassNames = new Set([].concat.apply([], dependencyStrings.map(dependencyString => dependencyString.split('-'))));
  const mockRoot = createMockRootFromClassNames(...allClassNames, svgNodesContainer);

  const jsonDependencies = dependencyStringsToJson(...dependencyStrings);
  return new Dependencies(jsonDependencies, mockRoot, svgDetailedDepsContainer, () => containerWidth);
};

module.exports = {createDependencies};