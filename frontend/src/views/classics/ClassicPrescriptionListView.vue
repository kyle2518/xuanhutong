<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NButton, NModal, NForm, NFormItem, NInput, NTag, NSpace, useMessage } from 'naive-ui'
import { classicApi } from '@/api/classicPrescriptions'

const message = useMessage()
const classics = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loading = ref(false)

const showModal = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref({ name: '', source: '', category: '', composition: '', efficacy: '', indications: '', usageMethod: '', sourceText: '', notes: '' })

const columns: any = [
  { title: '方名', key: 'name', render: (row: any) => row.name },
  { title: '出处', key: 'source', render: (row: any) => row.source },
  { title: '分类', key: 'category', render: (row: any) => row.category || '-' },
  { title: '功效', key: 'efficacy', ellipsis: { tooltip: true }, render: (row: any) => row.efficacy || '-' },
  {
    title: '操作', key: 'action', render: (row: any) => h('div', [
      h(NButton, { size: 'small', onClick: () => edit(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', style: { marginLeft: '6px' }, onClick: () => remove(row.id) }, { default: () => '删除' }),
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
  form.value = { name: row.name, source: row.source, category: row.category, composition: row.composition, efficacy: row.efficacy, indications: row.indications || '', usageMethod: row.usageMethod || '', sourceText: row.sourceText || '', notes: row.notes || '' }
  showModal.value = true
}

function add() {
  isEdit.value = false; editId.value = null
  form.value = { name: '', source: '伤寒杂病论', category: '', composition: '[{"herb_name":"","dosage":""}]', efficacy: '', indications: '', usageMethod: '', sourceText: '', notes: '' }
  showModal.value = true
}

async function save() {
  if (!form.value.name || !form.value.efficacy || !form.value.composition) {
    message.warning('请填写方名、组成和功效'); return
  }
  try {
    if (isEdit.value && editId.value) {
      await classicApi.update(editId.value, form.value)
    } else {
      await classicApi.create(form.value)
    }
    message.success(isEdit.value ? '更新成功' : '创建成功')
    showModal.value = false; fetch()
  } catch (e: any) { message.error('操作失败') }
}

async function remove(id: number) {
  try {
    await classicApi.delete(id)
    message.success('删除成功')
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

    <NModal v-model:show="showModal" :title="isEdit ? '编辑经典药方' : '添加经典药方'" style="width: 700px;">
      <div style="padding: 20px; max-height: 600px; overflow-y: auto;">
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
          <NFormItem label="组成" required>
            <NInput v-model:value="form.composition" type="textarea" placeholder='JSON格式：[{"herb_name":"桂枝","dosage":"三两"}]' rows="4" />
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
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="save">{{ isEdit ? '更新' : '保存' }}</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>
