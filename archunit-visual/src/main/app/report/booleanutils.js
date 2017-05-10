module.exports.booleanFunctions = boolExpr1 => ({
  implies: boolExpr2 => !boolExpr1 || boolExpr2
});
