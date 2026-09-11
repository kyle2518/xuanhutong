<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NSelect, NInput, NButton, NSpace, useMessage } from 'naive-ui'
import { aiApi } from '@/api/ai'
import { patientApi } from '@/api/patients'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const patientId = ref<number | null>(route.params.patientId ? Number(route.params.patientId) : null)
const chiefComplaint = ref('')
const patientOptions = ref<any[]>([])
const loading = ref(false)

async function searchPatients(q: string) {
  const res = await patientApi.list({ keyword: q || undefined, page: 1, size: 20 })
  patientOptions.value = (res.data.data?.records || []).map((p: any) => ({ label: p.name, value: p.id }))
}

async function start() {
  if (!patientId.value) { message.warning('请选择病人'); return }
  loading.value = true
  try {
    const res = await aiApi.start({
      patientId: patientId.value,
      chiefComplaint: chiefComplaint.value || undefined,
    })
    const id = res.data.data?.id
    message.success('诊断完成')
    router.push(`/ai/${id}`)
  } catch (e: any) {
    message.error(e.response?.data?.message || '诊断失败')
  } finally { loading.value = false }
}

onMounted(() => searchPatients(''))
</script>

<template>
  <div style="max-width: 720px;">
    <h2 style="margin-bottom: 16px;">发起 AI 辅助诊断</h2>
    <NCard>
      <NForm label-placement="left" label-width="90">
        <NFormItem label="选择病人" required>
          <NSelect v-model:value="patientId" :options="patientOptions" filterable clearable
            placeholder="输入姓名搜索病人" @search="searchPatients" style="width: 100%;" />
        </NFormItem>
        <NFormItem label="主诉">
          <NInput v-model:value="chiefComplaint" type="textarea" rows="3"
            placeholder="可选，例如：发热、恶寒、头痛两日" />
        </NFormItem>
      </NForm>
      <NSpace justify="end" style="margin-top: 8px;">
        <NButton @click="router.back()">取消</NButton>
        <NButton type="primary" :loading="loading" @click="start">开始诊断</NButton>
      </NSpace>
      <p v-if="loading" style="color:#999;font-size:13px;margin-top:12px;">
        AI 正在检索古籍、辨证论治，通常需要 30~90 秒，请稍候…
      </p>
    </NCard>
  </div>
</template>
