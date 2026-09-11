<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard, NDataTable, NButton, NTag, NModal, NInput, NInputNumber,
  NTabs, NTabPane, NSpace, NSpin, NList, NListItem, NEmpty, useMessage
} from 'naive-ui'
import { ragApi, type KnowledgeBase, type RagDocument, type SearchSource } from '@/api/rag'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const kbId = route.params.id as string
const kb = ref<KnowledgeBase | null>(null)
const loading = ref(false)
const notFound = ref(false)

const docs = ref<RagDocument[]>([])
const docsLoading = ref(false)
const showDeleteDoc = ref(false)
const deleteDocTarget = ref<RagDocument | null>(null)

const question = ref('')
const topK = ref(4)
const answering = ref(false)
const answer = ref('')
const sources = ref<SearchSource[]>([])

const docColumns: any[] = [
  { title: '文件名', key: 'filename', ellipsis: { tooltip: true } },
  { title: '类型', key: 'file_type', width: 90, render: (row: RagDocument) => row.file_type },
  { title: '分片数', key: 'chunk_count', width: 90 },
  {
    title: '状态', key: 'status', width: 90, render: (row: RagDocument) => {
      return h(NTag, { size: 'small', type: row.status === 'ready' ? 'success' : 'error' }, { default: () => row.status === 'ready' ? '已就绪' : row.status })
    }
  },
  { title: '上传时间', key: 'created_at', width: 170, render: (row: RagDocument) => fmtDate(row.created_at) },
  {
    title: '操作', key: 'action', width: 90, render: (row: RagDocument) => h('div', [
      h(NButton, { size: 'small', type: 'error', onClick: () => requestDeleteDoc(row) }, { default: () => '删除' })
    ])
  }
]

function fmtDate(v: string) {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 19)
}

