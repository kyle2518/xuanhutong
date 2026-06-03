import request from './request'

export const inventoryApi = {
  list: (params?: any) => request.get('/inventory', { params }),
  get: (id: number) => request.get(`/inventory/${id}`),
  create: (data: any) => request.post('/inventory', data),
  update: (id: number, data: any) => request.put(`/inventory/${id}`, data),
  batchUpdate: (data: any) => request.post('/inventory/batch-update', data),
  availableHerbs: (params?: any) => request.get('/inventory/available-herbs', { params }),
}
