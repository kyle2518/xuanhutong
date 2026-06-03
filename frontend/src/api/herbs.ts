import request from './request'

export const herbApi = {
  search: (params?: any) => request.get('/herbs', { params }),
  get: (id: number) => request.get(`/herbs/${id}`),
  getTooltip: (id: number) => request.get(`/herbs/${id}/tooltip`),
}
