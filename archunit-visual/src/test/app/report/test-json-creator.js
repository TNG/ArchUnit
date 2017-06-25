'use strict';

let changeFullName = (node, path) => {
  node.fullName = path + "." + node.fullName;
  if (node.children) {
    node.children.forEach(n => changeFullName(n, path));
  }
};

module.exports = {
  package: function (pkgname) {
    let res = {
      fullName: pkgname,
      name: pkgname,
      type: "package",
      children: []
    };
    let builder = {
      add: function (child) {
        changeFullName(child, res.fullName);
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
    let res = {
      fullName: simpleName,
      name: simpleName,
      type: type,
      children: [],
      interfaces: [],
      fieldAccesses: [],
      methodCalls: [],
      constructorCalls: [],
      anonImpl: []
    };
    let builder = {
      extending: function (superclassfullName) {
        res.superclass = superclassfullName;
        return builder;
      },
      implementing: function (interfacefullName) {
        res.interfaces.push(interfacefullName);
        return builder;
      },
      callingMethod: function (to, startCodeUnit, targetElement) {
        res.methodCalls.push({to: to, startCodeUnit: startCodeUnit, targetElement: targetElement});
        return builder;
      },
      callingConstructor: function (to, startCodeUnit, targetElement) {
        res.constructorCalls.push({to: to, startCodeUnit: startCodeUnit, targetElement: targetElement});
        return builder;
      },
      accessingField: function (to, startCodeUnit, targetElement) {
        res.fieldAccesses.push({to: to, startCodeUnit: startCodeUnit, targetElement: targetElement});
        return builder;
      },
      havingInnerClass: function (innerClass) {
        changeFullName(innerClass, res.fullName);
        res.children.push(innerClass);
        return builder;
      },
      implementingAnonymous: function (interfacefullName) {
        res.anonImpl.push(interfacefullName);
        return builder;
      },
      build: function () {
        return res;
      }
    };
    return builder;
  }
};