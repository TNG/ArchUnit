const {expect} = require('chai');
const sortInOrder = require('../../../../main/app/graph/infrastructure/graph-algorithms').sortInOrder;

describe('graph-algorithms', () => {
  it('should sort descendants of a node in order', () => {
    const node4 = {
      name: 'node4',
      children: []
    };
    const node3 = {
      name: 'node3',
      children: []
    };
    const node2_1 = {
      name: 'node2_1',
      children: [node3]
    };
    const node2_2 = {
      name: 'node2_2',
      children: [node3]
    };
    const node1 = {
      name: 'node1',
      children: [node2_1, node2_2]
    };
    const node2_a = {
      name: 'node2_a',
      children: [node4]
    };
    const rootNode = {
      name: 'root',
      children: [node1, node2_a]
    };

    const result = sortInOrder(rootNode, node =>  node.children);

    expect(result).to.be.deep.equal([rootNode, node1, node2_a, node2_1, node2_2, node4, node3]);
  });

  it('should throw on cyclic graph', () => {
    const node_W = {
      name: 'node_W',
      children: []
    };
    const node_S = {
      name: 'node_S',
      children: [node_W]
    };
    const node_E = {
      name: 'node_E',
      children: [node_S]
    };
    const node_N = {
      name: 'node_N',
      children: [node_E]
    };
    const rootNode = {
      name: 'root',
      children: [node_N]
    };
    node_W.children.push(node_N)

    expect(() => sortInOrder(rootNode, node =>  node.children)).to.throw('the graph is cyclic');
  });
});
