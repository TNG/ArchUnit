'use strict';

const predicates = require('./predicates');
const nodeTypes = require('./node-types.json');
const Vector = require('./vectors').Vector;
const Circle = require('./vectors').Circle;
const vectors = require('./vectors').vectors;

let layer = 0;

//FIXME: shorten this file; maybe outsource the translate-function to visualization-functions
//and VisualData to own file (and rename the class to make its function more understandable)

/**
 * Takes an enclosing circle radius and an inner circle relative to the enclosing circle's center.
 *
 * @param innerCircle (tuple consisting of x, y, r, where x and y coordinate are relative to the middle point of the enclosing circle)
 */
const translate = innerCircle => ({
  /**
   * Furthermore takes a translation vector with respect to the inner circle.
   * Calculates the x- and y- coordinate for a maximal translation of the inner circle,
   * keeping the inner circle fully enclosed within the outer circle.
   *
   * @param enclosingCircleRadius radius of the outer circle
   */
  withinEnclosingCircleOfRadius: enclosingCircleRadius => ({
    /**
     * @param translationVector translation vector to be applied to an inner circle
     * @return the center coordinates of the inner circle after translation, with respect to the enclosing circle's center.
     * Keeps the inner circle enclosed within the outer circle.
     */
    asFarAsPossibleInTheDirectionOf: translationVector => {
      const c1 = translationVector.x * translationVector.x + translationVector.y * translationVector.y;
      const c2 = Math.pow(enclosingCircleRadius - innerCircle.r, 2);
      const c3 = -Math.pow(innerCircle.y * translationVector.x - innerCircle.x * translationVector.y, 2);
      const c4 = -(innerCircle.x * translationVector.x + innerCircle.y * translationVector.y);
      const scale = (c4 + Math.sqrt(c3 + c2 * c1)) / c1;
      return {
        x: Math.trunc(innerCircle.x + scale * translationVector.x),
        y: Math.trunc(innerCircle.y + scale * translationVector.y)
      };
    }
  }),

  /**
   * Shifts the inner circle towards to the center of the parent circle (which is (0, 0)), so that the inner circle
   * is completely within the enclosing circle
   * @param enclosingCircleRadius radius of the outer circle
   * @param circlePadding minimum distance from inner nodes to containing nodes borders
   * @return the center coordinates of the inner circle after the shift into the enclosing circle
   */
  intoEnclosingCircleOfRadius: (enclosingCircleRadius, circlePadding) => {
    return vectors.norm(innerCircle, enclosingCircleRadius - innerCircle.r - circlePadding);
  }
});

const innerCircle = innerCirlce => ({
  isOutOfParentCircleOrIsChildOfRoot: parent => {
    const centerDistance = new Vector(innerCirlce.x, innerCirlce.y).length();
    return centerDistance + innerCirlce.r > parent.getRadius() && !parent.isRoot();
  },
  isOutOfParentCircle: (parentCircleRadius, circlePadding) => {
    const centerDistance = new Vector(innerCirlce.x, innerCirlce.y).length();
    return centerDistance + innerCirlce.r + circlePadding > parentCircleRadius;
  }
});

