import request from './request'

export const herbApi = {
  search: (params?: any) => request.get('/herbs', { params }),
  get: (id: number) => request.get(`/herbs/${id}`),
  getTooltip: (id: number) => request.get(`/herbs/${id}/tooltip`),
  create: (data: any) => request.post('/herbs', data),
  update: (id: number, data: any) => request.put(`/herbs/${id}`, data),
  delete: (id: number) => request.delete(`/herbs/${id}`),
  batch: (data: any[]) => request.post('/herbs/batch', data),
}
