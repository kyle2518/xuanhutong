<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NSpace, NInput, NDatePicker, useMessage } from 'naive-ui'
import { prescriptionApi } from '@/api/prescriptions'

const router = useRouter()
const message = useMessage()
const prescriptions = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const keyword = ref('')
const dateRange = ref<[number, number] | null>(null)

function maskPhone(phone: string) {
  if (!phone) return '-'
  const s = String(phone)
  return s.length === 11 ? `${s.slice(0, 3)}***${s.slice(7)}` : s
}

function formatDate(ts: number) {
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const columns: any = [
  { title: '序号', key: 'id', sorter: (a: any, b: any) => a.id - b.id },
  {
    title: '病人', key: 'patientName',
    render: (row: any) => `${row.patientName || '未知'}${row.patientPhone ? `（${maskPhone(row.patientPhone)}）` : ''}`,
  },
  { title: '诊断', key: 'diagnosis', render: (row: any) => row.diagnosis || '-', ellipsis: { tooltip: true } },
  { title: '剂数', key: 'totalDoses', sorter: (a: any, b: any) => (a.totalDoses || 0) - (b.totalDoses || 0) },
  { title: '签名状态', key: 'isSigned', render: (row: any) => row.isSigned ? '已签' : '未签' },
  {
    title: '创建时间', key: 'createdAt',
    sorter: (a: any, b: any) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime(),
    render: (row: any) => row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '-',
  },
  {
    title: '操作', key: 'action',
    render: (row: any) => h(NButton, { size: 'small', onClick: () => router.push(`/prescriptions/${row.id}`) }, { default: () => '详情' }),
  },
]

async function fetchRx() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 10, keyword: keyword.value || undefined }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = formatDate(dateRange.value[0])
      params.endDate = formatDate(dateRange.value[1])
    }
    const res = await prescriptionApi.list(params)
    prescriptions.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch { message.error('加载失败') }
  finally { loading.value = false }
}

function search() { page.value = 1; fetchRx() }
function reset() { keyword.value = ''; dateRange.value = null; page.value = 1; fetchRx() }

onMounted(fetchRx)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">处方管理</h2>
      <NButton type="primary" @click="router.push('/prescriptions/new')">新增处方</NButton>
    </div>
    <NCard>
      <NSpace style="margin-bottom: 12px;">
        <NInput v-model:value="keyword" placeholder="搜索病人名称或手机号" clearable style="width: 240px;" @keyup.enter="search" />
        <NDatePicker v-model:value="dateRange" type="daterange" clearable style="width: 260px;" />
        <NButton @click="search">搜索</NButton>
        <NButton @click="reset">重置</NButton>
      </NSpace>
      <NDataTable :columns="columns" :data="prescriptions" :loading="loading" :pagination="{
        page, pageSize: 10, itemCount: total,
        onChange: (p: number) => { page = p; fetchRx() }
      }" />
    </NCard>
  </div>
</template>
