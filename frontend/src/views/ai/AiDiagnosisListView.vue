<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NTag, NSpace, NModal, NInput, useMessage } from 'naive-ui'
import { aiApi } from '@/api/ai'
import { patientApi } from '@/api/patients'

const router = useRouter()
const message = useMessage()
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const patientMap = ref<Record<number, string>>({})

// 典籍管理
const showClassicModal = ref(false)
const classics = ref<any[]>([])
const bookTitle = ref('')
const uploadLoading = ref(false)

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

const classicColumns: any = [
  { title: '典籍名称', key: 'book_title' },
  { title: '分片数', key: 'chunks' },
]

async function loadClassics() {
  try {
    const res = await aiApi.listClassics()
    classics.value = res.data.data || []
  } catch { classics.value = [] }
}

function openClassicModal() {
  showClassicModal.value = true
  loadClassics()
}

async function onClassicFileInput(e: any) {
  const files: File[] = Array.from(e.target.files || [])
  if (!files.length) return
  uploadLoading.value = true
  try {
    for (const f of files) {
      await aiApi.uploadClassic(f, bookTitle.value || undefined)
      message.success(`${f.name} 上传成功`)
    }
    bookTitle.value = ''
    await loadClassics()
  } catch (err: any) {
    message.error(err?.response?.data?.message || '上传失败')
  } finally {
    uploadLoading.value = false
    e.target.value = ''
  }
}

onMounted(() => { fetchPatients(); fetch() })
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">AI 辅助诊断</h2>
      <NSpace>
        <NButton @click="openClassicModal">典籍管理</NButton>
        <NButton type="primary" @click="router.push('/ai/new')">发起诊断</NButton>
      </NSpace>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="list" :loading="loading"
        :pagination="{ page, pageSize: 10, itemCount: total, onChange: (p: number) => { page = p; fetch() } }" />
    </NCard>

    <!-- 典籍管理弹窗 -->
    <NModal v-model:show="showClassicModal" title="中医典籍管理" preset="card" style="width: 640px;" title-style="font-size:18px;font-weight:600;">
      <div style="margin-bottom: 16px;">
        <NSpace>
          <NInput v-model:value="bookTitle" placeholder="典籍名称（可选，默认用文件名）" style="width: 220px;" />
          <label style="display:inline-block;cursor:pointer;">
            <input type="file" accept=".txt,.md,.pdf" style="display:none;" @change="onClassicFileInput" />
            <NButton type="primary" tag="span" :loading="uploadLoading">上传典籍</NButton>
          </label>
        </NSpace>
        <p style="font-size:12px;color:#999;margin:6px 0 0;">支持 txt / md / pdf，自动条文切分并向量化，供 AI 辅助诊断检索</p>
      </div>
      <NDataTable :columns="classicColumns" :data="classics" :pagination="false" size="small" />
    </NModal>
  </div>
</template>
