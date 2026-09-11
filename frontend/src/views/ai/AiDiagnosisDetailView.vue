<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard, NButton, NDescriptions, NDescriptionsItem, NDataTable, NSpace,
  NInput, NModal, NTag, NAlert, NCollapse, NCollapseItem, useMessage,
} from 'naive-ui'
import { aiApi } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const detail = ref<any>({})
const draft = ref<any>({})
const trace = ref<any[]>([])
const warnings = ref<string[]>([])
const evidence = ref<any[]>([])

const showEdit = ref(false)
const showSign = ref(false)
const signature = ref('')
const editDraft = ref<any>({})

const statusMap: Record<string, { text: string; type: any }> = {
  DRAFT: { text: '草稿', type: 'warning' },
  SIGNED: { text: '已签字', type: 'success' },
  REJECTED: { text: '已驳回', type: 'error' },
}

async function load() {
  try {
    const res = await aiApi.get(Number(route.params.id))
    detail.value = res.data.data || {}
    draft.value = detail.value.draftContent || {}
    trace.value = detail.value.agentTrace || []
    warnings.value = detail.value.compatibilityWarnings || []
    evidence.value = draft.value.classic_evidence || detail.value.evidence || []
  } catch (e: any) { message.error(e.response?.data?.message || '加载失败') }
}

const herbColumns: any = [
  { title: '序号', key: 'idx', render: (_: any, idx: number) => idx + 1 },
  { title: '药材', key: 'herb_name' },
  { title: '剂量(g)', key: 'dosage_grams' },
  { title: '备注', key: 'notes', render: (row: any) => row.notes || '-' },
]

function openEdit() {
  editDraft.value = JSON.parse(JSON.stringify(draft.value))
  if (!Array.isArray(editDraft.value.herbs)) editDraft.value.herbs = []
  if (!Array.isArray(editDraft.value.cautions)) editDraft.value.cautions = []
  showEdit.value = true
}

async function saveEdit() {
  try {
    await aiApi.update(Number(route.params.id), editDraft.value)
    message.success('已更新草稿')
    showEdit.value = false
    await load()
  } catch (e: any) { message.error(e.response?.data?.message || '更新失败') }
}

function addHerb() { editDraft.value.herbs.push({ herb_name: '', dosage_grams: 0, notes: '' }) }
function removeHerb(i: number | string) { editDraft.value.herbs.splice(Number(i), 1) }

async function sign() {
  if (!signature.value) { message.warning('请输入签名'); return }
  try {
    const res = await aiApi.sign(Number(route.params.id), signature.value)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = 'ai_diagnosis_signed.pdf'; a.click()
    URL.revokeObjectURL(url)
    showSign.value = false
    await load()
    message.success('签署成功')
  } catch (e: any) { message.error(e.response?.data?.message || '签署失败') }
}

async function convert() {
  try {
    const res = await aiApi.convert(Number(route.params.id))
    const pid = res.data.data?.prescriptionId
    message.success('已转为正式处方')
    await load()
    if (pid) router.push(`/prescriptions/${pid}`)
  } catch (e: any) { message.error(e.response?.data?.message || '转处方失败') }
}

