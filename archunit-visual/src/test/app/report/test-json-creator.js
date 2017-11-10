'use strict';

const changeFullName = (node, path, separator) => {
  node.fullName = path + separator + node.fullName;
  if (node.children) {
    node.children.forEach(n => changeFullName(n, path, separator));
  }
};

module.exports = {
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
      children: [],
      interfaces: [],
      fieldAccesses: [],
      methodCalls: [],
      constructorCalls: [],
      anonymousImplementation: []
    };
    const builder = {
      extending: function (superclassfullName) {
        res.superclass = superclassfullName;
        return builder;
      },
      implementing: function (interfacefullName) {
        res.interfaces.push(interfacefullName);
        return builder;
      },
      callingMethod: function (target, startCodeUnit, targetCodeElement) {
        res.methodCalls.push({target: target, startCodeUnit: startCodeUnit, targetCodeElement: targetCodeElement});
        return builder;
      },
      callingConstructor: function (target, startCodeUnit, targetCodeElement) {
        res.constructorCalls.push({target: target, startCodeUnit: startCodeUnit, targetCodeElement: targetCodeElement});
        return builder;
      },
      accessingField: function (target, startCodeUnit, targetCodeElement) {
        res.fieldAccesses.push({target: target, startCodeUnit: startCodeUnit, targetCodeElement: targetCodeElement});
        return builder;
      },
      havingInnerClass: function (innerClass) {
        changeFullName(innerClass, res.fullName, '$');
        res.children.push(innerClass);
        return builder;
      },
      implementingAnonymous: function (interfacefullName) {
        res.anonymousImplementation.push(interfacefullName);
        return builder;
      },
      build: function () {
        return res;
      }
    };
    return builder;
  }
};