import request from './request'

export interface LoginParams { phone: string; code: string }
export interface RegisterParams { phone: string; code: string; name: string; password?: string; clinicName?: string }
export interface UserInfo { id: number; name: string; phone: string; role: string; clinicName?: string; avatarUrl?: string; signatureImageUrl?: string }

export const authApi = {
  sendSms: (phone: string) => request.post('/auth/send-sms', { phone }),
  login: (params: LoginParams) => request.post('/auth/login', params),
  register: (params: RegisterParams) => request.post('/auth/register', params),
  getProfile: () => request.get('/auth/me'),
  updateProfile: (data: any) => request.put('/auth/me', data),
}
