const nodeTypes = require('./node-types.json')

export type NodeType = string

export const CLASS: NodeType = nodeTypes["class"];
export const PACKAGE: NodeType = nodeTypes["package"];
export const INTERFACE: NodeType = nodeTypes["interface"];
