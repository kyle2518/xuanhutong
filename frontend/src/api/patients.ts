import request from './request'

export const patientApi = {
  list: (params?: any) => request.get('/patients', { params }),
  create: (data: any) => request.post('/patients', data),
  get: (id: number) => request.get(`/patients/${id}`),
  update: (id: number, data: any) => request.put(`/patients/${id}`, data),
  delete: (id: number) => request.delete(`/patients/${id}`),
}
