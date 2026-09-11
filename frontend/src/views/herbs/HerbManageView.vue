<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NButton, NSpace, NModal, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { herbApi } from '@/api/herbs'

const message = useMessage()
const herbs = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loading = ref(false)

const showFormModal = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<any>({})

const showDelete = ref(false)
const deleteTarget = ref<any>(null)

const showImportModal = ref(false)
const importFile = ref<File | null>(null)
const importLoading = ref(false)

function emptyForm() {
  return { pinyinName: '', chineseName: '', latinName: '', category: '', properties: '', meridianTropism: '', efficacy: '', indications: '', dosageRange: '', contraindications: '' }
}

const columns: any = [
  { title: '中文名', key: 'chineseName' },
  { title: '拼音', key: 'pinyinName' },
  { title: '分类', key: 'category', render: (row: any) => row.category || '-' },
  { title: '性味', key: 'properties', render: (row: any) => row.properties || '-' },
  { title: '功效', key: 'efficacy', ellipsis: { tooltip: true }, render: (row: any) => row.efficacy || '-' },
  {
    title: '操作', key: 'action', width: 140,
    render: (row: any) => h('div', [
      h(NButton, { size: 'small', onClick: () => edit(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', style: { marginLeft: '6px' }, onClick: () => confirmDelete(row) }, { default: () => '删除' }),
    ]),
  },
]

async function fetch() {
  loading.value = true
  try {
    const res = await herbApi.search({ keyword: keyword.value || undefined, p: page.value, s: 20 })
    herbs.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } finally { loading.value = false }
}

function add() {
  isEdit.value = false
  editId.value = null
  form.value = emptyForm()
  showFormModal.value = true
}

function edit(row: any) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    pinyinName: row.pinyinName, chineseName: row.chineseName, latinName: row.latinName || '',
    category: row.category || '', properties: row.properties || '', meridianTropism: row.meridianTropism || '',
    efficacy: row.efficacy || '', indications: row.indications || '', dosageRange: row.dosageRange || '', contraindications: row.contraindications || '',
  }
  showFormModal.value = true
}

async function save() {
  if (!form.value.chineseName || !form.value.pinyinName) { message.warning('请填写中文名和拼音名'); return }
  try {
    if (isEdit.value && editId.value) {
      await herbApi.update(editId.value, form.value)
    } else {
      await herbApi.create(form.value)
    }
    message.success(isEdit.value ? '更新成功' : '创建成功')
    showFormModal.value = false
    fetch()
  } catch (e: any) { message.error(e.response?.data?.message || '操作失败') }
}

function confirmDelete(row: any) {
  deleteTarget.value = row
  showDelete.value = true
}

async function doDelete() {
  if (!deleteTarget.value) return
  try {
    await herbApi.delete(deleteTarget.value.id)
    message.success('删除成功')
    showDelete.value = false
    deleteTarget.value = null
    fetch()
  } catch (e: any) { message.error(e.response?.data?.message || '删除失败') }
}

function onImportFile(e: any) {
  importFile.value = e.target.files?.[0] || null
}

function parseCsv(text: string): any[] {
  const lines = text.split(/\r?\n/).filter((l) => l.trim())
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map((h) => h.trim())
  const result: any[] = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(',')
    const obj: any = {}
    headers.forEach((hd, idx) => { obj[hd] = (cols[idx] || '').trim() })
    if (obj.chineseName && obj.pinyinName) result.push(obj)
  }
  return result
}

async function doImport() {
  if (!importFile.value) { message.warning('请先选择 CSV 文件'); return }
  importLoading.value = true
  try {
    const text = await importFile.value.text()
    const rows = parseCsv(text)
    if (!rows.length) { message.warning('未解析到有效数据，请检查 CSV 格式'); return }
    const res = await herbApi.batch(rows)
    message.success(`成功导入 ${res.data.data} 味药材`)
    showImportModal.value = false
    importFile.value = null
    fetch()
  } catch (e: any) { message.error(e.response?.data?.message || '导入失败') }
  finally { importLoading.value = false }
}

