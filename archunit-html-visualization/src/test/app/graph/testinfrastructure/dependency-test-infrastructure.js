'use strict';

const svg = require('../testinfrastructure/svg-mock');
const createMockRootFromClassNames = require('./mock-root-creator').createMockRootFromClassNames;

const TestDependencyCreator = class {
  constructor(dependencyCreator) {
    this._dependencyCreator = dependencyCreator;

    this.svgContainer = svg.createGroup();
    const svgNodesContainer = this.svgContainer.addGroup();
    this.svgDetailedDepsContainer = this.svgContainer.addGroup();

    this._index = 0;
    this.parentFrom = 'my.company.somePkg';
    this.parentTo = 'my.company.otherPkg';
    this.from = 'my.company.somePkg.SomeClass';
    this.to = 'my.company.otherPkg.OtherClass';

    const mockRoot = createMockRootFromClassNames(this.from, this.to, this.svgContainer);

    this.parentOriginNode = mockRoot.getByName(this.parentFrom);
    this.parentTargetNode = mockRoot.getByName(this.parentTo);
    this.originNode = mockRoot.getByName(this.from);
    this.targetNode = mockRoot.getByName(this.to);

    this._currentDescriptions = new Map();

    this._lineNumber = 1;
  }

  createElementaryDependency() {
    return this._dependencyCreator.createElementaryDependency({
      originNode: this.originNode,
      targetNode: this.targetNode,
      type: 'METHOD_CALL',
      description:
        `Method <my.company.somePkg.SomeClass.startMethod()> calls method <my.company.somePkg.OtherClass.targetMethod()> in (SomeClass.java:${this._lineNumber++})`
    });
  }

  shiftElementaryDependencyAtStart(elementaryDependency) {
    return this._dependencyCreator.shiftElementaryDependency(elementaryDependency, this.parentOriginNode, this.targetNode);
  }

  shiftElementaryDependencyAtEnd(elementaryDependency) {
    return this._dependencyCreator.shiftElementaryDependency(elementaryDependency, this.originNode, this.parentTargetNode);
  }

  shiftElementaryDependencyAtBothEnds(elementaryDependency) {
    return this._dependencyCreator.shiftElementaryDependency(elementaryDependency, this.parentOriginNode, this.parentTargetNode);
  }

  _createGroupedDependencyFrom(...elementaryDependencies) {
    const originNode = elementaryDependencies[0].originNode;
    const targetNode = elementaryDependencies[0].targetNode;
    const dependencyString = `${originNode.getFullName()}-${targetNode.getFullName()}`;

    if (!elementaryDependencies.every(d => d.originNode === originNode && d.targetNode === targetNode)) {
      throw new Error('The elementary dependencies must all have the same origin node respectively target node');
    }

    this._currentDescriptions.set(dependencyString, elementaryDependencies.map(d => d.description));
    const groupedDependency = this._dependencyCreator.getUniqueDependency(originNode, targetNode,
      callback => callback(groupedDependency),
      () => this._currentDescriptions.get(dependencyString), this.svgDetailedDepsContainer, () => this.svgContainer.width)
      .byGroupingDependencies(elementaryDependencies);
    return groupedDependency;
  }

  createAndShowGroupedDependencyFrom(...elementaryDependencies) {
    const groupedDependency = this._createGroupedDependencyFrom(...elementaryDependencies);
    groupedDependency.onNodeRimChanged();
    return groupedDependency;
  }

  createAndShowDefaultGroupedDependency() {
    const elementaryDep = this.createElementaryDependency();
    return this.createAndShowGroupedDependencyFrom(elementaryDep);
  }
};

module.exports.TestDependencyCreator = TestDependencyCreator;