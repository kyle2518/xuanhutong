<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NCard, NDataTable, NInput, NSpace, NTag, NModal, useMessage } from 'naive-ui'
import { patientApi } from '@/api/patients'

const router = useRouter()
const message = useMessage()

const patients = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const loading = ref(false)

const columns: any = [
  { title: '姓名', key: 'name', render: (row: any) => row.name },
  { title: '电话', key: 'phone' },
  { title: '性别', key: 'gender', render: (row: any) => row.gender === 0 ? '男' : row.gender === 1 ? '女' : '-' },
  { title: '年龄', key: 'age', render: (row: any) => row.age || '-' },
  { title: '住址', key: 'address', ellipsis: { tooltip: true }, render: (row: any) => row.address || '-' },
  { title: '创建时间', key: 'createdAt', render: (row: any) => row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '-' },
  {
    title: '操作', key: 'action', render: (row: any) => h('div', [
      h(NButton, { size: 'small', onClick: () => router.push(`/patients/${row.id}`) }, { default: () => '详情' }),
      h(NButton, { size: 'small', style: { marginLeft: '6px' }, onClick: () => router.push(`/patients/${row.id}/edit`) }, { default: () => '编辑' }),
    ])
  }
]
import { h } from 'vue'

async function fetchPatients() {
  loading.value = true
  try {
    const res = await patientApi.list({ page: page.value, size: pageSize.value, keyword: keyword.value || undefined })
    patients.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch (e: any) {
    message.error('加载病人列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchPatients)
watch([page, keyword], () => fetchPatients())
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">病人管理</h2>
      <NButton type="primary" @click="router.push('/patients/new')">新建病人</NButton>
    </div>
    <NCard>
      <NSpace style="margin-bottom: 12px;">
        <NInput v-model:value="keyword" placeholder="搜索姓名或电话" clearable style="width: 240px;" />
        <NButton @click="fetchPatients">搜索</NButton>
      </NSpace>
      <NDataTable :columns="columns" :data="patients" :loading="loading" :pagination="{
        page: page, pageSize: pageSize, itemCount: total,
        onChange: (p: number) => page = p,
        onPageSizeChange: (s: number) => { pageSize = s; page = 1 }
      }" />
    </NCard>
  </div>
</template>
