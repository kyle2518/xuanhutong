<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NTag, NDatePicker, NSpace, useMessage, NSelect } from 'naive-ui'
import { appointmentApi } from '@/api/appointments'

const router = useRouter()
const message = useMessage()
const appointments = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filterDate = ref<string | null>(null)
const filterStatus = ref<string | null>(null)

const columns: any = [
  { title: '预约人', key: 'patientName' },
  { title: '电话', key: 'patientPhone' },
  { title: '预约时间', key: 'appointmentTime', render: (row: any) => row.appointmentTime ? new Date(row.appointmentTime).toLocaleString() : '-' },
  { title: '状态', key: 'status', render: (row: any) => {
    const statusMap: any = { PENDING: 'warning', CONFIRMED: 'success', CANCELLED: 'default', COMPLETED: 'info' }
    const labelMap: any = { PENDING: '待确认', CONFIRMED: '已确认', CANCELLED: '已取消', COMPLETED: '已完成' }
    return h(NTag, { type: statusMap[row.status] || 'default', size: 'small' }, { default: () => labelMap[row.status] || row.status })
  }},
  { title: '备注', key: 'notes', render: (row: any) => row.notes || '-' },
  {
    title: '操作', key: 'action', render: (row: any) => h(NSpace, { size: 'small' }, {
      default: () => [
        h(NButton, { size: 'tiny', type: 'success', disabled: row.status !== 'PENDING', onClick: () => updateStatus(row.id, 'CONFIRMED') }, { default: () => '确认' }),
        h(NButton, { size: 'tiny', type: 'warning', disabled: row.status === 'COMPLETED' || row.status === 'CANCELLED', onClick: () => updateStatus(row.id, 'COMPLETED') }, { default: () => '完成' }),
        h(NButton, { size: 'tiny', type: 'error', disabled: row.status === 'CANCELLED', onClick: () => updateStatus(row.id, 'CANCELLED') }, { default: () => '取消' }),
      ]
    })
  }
]

async function fetchAppointments() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 10 }
    if (filterDate.value) params.date = filterDate.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await appointmentApi.list(params)
    appointments.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } finally { loading.value = false }
}

async function updateStatus(id: number, status: string) {
  try {
    await appointmentApi.updateStatus(id, status)
    message.success('状态更新成功')
    fetchAppointments()
  } catch { message.error('更新失败') }
}

onMounted(fetchAppointments)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">预约管理</h2>
      <NButton type="primary" @click="router.push('/appointments/qr')">生成预约二维码</NButton>
    </div>
    <NCard>
      <NSpace style="margin-bottom: 12px;">
        <NSelect v-model:value="filterStatus" :options="[{ label: '全部', value: '' }, { label: '待确认', value: 'PENDING' }, { label: '已确认', value: 'CONFIRMED' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' }]" clearable placeholder="状态筛选" style="width: 140px;" @update:value="() => { page = 1; fetchAppointments() }" />
        <NButton @click="fetchAppointments">刷新</NButton>
      </NSpace>
      <NDataTable :columns="columns" :data="appointments" :loading="loading" :pagination="{
        page, pageSize: 10, itemCount: total,
        onChange: (p: number) => { page = p; fetchAppointments() }
      }" />
    </NCard>
  </div>
</template>