async function fetchKb() {
  loading.value = true
  try {
    const res = await ragApi.getKnowledgeBase(kbId)
    kb.value = res.data
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function fetchDocs() {
  docsLoading.value = true
  try {
    const res = await ragApi.listDocuments(kbId)
    docs.value = res.data.items || []
  } catch (e: any) {
    message.error(e?.response?.data?.detail || '加载文档失败')
  } finally {
    docsLoading.value = false
  }
}

async function onFileInput(e: any) {
  const files: File[] = Array.from(e.target.files || [])
  if (!files.length) return
  for (const f of files) {
    try {
      await ragApi.uploadDocument(kbId, f)
      message.success(`${f.name} 上传成功`)
    } catch (err: any) {
      message.error(err?.response?.data?.detail || `${f.name} 上传失败`)
    }
  }
  e.target.value = ''
  fetchDocs()
  fetchKb()
}

function requestDeleteDoc(row: RagDocument) {
  deleteDocTarget.value = row
  showDeleteDoc.value = true
}

async function doDeleteDoc() {
  if (!deleteDocTarget.value) return
  try {
    await ragApi.deleteDocument(deleteDocTarget.value.id)
    message.success('删除成功')
    showDeleteDoc.value = false
    deleteDocTarget.value = null
    fetchDocs()
    fetchKb()
  } catch (e: any) {
    message.error(e?.response?.data?.detail || '删除失败')
  }
}

async function ask() {
  if (!question.value.trim()) { message.warning('请输入问题'); return }
  answering.value = true
  answer.value = ''
  sources.value = []
  try {
    const res = await ragApi.query({
      question: question.value.trim(),
      knowledge_base_id: kbId,
      top_k: topK.value
    })
    answer.value = res.data.answer
    sources.value = res.data.sources || []
  } catch (e: any) {
    message.error(e?.response?.data?.detail || '问答失败')
  } finally {
    answering.value = false
  }
}

onMounted(async () => {
  await fetchKb()
  if (!notFound.value) {
    fetchDocs()
    answer.value = '知识库已就绪，试试在上方输入一个问题（例如「四君子汤的功效是什么？」）。'
  }
})
</script>

<template>
  <div v-if="notFound">
    <NEmpty description="知识库不存在或已被删除">
      <template #extra>
        <NButton @click="router.push('/rag')">返回知识库列表</NButton>
      </template>
    </NEmpty>
  </div>
  <div v-else-if="kb">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div style="display: flex; align-items: center; gap: 12px;">
        <NButton size="small" @click="router.push('/rag')">← 返回</NButton>
        <h2 style="margin: 0;">{{ kb.name }}</h2>
        <NTag size="small" type="info">{{ kb.document_count }} 文档 / {{ kb.chunk_count }} 分片</NTag>
      </div>
    </div>
    <p v-if="kb.description" style="color:#666; margin:-6px 0 16px 4px;">{{ kb.description }}</p>

    <NCard style="min-height: 420px;">
      <NTabs type="line" animated>
        <NTabPane name="documents" tab="文档管理">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
            <span style="color:#666;">支持 txt / md / pdf / docx，自动分块并向量化</span>
            <label style="display: inline-block; cursor: pointer;">
              <input type="file" multiple accept=".txt,.md,.pdf,.docx" style="display: none;" @change="onFileInput" />
              <NButton type="primary" tag="span">上传文档</NButton>
            </label>
          </div>
          <NDataTable :columns="docColumns" :data="docs" :loading="docsLoading" :pagination="{ pageSize: 10 }" />
        </NTabPane>

        <NTabPane name="qa" tab="知识问答">
          <div style="display: flex; gap: 8px; align-items: flex-start; margin-bottom: 12px;">
            <NInput
              v-model:value="question"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入问题，例如：四君子汤的功效是什么？"
              style="flex: 1;"
              @keydown.enter.exact.prevent="ask"
            />
            <NSpace vertical align="center" justify="center" style="width: 100px;">
              <NButton type="primary" :loading="answering" style="width: 100%;" @click="ask">提问</NButton>
              <div style="font-size: 11px; color: #999; text-align: center; width: 100%; line-height: 1;">检索条数</div>
              <NInputNumber v-model:value="topK" :min="1" :max="20" size="small" style="width: 100%;" />
            </NSpace>
          </div>

          <NSpin :show="answering">
            <NCard size="small" style="min-height: 180px; margin-bottom: 12px; background: #fafafa;" :bordered="false">
              <template #header><span style="font-weight:600;">回答</span></template>
              <div style="white-space: pre-wrap; line-height: 1.8; color: #333;" v-if="answer">{{ answer }}</div>
              <NEmpty v-else description="暂无回答" style="padding: 40px 0;" />
            </NCard>
          </NSpin>

          <template v-if="sources.length">
            <div style="font-weight: 600; margin-bottom: 8px;">参考资料 ({{ sources.length }})</div>
            <NList hoverable clickable>
              <NListItem v-for="(s, i) in sources" :key="i">
                <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                  <span style="font-weight: 500; color: #18a058;">{{ s.source }}</span>
                  <NTag size="small" :type="s.score > 0.5 ? 'success' : 'default'">相似度 {{ s.score.toFixed(3) }}</NTag>
                </div>
                <div style="font-size: 13px; color: #666; line-height: 1.6;">{{ s.chunk }}</div>
              </NListItem>
            </NList>
          </template>
        </NTabPane>
      </NTabs>
    </NCard>

    <NModal v-model:show="showDeleteDoc" title="确认删除" preset="card" style="width: 420px;" title-style="font-size:18px;font-weight:600;">
      <p style="font-size:15px;margin:0 0 4px;">确认删除文档「<strong>{{ deleteDocTarget?.filename }}</strong>」并移除其向量索引吗？</p>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDeleteDoc = false">取消</NButton>
          <NButton type="error" @click="doDeleteDoc">确认删除</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
  <div v-else style="display: flex; justify-content: center; padding: 80px;">
    <NSpin />
  </div>
</template>