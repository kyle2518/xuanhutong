<script setup lang="ts">
import { ref, computed } from 'vue'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, useMessage, NDivider, NIcon, NTabs, NTabPane } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const message = useMessage()

const activeTab = ref('sms')
const phone = ref('')
const code = ref('')
const password = ref('')
const name = ref('')
const clinicName = ref('')
const isRegister = ref(false)
const codeSent = ref(false)
const loading = ref(false)
const countdown = ref(0)
const errorMsg = ref('')

async function sendCode() {
  if (!phone.value || phone.value.length !== 11) {
    message.warning('请输入正确的手机号')
    return
  }
  try {
    await auth.sendSms(phone.value)
    codeSent.value = true
    countdown.value = 60
    message.success('验证码已发送（开发环境：123456）')
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        codeSent.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e.response?.data?.message || '发送失败，请稍后重试')
  }
}

async function handleSubmit() {
  if (!phone.value) {
    message.warning('请输入手机号')
    return
  }
  if (isRegister.value && !name.value) {
    message.warning('请输入姓名')
    return
  }
  errorMsg.value = ''
  loading.value = true
  try {
    if (isRegister.value) {
      if (!code.value) { errorMsg.value = '请输入验证码'; return }
      await auth.register({
        phone: phone.value,
        code: code.value,
        name: name.value,
        password: password.value || undefined,
        clinicName: clinicName.value
      })
      message.success('注册成功，欢迎加入悬壶通！')
    } else {
      if (activeTab.value === 'sms') {
        if (!code.value) { errorMsg.value = '请输入验证码'; return }
        await auth.login(phone.value, code.value)
      } else {
        if (!password.value) { errorMsg.value = '请输入密码'; return }
        await auth.login(phone.value, undefined, password.value)
      }
      message.success('登录成功')
    }
    router.push('/')
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '操作失败，请重试'
  } finally {
    loading.value = false
  }
}

const submitLabel = computed(() => {
  if (isRegister.value) return '注册'
  return activeTab.value === 'sms' ? '登录' : '密码登录'
})
</script>

<template>
  <div class="login-page">
    <!-- Animated background -->
    <div class="bg-decoration">
      <div class="circle c1" />
      <div class="circle c2" />
      <div class="circle c3" />
      <div class="herb-pattern" />
    </div>

    <!-- Main card -->
    <div class="login-container">
      <!-- Left brand panel -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <span class="icon-text">悬</span>
          </div>
          <h1 class="brand-title">悬壶通</h1>
          <p class="brand-subtitle">中医门店管理系统</p>
          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-icon">🌿</span>
              <span>智能药方开单</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📋</span>
              <span>病人档案管理</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📦</span>
              <span>药材库存追踪</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📜</span>
              <span>经典药方传承</span>
            </div>
          </div>
        </div>
        <div class="brand-footer">
          <p>传承中医文化  ·  智慧诊疗管理</p>
        </div>
      </div>

      <!-- Right form panel -->
      <div class="form-panel">
        <div class="form-wrapper">
          <div class="form-header">
            <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
            <p>{{ isRegister ? '注册后即可使用全部功能' : '请输入账号信息登录系统' }}</p>
          </div>

          <!-- Login type tabs (only for login) -->
          <NTabs v-if="!isRegister" v-model:value="activeTab" type="line" animated @update:value="errorMsg = ''">
            <NTabPane name="sms" tab="验证码登录" />
            <NTabPane name="password" tab="密码登录" />
          </NTabs>

          <NForm label-placement="left" label-width="0" size="large">
            <NFormItem>
              <NInput v-model:value="phone" placeholder="请输入手机号" maxlength="11" :input-props="{ autocomplete: 'tel' }" />
            </NFormItem>

            <NFormItem v-if="!isRegister && activeTab === 'password'">
              <NInput v-model:value="password" type="password" placeholder="请输入密码" :input-props="{ autocomplete: 'current-password' }" />
            </NFormItem>

            <template v-if="isRegister || activeTab === 'sms'">
              <NFormItem>
                <NSpace style="width: 100%;">
                  <NInput v-model:value="code" placeholder="请输入验证码" maxlength="6" style="flex: 1;" />
                  <NButton :disabled="codeSent" @click="sendCode" :loading="countdown > 0" style="min-width: 120px;">
                    {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
                  </NButton>
                </NSpace>
              </NFormItem>
            </template>

            <NFormItem v-if="isRegister">
              <NInput v-model:value="name" placeholder="请输入您的姓名" />
            </NFormItem>

            <NFormItem v-if="isRegister">
              <NInput v-model:value="password" type="password" placeholder="设置登录密码（可选，默认为123456）" />
            </NFormItem>

            <NFormItem v-if="isRegister">
              <NInput v-model:value="clinicName" placeholder="诊所名称（选填）" />
            </NFormItem>
          </NForm>

          <!-- Inline error display -->
          <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

          <NButton type="primary" size="large" block :loading="loading" @click="handleSubmit" class="submit-btn">
            {{ submitLabel }}
          </NButton>

          <NDivider>
            <span style="color: #999; font-size: 13px;">
              {{ isRegister ? '已有账号？' : '还没有账号？' }}
            </span>
          </NDivider>

          <NButton text type="primary" size="large" @click="isRegister = !isRegister; errorMsg = ''" style="width: 100%;">
            {{ isRegister ? '去登录' : '创建新账号' }}
          </NButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0d3b1e 0%, #14532d 30%, #166534 60%, #1a3a2a 100%);
  position: relative;
  overflow: hidden;
}

