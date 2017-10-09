'use strict';

const predicates = require('./predicates');
const nodeTypes = require('./node-types.json');
const Vector = require('./vectors').Vector;

// FIXME: Test missing!! (There is only one for not dragging out)
/**
 * Takes an enclosing circle radius and an inner circle relative to the enclosing circle's center.
 * Furthermore takes a translation vector with respect to the inner circle.
 * Calculates the x- and y- coordinate for a maximal translation of the inner circle,
 * keeping the inner circle fully enclosed within the outer circle.
 *
 * @param innerCircle (tuple consisting of x, y, r, where x and y coordinate are relative to the middle point of the enclosing circle)
 */
const translate = innerCircle => ({
  /**
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
  })
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
    constructor(x = 0, y = 0, r = 0) {
      this.x = x;
      this.y = y;
      this.r = r;
      this._onMove = () => {
      };
      this._onChange = () => {
      };
    }

    changeRadius(r) {
      this.r = r;
    }

    changePosition(position) {
      this.x = position.x;
      this.y = position.y;
    }

    move(dx, dy, parent) {
      let newX = this.x + dx;
      let newY = this.y + dy;
      const centerDistance = new Vector(newX, newY).length();
      if (centerDistance + this.r > parent.getRadius() && !parent.isRoot()) {
        ({newX, newY} = translate(this)
          .withinEnclosingCircleOfRadius(parent.getRadius())
          .asFarAsPossibleInTheDirectionOf({x: dx, y: dy}));
      }
      this.x = newX;
      this.y = newY;

      return this._onMove();
    }
  };

  const newFilters = (root) => ({
    typeFilter: null,
    nameFilter: null,

    apply: function () {
      root._resetFiltering();
      const applyFilter = (node, filters) => {
        node.setFilteredChildren(filters.reduce((childrenSoFar, filter) => childrenSoFar.filter(filter), node._filteredChildren));
        node._filteredChildren.forEach(c => applyFilter(c, filters));
      };
      applyFilter(root, this.values());
      root.relayout();
    },

    values: function () {
      return [this.typeFilter, this.nameFilter].filter(f => !!f); // FIXME: We should not pass this object around to other modules (this is the reason for the name for now)
    }
  });

  const Node = class {
    constructor(jsonNode, root) {
      this._root = root;
      if (!root) {
        this._root = this;
      }
      this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);

      this._originalChildren = Array.from(jsonNode.children || []).map(jsonChild => new Node(jsonChild, this._root));
      this._originalChildren.forEach(c => c._parent = this);

      this._filteredChildren = this._originalChildren;
      this._folded = false;
      this._filters = newFilters(this);

      this._onCurrentChildrenChanged = () => {};

      this.visualData = new VisualData();
      this._text = new NodeText(this);

      this._onFold = () => new Promise(resolve => resolve());
      this._onDrag = () => {};
      this._updateViewOnFold = () => new Promise(resolve => resolve());

      if (!root) {
        this.updatePromise = new Promise(resolve => resolve());
        this.relayout();
      }
    }

    setFilteredChildren(filteredChildren) {
      this._filteredChildren = filteredChildren;
      this._onCurrentChildrenChanged();
    }

    setOnDrag(onDrag) {
      this._onDrag = onDrag;
      this._originalChildren.forEach(child => child.setOnDrag(onDrag));
    }

    setOnFold(onFold) {
      this._onFold = onFold;
      this._originalChildren.forEach(child => child.setOnFold(onFold));
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

    getOriginalChildren() {
      return this._originalChildren;
    }

    getCurrentChildren() {
      return this._folded ? [] : this._filteredChildren;
    }

    isRoot() {
      return !this._parent;
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
      if (!this._isLeaf()) {
        this._root.updatePromise = this._root.updatePromise.then(() => {
          this._folded = getFolded();
          this._onCurrentChildrenChanged();
          this._root.relayout();
          return Promise.all([this._onFold(this), this._updateViewOnFold()]);
        });
      }
    }

    fold() {
      this._setFolded(() => true);
    }

    changeFold() {
      this._setFolded(() => !this._folded);
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

    getX() {
      return this.visualData.x;
    }

    getY() {
      return this.visualData.y;
    }

    getRadius() {
      return this.visualData.r;
    }

    /**
     * Coordinates ({x, y}) with respect to the root node.
     */
    getAbsoluteCoords() {
      const selfAndPredecessors = this.getSelfAndPredecessors();
      return {
        r: this.visualData.r,
        x: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.x, 0),
        y: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.y, 0),
      }
    }

    initView(svgElement, callback) {
      this._view = new View(svgElement, this);
      this._onCurrentChildrenChanged = () => arrayDifference(this._originalChildren, this.getCurrentChildren()).forEach(child => child._view.hide());
      this.visualData._onMove = () => this._view.updatePosition(this.visualData);
      this._updateViewOnFold = () => {
        callback();
        return this._root._updateView();
      };

      if (!this.isRoot() && !this._isLeaf()) {
        this._view.onClick(() => {
          this.changeFold();
        });
      }

      this._view.onDrag((dx, dy) => {
        this._drag(dx, dy);
      });

      this._originalChildren.forEach(child => child.initView(this._view._svgElement, callback));
    }

    _updateView() {
      const promise = this._view.updateWithTransition(this.visualData, this._text.getY()).then(() => this._view.show());
      return Promise.all([promise, ...this.getCurrentChildren().map(child => child._updateView())]);
    }

    /**
     * We go bottom to top through the tree, always creating a circle packing of the children and an enclosing
     * circle around those for the current node.
     */
    relayout() {
      this.getCurrentChildren().forEach(d => d.relayout());

      if (this.isCurrentlyLeaf()) {
        this.visualData.changeRadius(calculateDefaultRadius(this));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        onlyChild.visualData.changePosition({x: 0, y:0});
        this.visualData.changeRadius(2 * onlyChild.getRadius());
      } else {
        const childCircles = this.getCurrentChildren().map(c => ({
          r: c.visualData.r,
          nodeVisualData: c.visualData
        }));
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        childCircles.forEach(c => c.nodeVisualData.changePosition(c));
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        this.visualData.changeRadius(r);
        this.visualData.changePosition(circle);
      }

      if (this.isRoot()) {
        this.visualData.changePosition({x: this.getRadius(), y: this.getRadius()}); // Shift root to the middle
      }
    }

    /**
     * Shifts this node and its children.
     *
     * @param dx The delta in x-direction
     * @param dy The delta in y-direction
     */
    _drag(dx, dy) {
      this._root.updatePromise.then(() => {
        this.visualData.move(dx, dy, this.getParent());
        this._onDrag(this);
      });
    }

    _resetFiltering() {
      this.getOriginalChildren().forEach(node => node._resetFiltering());
      this.setFilteredChildren(this.getOriginalChildren());
    }

    /**
     * Hides all nodes that don't contain the supplied filterString.
     *
     * @param nodeNameSubstring The node's full name needs to contain this text, to pass the filter. '*' matches any number of arbitrary characters.
     * @param exclude If true, the condition is inverted, i.e. nodes with names not containing the string will pass the filter.
     */
    filterByName(nodeNameSubstring, exclude) {
      const stringContainsSubstring = predicates.stringContains(nodeNameSubstring);
      const stringPredicate = exclude ? predicates.not(stringContainsSubstring) : stringContainsSubstring;
      const nodeNameSatisfies = stringPredicate => node => stringPredicate(node.getFullName());

      this._filters.nameFilter = node => node._matchesOrHasChildThatMatches(nodeNameSatisfies(stringPredicate));
      this._filters.apply();
    }

    filterByType(showInterfaces, showClasses) {
      let predicate = node => !node.isPackage();
      predicate = showInterfaces ? predicate : predicates.and(predicate, node => !node.isInterface());
      predicate = showClasses ? predicate : predicates.and(predicate, node => node.isInterface());

      this._filters.typeFilter = node => node._matchesOrHasChildThatMatches(predicate);
      this._filters.apply();
    }
  };

  return jsonRoot => {
    const root = new Node(jsonRoot);

    const map = new Map();
    root._callOnSelfThenEveryDescendant(n => map.set(n.getFullName(), n));
    root.getByName = name => map.get(name);

    return root;
  };
};

module.exports.init = (View, NodeText, visualizationFunctions, visualizationStyles) => {
  return {
    jsonToRoot: init(View, NodeText, visualizationFunctions, visualizationStyles)
  };
};