// src/features/products/services/attribute.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

// ─── Product Attributes ────────────────────────────────────────────────────────

export const attributeService = {
  /** Get all product attributes */
  getAll: () =>
    axiosClient.get(API_ENDPOINTS.ATTRIBUTES.GET_ALL),

  /** Get a product attribute by ID */
  getById: (id) =>
    axiosClient.get(API_ENDPOINTS.ATTRIBUTES.BY_ID.replace('{id}', id)),

  /** Get a product attribute by name */
  getByName: (name) =>
    axiosClient.get(API_ENDPOINTS.ATTRIBUTES.BY_NAME.replace('{name}', encodeURIComponent(name))),

  /** Delete a product attribute by ID */
  delete: (id) =>
    axiosClient.delete(API_ENDPOINTS.ATTRIBUTES.DELETE.replace('{id}', id)),
};

// ─── Attribute Values ──────────────────────────────────────────────────────────

export const attributeValueService = {
  /** Get attribute value by ID */
  getById: (id) =>
    axiosClient.get(API_ENDPOINTS.ATTRIBUTE_VALUES.BY_ID.replace('{id}', id)),

  /** Get all values for a specific attribute */
  getByAttributeId: (attributeId) =>
    axiosClient.get(
      API_ENDPOINTS.ATTRIBUTE_VALUES.BY_ATTRIBUTE_ID.replace('{attributeId}', attributeId)
    ),

  /** Find an attribute value by attribute ID + value string */
  getByAttributeAndValue: (attributeId, value) =>
    axiosClient.get(
      `${API_ENDPOINTS.ATTRIBUTE_VALUES.GET_BY_ATTR_AND_VALUE}?attributeId=${attributeId}&value=${encodeURIComponent(value)}`
    ),

  /** Update an attribute value */
  update: (id, data) =>
    axiosClient.put(
      API_ENDPOINTS.ATTRIBUTE_VALUES.UPDATE.replace('{id}', id),
      data
    ),

  /** Delete an attribute value by ID */
  delete: (id) =>
    axiosClient.delete(
      API_ENDPOINTS.ATTRIBUTE_VALUES.DELETE.replace('{id}', id)
    ),
};

export const attributeApi = attributeService;
export const attributeValueApi = attributeValueService;

export default attributeService;
