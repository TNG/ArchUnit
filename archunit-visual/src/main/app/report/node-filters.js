'use strict';

const nodeKinds = require('./node-kinds.json');

const isLeaf = node => node._filteredChildren.length === 0;

const leftTrim = str => {
  return str.replace(/^\s+/g, '');
};

const escapeRegExp = str => {
  return str.replace(/[-[\]/{}()+?.\\^$|]/g, '\\$&');
};

const nameContainsFilter = (filterString, exclude) => {
  filterString = leftTrim(filterString);
  const endsWith = filterString.endsWith(" ");
  filterString = filterString.trim();
  let regexString = escapeRegExp(filterString).replace(/\*/g, ".*");
  if (endsWith) {
    regexString = "(" + regexString + ")$";
  }

  const filter = node => {
    if (node.getType() === nodeKinds.package) {
      return node._filteredChildren.reduce((acc, c) => acc || filter(c), false);
    }
    else {
      const match = new RegExp(regexString).exec(node.getFullName());
      let res = match && match.length > 0;
      res = exclude ? !res : res;
      return res || (!isLeaf(node) && node._filteredChildren.reduce((acc, c) => acc || filter(c), false));
    }
  };
  return filter;
};

module.exports.nameContainsFilter = nameContainsFilter;