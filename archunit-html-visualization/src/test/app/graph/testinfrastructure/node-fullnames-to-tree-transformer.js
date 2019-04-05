'use strict';

const separators = ['.', '$'];

/**
 *
 * @param nodeFullNames full qualified names of all the packages and classes
 */
const createTreeFromNodeFullNames = (...nodeFullNames) => {
  const fullNameToNode = new Map();
  const roots = [];
  nodeFullNames.sort((fullName1, fullName2) => fullName1.length - fullName2.length);
  nodeFullNames.forEach(fullName => {
    const prefixFullNames = nodeFullNames.filter(otherFullName => fullName.startsWith(otherFullName) && separators.includes(fullName.charAt(otherFullName.length)));
    if (prefixFullNames.length > 0) {

      const prefixFullName = prefixFullNames[prefixFullNames.length - 1];
      const node = {
        fullName,
        name: fullName.substring(prefixFullName.length + 1),
        children: []
      };
      fullNameToNode.set(fullName, node);
      fullNameToNode.get(prefixFullName).children.push(node);
    } else {
      const node = {fullName, name: fullName, children: []};
      fullNameToNode.set(fullName, node);
      roots.push(node);
    }
  });
  return {
    fullName: 'default',
    name: 'default',
    children: roots
  }
};

module.exports.createTreeFromNodeFullNames = createTreeFromNodeFullNames;