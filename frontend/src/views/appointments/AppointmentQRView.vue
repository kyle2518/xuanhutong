<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NCard, NButton, NSpin, useMessage } from 'naive-ui'
import { appointmentApi } from '@/api/appointments'
import { useAuthStore } from '@/stores/auth'

const message = useMessage()
const auth = useAuthStore()
const qrUrl = ref('')
const loading = ref(true)
const bookingUrl = ref('')

onMounted(async () => {
  try {
    const res = await appointmentApi.generateQR()
    qrUrl.value = window.URL.createObjectURL(res.data as Blob)
    bookingUrl.value = `${window.location.origin}/wechat/book?doctorId=${auth.user?.id}`
  } catch { message.error('生成二维码失败') }
  finally { loading.value = false }
})

function downloadQR() {
  const a = window.document.createElement('a')
  a.href = qrUrl.value
  a.download = '预约二维码.png'
  a.click()
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px;">预约二维码</h2>
    <NCard style="max-width: 500px; margin: 0 auto; text-align: center;">
      <NSpin :show="loading">
        <div v-if="qrUrl">
          <img :src="qrUrl" alt="预约二维码" style="width: 300px; height: 300px;" />
          <p style="margin-top: 12px; color: #666; font-size: 13px;">
            将此二维码分享到微信，患者扫码即可预约
          </p>
          <p style="color: #999; font-size: 12px;">
            预约链接：{{ bookingUrl }}
          </p>
          <NButton type="primary" @click="downloadQR" style="margin-top: 12px;">下载二维码</NButton>
        </div>
      </NSpin>
    </NCard>
  </div>
</template>
