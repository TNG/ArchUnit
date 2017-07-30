'use strict';

const proxyquire = require("proxyquire");

module.exports.get = file => require(`../../../main/app/report/${file}`);

module.exports.getRewired = (file, config) => proxyquire(`../../../main/app/report/${file}`, config);