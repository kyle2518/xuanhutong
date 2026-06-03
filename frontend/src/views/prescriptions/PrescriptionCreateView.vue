<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NButton, NInput, NInputNumber, NDataTable, NSelect, NSpace, NPopover, NModal, NTag, useMessage, NForm, NFormItem, NDivider, NDescriptions, NDescriptionsItem } from 'naive-ui'
import { prescriptionApi } from '@/api/prescriptions'
import { herbApi } from '@/api/herbs'
import { patientApi } from '@/api/patients'
import { classicApi } from '@/api/classicPrescriptions'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// Patient selection
const patientId = ref(route.params.patientId ? Number(route.params.patientId) : null as number | null)
const patients = ref([])
const patientSearchLoading = ref(false)

// Herb items
interface RxItem {
  herbId: number | null
  herbName: string
  dosageGrams: number
  notes: string
}
const items = ref<RxItem[]>([{ herbId: null, herbName: '', dosageGrams: 9, notes: '' }])
const diagnosis = ref('')
const rxNotes = ref('')
const totalDoses = ref(7)

// Herb autocomplete
const herbOptions = ref<any[]>([])
const herbSearchLoading = ref(false)
const tooltipContent = ref('')

// Classic Rx modal
const showClassicModal = ref(false)
const classicList = ref<any[]>([])

// PDF preview
const pdfLoading = ref(false)
const showPdfPreview = ref(false)
const pdfUrl = ref('')
const signatureInput = ref('')

// Patient search
async function searchPatients(keyword: string) {
  if (!keyword || keyword.length < 1) { patients.value = []; return }
  patientSearchLoading.value = true
  try {
    const res = await patientApi.list({ keyword, page: 1, size: 10 })
    patients.value = (res.data.data?.records || []).map((p: any) => ({ label: `${p.name} (${p.phone})`, value: p.id }))
  } finally { patientSearchLoading.value = false }
}

// Herb search
async function searchHerbs(keyword: string, index: number) {
  if (!keyword || keyword.length < 1) { herbOptions.value = []; return }
  herbSearchLoading.value = true
  try {
    const res = await herbApi.search({ keyword, size: 20 })
    herbOptions.value = (res.data.data?.records || []).map((h: any) => ({
      label: `${h.chineseName} (${h.pinyinName})`,
      value: h.id,
      herb: h
    }))
  } finally { herbSearchLoading.value = false }
}

function selectHerb(herbId: number, index: number) {
  const selected = herbOptions.value.find(o => o.value === herbId)
  if (selected) {
    items.value[index].herbId = selected.herb.id
    items.value[index].herbName = selected.herb.chineseName
    tooltipContent.value = selected.herb.efficacy || ''
  }
}

async function fetchTooltip(herbId: number | null) {
  if (!herbId) { tooltipContent.value = ''; return }
  try {
    const res = await herbApi.getTooltip(herbId)
    tooltipContent.value = res.data.data || ''
  } catch { tooltipContent.value = '' }
}

function addItem() {
  items.value.push({ herbId: null, herbName: '', dosageGrams: 9, notes: '' })
}

function removeItem(index: number) {
  if (items.value.length > 1) items.value.splice(index, 1)
}

// Classic Rx
async function loadClassicRx() {
  const res = await classicApi.list({ page: 1, size: 50 })
  classicList.value = res.data.data?.records || []
  showClassicModal.value = true
}

function applyClassicRx(rx: any) {
  try {
    const composition = JSON.parse(rx.composition)
    items.value = composition.map((item: any) => ({
      herbId: null, herbName: item.herb_name, dosageGrams: parseInt(item.dosage) || 9, notes: ''
    }))
    diagnosis.value = rx.efficacy || ''
    showClassicModal.value = false
    message.success(`已加载经典药方：${rx.name}`)
  } catch { message.error('加载失败，请检查药方数据格式') }
}

// Preview PDF
async function previewPdf() {
  if (!patientId.value) { message.warning('请先选择病人'); return }
  if (items.value.some(i => !i.herbName)) { message.warning('请填写所有药材名称'); return }
  pdfLoading.value = true
  try {
    const requestData = {
      patientId: patientId.value, diagnosis: diagnosis.value,
      notes: rxNotes.value, totalDoses: totalDoses.value,
      items: items.value.map(i => ({ herbId: i.herbId, herbName: i.herbName, dosageGrams: i.dosageGrams, notes: i.notes }))
    }
    const res = await prescriptionApi.previewPdf(requestData)
    pdfUrl.value = URL.createObjectURL(res.data)
    showPdfPreview.value = true
  } catch (e: any) { message.error('PDF预览失败') }
  finally { pdfLoading.value = false }
}

