'use strict';

const changeFullName = (node, path, separator) => {
  node.fullName = path + separator + node.fullName;
  if (node.children) {
    node.children.forEach(n => changeFullName(n, path, separator));
  }
};

const createGraph = (root, dependencies) => ({
  root,
  dependencies
});

const nodeCreator = {
  defaultPackage: () => {
    const res = {
      fullName: 'default',
      name: 'default',
      type: 'package',
      children: []
    };
    const builder = {
      add: (child) => {
        res.children.push(child);
        return builder;
      },
      build: () => res
    };
    return builder;
  },
  package: (pkgname) => {
    const res = {
      fullName: pkgname,
      name: pkgname,
      type: 'package',
      children: []
    };
    const builder = {
      add: (child) => {
        changeFullName(child, res.fullName, '.');
        res.children.push(child);
        return builder;
      },
      build: () => res
    };
    return builder;
  },
  clazz: (simpleName, type) => {
    const res = {
      fullName: simpleName,
      name: simpleName,
      type: type,
      children: []
    };
    const builder = {
      havingInnerClass: (innerClass) => {
        changeFullName(innerClass, res.fullName, '$');
        res.children.push(innerClass);
        return builder;
      },
      build: () => res
    };
    return builder;
  }
};

const addDotBefore = str => str ? '.' + str : '';

const createDependencies = () => {
  const res = [];
  const betweenBuilder = type => ({
    from: (from, startCodeUnit = '') => ({
      to: (to, targetCodeUnit = '') => {
        res.push({
          type,
          originClass: from,
          targetClass: to,
          description: `<${from + addDotBefore(startCodeUnit)}> ${type} to <${to + addDotBefore(targetCodeUnit)}>`
        });
        return builder;
      }
    })
  });

  const builder = {
    addMethodCall: () => betweenBuilder('METHOD_CALL'),
    addConstructorCall: () => betweenBuilder('CONSTRUCTOR_CALL'),
    addFieldAccess: () => betweenBuilder('FIELD_ACCESS'),
    addInheritance: () => betweenBuilder('INHERITANCE'),
    build: () => {
      return res;
    }
  };
  return builder;
};

module.exports = {nodeCreator, createDependencies, createGraph};