/* Animated background */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: #4ade80;
}
.c1 { width: 600px; height: 600px; top: -200px; right: -100px; animation: float 20s ease-in-out infinite; }
.c2 { width: 400px; height: 400px; bottom: -100px; left: -50px; animation: float 15s ease-in-out infinite reverse; }
.c3 { width: 300px; height: 300px; top: 50%; left: 40%; animation: float 18s ease-in-out infinite; }
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}
.herb-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 80%, rgba(74,222,128,0.05) 1px, transparent 1px),
                    radial-gradient(circle at 80% 20%, rgba(74,222,128,0.05) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* Main container */
.login-container {
  display: flex;
  width: 960px;
  min-height: 580px;
  background: rgba(255,255,255,0.97);
  border-radius: 20px;
  box-shadow: 0 25px 80px rgba(0,0,0,0.3), 0 0 0 1px rgba(255,255,255,0.1);
  overflow: hidden;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

/* Brand panel */
.brand-panel {
  width: 440px;
  background: linear-gradient(160deg, #14532d 0%, #166534 40%, #15803d 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 40px;
  position: relative;
  overflow: hidden;
}
.brand-panel::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(74,222,128,0.2) 0%, transparent 70%);
  border-radius: 50%;
}
.brand-icon {
  width: 64px;
  height: 64px;
  background: rgba(255,255,255,0.15);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.2);
}
.icon-text {
  font-size: 32px;
  font-weight: bold;
}
.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 4px;
  letter-spacing: 4px;
}
.brand-subtitle {
  font-size: 15px;
  opacity: 0.7;
  margin: 0 0 36px;
  letter-spacing: 2px;
}
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.85;
}
.feature-icon { font-size: 20px; }
.brand-footer {
  font-size: 12px;
  opacity: 0.5;
  letter-spacing: 1px;
}

/* Form panel */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.form-wrapper {
  width: 100%;
  max-width: 360px;
}
.form-header {
  margin-bottom: 24px;
  text-align: center;
}
.form-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 6px;
}
.form-header p {
  font-size: 14px;
  color: #999;
  margin: 0;
}
.submit-btn {
  margin-top: 4px;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  background: linear-gradient(135deg, #166534, #15803d) !important;
  border: none !important;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #14532d, #166534) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(22,101,52,0.4);
}
.error-msg {
  color: #d03050;
  font-size: 13px;
  background: rgba(208,48,80,0.06);
  border: 1px solid rgba(208,48,80,0.2);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  text-align: center;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 90%;
    min-height: auto;
    margin: 20px;
  }
  .brand-panel {
    width: 100%;
    padding: 32px 24px;
  }
  .brand-features { display: none; }
  .form-panel { padding: 24px; }
}
</style>
