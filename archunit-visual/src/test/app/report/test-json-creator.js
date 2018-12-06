'use strict';

const changeFullName = (node, path, separator) => {
  node.fullName = path + separator + node.fullName;
  if (node.children) {
    node.children.forEach(n => changeFullName(n, path, separator));
  }
};

const createTestGraph = (root, dependencies) => ({
  root,
  dependencies
});

const testRoot = {
  package: function (pkgname) {
    const res = {
      fullName: pkgname,
      name: pkgname,
      type: 'package',
      children: []
    };
    const builder = {
      add: function (child) {
        changeFullName(child, res.fullName, '.');
        res.children.push(child);
        return builder;
      },
      build: function () {
        return res;
      }
    };
    return builder;
  },
  clazz: function (simpleName, type) {
    const res = {
      fullName: simpleName,
      name: simpleName,
      type: type,
      children: []
    };
    const builder = {
      havingInnerClass: function (innerClass) {
        changeFullName(innerClass, res.fullName, '$');
        res.children.push(innerClass);
        return builder;
      },
      build: function () {
        return res;
      }
    };
    return builder;
  }
};

const addDotBefore = str => str ? '.' + str : '';

const createTestDependencies = () => {
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

export {testRoot, createTestDependencies, createTestGraph}