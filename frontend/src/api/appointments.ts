import request from './request'

export const appointmentApi = {
  book: (data: any) => request.post('/appointments/book', data),
  list: (params?: any) => request.get('/appointments', { params }),
  get: (id: number) => request.get(`/appointments/${id}`),
  updateStatus: (id: number, status: string) => request.put(`/appointments/${id}/status?status=${status}`),
  generateQR: () => request.get('/appointments/generate-qr', { responseType: 'blob' }),
}
