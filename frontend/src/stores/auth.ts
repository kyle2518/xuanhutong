import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(JSON.parse(localStorage.getItem('user') || 'null'))
  const isAuthenticated = computed(() => !!token.value)

  async function sendSms(phone: string) {
    await authApi.sendSms(phone)
  }

  async function login(phone: string, code?: string, password?: string) {
    const res = await authApi.login({ phone, code: code || '', password: password || '' } as any)
    token.value = res.data.data.token
    user.value = res.data.data.user
    localStorage.setItem('token', token.value!)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function register(data: { phone: string; code: string; name: string; clinicName?: string; password?: string }) {
    const res = await authApi.register(data)
    token.value = res.data.data.token
    user.value = res.data.data.user
    localStorage.setItem('token', token.value!)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function fetchProfile() {
    const res = await authApi.getProfile()
    user.value = res.data.data
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  return { token, user, isAuthenticated, sendSms, login, register, fetchProfile, logout }
})
