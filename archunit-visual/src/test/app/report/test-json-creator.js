'use strict';

let simpleName = fullname => fullname.substring(fullname.lastIndexOf(".") + 1, fullname.length);

let changeFullName = (node, path) => {
  node.fullname = path + "." + node.fullname;
  if (node.children) node.children.forEach(n => changeFullName(n, path));
};

module.exports = {
  package: function (pgkname) {
    let res = {
      fullname: pgkname,
      name: simpleName(pgkname),
      type: "package",
      children: []
    };
    let builder = {
      add: function (child) {
        changeFullName(child, res.fullname);
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
      fullname: simpleName,
      name: simpleName,
      type: type,
      children: [],
      interfaces: [],
      fieldAccesses: [],
      methodCalls: [],
      constructorCalls: []
    };
    let builder = {
      extending: function (superclassfullname) {
        res.superclass = superclassfullname;
        return builder;
      },
      implementing: function (interfacefullname) {
        res.interfaces.push(interfacefullname);
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