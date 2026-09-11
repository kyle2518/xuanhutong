import request from './request'

export const aiApi = {
  start: (data: any) => request.post('/ai/diagnoses', data, { timeout: 180000 }),
  list: (params?: any) => request.get('/ai/diagnoses', { params }),
  get: (id: number) => request.get(`/ai/diagnoses/${id}`),
  update: (id: number, draft: any) => request.put(`/ai/diagnoses/${id}`, { draft }),
  sign: (id: number, signatureText: string) =>
    request.post(`/ai/diagnoses/${id}/sign`, { signatureText }, { responseType: 'blob' }),
  convert: (id: number, totalDoses?: number) =>
    request.post(`/ai/diagnoses/${id}/convert`, { totalDoses }),
  reject: (id: number) => request.post(`/ai/diagnoses/${id}/reject`),
  ingest: (data?: any) => request.post('/ai/knowledge/ingest', data),
  kbStatus: () => request.get('/ai/knowledge/status'),
  uploadClassic: (file: File, bookTitle?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (bookTitle) formData.append('bookTitle', bookTitle)
    return request.post('/ai/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  listClassics: () => request.get('/ai/knowledge/classics'),
}
