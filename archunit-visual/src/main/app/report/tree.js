'use strict';

const predicates = require('./predicates');
const nodeTypes = require('./node-types.json');
const Vector = require('./vectors').Vector;
const vectors = require('./vectors').vectors;
//FIXME: move to visualization.functions
const d3 = require('d3');

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
        newX: Math.trunc(innerCircle.x + scale * translationVector.x),
        newY: Math.trunc(innerCircle.y + scale * translationVector.y)
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
  const arrayDifference = (arr1, arr2) => arr1.filter(x => arr2.indexOf(x) < 0);

  const NodeDescription = class {
    constructor(name, fullName, type) {
      this.name = name;
      this.fullName = fullName;
      this.type = type;
    }
  };

  const VisualData = class {
    constructor(listener, x = 0, y = 0, r = 0) {
      /**
       * the x- and y-coordinate is always relative to the parent of the node
       * (with the middle point of the parent node as origin)
       * @type {number}
       */
      this.x = x;
      this.y = y;
      this.r = r;
      this._listener = listener;
    }

    moveToRadius(r) {
      this.r = r;
      return this._listener.onMovedToRadius();
    }

    jumpToRelativeDisplacement(dx, dy, parent) {
      let newX = this.x + dx;
      let newY = this.y + dy;
      if (innerCircle({x: newX, y: newY, r: this.r}).isOutOfParentCircleOrIsChildOfRoot(parent)) {
        ({newX, newY} = translate(this)
          .withinEnclosingCircleOfRadius(parent.getRadius())
          .asFarAsPossibleInTheDirectionOf({x: dx, y: dy}));
      }
      this.x = newX;
      this.y = newY;

      this._listener.onJumpedToPosition();
    }

    moveToIntermediatePosition() {
      return this._listener.onMovedToIntermediatePosition();
    }

    startMoveToIntermediatePosition() {
      return this._listener.onStartedMoveToIntermediatePosition();
    }

    setRelativePosition(x, y) {
      this.x = x;
      this.y = y;
      return this.moveToIntermediatePosition();
    }

    setAbsoluteIntermediatePositionAndUpdateAbsoluteNode(absoluteNode, parent) {
      let x = absoluteNode.x;
      let y = absoluteNode.y;
      if (parent) {
        x -= parent.getAbsoluteNode().x;
        y -= parent.getAbsoluteNode().y;
      }

      const circle = {x, y, r: this.r};
      if (parent && innerCircle(circle).isOutOfParentCircle(parent, visualizationStyles.getCirclePadding())) {
        ({
          x,
          y
        } = translate(circle).intoEnclosingCircleOfRadius(parent.getRadius(), visualizationStyles.getCirclePadding()));
      }
      this.x = x;
      this.y = y;

      absoluteNode.x = parent.getAbsoluteNode().x + this.x;
      absoluteNode.y = parent.getAbsoluteNode().y + this.y;
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
      this.visualData = new VisualData({
        onJumpedToPosition: () => this._view.jumpToPosition(this.visualData),
        onMovedToRadius: () => Promise.all([this._view.moveToRadius(this.visualData.r, this._text.getY()), onRadiusChanged(this.getRadius())]),
        onMovedToIntermediatePosition: () => this._view.moveToPosition(this.visualData).then(() => this._view.showIfVisible(this)),
        onStartedMoveToIntermediatePosition: () => this._view.startMoveToPosition(this.visualData)
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
        this._mustRelayout = false;
        this.relayoutCompletely = () => {
          this._mustRelayout = true;
          this.doNextAndWaitFor(() => {
            if (this._mustRelayout) {
              this._mustRelayout = false;
              return this._relayoutCompletely();
            }
            else {
              return Promise.resolve();
            }
          });
        }
      }
    }

    createAbsoluteNode() {
      const absoluteVisualData = this.getAbsoluteVisualData();
      this._absoluteNode = {
        fullName: this.getFullName(),
        r: this.getRadius(),
        x: absoluteVisualData.x,
        y: absoluteVisualData.y,
        originalNode: this,
        isNotFixed: () => this._absoluteNode.fx === undefined
      };
      if (this.getParent() && this.getParent().getCurrentChildren().length === 1) {
        this._absoluteNode.fx = this._absoluteNode.x;
        this._absoluteNode.fy = this._absoluteNode.y;
      }
    }

    getAbsoluteNode() {
      return this._absoluteNode;
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
      if (this.isRoot) {
        return this._parent ? this._parent.getSelfOrFirstPredecessorMatching(matchingFunction) : null;
      }
    }

    isPredecessorOf(nodeFullName) {
      const separator = /[\\.\\$]/;
      return nodeFullName.startsWith(this.getFullName())
        && separator.test(nodeFullName.substring(this.getFullName().length, this.getFullName().length + 1));
    }

    getSelfAndPredecessorsUntilExclusively(predecessor) {
      if (predecessor === this) {
        return [];
      }
      const predecessors = this._parent ? this._parent.getSelfAndPredecessorsUntilExclusively(predecessor) : [];
      return [this, ...predecessors];
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

    /**
     * Coordinates ({x, y}) with respect to the root node.
     */
    getAbsoluteVisualData() {
      const selfAndPredecessors = this.getSelfAndPredecessors();
      return {
        r: this.visualData.r,
        x: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.x, 0),
        y: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.y, 0),
      }
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
        promises.push(onlyChild.visualData.setRelativePosition(0, 0));
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
        promises.push(this.visualData.setRelativePosition(this.getRadius(), this.getRadius())); // Shift root to the middle
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

      this.createAbsoluteNode();

      let promises = [];

      while (currentNodes.size > 0) {

        const newNodesArray = [].concat.apply([], Array.from(currentNodes.values()).map(node => node.getCurrentChildren()));
        const newNodes = new Map();

        newNodesArray.forEach(node => node.createAbsoluteNode());
        //add to newNodes and allLayoutedNodesSoFar
        newNodesArray.forEach(node => newNodes.set(node.getFullName(), node));
        newNodesArray.forEach(node => allLayoutedNodesSoFar.set(node.getFullName(), node));
        //take only links having at least one new end node and having both end nodes in allLayoutedNodesSoFar
        const currentLinks = allLinks.filter(link => (newNodes.has(link.source) || newNodes.has(link.target)) && (allLayoutedNodesSoFar.has(link.source) && allLayoutedNodesSoFar.has(link.target)));

        if (newNodes.size === 0) {
          break;
        }

        const countLinksOfNode = (allLinks, node) => allLinks.filter(d => d.source === node || d.target === node).length;

        const simulation = d3.forceSimulation()
          .alphaDecay(0.06)
          .force('link', d3.forceLink()
            .id(n => n.fullName)
            .distance(d => d.source.r + d.target.r + 2 * visualizationStyles.getCirclePadding())
            .strength(link => 3 / Math.min(countLinksOfNode(currentLinks, link.source), countLinksOfNode(currentLinks, link.target)))
            .iterations(2))
          .stop();

        //update nodes and re-update allNodes
        const ticked = () =>
          Array.from(newNodes.values()).forEach(node => node.visualData.setAbsoluteIntermediatePositionAndUpdateAbsoluteNode(node.getAbsoluteNode(), node.getParent()));

        simulation.nodes(Array.from(allLayoutedNodesSoFar.values()).map(node => node.getAbsoluteNode()));
        simulation.force('link').links(currentLinks);

        const allCollisionSimulations = Array.from(currentNodes.values()).filter(node => !node.isCurrentlyLeaf()).map(node => {
          const collisionSimulation = d3.forceSimulation()
            .alphaDecay(0.02)
            //more iterations promise a better result (that means a higher probability that no nodes are overlapping)
            .force('collide', d3.forceCollide().radius(n => n.r + visualizationStyles.getCirclePadding()).iterations(3));
          collisionSimulation.nodes(node.getCurrentChildren().map(n => n.getAbsoluteNode()))
            .stop();
          return collisionSimulation;
        });

        const updateInterval = 50;
        let timeOfLastUpdate = new Date().getTime();
        const updateViewsIfNecessary = () => {
          if ((new Date().getTime() - timeOfLastUpdate > updateInterval)) {
            promises = promises.concat(Array.from(newNodes.values()).filter(node => node.getAbsoluteNode().isNotFixed()).map(node => node.visualData.startMoveToIntermediatePosition()));
            timeOfLastUpdate = new Date().getTime();
          }
        };

        const runSimulations = (simulations, mainSimulation, iterationStart) => {
          let i = iterationStart;
          for (let n = Math.ceil(Math.log(mainSimulation.alphaMin()) / Math.log(1 - mainSimulation.alphaDecay())); i < n; ++i) {
            simulations.forEach(s => s.tick());
            ticked();
            updateViewsIfNecessary();
          }
          return i;
        };

        const k = runSimulations([simulation, ...allCollisionSimulations], simulation, 0);
        //run the remaining simulations of collision
        runSimulations(allCollisionSimulations, allCollisionSimulations[0], k);

        Array.from(newNodes.values()).forEach(node => {
          if (node.getAbsoluteNode().isNotFixed()) {
            promises.push(node.visualData.moveToIntermediatePosition());
          }
          node.getAbsoluteNode().fx = node.getAbsoluteNode().x;
          node.getAbsoluteNode().fy = node.getAbsoluteNode().y;
        });

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