async function reject() {
  try {
    await aiApi.reject(Number(route.params.id))
    message.success('已驳回')
    await load()
  } catch (e: any) { message.error(e.response?.data?.message || '操作失败') }
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">AI 辅助诊断详情</h2>
      <NSpace>
        <NTag :type="statusMap[detail.status]?.type || 'default'" size="medium">
          {{ statusMap[detail.status]?.text || detail.status }}
        </NTag>
        <template v-if="detail.status === 'DRAFT'">
          <NButton size="small" @click="openEdit">编辑</NButton>
          <NButton size="small" type="primary" @click="showSign = true">签字</NButton>
          <NButton size="small" type="error" @click="reject">驳回</NButton>
        </template>
        <template v-else-if="detail.status === 'SIGNED' && !detail.convertedPrescriptionId">
          <NButton size="small" type="primary" @click="convert">一键转处方</NButton>
        </template>
        <NButton v-if="detail.convertedPrescriptionId" size="small" type="success"
          @click="router.push(`/prescriptions/${detail.convertedPrescriptionId}`)">
          查看处方 #{{ detail.convertedPrescriptionId }}
        </NButton>
      </NSpace>
    </div>

    <!-- 配伍警告 -->
    <NAlert v-if="warnings.length" type="warning" title="配伍禁忌提醒" style="margin-bottom: 16px;">
      <ul style="margin: 0; padding-left: 18px;">
        <li v-for="(w, i) in warnings" :key="i">{{ w }}</li>
      </ul>
    </NAlert>

    <!-- 草案 -->
    <NCard title="辨证论治草案" style="margin-bottom: 16px;">
      <NDescriptions :column="2">
        <NDescriptionsItem label="辨证">{{ draft.syndrome_pattern || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="治法">{{ draft.treatment_principle || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="推荐方剂">{{ draft.recommended_formula || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="方剂出处">{{ draft.formula_source || '-' }}</NDescriptionsItem>
      </NDescriptions>
      <div v-if="draft.usage_instructions" style="margin-top: 12px;">
        <strong>用药说明：</strong>{{ draft.usage_instructions }}
      </div>
      <div v-if="draft.rationale" style="margin-top: 8px;">
        <strong>方解：</strong>{{ draft.rationale }}
      </div>
    </NCard>

    <NCard title="方剂组成" style="margin-bottom: 16px;">
      <NDataTable :columns="herbColumns" :data="draft.herbs || []" :pagination="false" />
    </NCard>

    <NCard title="古籍依据" style="margin-bottom: 16px;">
      <div v-for="(ev, i) in evidence" :key="i" style="margin-bottom: 10px;">
        <NTag size="small" type="info">{{ ev.book }}{{ ev.chapter ? '·' + ev.chapter : '' }}</NTag>
        <p style="margin: 6px 0 0; color: #555; font-size: 13px; white-space: pre-wrap;">{{ ev.source_text }}</p>
      </div>
      <p v-if="!evidence.length" style="color: #999;">暂无古籍引文</p>
    </NCard>

    <!-- Agent 执行轨迹 -->
    <NCard title="Agent 执行轨迹（可解释性）" style="margin-bottom: 16px;">
      <NCollapse v-if="trace.length">
        <NCollapseItem v-for="(t, i) in trace" :key="i" :name="i">
          <template #header>
            <NSpace align="center">
              <NTag size="small">{{ t.step }}. {{ t.tool_name }}</NTag>
            </NSpace>
          </template>
          <p style="margin: 4px 0; font-size: 13px;"><strong>调用参数：</strong>{{ t.tool_input }}</p>
          <p style="margin: 4px 0; font-size: 13px; white-space: pre-wrap; color: #555;"><strong>返回：</strong>{{ t.observation }}</p>
        </NCollapseItem>
      </NCollapse>
      <p v-else style="color: #999;">暂无轨迹</p>
    </NCard>

    <!-- 编辑弹窗 -->
    <NModal v-model:show="showEdit" title="编辑草案" preset="card" style="width: 760px;" title-style="font-size:18px;font-weight:600;">
      <div style="max-height: 560px; overflow-y: auto; padding-right: 4px;">
        <NInput v-model:value="editDraft.syndrome_pattern" placeholder="辨证" style="margin-bottom: 8px;" />
        <NInput v-model:value="editDraft.treatment_principle" placeholder="治法" style="margin-bottom: 8px;" />
        <NInput v-model:value="editDraft.recommended_formula" placeholder="推荐方剂" style="margin-bottom: 8px;" />
        <NInput v-model:value="editDraft.usage_instructions" type="textarea" placeholder="用药说明" rows="2" style="margin-bottom: 8px;" />
        <div style="margin: 12px 0 6px; font-weight: 600;">组成</div>
        <div v-for="(h, i) in editDraft.herbs" :key="i" style="display: flex; gap: 6px; margin-bottom: 6px;">
          <NInput v-model:value="h.herb_name" placeholder="药材" style="flex: 2;" />
          <NInput v-model:value="h.dosage_grams" placeholder="克" style="width: 80px;" />
          <NInput v-model:value="h.notes" placeholder="备注" style="flex: 1;" />
          <NButton size="small" type="error" @click="removeHerb(i)">✕</NButton>
        </div>
        <NButton dashed size="small" block @click="addHerb">+ 添加药材</NButton>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showEdit = false">取消</NButton>
          <NButton type="primary" @click="saveEdit">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 签字弹窗 -->
    <NModal v-model:show="showSign" title="签字确认" preset="card" style="width: 420px;" title-style="font-size:18px;font-weight:600;">
      <p style="margin-bottom: 12px;">签字后生成正式治疗方案 PDF，请输入医师签名：</p>
      <NInput v-model:value="signature" placeholder="请输入您的签名" style="margin-bottom: 16px;" />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showSign = false">取消</NButton>
          <NButton type="primary" @click="sign">确认签署</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
