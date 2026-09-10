<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NDataTable, NButton, NTag, NModal, NForm, NFormItem, NInput, NSpace, useMessage } from 'naive-ui'
import { ragApi, type KnowledgeBase } from '@/api/rag'

const router = useRouter()
const message = useMessage()

const items = ref<KnowledgeBase[]>([])
const loading = ref(false)
const health = ref<'ok' | 'down'>('ok')

const showCreate = ref(false)
const form = ref({ name: '', description: '' })
const creating = ref(false)

const showDelete = ref(false)
const deleteTarget = ref<KnowledgeBase | null>(null)
const deleting = ref(false)

const columns: any[] = [
  { title: '名称', key: 'name', render: (row: KnowledgeBase) => row.name },
  { title: '描述', key: 'description', ellipsis: { tooltip: true }, render: (row: KnowledgeBase) => row.description || '-' },
  { title: '文档数', key: 'document_count', width: 90, render: (row: KnowledgeBase) => row.document_count },
  { title: '分片数', key: 'chunk_count', width: 90, render: (row: KnowledgeBase) => row.chunk_count },
  {
    title: '状态', key: 'status', width: 90, render: (row: KnowledgeBase) => {
      return h(NTag, { size: 'small', type: row.chunk_count > 0 ? 'success' : 'default' }, { default: () => row.chunk_count > 0 ? '已索引' : '空' })
    }
  },
  { title: '更新时间', key: 'updated_at', width: 170, render: (row: KnowledgeBase) => fmtDate(row.updated_at) },
  {
    title: '操作', key: 'action', width: 180, render: (row: KnowledgeBase) => h('div', [
      h(NButton, { size: 'small', type: 'primary', onClick: () => router.push(`/rag/knowledge-bases/${row.id}`) }, { default: () => '管理' }),
      h(NButton, { size: 'small', type: 'error', style: { marginLeft: '6px' }, onClick: () => requestDelete(row) }, { default: () => '删除' }),
    ])
  }
]

function fmtDate(v: string) {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 19)
}

async function fetchHealth() {
  try {
    const res = await ragApi.health()
    health.value = res.data?.status === 'ok' ? 'ok' : 'down'
  } catch {
    health.value = 'down'
  }
}

async function fetch() {
  loading.value = true
  try {
    const res = await ragApi.listKnowledgeBases()
    items.value = res.data.items || []
  } catch {
    message.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', description: '' }
  showCreate.value = true
}

async function doCreate() {
  if (!form.value.name.trim()) { message.warning('请填写知识库名称'); return }
  creating.value = true
  try {
    await ragApi.createKnowledgeBase({ name: form.value.name.trim(), description: form.value.description.trim() })
    message.success('创建成功')
    showCreate.value = false
    fetch()
  } catch (e: any) {
    message.error(e?.response?.data?.detail || '创建失败')
  } finally {
    creating.value = false
  }
}

function requestDelete(row: KnowledgeBase) {
  deleteTarget.value = row
  showDelete.value = true
}

async function doDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await ragApi.deleteKnowledgeBase(deleteTarget.value.id)
    message.success('删除成功')
    showDelete.value = false
    deleteTarget.value = null
    fetch()
  } catch (e: any) {
    message.error(e?.response?.data?.detail || '删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetch()
  fetchHealth()
})
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">知识库管理</h2>
      <NSpace align="center">
        <NTag size="small" :type="health === 'ok' ? 'success' : 'error'">
          RAG 服务 {{ health === 'ok' ? '正常' : '不可用' }}
        </NTag>
        <NButton type="primary" @click="openCreate">新建知识库</NButton>
      </NSpace>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="items" :loading="loading" :pagination="{ pageSize: 20 }" />
    </NCard>

    <NModal v-model:show="showCreate" title="新建知识库" preset="card" style="width: 480px;" title-style="font-size:18px;font-weight:600;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="名称" required>
          <NInput v-model:value="form.name" placeholder="例如：门诊中医诊疗规范" maxlength="100" />
        </NFormItem>
        <NFormItem label="描述">
          <NInput v-model:value="form.description" type="textarea" placeholder="可选：说明知识库内容范围" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showCreate = false">取消</NButton>
          <NButton type="primary" :loading="creating" @click="doCreate">创建</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="showDelete" title="确认删除" preset="card" style="width: 420px;" title-style="font-size:18px;font-weight:600;">
      <p style="font-size:15px;margin:0 0 4px;">确认删除知识库「<strong>{{ deleteTarget?.name }}</strong>」吗？</p>
      <p style="font-size:13px;color:#999;margin:0;">将同时删除其中的全部文档与向量索引，不可恢复。</p>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDelete = false">取消</NButton>
          <NButton type="error" :loading="deleting" @click="doDelete">确认删除</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>