<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NButton, NModal, NForm, NFormItem, NInput, NInputNumber, NTag, NSpace, NSelect, NPopover, useMessage } from 'naive-ui'
import { classicApi } from '@/api/classicPrescriptions'
import { herbApi } from '@/api/herbs'

const message = useMessage()
const classics = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loading = ref(false)

const showModal = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref({ name: '', source: '', category: '', efficacy: '', indications: '', usageMethod: '', sourceText: '', notes: '' })
const showDeleteConfirm = ref(false)
const deleteTarget = ref<{ id: number; name: string } | null>(null)

// Herb composition editor
interface HerbItem { herbName: string; dosage: string }
const herbItems = ref<HerbItem[]>([])
const herbOptions = ref<any[]>([])
const herbSearchLoading = ref(false)
const herbTooltip = ref('')

const columns: any = [
  { title: '方名', key: 'name', render: (row: any) => row.name },
  { title: '出处', key: 'source', render: (row: any) => row.source },
  { title: '分类', key: 'category', render: (row: any) => row.category || '-' },
  { title: '功效', key: 'efficacy', ellipsis: { tooltip: true }, render: (row: any) => row.efficacy || '-' },
  {
    title: '操作', key: 'action', render: (row: any) => h('div', [
      h(NButton, { size: 'small', onClick: () => edit(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', style: { marginLeft: '6px' }, onClick: () => confirmDelete(row) }, { default: () => '删除' }),
    ])
  }
]

async function fetch() {
  loading.value = true
  try {
    const res = await classicApi.list({ page: page.value, size: 20, keyword: keyword.value || undefined })
    classics.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } finally { loading.value = false }
}

function edit(row: any) {
  isEdit.value = true; editId.value = row.id
  form.value = { name: row.name, source: row.source, category: row.category || '', efficacy: row.efficacy, indications: row.indications || '', usageMethod: row.usageMethod || '', sourceText: row.sourceText || '', notes: row.notes || '' }
  // Parse composition JSON into editable items
  parseComposition(row.composition)
  showModal.value = true
}

function add() {
  isEdit.value = false; editId.value = null
  form.value = { name: '', source: '伤寒杂病论', category: '', efficacy: '', indications: '', usageMethod: '', sourceText: '', notes: '' }
  herbItems.value = [{ herbName: '', dosage: '' }]
  showModal.value = true
}

function parseComposition(json: string) {
  try {
    const arr = JSON.parse(json)
    herbItems.value = arr.map((item: any) => ({ herbName: item.herb_name || '', dosage: item.dosage || '' }))
  } catch {
    herbItems.value = [{ herbName: '', dosage: '' }]
  }
}

function buildComposition(): string {
  const valid = herbItems.value.filter(i => i.herbName.trim())
  return JSON.stringify(valid.map(i => ({ herb_name: i.herbName.trim(), dosage: i.dosage.trim() || '适量' })))
}

async function searchHerbs(keyword: string) {
  if (!keyword || keyword.length < 1) { herbOptions.value = []; return }
  herbSearchLoading.value = true
  try {
    const res = await herbApi.search({ keyword, size: 20 })
    herbOptions.value = (res.data.data?.records || []).map((h: any) => ({
      label: `${h.chineseName} (${h.pinyinName})`,
      value: h.chineseName,
      herb: h
    }))
  } finally { herbSearchLoading.value = false }
}

function selectHerb(index: number, name: string) {
  herbItems.value[index].herbName = name
  const found = herbOptions.value.find(o => o.value === name)
  if (found) herbTooltip.value = found.herb.efficacy || ''
}

function addItem() { herbItems.value.push({ herbName: '', dosage: '' }) }
function removeItem(index: number) { if (herbItems.value.length > 1) herbItems.value.splice(index, 1) }
function increaseDose(index: number) {
  const v = parseInt(herbItems.value[index].dosage) || 0
  herbItems.value[index].dosage = String(v + 1)
}
function decreaseDose(index: number) {
  const v = parseInt(herbItems.value[index].dosage) || 1
  if (v > 1) herbItems.value[index].dosage = String(v - 1)
}

async function save() {
  if (!form.value.name || !form.value.efficacy) { message.warning('请填写方名和功效'); return }
  if (herbItems.value.filter(i => i.herbName.trim()).length === 0) { message.warning('请至少添加一味药材'); return }
  const composition = buildComposition()
  try {
    if (isEdit.value && editId.value) {
      await classicApi.update(editId.value, { ...form.value, composition })
    } else {
      await classicApi.create({ ...form.value, composition })
    }
    message.success(isEdit.value ? '更新成功' : '创建成功')
    showModal.value = false; fetch()
  } catch (e: any) { message.error('操作失败') }
}

function confirmDelete(row: any) {
  deleteTarget.value = { id: row.id, name: row.name }
  showDeleteConfirm.value = true
}

async function doDelete() {
  if (!deleteTarget.value) return
  try {
    await classicApi.delete(deleteTarget.value.id)
    message.success('删除成功')
    showDeleteConfirm.value = false; deleteTarget.value = null
    fetch()
  } catch { message.error('删除失败') }
}

onMounted(fetch)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">经典药方</h2>
      <NSpace>
        <NInput v-model:value="keyword" placeholder="搜索药方" clearable @keyup.enter="fetch" style="width: 200px;" />
        <NButton @click="fetch">搜索</NButton>
        <NButton type="primary" @click="add">添加药方</NButton>
      </NSpace>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="classics" :loading="loading" :pagination="{
        page, pageSize: 20, itemCount: total,
        onChange: (p: number) => { page = p; fetch() }
      }" />
    </NCard>

    <NModal v-model:show="showModal" :title="isEdit ? '编辑经典药方' : '添加经典药方'" preset="card" style="width:750px;" title-style="font-size:18px;font-weight:600;">
      <div style="max-height:520px;overflow-y:auto;padding-right:4px;">
        <NForm label-placement="left" label-width="80">
          <NFormItem label="方名" required>
            <NInput v-model:value="form.name" placeholder="例如：桂枝汤" />
          </NFormItem>
          <NFormItem label="出处" required>
            <NInput v-model:value="form.source" placeholder="例如：伤寒杂病论" />
          </NFormItem>
          <NFormItem label="分类">
            <NInput v-model:value="form.category" placeholder="例如：解表剂" />
          </NFormItem>
          <!-- Herb composition editor -->
          <NFormItem label="组成" required>
            <NCard size="small" style="background:#fafafa;">
              <div v-for="(item, index) in herbItems" :key="index" style="display:flex;gap:6px;align-items:center;margin-bottom:6px;">
                <span style="width:20px;text-align:center;flex-shrink:0;color:#999;">{{ index + 1 }}</span>
                <NSelect v-model:value="item.herbName" :options="herbOptions" filterable remote clearable
                  placeholder="搜索药材名称" @search="searchHerbs" @update:value="(v: string) => selectHerb(index, v)"
                  :loading="herbSearchLoading" style="flex:2;" />
                <NPopover trigger="hover" v-if="herbTooltip">
                  <template #trigger><span style="cursor:help;color:#18a058;font-size:14px;">ⓘ</span></template>
                  <div style="max-width:280px;white-space:pre-wrap;font-size:12px;">{{ herbTooltip }}</div>
                </NPopover>
                <NButton size="tiny" @click="decreaseDose(index)" style="flex-shrink:0;">−</NButton>
                <NInput v-model:value="item.dosage" placeholder="用量" style="width:80px;text-align:center;" />
                <NButton size="tiny" @click="increaseDose(index)" style="flex-shrink:0;">+</NButton>
                <NButton size="tiny" type="error" :disabled="herbItems.length === 1" @click="removeItem(index)" style="flex-shrink:0;">✕</NButton>
              </div>
              <NButton dashed size="small" block @click="addItem">+ 添加药材</NButton>
            </NCard>
          </NFormItem>
          <NFormItem label="功效" required>
            <NInput v-model:value="form.efficacy" type="textarea" placeholder="请输入功效" rows="2" />
          </NFormItem>
          <NFormItem label="主治">
            <NInput v-model:value="form.indications" type="textarea" />
          </NFormItem>
          <NFormItem label="用法">
            <NInput v-model:value="form.usageMethod" type="textarea" />
          </NFormItem>
          <NFormItem label="原文">
            <NInput v-model:value="form.sourceText" type="textarea" rows="3" />
          </NFormItem>
          <NFormItem label="备注">
            <NInput v-model:value="form.notes" type="textarea" />
          </NFormItem>
        </NForm>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="save">{{ isEdit ? '更新' : '保存' }}</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- Delete Confirmation -->
    <NModal v-model:show="showDeleteConfirm" title="确认删除" preset="card" style="width:420px;" title-style="font-size:18px;font-weight:600;">
      <p style="font-size:15px;margin:0 0 4px;">确认删除药方「<strong>{{ deleteTarget?.name }}</strong>」吗？</p>
      <p style="font-size:13px;color:#999;margin:0;">删除后不可恢复。</p>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDeleteConfirm = false">取消</NButton>
          <NButton type="error" @click="doDelete">确认删除</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
