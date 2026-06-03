import request from './request'

export const recordApi = {
  list: (patientId: number, params?: any) => request.get(`/patients/${patientId}/records`, { params }),
  create: (patientId: number, data: any) => request.post(`/patients/${patientId}/records`, data),
  get: (id: number) => request.get(`/records/${id}`),
  update: (id: number, data: any) => request.put(`/records/${id}`, data),
  delete: (id: number) => request.delete(`/records/${id}`),
  uploadReport: (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/records/${id}/upload-report`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  exportPdf: (patientId: number) => request.get(`/patients/${patientId}/export-pdf`, { responseType: 'blob' }),
}
