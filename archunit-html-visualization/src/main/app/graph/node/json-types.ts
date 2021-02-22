import {NodeType} from "./node-type";

export interface JsonNode {
  children: [JsonNode]
  name: string
  fullName: string
  type: NodeType
}
