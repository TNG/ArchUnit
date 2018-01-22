'use strict';

const predicates = require('./predicates');
const nodeTypes = require('./node-types.json');
const Vector = require('./vectors').Vector;
const vectors = require('./vectors').vectors;

// FIXME: Test missing!! (There is only one for not dragging out)
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
  isOutOfParentCircle: (parent, circlePadding) => {
    const centerDistance = new Vector(innerCirlce.x, innerCirlce.y).length();
    return centerDistance + innerCirlce.r + circlePadding > parent.getRadius();
  }
});

const init = (View, NodeText, visualizationFunctions, visualizationStyles) => {

  const packCirclesAndReturnEnclosingCircle = visualizationFunctions.packCirclesAndReturnEnclosingCircle;
  const calculateDefaultRadius = visualizationFunctions.calculateDefaultRadius;
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

  const withRadius = (position, r) => {
    position.r = r;
    return position;
  };

  const getAbsolutePositionOfNodeOrZero = node => node ? node.visualData.absolutePosition : Vector.zeroVector();

  const AbsolutePosition = class extends Vector {
    constructor(x, y, r) {
      super(x, y);
      this.r = r;
      this._isFixed = false;
    }

    isFixed() {
      return this._isFixed;
    }

    getRelativePosition(parent) {
      return Vector.from(this).sub(getAbsolutePositionOfNodeOrZero(parent));
    }

    update(relativePosition, parent) {
      this.changeTo(relativePosition).add(getAbsolutePositionOfNodeOrZero(parent));
      this._updateFixPosition();
    }

    _updateFixPosition() {
      if (this._isFixed) {
        this.fx = this.x;
        this.fy = this.y;
      }
    }

    fix() {
      this._isFixed = true;
      this._updateFixPosition();
    }

    unfix() {
      this._isFixed = false;
      this.fx = undefined;
      this.fy = undefined;
    }
  };

  const VisualData = class {
    constructor(node, listener, x = 0, y = 0, r = 0) {
      this.node = node;
      /**
       * the x- and y-coordinate is always relative to the parent of the node
       * (with the middle point of the parent node as origin)
       * @type {number}
       */
      this.relativePosition = new Vector(x, y);
      this.absolutePosition = new AbsolutePosition(x, y, r);
      this.r = r;
      this._listener = listener;
    }

    moveToRadius(r) {
      this.r = r;
      this.absolutePosition.r = r;
      return this._listener.onMovedToRadius();
    }

    jumpToRelativeDisplacement(dx, dy, parent) {
      const directionVector = vectors.vectorOf(dx, dy);
      let newRelativePosition = vectors.addVectors(this.relativePosition, directionVector);
      if (innerCircle(withRadius(newRelativePosition, this.r)).isOutOfParentCircleOrIsChildOfRoot(parent)) {
        newRelativePosition = translate(withRadius(this.relativePosition, this.r))
          .withinEnclosingCircleOfRadius(parent.getRadius())
          .asFarAsPossibleInTheDirectionOf(directionVector);
      }
      this.relativePosition.changeTo(newRelativePosition);
      this._updateAbsolutePositionAndDescendants();
      this._listener.onJumpedToPosition();
    }

    _updateAbsolutePosition() {
      this.absolutePosition.update(this.relativePosition, this.node.getParent());
    }

    _updateAbsolutePositionAndDescendants() {
      this._updateAbsolutePosition();
      this.node.getCurrentChildren().forEach(child => child.visualData._updateAbsolutePositionAndDescendants());
    }

    _updateAbsolutePositionAndChildren() {
      this._updateAbsolutePosition();
      this.node.getCurrentChildren().forEach(child => child.visualData._updateAbsolutePosition());
    }

    startMoveToIntermediatePosition() {
      if (!this.absolutePosition.isFixed()) {
        return this._listener.onMovedToIntermediatePosition();
      }
      return Promise.resolve();
    }

    completeMoveToIntermediatePosition() {
      this._updateAbsolutePositionAndChildren();
      if (!this.absolutePosition.isFixed()) {
        this.absolutePosition.fix();
        return this._listener.onMovedToPosition();
      }
      return Promise.resolve();
    }

    moveToPosition(x, y) {
      this.relativePosition.changeTo({x, y});
      return this.completeMoveToIntermediatePosition();
    }

    takeAbsolutePosition(parent) {
      let newRelativePosition = this.absolutePosition.getRelativePosition(parent);
      const circle = withRadius(newRelativePosition, this.r);
      if (parent && innerCircle(circle).isOutOfParentCircle(parent, visualizationStyles.getCirclePadding())) {
        newRelativePosition = translate(circle).intoEnclosingCircleOfRadius(parent.getRadius(), visualizationStyles.getCirclePadding());
      }
      this.relativePosition.changeTo(newRelativePosition);
      this.absolutePosition.update(this.relativePosition, parent);
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

  //FIXME: maybe set svgContainer already in initNodeView, because Node does not need to know the svg-container?
  const Node = class {
    constructor(jsonNode, svgContainer, onRadiusChanged = () => Promise.resolve(), root = null) {
      this._root = root;
      if (!root) {
        this._root = this;
        this._isVisible = true;
      }
      this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);
      this._text = new NodeText(this);
      this._folded = false;

      this._view = new View(svgContainer, this, () => this._changeFoldIfInnerNodeAndRelayout(), (dx, dy) => this._drag(dx, dy));
      this.visualData = new VisualData(this,
        {
          onJumpedToPosition: () => this._view.jumpToPosition(this.visualData.relativePosition),
          onMovedToRadius: () => Promise.all([this._view.moveToRadius(this.visualData.r, this._text.getY()), onRadiusChanged(this.getRadius())]),
          onMovedToPosition: () => this._view.moveToPosition(this.visualData.relativePosition).then(() => this._view.showIfVisible(this)),
          onMovedToIntermediatePosition: () => this._view.startMoveToPosition(this.visualData.relativePosition)
        });

      this._originalChildren = Array.from(jsonNode.children || []).map(jsonChild => new Node(jsonChild, this._view._svgElement, () => Promise.resolve(), this._root));
      this._originalChildren.forEach(c => c._parent = this);

      this._setFilteredChildren(this._originalChildren);
      this._filters = newFilters(this);
      this._listener = [];

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

    getSelfOrFirstPredecessorMatching(matchingFunction) {
      if (matchingFunction(this)) {
        return this;
      }
      return this._parent ? this._parent.getSelfOrFirstPredecessorMatching(matchingFunction) : null;
    }

    isPredecessorOf(nodeFullName) {
      const separator = /[\\.\\$]/;
      return this.isRoot() || nodeFullName.startsWith(this.getFullName())
        && separator.test(nodeFullName.substring(this.getFullName().length, this.getFullName().length + 1));
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
      return this.visualData.r;
    }


    //TODO: add test for this scenario: filter and unfilter --> check, if foldable is in css-class again,
    // if all children are filtered away
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
      this._callOnSelfThenEveryDescendant(node => node.visualData.absolutePosition.unfix());
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
        promises.push(this.visualData.moveToRadius(calculateDefaultRadius(this)));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        promises.push(onlyChild.visualData.moveToPosition(0, 0));
        promises.push(this.visualData.moveToRadius(Math.max(calculateDefaultRadius(this), 2 * onlyChild.getRadius())));
      } else {
        const childCircles = this.getCurrentChildren().map(c => ({
          r: c.visualData.r
        }));
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        promises.push(this.visualData.moveToRadius(r));
      }

      if (this.isRoot()) {
        promises.push(this.visualData.moveToPosition(this.getRadius(), this.getRadius())); // Shift root to the middle
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
          node.visualData.absolutePosition.id = node.getFullName();
          return node.visualData.absolutePosition;
        };

        const padding = visualizationStyles.getCirclePadding();
        const allLayoutedNodesSoFarAbsNodes = Array.from(allLayoutedNodesSoFar.values()).map(node => getAbsolutePositionWithNodeId(node));
        const simulation = createForceLinkSimulation(padding, allLayoutedNodesSoFarAbsNodes, currentLinks);

        const currentInnerNodes = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf());
        const allCollisionSimulations = currentInnerNodes.map(node =>
          createForceCollideSimulation(padding, node.getCurrentChildren().map(n => getAbsolutePositionWithNodeId(n))));

        let timeOfLastUpdate = new Date().getTime();

        const onTick = () => {
          newNodesArray.forEach(node => node.visualData.takeAbsolutePosition(node.getParent()));
          const updateInterval = 100;
          if ((new Date().getTime() - timeOfLastUpdate > updateInterval)) {
            promises = promises.concat(newNodesArray.map(node => node.visualData.startMoveToIntermediatePosition()));
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

        newNodesArray.forEach(node => node.visualData.completeMoveToIntermediatePosition());
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
        this.visualData.jumpToRelativeDisplacement(dx, dy, this.getParent());
        this._listener.forEach(listener => listener.onDrag(this));
      });
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