onMounted(fetch)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">药材管理</h2>
      <NSpace>
        <NButton @click="showImportModal = true">批量导入</NButton>
        <NButton type="primary" @click="add">新增药材</NButton>
      </NSpace>
    </div>
    <NCard>
      <NSpace style="margin-bottom: 12px;">
        <NInput v-model:value="keyword" placeholder="搜索中文名或拼音" clearable style="width: 240px;" @keyup.enter="() => { page = 1; fetch() }" />
        <NButton @click="() => { page = 1; fetch() }">搜索</NButton>
      </NSpace>
      <NDataTable :columns="columns" :data="herbs" :loading="loading" :pagination="{
        page, pageSize: 20, itemCount: total, onChange: (p: number) => { page = p; fetch() }
      }" />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showFormModal" :title="isEdit ? '编辑药材' : '新增药材'" preset="card" style="width: 720px;" title-style="font-size:18px;font-weight:600;">
      <div style="max-height: 560px; overflow-y: auto; padding-right: 4px;">
        <NForm label-placement="left" label-width="80">
          <NFormItem label="中文名" required><NInput v-model:value="form.chineseName" placeholder="例如：半夏" /></NFormItem>
          <NFormItem label="拼音名" required><NInput v-model:value="form.pinyinName" placeholder="例如：banxia" /></NFormItem>
          <NFormItem label="拉丁名"><NInput v-model:value="form.latinName" placeholder="例如：Pinelliae Rhizoma" /></NFormItem>
          <NFormItem label="分类"><NInput v-model:value="form.category" placeholder="例如：化痰止咳平喘药" /></NFormItem>
          <NFormItem label="性味"><NInput v-model:value="form.properties" placeholder="例如：辛，温；有毒" /></NFormItem>
          <NFormItem label="归经"><NInput v-model:value="form.meridianTropism" placeholder="例如：归脾、胃、肺经" /></NFormItem>
          <NFormItem label="功效"><NInput v-model:value="form.efficacy" type="textarea" rows="2" placeholder="功效" /></NFormItem>
          <NFormItem label="主治"><NInput v-model:value="form.indications" type="textarea" rows="2" placeholder="主治" /></NFormItem>
          <NFormItem label="常用剂量"><NInput v-model:value="form.dosageRange" placeholder="例如：3-9g" /></NFormItem>
          <NFormItem label="禁忌"><NInput v-model:value="form.contraindications" type="textarea" rows="2" placeholder="禁忌" /></NFormItem>
        </NForm>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showFormModal = false">取消</NButton>
          <NButton type="primary" @click="save">{{ isEdit ? '更新' : '保存' }}</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 删除确认 -->
    <NModal v-model:show="showDelete" title="确认删除" preset="card" style="width: 420px;" title-style="font-size:18px;font-weight:600;">
      <p style="margin:0 0 4px;">确认删除药材「<strong>{{ deleteTarget?.chineseName }}</strong>」吗？</p>
      <p style="font-size:13px;color:#999;margin:0;">删除后不可恢复（逻辑删除，历史处方不受影响）。</p>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDelete = false">取消</NButton>
          <NButton type="error" @click="doDelete">确认删除</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 批量导入 -->
    <NModal v-model:show="showImportModal" title="批量导入药材" preset="card" style="width: 560px;" title-style="font-size:18px;font-weight:600;">
      <div>
        <p style="font-size:13px;color:#666;margin:0 0 8px;">CSV 格式，第一行为表头，列名（英文）：</p>
        <p style="font-size:12px;color:#999;margin:0 0 12px;background:#fafafa;padding:8px;border-radius:4px;">
          chineseName,pinyinName,latinName,category,properties,meridianTropism,efficacy,indications,dosageRange,contraindications
        </p>
        <input type="file" accept=".csv" @change="onImportFile" />
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showImportModal = false">取消</NButton>
          <NButton type="primary" :loading="importLoading" @click="doImport">开始导入</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
