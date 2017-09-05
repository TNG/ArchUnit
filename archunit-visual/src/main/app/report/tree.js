'use strict';

const predicates = require('./predicates');
const nodeTypes = require('./node-types.json');
const Vector = require('./vectors').Vector;
const vectors = require('./vectors').vectors;

const init = (View, NodeText, visualizationFunctions, visualizationStyles, jsonToDependencies) => {

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

  const fold = (node, folded) => {
    if (!node.isLeaf()) {
      node._folded = folded;
      return true;
    }
    return false;
  };

  const getRoot = node => {
    let root = node;
    while (root._parent) {
      root = root._parent;
    }
    return root;
  };

  const getDependencies = node => {
    return getRoot(node)._dependencies;
  };

  const VisualData = class {
    constructor(x = 0, y = 0, r = 0) {
      this.x = x;
      this.y = y;
      this.r = r;
    }

    move(dx, dy, parent) {
      let newX = this.x + dx;
      let newY = this.y + dy;
      const centerDistance = new Vector(newX, newY).length();
      if (centerDistance + this.r > parent.getRadius() && !parent.isRoot()) {
        const maximumVector = vectors.getMaximumShiftedVectorWithinCircle(parent.getRadius(), this, {x: dx, y: dy});
        newX = Math.trunc(maximumVector.x);
        newY = Math.trunc(maximumVector.y);
      }
      this.x = newX;
      this.y = newY;
    }

    update(x, y, r) {
      this.x = x;
      this.y = y;
      if (r) {
        this.r = r;
      }
    }
  };

  const newFilters = (root) => ({
    typeFilter: null,
    nameFilter: null,

    apply: function () {
      root.resetFiltering();
      const applyFilter = (node, filters) => {
        node._filteredChildren = filters.reduce((childrenSoFar, filter) => childrenSoFar.filter(filter), node._filteredChildren);
        node._filteredChildren.forEach(c => applyFilter(c, filters));
      };
      applyFilter(root, this.values());
    },

    values: function () {
      return [this.typeFilter, this.nameFilter].filter(f => !!f); // FIXME: We should not pass this object around to other modules (this is the reason for the name for now)
    }
  });

  const Node = class {
    constructor(jsonNode) {
      this._description = new NodeDescription(jsonNode.name, jsonNode.fullName, jsonNode.type);

      this._originalChildren = Array.from(jsonNode.children || []).map(jsonChild => new Node(jsonChild));
      this._originalChildren.forEach(c => c._parent = this);

      this._filteredChildren = this._originalChildren;
      this._folded = false;
      this._filters = newFilters(this);

      this.visualData = new VisualData();
      this._text = new NodeText(this);

      if (this.isRoot()) {
        this.relayout();
      }
    }

    isPackage() {
      return this.getType() === nodeTypes.package;
    }

    isInterface() {
      return this.getType() === nodeTypes.interface;
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

    getType() {
      return this._description.type;
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

    isLeaf() {
      return this._filteredChildren.length === 0;
    }

    isCurrentlyLeaf() {
      return this.isLeaf() || this._folded;
    }

    isChildOf(node) {
      if (node === this) {
        return true; // FIXME: Why does a method called 'isChildOf' return true for the node itself??
      }
      return node.getDescendants().indexOf(this) !== -1;
    }

    isFolded() {
      return this._folded;
    }

    fold() {
      const wasFolded = fold(this, true);
      if (wasFolded) {
        getDependencies(this).changeFold(this.getFullName(), this.isFolded());
      }
      return wasFolded;
    }

    changeFold() {
      const foldChanged = fold(this, !this._folded);
      if (foldChanged) {
        getDependencies(this).changeFold(this.getFullName(), this.isFolded());
        getRoot(this).relayout();
      }
      return foldChanged;
    }

    getFilters() {
      return this._filters;
    }

    getClass() {
      const foldableStyle = this.isLeaf() ? "not-foldable" : "foldable";
      return `node ${this.getType()} ${foldableStyle}`;
    }

    getSelfAndDescendants() {
      return [this, ...this.getDescendants()];
    }

    getSelfAndPredecessors() {
      const result = [];
      let current = this;
      while (current) {
        result.push(current);
        current = current._parent;
      }
      return result;
    }

    getVisibleDependencies() {
      return getDependencies(this).getVisible();
    }

    getDescendants() {
      const result = [];
      this.getCurrentChildren().forEach(child => child.callOnSelfThenEveryDescendant(node => result.push(node)));
      return result;
    }

    callOnSelfThenEveryDescendant(fun) {
      fun(this);
      this.getCurrentChildren().forEach(c => c.callOnSelfThenEveryDescendant(fun));
    }

    callOnEveryDescendantThenSelf(fun) {
      this.getCurrentChildren().forEach(c => c.callOnEveryDescendantThenSelf(fun));
      fun(this);
    }

    /**
     * @param predicate A predicate (i.e. function Node -> boolean)
     * @return true, iff this Node or any child (after filtering) matches the predicate
     */
    matchesOrHasChildThatMatches(predicate) {
      return predicate(this) || this._filteredChildren.some(node => node.matchesOrHasChildThatMatches(predicate));
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

    getCoords() {
      return {x: this.getX(), y: this.getY()};
    }

    getAbsoluteVisualData() {
      const selfAndPredecessors = this.getSelfAndPredecessors();
      return {
        r: this.visualData.r,
        x: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.x, 0),
        y: selfAndPredecessors.reduce((sum, predecessor) => sum + predecessor.visualData.y, 0),
      }
    }

    initView(svgElement, onNodeFoldChanged, onMoved) {
      this._view = new View(svgElement, this);
      if (!this.isRoot() && !this.isLeaf()) {
        this._view.onClick(() => {
          if (this.changeFold()) {
            onNodeFoldChanged();
          }
        });
      }
      this._view.onDrag((dx, dy) => {
        this.drag(dx, dy);
        onMoved(this);
      });
      this._originalChildren.forEach(child => child.initView(this._view._svgElement, onNodeFoldChanged, onMoved));
    }

    updateView() {
      arrayDifference(this._originalChildren, this.getCurrentChildren()).forEach(child => child._view.hide());
      const promise = this._view.update(this.visualData, this._text.getY()).then(() => this._view.show());
      return Promise.all([promise, ...this.getCurrentChildren().map(child => child.updateView())]);
    }

    updatePositionOfView() {
      this._view.updatePosition(this.visualData);
    }

    /**
     * We go bottom to top through the tree, always creating a circle packing of the children and an enclosing
     * circle around those for the current node. The coordinates of the circle of any node will be shifted, when
     * the next higher circle packing is created, thus the coordinates of the children run out of sync (we would
     * have to adjust those recursively in every step, wasting performance).
     * We'll fix this in _finishLayout().
     *
     * @private
     */
    _prepareLayout() {
      this.getCurrentChildren().forEach(d => d._prepareLayout());

      if (this.isCurrentlyLeaf()) {
        this.visualData.update(0, 0, calculateDefaultRadius(this));
      } else if (this.getCurrentChildren().length === 1) {
        const onlyChild = this.getCurrentChildren()[0];
        this.visualData.update(onlyChild.getX(), onlyChild.getY(), 3 * onlyChild.getRadius());
      } else {
        const childCircles = this.getCurrentChildren().map(c => c.visualData);
        const circle = packCirclesAndReturnEnclosingCircle(childCircles, visualizationStyles.getCirclePadding());
        const r = Math.max(circle.r, calculateDefaultRadius(this));
        this.visualData.update(circle.x, circle.y, r);
      }
    }

    relayout() {
      this._prepareLayout();

      if (this.isRoot()) {
        this.visualData.update(this.getRadius(), this.getRadius()); // Shift root to the middle
      }
    }

    /**
     * Shifts this node and its children.
     *
     * @param dx The delta in x-direction
     * @param dy The delta in y-direction
     */
    drag(dx, dy) {
      this.visualData.move(dx, dy, this.getParent());
    }

    resetFiltering() {
      this.getOriginalChildren().forEach(node => node.resetFiltering());
      this._filteredChildren = this.getOriginalChildren();
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

      this._filters.nameFilter = node => node.matchesOrHasChildThatMatches(nodeNameSatisfies(stringPredicate));
      this._filters.apply();
      getDependencies(this).setNodeFilters(getRoot(this).getFilters());
    }

    filterByType(showInterfaces, showClasses) {
      let predicate = node => !node.isPackage();
      predicate = showInterfaces ? predicate : predicates.and(predicate, node => !node.isInterface());
      predicate = showClasses ? predicate : predicates.and(predicate, node => node.isInterface());

      this._filters.typeFilter = node => node.matchesOrHasChildThatMatches(predicate);
      this._filters.apply();
      getDependencies(this).setNodeFilters(getRoot(this).getFilters());
    }

    resetFilterByType() {
      this._filters.typeFilter = null;
      this._filters.apply();
      getDependencies(this).setNodeFilters(getRoot(this).getFilters());
    }
  };

  return jsonRoot => {
    const root = new Node(jsonRoot);

    const map = new Map();
    root.callOnSelfThenEveryDescendant(n => map.set(n.getFullName(), n));
    root.getByName = name => map.get(name);

    root._dependencies = jsonToDependencies(jsonRoot, root);
    root.getDetailedDependenciesOf = (from, to) => root._dependencies.getDetailedDependenciesOf(from, to);
    root.filterDependenciesByType = (typeFilterConfig) => root._dependencies.filterByType(typeFilterConfig);
    root.resetFilterDependenciesByType = () => root._dependencies.resetFilterByType();

    return root;
  };
};

module.exports.init = (View, NodeText, visualizationFunctions, visualizationStyles, jsonToDependencies) => {
  return {
    jsonToRoot: init(View, NodeText, visualizationFunctions, visualizationStyles, jsonToDependencies)
  };
};