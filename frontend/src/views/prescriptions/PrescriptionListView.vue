<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NSpace, useMessage } from 'naive-ui'
import { prescriptionApi } from '@/api/prescriptions'

const router = useRouter()
const message = useMessage()
const prescriptions = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const columns: any = [
  { title: '病人ID', key: 'patientId' },
  { title: '诊断', key: 'diagnosis', render: (row: any) => row.diagnosis || '-', ellipsis: { tooltip: true } },
  { title: '剂数', key: 'totalDoses' },
  { title: '签名状态', key: 'isSigned', render: (row: any) => row.isSigned ? '已签' : '未签' },
  { title: '创建时间', key: 'createdAt', render: (row: any) => row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '-' },
  {
    title: '操作', key: 'action', render: (row: any) => h(NButton, { size: 'small', onClick: () => router.push(`/prescriptions/${row.id}`) }, { default: () => '详情' }),
  }
]

async function fetchRx() {
  loading.value = true
  try {
    const res = await prescriptionApi.list({ page: page.value, size: 10 })
    prescriptions.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch { message.error('加载失败') }
  finally { loading.value = false }
}

onMounted(fetchRx)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">处方管理</h2>
      <NButton type="primary" @click="router.push('/prescriptions/new')">新增处方</NButton>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="prescriptions" :loading="loading" :pagination="{
        page, pageSize: 10, itemCount: total,
        onChange: (p: number) => { page = p; fetchRx() }
      }" />
    </NCard>
  </div>
</template>
