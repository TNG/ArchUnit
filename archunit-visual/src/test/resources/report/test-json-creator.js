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
    return {
      fullname: simpleName,
      name: simpleName,
      type: type
    }
  }
};