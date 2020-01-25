'use strict';

const predicates = require('../infrastructure/predicates');
const nodeText = require('./node-text');
const {NodeCircle, RootRect} = require('./node-shapes');
const {buildFilterGroup} = require('../filter');
const sortTopological = require('../infrastructure/graph-algorithms').sortTopological;

const nodeTypes = require('./node-types.json');

const init = (NodeView, RootView, visualizationFunctions, visualizationStyles) => {

  const NodeText = nodeText.init(visualizationStyles);

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
      this._folded = false;
      this._listeners = [];
    }

    addListener(listener) {
      this._listeners.push(listener);
      this._originalChildren.forEach(child => child.addListener(listener));
    }

    _setFilteredChildren(filteredChildren) {
      this._filteredChildren = filteredChildren;
      this._view.foldable = !this._isLeaf();
      this._updateViewOnCurrentChildrenChanged();
    }

    isPackage() {
      return this._description.type === nodeTypes.package;
    }

    _isInterface() {
      return this._description.type === nodeTypes.interface;
    }

    getFullName() {
      return this._description.fullName;
    }

    get parent() {
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

    isPredecessorOf(otherNode) {
      while (!otherNode.isRoot()) {
        if (otherNode.parent === this) {
          return true;
        }
        otherNode = otherNode.parent;
      }
      return false;
    }

    isPredecessorOfNodeOrItself(otherNode) {
      if (this === otherNode) {
        return true;
      }

      while (!otherNode.isRoot()) {
        if (otherNode.parent === this) {
          return true;
        }
        otherNode = otherNode.parent;
      }
      return false;
    }

    overlapsWith() {
      throw new Error('not implemented');
    }

    getNameWidth() {
      throw new Error('not implemented');
    }

    getSelfOrFirstPredecessorMatching() {
      throw new Error('not implemented');
    }

    getSelfAndPredecessorsUntilExclusively() {
      throw new Error('not implemented');
    }

    isRoot() {
      throw new Error('not implemented');
    }

    /**
     * used for folding all nodes containing no violations
     * @param nodes
     * @return {boolean}
     */
    foldNodesWithMinimumDepthThatHaveNotDescendants(nodes) {
      const childrenWithResults = this.getCurrentChildren().map(child => ({
        node: child,
        canBeHidden: child.foldNodesWithMinimumDepthThatHaveNotDescendants(nodes)
      }));
      const thisCanBeFolded = childrenWithResults.every(n => n.canBeHidden);
      if (thisCanBeFolded) {
        if (nodes.has(this) || (this.isFolded() && [...nodes].some(n => this.isPredecessorOfNodeOrItself(n)))) {
          this._fold();
          return false;
        }
        return true;
      } else {
        childrenWithResults.filter(n => n.canBeHidden).forEach(n => n.node._fold());
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
        this.parent.callOnEveryPredecessorThenSelf(fun);
      }
      fun(this);
    }

    _matchesOrHasChildThatMatches(predicate) {
      return predicate(this) || this._originalChildren.some(node => node._matchesOrHasChildThatMatches(predicate));
    }

    _updateViewOnCurrentChildrenChanged() {
      arrayDifference(this._originalChildren, this.getCurrentChildren()).forEach(child => child._hide());
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
        promises.push(this._nodeShape.changeRadius(calculateDefaultRadius(this)));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        promises.push(onlyChild._nodeShape.moveToPosition(0, 0));
        promises.push(this._nodeShape.changeRadius(calculateDefaultRadiusForNodeWithOneChild(this,
          onlyChild.getRadius(), visualizationStyles.getNodeFontSize())));
      } else {
        const childCircles = this.getCurrentChildren().map(c => ({
          r: c.getRadius()
        }));
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        promises.push(this._nodeShape.changeRadius(r));
      }
      return Promise.all([...childrenPromises, ...promises]);
    }
  };

  const Root = class extends Node {
    constructor(jsonNode, onSizeChanged, onSizeExpanded, onJumpedToPosition, onNodeFilterStringChanged) {
      super(jsonNode, 0);

      this._view = new RootView(this.getFullName(), event => {
        if (event.key === 'Alt' || event.key === 'Control') {
          this.relayoutCompletely();
        }
      });

      this._parent = this;

      this._onNodeFilterStringChanged = onNodeFilterStringChanged;
      this._nameFilterString = '';

      this._nodeShape = new RootRect(this,
        {
          onJumpedToPosition: (offsetPosition) => {
            this._view.jumpToPosition(this._nodeShape.relativePosition);
            onJumpedToPosition(offsetPosition);
          },
          onSizeChanged: () => onSizeChanged(this._nodeShape.absoluteRect.halfWidth, this._nodeShape.absoluteRect.halfHeight),
          onNodeRimChanged: () => this._listeners.forEach(listener => listener.onNodeRimChanged(this)),
          onMovedToPosition: () => this._view.moveToPosition(this._nodeShape.relativePosition),
          onRimPositionChanged: () => onSizeExpanded(this._nodeShape.absoluteRect.halfWidth, this._nodeShape.absoluteRect.halfHeight)
        });

      const children = Array.from(jsonNode.children || []).map((jsonChild, i) => new InnerNode(jsonChild, i, this, this));
      children.forEach(child => this._view.addChildView(child._view));
      this._originalChildren = children;
      this._setFilteredChildren(this._originalChildren);

      this._initializeFilterGroup();
      this._initializeNodeMap();

      this._updatePromise = Promise.resolve();
      this._mustRelayout = false;
    }

    enforceCompleteRelayout() {
      this.scheduleAction(() => this._relayoutCompletely());
    }

    relayoutCompletely() {
      this._mustRelayout = true;
      this.scheduleAction(() => {
        if (this._mustRelayout) {
          this._mustRelayout = false;
          return this._relayoutCompletely();
        }
      });
    }

    scheduleAction(func) {
      this._updatePromise = this._updatePromise.then(func);
    }

    getByName(name) {
      return this._map.get(name);
    }

    _initializeNodeMap() {
      this._map = new Map();
      this._callOnSelfThenEveryDescendant(n => this._map.set(n.getFullName(), n));
    }

    _initializeFilterGroup() {
      this._filterGroup =
        buildFilterGroup('nodes', this._getFilterObject())
          .addStaticFilter('type', () => this._getTypeFilter(true, true), ['nodes.typeAndName'])
          .withStaticFilterPrecondition(true)
          .addDynamicFilter('name', () => this._getNameFilter(), ['nodes.typeAndName'])
          .withStaticFilterPrecondition(true)
          //Hint: the _matchesOrHasChildThatMatches-method must be used already here, not only for the combinedFilter, because otherwise dependencies
          //of classes, that do not match the filter but have inner classes matching the filter, would be hidden
          .addStaticFilter('typeAndName', node => node._matchesOrHasChildThatMatches(c => c.matchesFilter('type') && c.matchesFilter('name')), ['nodes.combinedFilter'])
          .withStaticFilterPrecondition(true)
          .addDynamicFilter('visibleViolations', () => this.getVisibleViolationsFilter(), ['nodes.combinedFilter'])
          .withStaticFilterPrecondition(false)
          //TODO: maybe change to node => node.matchesFilter('typeAndName') && node._matchesOrHasChildThatMatches(c => c.matchesFilter('visibleViolations'))
          .addStaticFilter('combinedFilter', node => node._matchesOrHasChildThatMatches(c => c.matchesFilter('typeAndName') && c.matchesFilter('visibleViolations')))
          .withStaticFilterPrecondition(true)
          .build();
    }

    get view() {
      return this._view;
    }

    overlapsWith() {
      return false;
    }

    _focus() {
    }

    get svgSelectionForDependencies() {
      return this._view.svgSelectionForDependencies;
    }

    getNameWidth() {
      return 0;
    }

    get filterGroup() {
      return this._filterGroup;
    }

    _getFilterObject() {
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
      const matchesPatternSubstring = predicates.matchesPattern(this._nameFilterString);
      const nodeNameSatisfies = stringPredicate => node => stringPredicate(node.getFullName());
      return node => !node.isPackage() && nodeNameSatisfies(matchesPatternSubstring)(node);
    }

    _getTypeFilter(showInterfaces, showClasses) {
      let predicate = node => !node.isPackage();
      predicate = showInterfaces ? predicate : predicates.and(predicate, node => !node._isInterface());
      predicate = showClasses ? predicate : predicates.and(predicate, node => node._isInterface());
      return node => predicate(node);
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
      throw new Error('the given node does not exist');
    }

    isRoot() {
      return true;
    }

    _initialFold() {
    }

    _fold() {
    }

    unfold() {
    }

    _changeFoldIfInnerNodeAndRelayout() {
    }

    getSelfAndPredecessors() {
      return [this];
    }

    _relayoutCompletely() {
      this.getCurrentChildren().forEach(c => c._callOnSelfThenEveryDescendant(node => node._nodeShape.unfix()));

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
        const allLayoutedNodesSoFarAbsNodes = Array.from(allLayoutedNodesSoFar.values()).map(node => node.absoluteFixableCircle);
        const simulation = createForceLinkSimulation(padding, allLayoutedNodesSoFarAbsNodes, currentLinks);

        const currentInnerNodes = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf());
        const allCollisionSimulations = currentInnerNodes.map(node =>
          createForceCollideSimulation(padding, node.getCurrentChildren().map(n => n.absoluteFixableCircle)));

        let timeOfLastUpdate = new Date().getTime();

        const onTick = () => {
          newNodesArray.forEach(node => node._nodeShape.takeAbsolutePosition(padding));
          const updateInterval = 100;
          if ((new Date().getTime() - timeOfLastUpdate > updateInterval)) {
            promises = promises.concat(newNodesArray.map(node => node._nodeShape.startMoveToIntermediatePosition()));
            timeOfLastUpdate = new Date().getTime();
          }
        };

        const k = runSimulations([simulation, ...allCollisionSimulations], simulation, 0, onTick);
        //run the remaining simulations of collision
        runSimulations(allCollisionSimulations, allCollisionSimulations[0], k, onTick);

        newNodesArray.forEach(node => promises.push(node._nodeShape.completeMoveToIntermediatePosition()));
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

      this._text = new NodeText(this);

      this._view = new NodeView(
        {
          nodeName: this._description.name,
          fullNodeName: this.getFullName(),
          nodeType: this._description.type
        }, {
          clickHandler: event => {
            if (event.ctrlKey || event.altKey) {
              this._root._addNodeToExcludeFilter(this.getFullName());
            } else {
              this._changeFoldIfInnerNodeAndRelayout()
            }
            return false;
          },
          dragHandler: (dx, dy) => this._drag(dx, dy)
        });

      this._matchesFilter = new Map();
      this._nodeShape = new NodeCircle(this,
        {
          onJumpedToPosition: () => this._view.jumpToPosition(this._nodeShape.relativePosition),
          onRadiusChanged: () => this._view.changeRadius(this.getRadius(), this._text.getY()),
          onRadiusSet: () => this._view.setRadius(this.getRadius(), this._text.getY()),
          onNodeRimChanged: () => this._listeners.forEach(listener => listener.onNodeRimChanged(this)),
          onMovedToPosition: () => this._view.moveToPosition(this._nodeShape.relativePosition).then(() => this._view.show()),
          onMovedToIntermediatePosition: () => this._view.startMoveToPosition(this._nodeShape.relativePosition)
        });

      const children = Array.from(jsonNode.children || []).map((jsonChild, i) => new InnerNode(jsonChild, i, this._root, this));
      children.forEach(child => this._view.addChildView(child._view));
      this._originalChildren = children;
      this._setFilteredChildren(this._originalChildren);
    }

    get absoluteFixableCircle() {
      return this._nodeShape.absoluteFixableCircle;
    }

    getRadius() {
      return this.absoluteFixableCircle.r;
    }

    overlapsWith(otherNode) {
      const ownPredecessor = this.getSelfOrFirstPredecessorMatching(node => node.parent.isPredecessorOfNodeOrItself(otherNode));
      const otherPredecessor = otherNode.getSelfOrFirstPredecessorMatching(node => node.parent.isPredecessorOfNodeOrItself(this));
      if (ownPredecessor.parent === otherPredecessor || otherPredecessor.parent === ownPredecessor) {
        return false;
      } else {
        return ownPredecessor.liesInFrontOf(otherPredecessor) ? ownPredecessor._nodeShape.overlapsWith(otherNode._nodeShape) : otherPredecessor._nodeShape.overlapsWith(this._nodeShape);
      }
    }

    get svgSelectionForDependencies() {
      return this._view.svgSelectionForDependencies;
    }

    liesInFrontOf(otherNode) {
      const ownPredecessor = this.getSelfOrFirstPredecessorMatching(node => node.parent.isPredecessorOfNodeOrItself(otherNode));
      const otherPredecessor = otherNode.getSelfOrFirstPredecessorMatching(node => node.parent.isPredecessorOfNodeOrItself(this));
      return ownPredecessor.parent === otherPredecessor || (otherPredecessor.parent !== ownPredecessor && ownPredecessor._layerWithinParentNode > otherPredecessor._layerWithinParentNode);
    }

    /**
     * Shifts this node and its children.
     *
     * @param dx The delta in x-direction
     * @param dy The delta in y-direction
     */
    _drag(dx, dy) {
      this._root.scheduleAction(() => {
        this._nodeShape.jumpToRelativeDisplacement(dx, dy, visualizationStyles.getCirclePadding());
        this._listeners.forEach(listener => listener.onNodeRimChanged(this)); //FIXME: does this really make sense??
        this._focus();
      });
    }

    //FIXME: clean up
    //FIXME: improve performance
    //FIXME: Bug: when the dragged node a has a dependency to a node b, which is on a deeper level than node a, and node b is overlapped by
    // a sibling node at the position where the dependency to a touches its rim, than node b is not set to the foreground relative to its overlapping
    // sibling --> solution: after writing tests for the whole dragging stuff:
    // wenn eine Node fokussiert wird, dann sollen also alle von ihr abhängigen Nodes innerhalb ihrer Parents und Vorgänger in den Vordergrund
    // gebracht werden...ist aber nicht so trivial...
    //FIXME: when right after loading the html page a node is dragged, an error occurs and the view stucks
    //TODO: maybe move this partly into graph??
    _focus() {
      const dependenciesWithinParent = this._root.getDependenciesDirectlyWithinNode(this.parent)
        .map(d => ({
          dependency: d,
          siblingContainingOrigin: d.originNode.getSelfOrFirstPredecessorMatching(pred => pred.parent === this.parent),
          siblingContainingTarget: d.targetNode.getSelfOrFirstPredecessorMatching(pred => pred.parent === this.parent)
        }));

      const descendantsOfEachNode = new Map();

      const dependentNodesWithDependencies = this._getDependentNodesOfNodeFrom(dependenciesWithinParent);
      const dependentNodesOfThis = [...dependentNodesWithDependencies.keys()];

      descendantsOfEachNode.set(this, dependentNodesOfThis);
      dependentNodesOfThis.forEach(node => descendantsOfEachNode.set(node, []));

      dependentNodesOfThis.forEach((node1, i) =>
        dependentNodesOfThis.slice(i + 1).forEach(node2 => {
          if (node1._nodeShape.overlapsWith(node2._nodeShape)) {
            const dependencies1 = dependentNodesWithDependencies.get(node1);
            const dependencies2 = dependentNodesWithDependencies.get(node2);

            if (dependencies1.some(d => node2._nodeShape.containsPoint(this._getEndPointOfDependencyBelongingToNode(d, node1)))) {
              descendantsOfEachNode.get(node1).push(node2);
            } else if (dependencies2.some(d => node1._nodeShape.containsPoint(this._getEndPointOfDependencyBelongingToNode(d, node2)))) {
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

      // FIXME: A node should only know itself and its children, not siblings --> so: let his method operate on the children of a node
      nodesInDrawOrder.reverse().forEach(node => {
        node._view.detachFromParent();
        this.parent._view.addChildView(node._view);
      });

      dependenciesWithinParent.forEach(this._setDependencyInForegroundOrBackground);

      this._parent._focus(this);
    }

    _setDependencyInForegroundOrBackground(dependency) {
        const nodeOverlapsDependencyEndPointButIsNotSibling = (siblingContainingEndNode, node, point) =>
        !siblingContainingEndNode !== node && node._nodeShape.containsPoint(point);

        const siblingOverlappingStartNode = new Set(this.parent._filteredChildren.filter(c =>
        nodeOverlapsDependencyEndPointButIsNotSibling(dependency.siblingContainingOrigin, c, dependency.dependency.startPoint)));
        const siblingOverlappingEndNode = new Set(this.parent._filteredChildren.filter(c =>
        nodeOverlapsDependencyEndPointButIsNotSibling(dependency.siblingContainingTarget, c, dependency.dependency.endPoint)));

        const endNodes = [dependency.siblingContainingOrigin, dependency.siblingContainingTarget];

        const nodeOverlapsBothEndPointsAndLiesInBetween = () => [...siblingOverlappingStartNode].some(n => siblingOverlappingEndNode.has(n) &&
        endNodes.some(e => e._layerWithinParentNode > n._layerWithinParentNode)
        && endNodes.some(e => e._layerWithinParentNode < n._layerWithinParentNode));
        const nodeOverlapsOneEndPointAndLiesInFrontOfTheOverlappedNodeButBehindTheOther = () =>
        [...siblingOverlappingStartNode].some(n => n._layerWithinParentNode > dependency.siblingContainingOrigin._layerWithinParentNode && n._layerWithinParentNode < dependency.siblingContainingTarget._layerWithinParentNode)
        || [...siblingOverlappingEndNode].some(n => n._layerWithinParentNode > dependency.siblingContainingTarget._layerWithinParentNode && n._layerWithinParentNode < dependency.siblingContainingOrigin._layerWithinParentNode);

        if (nodeOverlapsBothEndPointsAndLiesInBetween() || nodeOverlapsOneEndPointAndLiesInFrontOfTheOverlappedNodeButBehindTheOther()) {
        dependency.dependency.setContainerEndNodeToEndNodeInBackground();
      } else {
        dependency.dependency.setContainerEndNodeToEndNodeInForeground();
      }
    }

    _getEndPointOfDependencyBelongingToNode(dependency, node) {
      if (dependency.siblingContainingOrigin === node) {
        return dependency.dependency.startPoint;
      } else if (dependency.siblingContainingTarget === node) {
        return dependency.dependency.endPoint;
      } else {
        throw new Error('the node must be one the predecessor in the current node of one of the end nodes of the dependency');
      }
    };

    _getDependentNodesOfNodeFrom(dependenciesWithinParent) {
      const dependenciesFromNode = dependenciesWithinParent.filter(d => d.siblingContainingOrigin === this);
      const dependenciesToNode = dependenciesWithinParent.filter(d => d.siblingContainingTarget === this);

      const nodesWithDependencies = new Map();
      const dependentNodes = dependenciesFromNode.map(d => d.siblingContainingTarget)
      .concat(dependenciesToNode.map(d => d.siblingContainingOrigin));
      dependentNodes.forEach(node => nodesWithDependencies.set(node, []));

      dependenciesFromNode.forEach(d => nodesWithDependencies.get(d.siblingContainingTarget).push(d));
      dependenciesToNode.forEach(d => nodesWithDependencies.get(d.siblingContainingOrigin).push(d));
      return nodesWithDependencies;
    };

    _hide() {
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
        this._root.scheduleAction(() => this._setFolded(!this._folded, () => this._listeners.forEach(listener => listener.onFoldFinished(this))));
        this._root.enforceCompleteRelayout();
      }
    }

    _fold() {
      if (!this._folded) {
        this._setFoldedIfInnerNode(true);
      }
    }

    unfold() {
      if (this._folded) {
        this._setFoldedIfInnerNode(false);
      }
    }

    _setFoldedIfInnerNode(folded) {
      if (!this._isLeaf()) {
        this._setFolded(folded, () => this._listeners.forEach(listener => listener.onFold(this)));
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
