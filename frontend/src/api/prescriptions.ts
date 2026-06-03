import request from './request'

export const prescriptionApi = {
  list: (params?: any) => request.get('/prescriptions', { params }),
  create: (data: any) => request.post('/prescriptions', data),
  get: (id: number) => request.get(`/prescriptions/${id}`),
  update: (id: number, data: any) => request.put(`/prescriptions/${id}`, data),
  delete: (id: number) => request.delete(`/prescriptions/${id}`),
  previewPdf: (data: any, id?: number) => request.post(`/prescriptions/pdf-preview?id=${id || ''}`, data, { responseType: 'blob' }),
  sign: (id: number, signatureData: string) => request.post(`/prescriptions/${id}/sign`, { signatureData }, { responseType: 'blob' }),
}