const init = (View, NodeText, visualizationFunctions, visualizationStyles) => {

  const packCirclesAndReturnEnclosingCircle = visualizationFunctions.packCirclesAndReturnEnclosingCircle;
  const calculateDefaultRadius = visualizationFunctions.calculateDefaultRadius;
  const calculateDefaultRadiusForNodeWithOneChild = visualizationFunctions.calculateDefaultRadiusForNodeWithOneChild;
  const createForceLinkSimulation = visualizationFunctions.createForceLinkSimulation;
  const createForceCollideSimulation = visualizationFunctions.createForceCollideSimulation;
  const arrayDifference = (arr1, arr2) => arr1.filter(x => arr2.indexOf(x) < 0);

  const NodeDescription = class {
    constructor(name, fullName, type) {
      this.name = name;
      this.fullName = fullName;
      this.type = type;
    }
  };

  const AbsoluteCircle = class extends Circle {
    constructor(x, y, r) {
      super(x, y, r);
      this._fixed = false;
    }

    isFixed() {
      return this._fixed;
    }

    getPositionRelativeTo(parentPosition) {
      return Vector.from(this).sub(parentPosition);
    }

    update(relativePosition, parentPosition) {
      this.changeTo(relativePosition).add(parentPosition);
      this._updateFixPosition();
    }

    fix() {
      this._fixed = true;
      this._updateFixPosition();
    }

    unfix() {
      this._fixed = false;
      this.fx = undefined;
      this.fy = undefined;
    }

    _updateFixPosition() {
      if (this._fixed) {
        this.fx = this.x;
        this.fy = this.y;
      }
    }
  };

  const CircleData = class {
    constructor(node, listener, x = 0, y = 0, r = 0) {
      this.node = node;
      /**
       * the x- and y-coordinate is always relative to the parent of the node
       * (with the middle point of the parent node as origin)
       * @type {number}
       */
      this.relativePosition = new Vector(x, y);
      this.absoluteCircle = new AbsoluteCircle(x, y, r);
      this._listener = listener;
    }

    getRadius() {
      return this.absoluteCircle.r;
    }

    changeRadius(r) {
      this.absoluteCircle.r = r;
      return this._listener.onRadiusChanged();
    }

    jumpToRelativeDisplacement(dx, dy, parent) {
      const directionVector = new Vector(dx, dy);
      let newRelativePosition = vectors.add(this.relativePosition, directionVector);
      if (innerCircle(Circle.from(newRelativePosition, this.getRadius())).isOutOfParentCircleOrIsChildOfRoot(parent)) {
        newRelativePosition = translate(Circle.from(this.relativePosition, this.getRadius()))
          .withinEnclosingCircleOfRadius(parent.getRadius())
          .asFarAsPossibleInTheDirectionOf(directionVector);
      }
      this.relativePosition.changeTo(newRelativePosition);
      this._updateAbsolutePositionAndDescendants();
      this._listener.onJumpedToPosition();
    }

    _updateAbsolutePosition() {
      this.absoluteCircle.update(this.relativePosition, this.node.getParentCircle());
    }

    _updateAbsolutePositionAndDescendants() {
      this._updateAbsolutePosition();
      this.node.getCurrentChildren().forEach(child => child.circleData._updateAbsolutePositionAndDescendants());
    }

    _updateAbsolutePositionAndChildren() {
      this._updateAbsolutePosition();
      this.node.getCurrentChildren().forEach(child => child.circleData._updateAbsolutePosition());
    }

    startMoveToIntermediatePosition() {
      if (!this.absoluteCircle.isFixed()) {
        return this._listener.onMovedToIntermediatePosition();
      }
      return Promise.resolve();
    }

    completeMoveToIntermediatePosition() {
      this._updateAbsolutePositionAndChildren();
      if (!this.absoluteCircle.isFixed()) {
        this.absoluteCircle.fix();
        return this._listener.onMovedToPosition();
      }
      return Promise.resolve();
    }

    moveToPosition(x, y) {
      this.relativePosition.changeTo({x, y});
      return this.completeMoveToIntermediatePosition();
    }

    takeAbsolutePosition(parentCircle) {
      let newRelativePosition = this.absoluteCircle.getPositionRelativeTo(parentCircle);
      const circle = Circle.from(newRelativePosition, this.getRadius());
      if (parentCircle && innerCircle(circle).isOutOfParentCircle(parentCircle.r, visualizationStyles.getCirclePadding())) {
        newRelativePosition = translate(circle).intoEnclosingCircleOfRadius(parentCircle.r, visualizationStyles.getCirclePadding());
      }
      this.relativePosition.changeTo(newRelativePosition);
      this.absoluteCircle.update(this.relativePosition, parentCircle);
    }
  };

  const newFilters = (root) => ({
    typeFilter: null,
    nameFilter: null,

    apply: function () {
      root._resetFiltering();
      const applyFilter = (node, filter) => {
        node._setFilteredChildren(node._filteredChildren.filter(filter));
        node._filteredChildren.forEach(c => applyFilter(c, filter));
      };
      this.values().forEach(filter => applyFilter(root, filter));
    },

    values: function () {
      return [this.typeFilter, this.nameFilter].filter(f => !!f); // FIXME: We should not pass this object around to other modules (this is the reason for the name for now)
    }
  });

  const Node = class {
    constructor(jsonNode, svgContainer, onRadiusChanged = () => Promise.resolve(), root = null) {
      this.layer = layer++;
      this._root = root;
      if (!root) {
        this._root = this;
        this._isVisible = true;
      }
      this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);
      this._text = new NodeText(this);
      this._folded = false;

      this._view = new View(svgContainer, this, () => this._changeFoldIfInnerNodeAndRelayout(), (dx, dy) => this._drag(dx, dy));
      this.circleData = new CircleData(this,
        {
          onJumpedToPosition: () => this._view.jumpToPosition(this.circleData.relativePosition),
          onRadiusChanged: () => Promise.all([this._view.changeRadius(this.circleData.getRadius(), this._text.getY()), onRadiusChanged(this.getRadius())]),
          onMovedToPosition: () => this._view.moveToPosition(this.circleData.relativePosition).then(() => this._view.showIfVisible(this)),
          onMovedToIntermediatePosition: () => this._view.startMoveToPosition(this.circleData.relativePosition)
        });

      this._originalChildren = Array.from(jsonNode.children || []).map(jsonChild => new Node(jsonChild, this._view._svgElement, () => Promise.resolve(), this._root));
      this._originalChildren.forEach(c => c._parent = this);

      this._setFilteredChildren(this._originalChildren);
      this._filters = newFilters(this);
      this._listener = [];

      //FIXME: shorten/outsource this
      if (!root) {
        this._updatePromise = Promise.resolve();
        const map = new Map();
        this._callOnSelfThenEveryDescendant(n => map.set(n.getFullName(), n));
        this.getByName = name => map.get(name);
        this.doNextAndWaitFor = fun => this._updatePromise = this._updatePromise.then(fun);
        this.doNext = fun => this._updatePromise.then(fun);
        let mustRelayout = false;
        this.relayoutCompletely = () => {
          mustRelayout = true;
          this.doNextAndWaitFor(() => {
            if (mustRelayout) {
              mustRelayout = false;
              return this._relayoutCompletely();
            }
            else {
              return Promise.resolve();
            }
          });
        }
      }
    }

    addListener(listener) {
      this._listener.push(listener);
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

    getText() {
      return this._text;
    }

    getParent() {
      return this._parent;
    }

    getParentCircle() {
      return !this.isRoot() ? this.getParent().circleData.absoluteCircle : undefined;
    }

    getSelfOrFirstPredecessorMatching(matchingFunction) {
      if (matchingFunction(this)) {
        return this;
      }
      return this._parent ? this._parent.getSelfOrFirstPredecessorMatching(matchingFunction) : null;
    }

    isPredecessorOf(nodeFullName) {
      const separator = /[\\.\\$]/;
      return this.isRoot() || (nodeFullName.startsWith(this.getFullName())
        && separator.test(nodeFullName.substring(this.getFullName().length, this.getFullName().length + 1)));
    }

    getSelfAndPredecessorsUntilExclusively(predecessor) {
      if (predecessor === this) {
        return [];
      }
      const predecessors = this._parent ? this._parent.getSelfAndPredecessorsUntilExclusively(predecessor) : [];
      return [...predecessors, this];
    }

    getOriginalChildren() {
      return this._originalChildren;
    }

    getCurrentChildren() {
      return this._folded ? [] : this._filteredChildren;
    }

    isRoot() {
      return this._root === this;
    }

    _isLeaf() {
      return this._filteredChildren.length === 0;
    }

    isCurrentlyLeaf() {
      return this._isLeaf() || this._folded;
    }

    isFolded() {
      return this._folded;
    }

    _setFolded(getFolded) {
      this._folded = getFolded();
      this._updateViewOnCurrentChildrenChanged();
      this._listener.forEach(listener => listener.onFold(this));
    }

    foldIfInnerNode() {
      if (!this.isRoot() && !this._isLeaf()) {
        this._setFolded(() => true);
      }
    }

    _changeFoldIfInnerNodeAndRelayout() {
      if (!this.isRoot() && !this._isLeaf()) {
        this._setFolded(() => !this._folded);
        this._root.relayoutCompletely();
      }
    }

    getFilters() {
      return this._filters;
    }

    getClass() {
      const foldableStyle = this._isLeaf() ? "not-foldable" : "foldable";
      return `node ${this._description.type} ${foldableStyle}`;
    }

    getSelfAndDescendants() {
      return [this, ...this._getDescendants()];
    }

    _getDescendantsExceptNodeAndItsDescendants(node) {
      const filteredChildren = this.getCurrentChildren().filter(child => child !== node);
      const result = filteredChildren.map(child => child._getDescendantsExceptNodeAndItsDescendants(node));
      return [].concat.apply([], [filteredChildren, ...result]);
    }

    getSelfAndPredecessors() {
      const predecessors = this._parent ? this._parent.getSelfAndPredecessors() : [];
      return [this, ...predecessors];
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

    callOnEveryDescendantThenSelf(fun) {
      this.getCurrentChildren().forEach(c => c.callOnEveryDescendantThenSelf(fun));
      fun(this);
    }

    /**
     * @param predicate A predicate (i.e. function Node -> boolean)
     * @return true, iff this Node or any child (after filtering) matches the predicate
     */
    _matchesOrHasChildThatMatches(predicate) {
      return predicate(this) || this._filteredChildren.some(node => node._matchesOrHasChildThatMatches(predicate));
    }

    getRadius() {
      return this.circleData.getRadius();
    }

    _updateViewOnCurrentChildrenChanged() {
      this._view.updateNodeType(this.getClass());
      arrayDifference(this._originalChildren, this.getCurrentChildren()).forEach(child => child.hide());
      this.getCurrentChildren().forEach(child => child._isVisible = true);
    }

    hide() {
      this._isVisible = false;
      this._view.hide();
    }

    isVisible() {
      return this._isVisible;
    }

    _relayoutCompletely() {
      this._callOnSelfThenEveryDescendant(node => node.circleData.absoluteCircle.unfix());
      const promiseInitialLayout = this._initialLayout();
      const promiseForceLayout = this._forceLayout();
      return Promise.all([promiseInitialLayout, promiseForceLayout]);
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
        promises.push(this.circleData.changeRadius(calculateDefaultRadius(this)));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        promises.push(onlyChild.circleData.moveToPosition(0, 0));
        promises.push(this.circleData.changeRadius(calculateDefaultRadiusForNodeWithOneChild(this,
          onlyChild.getRadius(), visualizationStyles.getNodeFontSize())));
      } else {
        const childCircles = this.getCurrentChildren().map(c => ({
          r: c.circleData.getRadius()
        }));
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        promises.push(this.circleData.changeRadius(r));
      }

      if (this.isRoot()) {
        promises.push(this.circleData.moveToPosition(this.getRadius(), this.getRadius())); // Shift root to the middle
      }
      return Promise.all([...childrenPromises, ...promises]);
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
        newNodesArray.forEach(node => allLayoutedNodesSoFar.set(node.getFullName(), node));
        //take only links having at least one new end node and having both end nodes in allLayoutedNodesSoFar
        const currentLinks = allLinks.filter(link => (newNodes.has(link.source) || newNodes.has(link.target))
          && (allLayoutedNodesSoFar.has(link.source) && allLayoutedNodesSoFar.has(link.target)));

        if (newNodes.size === 0) {
          break;
        }

        const getAbsolutePositionWithNodeId = node => {
          node.circleData.absoluteCircle.id = node.getFullName();
          return node.circleData.absoluteCircle;
        };

        const padding = visualizationStyles.getCirclePadding();
        const allLayoutedNodesSoFarAbsNodes = Array.from(allLayoutedNodesSoFar.values()).map(node => getAbsolutePositionWithNodeId(node));
        const simulation = createForceLinkSimulation(padding, allLayoutedNodesSoFarAbsNodes, currentLinks);

        const currentInnerNodes = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf());
        const allCollisionSimulations = currentInnerNodes.map(node =>
          createForceCollideSimulation(padding, node.getCurrentChildren().map(n => getAbsolutePositionWithNodeId(n))));

        let timeOfLastUpdate = new Date().getTime();

        const onTick = () => {
          newNodesArray.forEach(node => node.circleData.takeAbsolutePosition(node.getParentCircle()));
          const updateInterval = 100;
          if ((new Date().getTime() - timeOfLastUpdate > updateInterval)) {
            promises = promises.concat(newNodesArray.map(node => node.circleData.startMoveToIntermediatePosition()));
            timeOfLastUpdate = new Date().getTime();
          }
        };

        const runSimulations = (simulations, mainSimulation, iterationStart) => {
          let i = iterationStart;
          for (let n = Math.ceil(Math.log(mainSimulation.alphaMin()) / Math.log(1 - mainSimulation.alphaDecay())); i < n; ++i) {
            simulations.forEach(s => s.tick());
            onTick();
          }
          return i;
        };

        const k = runSimulations([simulation, ...allCollisionSimulations], simulation, 0);
        //run the remaining simulations of collision
        runSimulations(allCollisionSimulations, allCollisionSimulations[0], k);

        newNodesArray.forEach(node => node.circleData.completeMoveToIntermediatePosition());
        currentNodes = newNodes;
      }

      this._listener.forEach(listener => promises.push(listener.onLayoutChanged()));

      return Promise.all(promises);
    }

    /**
     * Shifts this node and its children.
     *
     * @param dx The delta in x-direction
     * @param dy The delta in y-direction
     */
    _drag(dx, dy) {
      this._root.doNextAndWaitFor(() => {
        this.circleData.jumpToRelativeDisplacement(dx, dy, this.getParent());
        this._listener.forEach(listener => listener.onDrag(this));

        const nodesWithPotentialDependencies = this._root._getDescendants().filter(node => node._description.type !== nodeTypes.package || node.isFolded());
        this._listener.forEach(listener => listener.resetNodesOverlapping());
        nodesWithPotentialDependencies.reduce((acc, node) => node._checkOverlappingWithNodes(acc), nodesWithPotentialDependencies);
        this._listener.forEach(listener => listener.finishOnNodesOverlapping());
      });
    }

    _checkOverlappingWithNodes(nodes) {
      const nodesWithoutOwnDescendants = nodes.filter(node => !(node === this || this.isPredecessorOf(node.getFullName())));

      const isOverlappingWithAnyNode = nodesWithoutOwnDescendants.map(node => this._checkOverlappingWithSingleNode(node)).some(bol => bol);
      if (this._description.type !== nodeTypes.package || isOverlappingWithAnyNode) {
        return nodes.filter(node => node !== this);
      }
      else {
        return nodesWithoutOwnDescendants.filter(node => node !== this);
      }
    }

    _checkOverlappingWithSingleNode(node) {
      const middlePointDistance = vectors.distance(this.circleData.absoluteCircle, node.circleData.absoluteCircle);
      const areOverlapping = middlePointDistance <= this.getRadius() + node.getRadius();
      const sortedNodes = this.layer < node.layer ? {first: this, second: node} : {first: node, second: this};
      if (areOverlapping && sortedNodes.second._description.type !== nodeTypes.package) {
        this._listener.forEach(listener => listener.onNodesOverlapping(sortedNodes.first.getFullName(),
          sortedNodes.second.circleData.absoluteCircle));
      }
      return areOverlapping;
    }

    _resetFiltering() {
      this.getOriginalChildren().forEach(node => node._resetFiltering());
      this._setFilteredChildren(this.getOriginalChildren());
    }

    /**
     * Hides all nodes that don't contain the supplied filterString.
     *
     * @param nodeNameSubstring The node's full name needs to contain this text, to pass the filter.
     * '*' matches any number of arbitrary characters. If nodeNamesSubstring ends with a space,
     * the node's full name has to end with nodeNamesSubstring to pass the filter.
     * @param exclude If true, the condition is inverted,
     * i.e. nodes with names not containing the string will pass the filter.
     */
    filterByName(nodeNameSubstring, exclude) {
      const stringContainsSubstring = predicates.stringContains(nodeNameSubstring);
      const stringPredicate = exclude ? predicates.not(stringContainsSubstring) : stringContainsSubstring;
      const nodeNameSatisfies = stringPredicate => node => stringPredicate(node.getFullName());

      this._filters.nameFilter = node => node._matchesOrHasChildThatMatches(nodeNameSatisfies(stringPredicate));
      this._root.doNextAndWaitFor(() => this._filters.apply());
    }

    filterByType(showInterfaces, showClasses) {
      let predicate = node => !node.isPackage();
      predicate = showInterfaces ? predicate : predicates.and(predicate, node => !node.isInterface());
      predicate = showClasses ? predicate : predicates.and(predicate, node => node.isInterface());

      this._filters.typeFilter = node => node._matchesOrHasChildThatMatches(predicate);
      this._root.doNextAndWaitFor(() => this._filters.apply());
    }
  };

  return Node;
};

module.exports.init = (View, NodeText, visualizationFunctions, visualizationStyles) => {
  return {
    Node: init(View, NodeText, visualizationFunctions, visualizationStyles)
  };
};