// Save prescription
async function savePrescription() {
  if (!patientId.value) { message.warning('请先选择病人'); return }
  pdfLoading.value = true
  try {
    const requestData = {
      patientId: patientId.value, diagnosis: diagnosis.value,
      notes: rxNotes.value, totalDoses: totalDoses.value,
      items: items.value.map(i => ({ herbId: i.herbId, herbName: i.herbName, dosageGrams: i.dosageGrams, notes: i.notes }))
    }
    const res = await prescriptionApi.create(requestData)
    message.success('药方创建成功')
    router.push(`/prescriptions/${res.data.data.id}`)
  } catch (e: any) { message.error(e.response?.data?.message || '保存失败') }
  finally { pdfLoading.value = false }
}

// Sign
async function signPrescription(prescriptionId: number) {
  if (!signatureInput.value) { message.warning('请签署姓名'); return }
  try {
    const res = await prescriptionApi.sign(prescriptionId, signatureInput.value)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url; a.download = 'prescription_signed.pdf'; a.click()
    URL.revokeObjectURL(url)
    message.success('签署成功')
  } catch (e: any) { message.error('签署失败') }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px;">新增处方</h2>

    <!-- Patient Selector -->
    <NCard title="选择病人" style="margin-bottom: 16px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="病人">
          <NSelect v-model:value="patientId" :options="patients" filterable remote clearable placeholder="输入姓名或电话搜索病人"
            @search="searchPatients" :loading="patientSearchLoading" />
        </NFormItem>
      </NForm>
    </NCard>

    <!-- Rx Items Table -->
    <NCard title="药方组成" style="margin-bottom: 16px;">
      <template #header-extra>
        <NButton size="small" @click="loadClassicRx">从经典药方导入</NButton>
      </template>
      <div v-for="(item, index) in items" :key="index" style="display: flex; gap: 8px; align-items: center; margin-bottom: 8px;">
        <span style="width: 24px; text-align: center;">{{ index + 1 }}</span>
        <NSelect v-model:value="item.herbId" :options="herbOptions" filterable remote clearable placeholder="输入药材名称搜索"
          @search="(q: string) => searchHerbs(q, index)" @update:value="(v: number) => selectHerb(v, index)"
          @focus="() => fetchTooltip(item.herbId)" :loading="herbSearchLoading" style="flex: 2;" />
        <NPopover trigger="hover" v-if="tooltipContent">
          <template #trigger>
            <span style="cursor: help; color: #18a058;">ⓘ</span>
          </template>
          <div style="max-width: 300px; white-space: pre-wrap; font-size: 13px;">{{ tooltipContent }}</div>
        </NPopover>
        <NInputNumber v-model:value="item.dosageGrams" :min="0" :step="1" style="width: 80px;" placeholder="克数" />
        <span style="min-width: 20px;">g</span>
        <NInput v-model:value="item.notes" placeholder="特殊用法" style="flex: 1;" />
        <NButton size="small" type="error" :disabled="items.length === 1" @click="removeItem(index)">删除</NButton>
      </div>
      <NButton dashed block @click="addItem" style="margin-top: 8px;">+ 添加药材</NButton>
    </NCard>

    <!-- Diagnosis and Notes -->
    <NCard title="诊断与用法" style="margin-bottom: 16px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="诊断">
          <NInput v-model:value="diagnosis" placeholder="请输入中医诊断" />
        </NFormItem>
        <NFormItem label="用法">
          <NInput v-model:value="rxNotes" type="textarea" placeholder="例如：水煎服，每日一剂，分两次服用" rows="2" />
        </NFormItem>
        <NFormItem label="剂数">
          <NInputNumber v-model:value="totalDoses" :min="1" :max="30" />
        </NFormItem>
      </NForm>
    </NCard>

    <!-- Actions -->
    <div style="display: flex; justify-content: flex-end; gap: 12px;">
      <NButton @click="router.back()">取消</NButton>
      <NButton :loading="pdfLoading" @click="previewPdf">预览PDF</NButton>
      <NButton type="primary" :loading="pdfLoading" @click="savePrescription">创建药方</NButton>
    </div>

    <!-- Classic Rx Modal -->
    <NModal v-model:show="showClassicModal" title="经典药方" style="width: 700px;">
      <div style="padding: 12px; max-height: 500px; overflow-y: auto;">
        <NCard v-for="rx in classicList" :key="rx.id" size="small" style="margin-bottom: 8px; cursor: pointer;" @click="applyClassicRx(rx)">
          <div style="font-weight: bold;">{{ rx.name }} <NTag size="small">{{ rx.source }}</NTag></div>
          <div style="font-size: 13px; color: #666; margin-top: 4px;">{{ rx.efficacy }}</div>
        </NCard>
      </div>
    </NModal>

    <!-- PDF Preview Modal -->
    <NModal v-model:show="showPdfPreview" title="药方预览" style="width: 800px;">
      <div style="padding: 12px;">
        <iframe v-if="pdfUrl" :src="pdfUrl" width="100%" height="500px" frameborder="0"></iframe>
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 12px;">
          <NSpace>
            <NInput v-model:value="signatureInput" placeholder="请输入签名" />
            <NButton type="primary" @click="() => signPrescription(0)">签署并下载</NButton>
          </NSpace>
          <NButton @click="showPdfPreview = false">关闭</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>
