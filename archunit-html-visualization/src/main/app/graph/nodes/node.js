'use strict';

const predicates = require('../infrastructure/predicates');
const {NodeCircle, RootRect} = require('./node-shapes');
const {buildFilterGroup} = require('../filter');
const sortTopological = require('../infrastructure/graph-algorithms').sortTopological;

const nodeTypes = require('./node-types.json');

const fullNameSeparators = {
  packageSeparator: '.',
  classSeparator: '$'
};

const init = (NodeView, RootView, NodeText, visualizationFunctions, visualizationStyles) => {

  const packCirclesAndReturnEnclosingCircle = visualizationFunctions.packCirclesAndReturnEnclosingCircle;
  const calculateDefaultRadius = visualizationFunctions.calculateDefaultRadius;
  const calculateDefaultRadiusForNodeWithOneChild = visualizationFunctions.calculateDefaultRadiusForNodeWithOneChild;
  const createForceLinkSimulation = visualizationFunctions.createForceLinkSimulation;
  const createForceCollideSimulation = visualizationFunctions.createForceCollideSimulation;
  const runSimulations = visualizationFunctions.runSimulations;
  const arrayDifference = (arr1, arr2) => arr1.filter(x => arr2.indexOf(x) < 0);

  const NodeDescription = class {
    constructor(name, fullName, type) {
      this.name = name;
      this.fullName = fullName;
      this.type = type;
    }
  };

  const Node = class {
    constructor(jsonNode, layerWithinParentNode) {
      this._layerWithinParentNode = layerWithinParentNode;
      this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);
      this._text = new NodeText(this);
      this._folded = false;
      this._listeners = [];
    }

    addListener(listener) {
      this._listeners.push(listener);
      this._originalChildren.forEach(child => child.addListener(listener));
    }

    _setFilteredChildren(filteredChildren) {
      this._filteredChildren = filteredChildren;
      this._updateViewOnCurrentChildrenChanged();
    }

    isPackage() {
      return this._description.type === nodeTypes.package;
    }

    isInterface() {
      return this._description.type === nodeTypes.interface;
    }

    getName() {
      return this._description.name;
    }

    getFullName() {
      return this._description.fullName;
    }

    getParent() {
      return this._parent;
    }

    getOriginalChildren() {
      return this._originalChildren;
    }

    getCurrentChildren() {
      return this._folded ? [] : this._filteredChildren;
    }

    _isLeaf() {
      return this._filteredChildren.length === 0;
    }

    isCurrentlyLeaf() {
      return this._isLeaf() || this._folded;
    }

    isPredecessorOf(nodeFullName) {
      const keyAfterFullName = nodeFullName.charAt(this.getFullName().length);
      return nodeFullName.startsWith(this.getFullName())
        && (keyAfterFullName === fullNameSeparators.packageSeparator
          || keyAfterFullName === fullNameSeparators.classSeparator);
    }

    isPredecessorOfOrNodeItself(nodeFullName) {
      const keyAfterFullName = nodeFullName.charAt(this.getFullName().length);
      return nodeFullName.startsWith(this.getFullName())
        && (keyAfterFullName.length === 0
          || keyAfterFullName === fullNameSeparators.packageSeparator
          || keyAfterFullName === fullNameSeparators.classSeparator);
    }

    isPredecessorOfNodeOrItself(otherNode) {
      if (this === otherNode) {
        return true;
      }

      while (!otherNode.isRoot()) {
        if (otherNode.getParent() === this) {
          return true;
        }
        otherNode = otherNode.getParent();
      }
      return false;
    }

    foldNodesWithMinimumDepthThatHaveNotDescendants(nodes) {
      const childrenWithResults = this.getCurrentChildren().map(child => ({
        node: child,
        canBeHidden: child.foldNodesWithMinimumDepthThatHaveNotDescendants(nodes)
      }));
      const thisCanBeFolded = childrenWithResults.every(n => n.canBeHidden);
      if (thisCanBeFolded) {
        if (nodes.has(this) || (this.isFolded() && [...nodes].some(n => this.isPredecessorOfOrNodeItself(n.getFullName())))) {
          this.fold();
          return false;
        }
        return true;
      } else {
        childrenWithResults.filter(n => n.canBeHidden).forEach(n => n.node.fold());
        return false;
      }
    }

    isFolded() {
      return this._folded;
    }

    _setFolded(newFolded, callback) {
      this._folded = newFolded;
      this._updateViewOnCurrentChildrenChanged();
      callback();
    }

    _getClass() {
      const foldableStyle = this._isLeaf() ? "not-foldable" : "foldable";
      return `node ${this._description.type} ${foldableStyle}`;
    }

    // FIXME: Only used by tests
    getSelfAndDescendants() {
      return [this, ...this._getDescendants()];
    }

    _getDescendants() {
      const result = [];
      this.getCurrentChildren().forEach(child => child._callOnSelfThenEveryDescendant(node => result.push(node)));
      return result;
    }

    _callOnSelfThenEveryDescendant(fun) {
      fun(this);
      this.getCurrentChildren().forEach(c => c._callOnSelfThenEveryDescendant(fun));
    }

    _callOnEveryDescendantThenSelf(fun) {
      this.getCurrentChildren().forEach(c => c._callOnEveryDescendantThenSelf(fun));
      fun(this);
    }

    callOnEveryPredecessorThenSelf(fun) {
      if (!this.isRoot()) {
        this.getParent().callOnEveryPredecessorThenSelf(fun);
      }
      fun(this);
    }

    _matchesOrHasChildThatMatches(predicate) {
      return predicate(this) || this._originalChildren.some(node => node._matchesOrHasChildThatMatches(predicate));
    }

    getRadius() {
      return this.nodeShape.getRadius();
    }

    _updateViewOnCurrentChildrenChanged() {
      this._view.updateNodeType(this._getClass());
      arrayDifference(this._originalChildren, this.getCurrentChildren()).forEach(child => child._hide());
      this.getCurrentChildren().forEach(child => child._isVisible = true);
    }

    isVisible() {
      return this._isVisible;
    }

    /**
     * We go bottom to top through the tree, always creating a circle packing of the children and an enclosing
     * circle around those for the current node (but the circle packing is not applied to the nodes, it is only
     * for the radius-calculation)
     */
    _initialLayout() {
      const childrenPromises = this.getCurrentChildren().map(d => d._initialLayout());

      const promises = [];
      if (this.isCurrentlyLeaf()) {
        promises.push(this.nodeShape.changeRadius(calculateDefaultRadius(this)));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        promises.push(onlyChild.nodeShape.moveToPosition(0, 0));
        promises.push(this.nodeShape.changeRadius(calculateDefaultRadiusForNodeWithOneChild(this,
          onlyChild.getRadius(), visualizationStyles.getNodeFontSize())));
      } else {
        const childCircles = this.getCurrentChildren().map(c => ({
          r: c.nodeShape.getRadius()
        }));
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        promises.push(this.nodeShape.changeRadius(r));
      }
      return Promise.all([...childrenPromises, ...promises]);
    }
  };

  const Root = class extends Node {
    constructor(jsonNode, svgContainer, onSizeChanged, onSizeExpanded, onNodeFilterStringChanged) {
      super(jsonNode, 0);

      this._view = new RootView(svgContainer, this);

      this._root = this;
      this._parent = this;

      this._onNodeFilterStringChanged = onNodeFilterStringChanged;
      this._nameFilterString = '';

      this.nodeShape = new RootRect(this,
        {
          onJumpedToPosition: directionVector => this._view.jumpToPosition(this.nodeShape.relativePosition, directionVector),
          onRadiusChanged: () => onSizeChanged(this.nodeShape.absoluteRect.halfWidth, this.nodeShape.absoluteRect.halfHeight),
          onRadiusSet: () => this._listeners.forEach(listener => listener.onDrag(this)),
          onMovedToPosition: () => this._view.moveToPosition(this.nodeShape.relativePosition),
          onRimPositionChanged: () => onSizeExpanded(this.nodeShape.absoluteRect.halfWidth, this.nodeShape.absoluteRect.halfHeight)
        });

      const children = Array.from(jsonNode.children || []).map((jsonChild, i) => new InnerNode(jsonChild, i, this, this));
      children.forEach(child => this._view.addChildView(child._view));
      this._originalChildren = children;
      this._setFilteredChildren(this._originalChildren);

      this._filterGroup =
        buildFilterGroup('nodes', this.getFilterObject())
          .addStaticFilter('type', () => true, ['nodes.typeAndName'])
          .withStaticFilterPrecondition(true)
          .addDynamicFilter('name', () => this._getNameFilter(), ['nodes.typeAndName'])
          .withStaticFilterPrecondition(true)
          .addStaticFilter('typeAndName', node => node._matchesOrHasChildThatMatches(c => c.matchesFilter('type') && c.matchesFilter('name')), ['nodes.combinedFilter'])
          .withStaticFilterPrecondition(true)
          .addDynamicFilter('visibleViolations', () => this._getVisibleViolationsFilter(), ['nodes.combinedFilter'])
          .withStaticFilterPrecondition(false)
          .addStaticFilter('combinedFilter', node => node._matchesOrHasChildThatMatches(c => c.matchesFilter('typeAndName') && c.matchesFilter('visibleViolations')))
          .withStaticFilterPrecondition(true)
          .build();

      this._updatePromise = Promise.resolve();
      const map = new Map();
      this._callOnSelfThenEveryDescendant(n => map.set(n.getFullName(), n));
      this.getByName = name => map.get(name);
      this.doNextAndWaitFor = fun => this._updatePromise = this._updatePromise.then(fun);
      let mustRelayout = false;
      this.relayoutCompletely = () => {
        mustRelayout = true;
        this.doNextAndWaitFor(() => {
          if (mustRelayout) {
            mustRelayout = false;
            return this._relayoutCompletely();
          } else {
            return Promise.resolve();
          }
        });
      }
    }

    overlapsWith() {
      return false;
    }

    _focus(focusedChildNode) {
      this._listeners.forEach(listener => listener.onNodesFocused(focusedChildNode));
    }

    get svgElementForChildren() {
      return this._view.svgElementForChildren;
    }

    get svgElementForDependencies() {
      return this._view.svgElementForDependencies;
    }

    getNameWidth() {
      return 0;
    }

    get filterGroup() {
      return this._filterGroup;
    }

    getFilterObject() {
      const runFilter = (node, filter, key) => {
        node._matchesFilter.set(key, filter(node));
        node._originalChildren.forEach(c => runFilter(c, filter, key));
      };

      const applyFilterToNode = node => {
        node._setFilteredChildren(node._originalChildren.filter(c => c.matchesFilter('combinedFilter')));
        node._filteredChildren.forEach(c => applyFilterToNode(c));
      };

      return {
        runFilter: (filter, key) => this._originalChildren.forEach(c => runFilter(c, filter, key)),

        applyFilters: () => applyFilterToNode(this)
      };
    }

    changeTypeFilter(showInterfaces, showClasses) {
      this._filterGroup.getFilter('type').filter = this._getTypeFilter(showInterfaces, showClasses);
    }

    foldNodesWithMinimumDepthThatHaveNoViolations() {
      //FIXME: better use another function for getting the nodes involved in violations
      this.foldNodesWithMinimumDepthThatHaveNotDescendants(this.getNodesInvolvedInVisibleViolations());
    }

    /**
     * changes the name-filter so that the given node is excluded
     * @param nodeFullName fullname of the node to exclude
     */
    _addNodeToExcludeFilter(nodeFullName) {
      this._nameFilterString = [this._nameFilterString, '~' + nodeFullName].filter(el => el).join('|');
      this._onNodeFilterStringChanged(this._nameFilterString);
    }

    set nameFilterString(value) {
      this._nameFilterString = value;
    }

    /**
     * The node's full name needs to equal the root._nameFilterString or have this text as prefix
     * with a following . or $, to pass the filter.
     * '*' matches any number of arbitrary characters.
     */
    _getNameFilter() {
      const stringEqualsSubstring = predicates.stringEquals(this._nameFilterString);
      const nodeNameSatisfies = stringPredicate => node => stringPredicate(node.getFullName());
      return node => nodeNameSatisfies(stringEqualsSubstring)(node);
    }

    _getTypeFilter(showInterfaces, showClasses) {
      let predicate = node => !node.isPackage();
      predicate = showInterfaces ? predicate : predicates.and(predicate, node => !node.isInterface());
      predicate = showClasses ? predicate : predicates.and(predicate, node => node.isInterface());
      return node => predicate(node);
    }

    _getVisibleViolationsFilter() {
      const hasNodeVisibleViolation = this.getHasNodeVisibleViolation();
      return node => hasNodeVisibleViolation(node);
    }

    foldAllNodes() {
      this._callOnEveryDescendantThenSelf(node => node._initialFold());
    }

    getSelfOrFirstPredecessorMatching(matchingFunction) {
      if (matchingFunction(this)) {
        return this;
      }
      return null;
    }

    isPredecessorOf() {
      return true;
    }

    getSelfAndPredecessorsUntilExclusively(predecessor) {
      if (predecessor === this) {
        return [];
      }
      return [this];
    }

    isRoot() {
      return true;
    }

    _initialFold() {
    }

    fold() {
    }

    unfold() {
    }

    _changeFoldIfInnerNodeAndRelayout() {
    }

    getSelfAndPredecessors() {
      return [this];
    }

    _relayoutCompletely() {
      this.getCurrentChildren().forEach(c => c._callOnSelfThenEveryDescendant(node => node.nodeShape.absoluteCircle.position.unfix()));

      const promiseInitialLayout = this._initialLayout();
      const promiseForceLayout = this._forceLayout();
      return Promise.all([promiseInitialLayout, promiseForceLayout]);
    }

    /**
     * We go top bottom through the tree, always applying a force-layout to all nodes so far (that means to all nodes
     * at the current level and all nodes above), while the nodes not on the current level are fixed (and so only
     * influence the other nodes)
     */
    _forceLayout() {
      const allLinks = this.getLinks();

      const allLayoutedNodesSoFar = new Map();
      let currentNodes = new Map();
      currentNodes.set(this.getFullName(), this);

      let promises = [];

      while (currentNodes.size > 0) {

        const newNodesArray = [].concat.apply([], Array.from(currentNodes.values()).map(node => node.getCurrentChildren()));
        const newNodes = new Map();
        newNodesArray.forEach(node => newNodes.set(node.getFullName(), node));
        if (newNodes.size === 0) {
          break;
        }

        newNodesArray.forEach(node => allLayoutedNodesSoFar.set(node.getFullName(), node));
        //take only links having at least one new end node and having both end nodes in allLayoutedNodesSoFar
        const currentLinks = allLinks.filter(link => (newNodes.has(link.source) || newNodes.has(link.target))
          && (allLayoutedNodesSoFar.has(link.source) && allLayoutedNodesSoFar.has(link.target)));

        const padding = visualizationStyles.getCirclePadding();
        const allLayoutedNodesSoFarAbsNodes = Array.from(allLayoutedNodesSoFar.values()).map(node => node.nodeShape.absoluteCircle);
        const simulation = createForceLinkSimulation(padding, allLayoutedNodesSoFarAbsNodes, currentLinks);

        const currentInnerNodes = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf());
        const allCollisionSimulations = currentInnerNodes.map(node =>
          createForceCollideSimulation(padding, node.getCurrentChildren().map(n => n.nodeShape.absoluteCircle)));

        let timeOfLastUpdate = new Date().getTime();

        const onTick = () => {
          newNodesArray.forEach(node => node.nodeShape.takeAbsolutePosition(padding));
          const updateInterval = 100;
          if ((new Date().getTime() - timeOfLastUpdate > updateInterval)) {
            promises = promises.concat(newNodesArray.map(node => node.nodeShape.startMoveToIntermediatePosition()));
            timeOfLastUpdate = new Date().getTime();
          }
        };

        const k = runSimulations([simulation, ...allCollisionSimulations], simulation, 0, onTick);
        //run the remaining simulations of collision
        runSimulations(allCollisionSimulations, allCollisionSimulations[0], k, onTick);

        newNodesArray.forEach(node => node.nodeShape.completeMoveToIntermediatePosition());
        currentNodes = newNodes;
      }

      this._listeners.forEach(listener => promises.push(listener.onLayoutChanged()));

      return Promise.all(promises);
    }
  };

  const InnerNode = class extends Node {
    constructor(jsonNode, layerWithinParentNode, root, parent) {
      super(jsonNode, layerWithinParentNode);

      this._root = root;
      this._parent = parent;

      this._view = new NodeView(
        {
          nodeName: this.getName(),
          fullNodeName: this.getFullName()
        }, {
          onClick: () => this._changeFoldIfInnerNodeAndRelayout(),
          onDrag: (dx, dy) => this._drag(dx, dy),
          onCtrlClick: () => this._root._addNodeToExcludeFilter(this.getFullName())
        });

      this._matchesFilter = new Map();
      this.nodeShape = new NodeCircle(this,
        {
          onJumpedToPosition: () => this._view.jumpToPosition(this.nodeShape.relativePosition),
          onRadiusChanged: () => this._view.changeRadius(this.nodeShape.getRadius(), this._text.getY()),
          onRadiusSet: () => {
            this._view.setRadius(this.nodeShape.getRadius(), this._text.getY());
            this._listeners.forEach(listener => listener.onDrag(this));
          },
          onMovedToPosition: () => this._view.moveToPosition(this.nodeShape.relativePosition).then(() => this._view.showIfVisible(this)),
          onMovedToIntermediatePosition: () => this._view.startMoveToPosition(this.nodeShape.relativePosition)
        });

      const children = Array.from(jsonNode.children || []).map((jsonChild, i) => new InnerNode(jsonChild, i, this._root, this));
      children.forEach(child => this._view.addChildView(child._view));
      this._originalChildren = children;
      this._setFilteredChildren(this._originalChildren);
    }

    overlapsWith(otherNode) {
      const ownPredecessor = this.getSelfOrFirstPredecessorMatching(node => node.getParent().isPredecessorOfNodeOrItself(otherNode));
      const otherPredecessor = otherNode.getSelfOrFirstPredecessorMatching(node => node.getParent().isPredecessorOfNodeOrItself(this));
      if (ownPredecessor.getParent() === otherPredecessor || otherPredecessor.getParent() === ownPredecessor) {
        return false;
      } else {
        return ownPredecessor.liesInFrontOf(otherPredecessor) ? ownPredecessor.nodeShape.overlapsWith(otherNode.nodeShape) : otherPredecessor.nodeShape.overlapsWith(this.nodeShape);
      }
    }

    get svgElementForChildren() {
      return this._view.svgElementForChildren;
    }

    get svgElementForDependencies() {
      return this._view._svgElementForDependencies;
    }

    liesInFrontOf(otherNode) {
      const ownPredecessor = this.getSelfOrFirstPredecessorMatching(node => node.getParent().isPredecessorOfNodeOrItself(otherNode));
      const otherPredecessor = otherNode.getSelfOrFirstPredecessorMatching(node => node.getParent().isPredecessorOfNodeOrItself(this));
      return ownPredecessor.getParent() === otherPredecessor || (otherPredecessor.getParent() !== ownPredecessor && ownPredecessor._layerWithinParentNode > otherPredecessor._layerWithinParentNode);
    }

    /**
     * Shifts this node and its children.
     *
     * @param dx The delta in x-direction
     * @param dy The delta in y-direction
     */
    _drag(dx, dy) {
      this._root.doNextAndWaitFor(() => {
        this.nodeShape.jumpToRelativeDisplacement(dx, dy, visualizationStyles.getCirclePadding());
        this._focus();
        this._listeners.forEach(listener => listener.onDrag(this));
      });
    }

    _focus() {
      const dependenciesWithinParent = this._root.getDependenciesDirectlyWithinNode(this.getParent())
        .map(d => ({
          dependency: d,
          siblingContainingOrigin: d.originNode.getSelfOrFirstPredecessorMatching(pred => pred.getParent() === this.getParent()),
          siblingContainingTarget: d.targetNode.getSelfOrFirstPredecessorMatching(pred => pred.getParent() === this.getParent())
        }));

      const getDependentNodesWithDependenciesOf = node => {
        const dependenciesFromNode = dependenciesWithinParent.filter(d => d.siblingContainingOrigin === node);
        const dependenciesToNode = dependenciesWithinParent.filter(d => d.siblingContainingTarget === node);

        const nodesWithDependencies = new Map();
        const dependentNodes = dependenciesFromNode.map(d => d.siblingContainingTarget)
          .concat(dependenciesToNode.map(d => d.siblingContainingOrigin));
        dependentNodes.forEach(node => nodesWithDependencies.set(node, []));

        dependenciesFromNode.forEach(d => nodesWithDependencies.get(d.siblingContainingTarget).push(d));
        dependenciesToNode.forEach(d => nodesWithDependencies.get(d.siblingContainingOrigin).push(d));
        return nodesWithDependencies;
      };

      const descendantsOfEachNode = new Map();

      const dependentNodesWithDependencies = getDependentNodesWithDependenciesOf(this);
      const dependentNodesOfThis = [...dependentNodesWithDependencies.keys()];

      descendantsOfEachNode.set(this, dependentNodesOfThis);
      dependentNodesOfThis.forEach(node => descendantsOfEachNode.set(node, []));

      const getEndPointOfDependencyBelongingToNode = (dependency, node) => {
        if (dependency.siblingContainingOrigin === node) {
          return dependency.dependency.visualData.startPoint;
        } else if (dependency.siblingContainingTarget === node) {
          return dependency.dependency.visualData.endPoint;
        } else {
          throw new Error('the node must be one the predecessor in the current node of one of the end nodes of the dependency');
        }
      };

      dependentNodesOfThis.forEach((node1, i) =>
        dependentNodesOfThis.slice(i + 1).forEach(node2 => {
          if (node1.nodeShape.overlapsWith(node2.nodeShape)) {
            const dependencies1 = dependentNodesWithDependencies.get(node1);
            const dependencies2 = dependentNodesWithDependencies.get(node2);

            if (dependencies1.some(d => node2.nodeShape.containsPoint(getEndPointOfDependencyBelongingToNode(d, node1)))) {
              descendantsOfEachNode.get(node1).push(node2);
            } else if (dependencies2.some(d => node1.nodeShape.containsPoint(getEndPointOfDependencyBelongingToNode(d, node2)))) {
              descendantsOfEachNode.get(node2).push(node1);
            }
          }
        }));

      const nodesInDrawOrder = sortTopological(this, node => descendantsOfEachNode.get(node));
      const nodesToFocusSet = new Set(nodesInDrawOrder);

      const otherChildren = this._parent._originalChildren.filter(c => !nodesToFocusSet.has(c));
      otherChildren.sort((c1, c2) => c1._layerWithinParentNode - c2._layerWithinParentNode);
      otherChildren.forEach((c, i) => c._layerWithinParentNode = i);
      const sum = this._parent._originalChildren.length;
      nodesInDrawOrder.forEach((n, i) => n._layerWithinParentNode = sum - i - 1);

      // FIXME: A node should only know itself and its children, not siblings
      nodesInDrawOrder.reverse().forEach(node => {
        node._view.detachFromParent();
        this.getParent()._view.addChildView(node._view);
      });

      const nodeOverlapsDependencyEndPointButIsNotSibling = (siblingContainingEndNode, node, point) =>
        !siblingContainingEndNode !== node && node.nodeShape.containsPoint(point);

      dependenciesWithinParent.forEach(d => {
        const siblingOverlappingStartNode = new Set(this.getParent()._filteredChildren.filter(c =>
          nodeOverlapsDependencyEndPointButIsNotSibling(d.siblingContainingOrigin, c, d.dependency.visualData.startPoint)));
        const siblingOverlappingEndNode = new Set(this.getParent()._filteredChildren.filter(c =>
          nodeOverlapsDependencyEndPointButIsNotSibling(d.siblingContainingTarget, c, d.dependency.visualData.endPoint)));

        const endNodes = [d.siblingContainingOrigin, d.siblingContainingTarget];

        const nodeOverlapsBothEndPointsAndLiesInBetween = () => [...siblingOverlappingStartNode].some(n => siblingOverlappingEndNode.has(n) &&
          endNodes.some(e => e._layerWithinParentNode > n._layerWithinParentNode)
          && endNodes.some(e => e._layerWithinParentNode < n._layerWithinParentNode));
        const nodeOverlapsOneEndPointAndLiesInFrontOfTheOverlappedNodeButBehindTheOther = () =>
          [...siblingOverlappingStartNode].some(n => n._layerWithinParentNode > d.siblingContainingOrigin._layerWithinParentNode && n._layerWithinParentNode < d.siblingContainingTarget._layerWithinParentNode)
          || [...siblingOverlappingEndNode].some(n => n._layerWithinParentNode > d.siblingContainingTarget._layerWithinParentNode && n._layerWithinParentNode < d.siblingContainingOrigin._layerWithinParentNode);

        if (nodeOverlapsBothEndPointsAndLiesInBetween() || nodeOverlapsOneEndPointAndLiesInFrontOfTheOverlappedNodeButBehindTheOther()) {
          d.dependency.containerEndNode = d.dependency.calcEndNodeInBackground();
        } else {
          d.dependency.containerEndNode = d.dependency.calcEndNodeInForeground();
        }
      });

      this._parent._focus(this);
    }

    shiftLayerToEnd() {
      this._parent._originalChildren.filter(node => node._layerWithinParentNode > this._layerWithinParentNode)
        .forEach(node => node._layerWithinParentNode -= 1);
      this._layerWithinParentNode = this._parent._originalChildren.length - 1;
    }

    _hide() {
      this._isVisible = false;
      this._view.hide();
    }

    getNameWidth() {
      return this._view.getTextWidth();
    }

    matchesFilter(key) {
      if (!this._matchesFilter.has(key)) {
        throw new Error('invalid filter key');
      }
      return this._matchesFilter.get(key);
    }

    getSelfOrFirstPredecessorMatching(matchingFunction) {
      if (matchingFunction(this)) {
        return this;
      }
      return this._parent.getSelfOrFirstPredecessorMatching(matchingFunction);
    }

    getSelfAndPredecessorsUntilExclusively(predecessor) {
      if (predecessor === this) {
        return [];
      }
      const predecessors = this._parent.getSelfAndPredecessorsUntilExclusively(predecessor);
      return [...predecessors, this];
    }

    isRoot() {
      return false;
    }

    _initialFold() {
      this._setFoldedIfInnerNode(true);
    }

    _changeFoldIfInnerNodeAndRelayout() {
      if (!this._isLeaf()) {
        this._setFolded(!this._folded, () => this._listeners.forEach(listener => listener.onFold(this)));
        this._root.relayoutCompletely();
      }
    }

    fold() {
      if (!this._folded) {
        this._setFoldedIfInnerNode(true);
      }
    }

    unfold() {
      if (this._folded) {
        this._setFoldedIfInnerNode(false);
      }
    }

    // FIXME: Why does _setFoldedIfInnerNode(..) trigger 'onINITIALFold'??
    _setFoldedIfInnerNode(folded) {
      if (!this._isLeaf()) {
        this._setFolded(folded, () => this._listeners.forEach(listener => listener.onInitialFold(this)));
      }
    }

    getSelfAndPredecessors() {
      const predecessors = this._parent.getSelfAndPredecessors();
      return [this, ...predecessors];
    }
  };

  return Root;
};

module.exports = {init};