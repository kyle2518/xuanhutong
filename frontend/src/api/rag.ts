import request from './request'

export interface KnowledgeBase {
  id: string
  name: string
  description: string
  collection_name: string
  document_count: number
  chunk_count: number
  created_at: string
  updated_at: string
}

export interface RagDocument {
  id: string
  knowledge_base_id: string
  collection_name: string
  filename: string
  file_type: string
  chunk_count: number
  status: string
  error: string | null
  created_at: string
}

export interface SearchSource {
  document_id: string
  source: string
  chunk: string
  score: number
  knowledge_base_id: string
}

export const ragApi = {
  health: () => request.get('/rag/health'),
  listKnowledgeBases: () => request.get<{ items: KnowledgeBase[]; total: number }>('/rag/knowledge-bases'),
  getKnowledgeBase: (id: string) => request.get<KnowledgeBase>(`/rag/knowledge-bases/${id}`),
  createKnowledgeBase: (data: { name: string; description?: string }) => request.post<KnowledgeBase>('/rag/knowledge-bases', data),
  deleteKnowledgeBase: (id: string) => request.delete(`/rag/knowledge-bases/${id}`),
  listDocuments: (kbId: string) => request.get<{ items: RagDocument[]; total: number }>(`/rag/knowledge-bases/${kbId}/documents`),
  uploadDocument: (kbId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post<RagDocument>(`/rag/knowledge-bases/${kbId}/documents`, form)
  },
  deleteDocument: (id: string) => request.delete(`/rag/documents/${id}`),
  query: (data: { question: string; knowledge_base_id?: string | null; top_k?: number; system_prompt?: string | null }) =>
    request.post<{ question: string; answer: string; sources: SearchSource[] }>('/rag/query', data)
}