<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NCard, NButton, NInput, NSpin, useMessage } from 'naive-ui'
import { prescriptionApi } from '@/api/prescriptions'

const route = useRoute()
const message = useMessage()
const pdfUrl = ref('')
const loading = ref(true)
const signature = ref('')
const signing = ref(false)

onMounted(async () => {
  try {
    const res = await prescriptionApi.previewPdf({}, Number(route.params.id))
    pdfUrl.value = URL.createObjectURL(res.data)
  } catch { message.error('加载失败') }
  finally { loading.value = false }
})

async function sign() {
  if (!signature.value) { message.warning('请输入签名'); return }
  signing.value = true
  try {
    const res = await prescriptionApi.sign(Number(route.params.id), signature.value)
    const url = URL.createObjectURL(res.data)
    pdfUrl.value = url
    message.success('签署成功')
  } catch { message.error('签署失败') }
  finally { signing.value = false }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px;">药方预览</h2>
    <NCard>
      <NSpin :show="loading">
        <iframe v-if="pdfUrl" :src="pdfUrl" width="100%" height="600px" frameborder="0" style="border: 1px solid #eee; border-radius: 4px;" />
      </NSpin>
      <div style="display: flex; justify-content: center; gap: 12px; margin-top: 16px;">
        <NInput v-model:value="signature" placeholder="请输入医师签名" style="width: 200px;" />
        <NButton type="primary" :loading="signing" @click="sign">签署药方</NButton>
      </div>
    </NCard>
  </div>
</template>
