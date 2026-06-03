<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NCard, NButton, NDescriptions, NDescriptionsItem, NDataTable, NSpace, NModal, NForm, NFormItem, NInput, NDatePicker, useMessage } from 'naive-ui'
import { patientApi } from '@/api/patients'
import { recordApi } from '@/api/medicalRecords'
import { prescriptionApi } from '@/api/prescriptions'
import { h } from 'vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const patient = ref<any>({})
const records = ref([])
const prescriptions = ref([])

const showRecordModal = ref(false)
const recordForm = ref({ visitDate: Date.now(), diagnosis: '', symptoms: '', treatmentMethod: '', notes: '' })

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const [pRes, rRes, rxRes] = await Promise.all([
      patientApi.get(id),
      recordApi.list(id, { page: 1, size: 50 }),
      prescriptionApi.list({ patientId: id, page: 1, size: 50 })
    ])
    patient.value = pRes.data.data
    records.value = rRes.data.data?.records || []
    prescriptions.value = rxRes.data.data?.records || []
  } catch { message.error('加载数据失败') }
})

const recordColumns: any = [
  { title: '就诊日期', key: 'visitDate', render: (row: any) => row.visitDate ? new Date(row.visitDate).toLocaleString() : '-' },
  { title: '诊断', key: 'diagnosis', render: (row: any) => row.diagnosis || '-', ellipsis: { tooltip: true } },
  { title: '治法', key: 'treatmentMethod', render: (row: any) => row.treatmentMethod || '-' },
]

const rxColumns: any = [
  { title: '诊断', key: 'diagnosis', render: (row: any) => row.diagnosis || '-' },
  { title: '剂数', key: 'totalDoses' },
  { title: '签名状态', key: 'isSigned', render: (row: any) => row.isSigned ? '已签' : '未签' },
  { title: '创建时间', key: 'createdAt', render: (row: any) => row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '-' },
]

async function createRecord() {
  if (!recordForm.value.visitDate) { message.warning('请选择就诊日期'); return }
  try {
    await recordApi.create(patient.value.id, { ...recordForm.value, visitDate: new Date(recordForm.value.visitDate).toISOString() })
    message.success('就诊记录创建成功')
    showRecordModal.value = false
    const res = await recordApi.list(patient.value.id, { page: 1, size: 50 })
    records.value = res.data.data?.records || []
  } catch (e: any) { message.error(e.response?.data?.message || '操作失败') }
}

async function exportPdf() {
  try {
    const res = await recordApi.exportPdf(patient.value.id)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url; a.download = `patient_${patient.value.name}_data.pdf`; a.click()
    URL.revokeObjectURL(url)
  } catch { message.error('导出失败') }
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">病人详情</h2>
      <NSpace>
        <NButton @click="exportPdf">导出病历PDF</NButton>
        <NButton @click="router.push(`/patients/${route.params.id}/edit`)">编辑</NButton>
        <NButton type="primary" @click="router.push(`/prescriptions/new/${route.params.id}`)">开具处方</NButton>
      </NSpace>
    </div>

    <NCard title="基本信息" style="margin-bottom: 16px;">
      <NDescriptions :column="3">
        <NDescriptionsItem label="姓名">{{ patient.name }}</NDescriptionsItem>
        <NDescriptionsItem label="手机号">{{ patient.phone }}</NDescriptionsItem>
        <NDescriptionsItem label="性别">{{ patient.gender === 0 ? '男' : patient.gender === 1 ? '女' : '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="年龄">{{ patient.age || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="住址">{{ patient.address || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="主诉">{{ patient.chiefComplaint || '-' }}</NDescriptionsItem>
      </NDescriptions>
    </NCard>

    <NCard title="就诊记录" style="margin-bottom: 16px;">
      <template #header-extra>
        <NButton size="small" @click="showRecordModal = true">添加就诊记录</NButton>
      </template>
      <NDataTable :columns="recordColumns" :data="records" :pagination="false" />
    </NCard>

    <NCard title="药方记录">
      <NDataTable :columns="rxColumns" :data="prescriptions" :pagination="false" />
    </NCard>

    <NModal v-model:show="showRecordModal" title="添加就诊记录">
      <div style="padding: 20px;">
        <NForm label-placement="left" label-width="80">
          <NFormItem label="就诊日期" required>
            <NDatePicker v-model:value="recordForm.visitDate" type="datetime" />
          </NFormItem>
          <NFormItem label="症状">
            <NInput v-model:value="recordForm.symptoms" type="textarea" placeholder="请输入症状" />
          </NFormItem>
          <NFormItem label="诊断">
            <NInput v-model:value="recordForm.diagnosis" type="textarea" placeholder="请输入诊断" />
          </NFormItem>
          <NFormItem label="治法">
            <NInput v-model:value="recordForm.treatmentMethod" type="textarea" placeholder="请输入治疗方法" />
          </NFormItem>
          <NFormItem label="备注">
            <NInput v-model:value="recordForm.notes" type="textarea" />
          </NFormItem>
        </NForm>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <NButton @click="showRecordModal = false">取消</NButton>
          <NButton type="primary" @click="createRecord">保存</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>
