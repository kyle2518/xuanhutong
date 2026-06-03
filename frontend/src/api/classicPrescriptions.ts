import request from './request'

export const classicApi = {
  list: (params?: any) => request.get('/classic-prescriptions', { params }),
  get: (id: number) => request.get(`/classic-prescriptions/${id}`),
  create: (data: any) => request.post('/classic-prescriptions', data),
  update: (id: number, data: any) => request.put(`/classic-prescriptions/${id}`, data),
  delete: (id: number) => request.delete(`/classic-prescriptions/${id}`),
}
