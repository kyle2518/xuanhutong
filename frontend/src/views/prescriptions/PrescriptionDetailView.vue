<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NCard, NButton, NDataTable, NDescriptions, NDescriptionsItem, NSpace, NInput, NModal, useMessage } from 'naive-ui'
import { prescriptionApi } from '@/api/prescriptions'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const detail = ref<any>({})
const items = ref([])
const showSignModal = ref(false)
const signature = ref('')
const patientName = ref('')
const patientPhone = ref('')

onMounted(async () => {
  try {
    const res = await prescriptionApi.get(Number(route.params.id))
    detail.value = res.data.data?.prescription || {}
    items.value = res.data.data?.items || []
    patientName.value = res.data.data?.patientName || ''
    patientPhone.value = res.data.data?.patientPhone || ''
  } catch { message.error('加载失败') }
})

async function sign() {
  if (!signature.value) { message.warning('请输入签名'); return }
  try {
    const res = await prescriptionApi.sign(Number(route.params.id), signature.value)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = 'prescription_signed.pdf'; a.click()
    URL.revokeObjectURL(url)
    showSignModal.value = false
    // Reload detail to update isSigned status
    const updated = await prescriptionApi.get(Number(route.params.id))
    detail.value = updated.data.data?.prescription || {}
    message.success('签署成功')
  } catch (e: any) { message.error(e.response?.data?.message || '签署失败') }
}

async function preview() {
  try {
    const res = await prescriptionApi.previewPdf({}, Number(route.params.id))
    const url = URL.createObjectURL(res.data)
    window.open(url)
  } catch { message.error('预览失败') }
}

const columns: any = [
  { title: '序号', key: 'sortOrder', render: (_: any, idx: number) => idx + 1 },
  { title: '药材名称', key: 'herbName' },
  { title: '剂量(g)', key: 'dosageGrams' },
  { title: '特殊用法', key: 'notes', render: (row: any) => row.notes || '-' },
]
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px;">药方详情</h2>
    <NCard title="基本信息" style="margin-bottom: 16px;">
      <template #header-extra>
        <NSpace>
          <NButton size="small" @click="preview">预览PDF</NButton>
          <NButton size="small" type="primary" @click="showSignModal = true" :disabled="detail.isSigned">签署</NButton>
        </NSpace>
      </template>
      <NDescriptions :column="3">
        <NDescriptionsItem label="病人名称">{{ patientName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="手机号">{{ patientPhone || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="创建时间">{{ detail.createdAt ? new Date(detail.createdAt).toLocaleString() : '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="诊断">{{ detail.diagnosis || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="剂数">{{ detail.totalDoses || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="签名状态">{{ detail.isSigned ? '已签' : '未签' }}</NDescriptionsItem>
      </NDescriptions>
      <div v-if="detail.notes" style="margin-top: 8px;">
        <strong>用法：</strong>{{ detail.notes }}
      </div>
    </NCard>

    <NCard title="药材明细">
      <NDataTable :columns="columns" :data="items" :pagination="false" />
    </NCard>

    <NModal v-model:show="showSignModal" title="签署药方" preset="card" style="width:420px;" title-style="font-size:18px;font-weight:600;">
      <p style="margin-bottom:12px;">请输入医师签名：</p>
      <NInput v-model:value="signature" placeholder="请输入您的签名" style="margin-bottom: 16px;" />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showSignModal = false">取消</NButton>
          <NButton type="primary" @click="sign">确认签署</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
