<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NTag, NSpace } from 'naive-ui'
import { aiApi } from '@/api/ai'
import { patientApi } from '@/api/patients'

const router = useRouter()
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const patientMap = ref<Record<number, string>>({})

const statusMap: Record<string, { text: string; type: any }> = {
  DRAFT: { text: '草稿', type: 'warning' },
  SIGNED: { text: '已签字', type: 'success' },
  REJECTED: { text: '已驳回', type: 'error' },
}

async function fetchPatients() {
  try {
    const res = await patientApi.list({ page: 1, size: 100 })
    const map: Record<number, string> = {}
    for (const p of res.data.data?.records || []) map[p.id] = p.name
    patientMap.value = map
  } catch { /* 忽略 */ }
}

async function fetch() {
  loading.value = true
  try {
    const res = await aiApi.list({ p: page.value, s: 10 })
    list.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } finally { loading.value = false }
}

const columns: any = [
  { title: 'ID', key: 'id', width: 60 },
  {
    title: '病人', key: 'patientId',
    render: (row: any) => patientMap.value[row.patientId] || `#${row.patientId}`,
  },
  {
    title: '主诉', key: 'chiefComplaint', ellipsis: { tooltip: true },
    render: (row: any) => row.chiefComplaint || '-',
  },
  {
    title: '状态', key: 'status',
    render: (row: any) => h(NTag, { type: statusMap[row.status]?.type || 'default', size: 'small' },
      { default: () => statusMap[row.status]?.text || row.status }),
  },
  { title: '创建时间', key: 'createdAt', render: (row: any) => row.createdAt || '-' },
  {
    title: '操作', key: 'action',
    render: (row: any) => h(NButton, { size: 'small', onClick: () => router.push(`/ai/${row.id}`) },
      { default: () => '查看' }),
  },
]

onMounted(() => { fetchPatients(); fetch() })
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">AI 辅助诊断</h2>
      <NSpace>
        <NButton type="primary" @click="router.push('/ai/new')">发起诊断</NButton>
      </NSpace>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="list" :loading="loading"
        :pagination="{ page, pageSize: 10, itemCount: total, onChange: (p: number) => { page = p; fetch() } }" />
    </NCard>
  </div>
</template>
