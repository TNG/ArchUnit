'use strict';

const svg = require('../testinfrastructure/svg-mock');
const NodeMock = require('./node-mock');

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

    this.parentOriginNode = new NodeMock(this.parentFrom, true, {x: 20, y: 110}, 40, this._index++, svgNodesContainer);
    this.originNode = new NodeMock(this.from, false, {x: 10, y: 100}, 20, this._index++, svgNodesContainer);

    this.parentTargetNode = new NodeMock(this.parentTo, true, {x: 200, y: 190}, 40, this._index++, svgNodesContainer);
    this.targetNode = new NodeMock(this.to, false, {x: 210, y: 200}, 20, this._index++, svgNodesContainer);

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