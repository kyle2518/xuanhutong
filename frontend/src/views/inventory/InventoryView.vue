<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NButton, NInputNumber, NTag, NSpace, NModal, NForm, NFormItem, NInput, NSelect, useMessage } from 'naive-ui'
import { inventoryApi } from '@/api/inventory'

const message = useMessage()
const inventory = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

// Edit modal
const showEditModal = ref(false)
const editItem = ref<any>({})
const editForm = ref({ stockGrams: 0, minStockAlert: 0, unitPrice: 0 })

// Add herb modal
const showAddModal = ref(false)
const addHerbId = ref<number | null>(null)
const addLoading = ref(false)
const availableHerbs = ref<any[]>([])
const herbSearchLoading = ref(false)

const columns: any = [
  { title: '药材名称', key: 'herbName', render: (row: any) => row.herbName },
  { title: '分类', key: 'category', render: (row: any) => row.category || '-' },
  { title: '库存(g)', key: 'stockGrams', render: (row: any) => {
    const isLow = row.isLowStock
    return h('span', { style: { color: isLow ? '#d03050' : '', fontWeight: isLow ? 'bold' : '' } }, String(row.stockGrams || 0))
  }},
  { title: '预警值(g)', key: 'minStockAlert', render: (row: any) => row.minStockAlert || '-' },
  { title: '状态', key: 'isLowStock', render: (row: any) => row.isLowStock ? h(NTag, { type: 'error', size: 'small' }, { default: () => '库存低' }) : h(NTag, { type: 'success', size: 'small' }, { default: () => '正常' }) },
  {
    title: '操作', key: 'action', render: (row: any) => h(NButton, { size: 'small', onClick: () => edit(row) }, { default: () => '编辑' })
  }
]

async function fetchInventory() {
  loading.value = true
  try {
    const res = await inventoryApi.list({ page: page.value, size: 50 })
    inventory.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } finally { loading.value = false }
}

function edit(item: any) {
  editItem.value = item
  editForm.value = { stockGrams: item.stockGrams || 0, minStockAlert: item.minStockAlert || 0, unitPrice: item.unitPrice || 0 }
  showEditModal.value = true
}

async function saveEdit() {
  try {
    await inventoryApi.update(editItem.value.id, editForm.value)
    message.success('更新成功')
    showEditModal.value = false
    fetchInventory()
  } catch (e: any) { message.error(e.response?.data?.message || '更新失败') }
}

// Add herb functions
async function searchAvailableHerbs(keyword: string) {
  herbSearchLoading.value = true
  try {
    const res = await inventoryApi.availableHerbs({ keyword, size: 30 })
    availableHerbs.value = (res.data.data?.records || []).map((h: any) => ({
      label: `${h.herbName} (${h.pinyinName}) - ${h.category || ''}`,
      value: h.herbId
    }))
  } finally { herbSearchLoading.value = false }
}

async function addHerb() {
  if (!addHerbId.value) { message.warning('请选择药材'); return }
  addLoading.value = true
  try {
    await inventoryApi.create({ herbId: addHerbId.value })
    message.success('药材已添加到库存')
    showAddModal.value = false
    addHerbId.value = null
    availableHerbs.value = []
    fetchInventory()
  } catch (e: any) { message.error(e.response?.data?.message || '添加失败') }
  finally { addLoading.value = false }
}

function openAddModal() {
  addHerbId.value = null
  availableHerbs.value = []
  showAddModal.value = true
}

onMounted(fetchInventory)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="margin: 0;">药材库存管理</h2>
      <NButton type="primary" @click="openAddModal">+ 添加药材</NButton>
    </div>
    <NCard>
      <NDataTable :columns="columns" :data="inventory" :loading="loading" :pagination="{
        page, pageSize: 50, itemCount: total,
        onChange: (p: number) => { page = p; fetchInventory() }
      }" />
    </NCard>

    <!-- Edit Modal -->
    <NModal v-model:show="showEditModal" title="编辑库存">
      <div style="padding: 20px;">
        <p style="margin-bottom: 12px;"><strong>药材：</strong>{{ editItem.herbName }}</p>
        <NForm label-placement="left" label-width="100">
          <NFormItem label="库存(克)">
            <NInputNumber v-model:value="editForm.stockGrams" :min="0" />
          </NFormItem>
          <NFormItem label="预警值(克)">
            <NInputNumber v-model:value="editForm.minStockAlert" :min="0" />
          </NFormItem>
          <NFormItem label="单价(元/克)">
            <NInputNumber v-model:value="editForm.unitPrice" :min="0" :step="0.01" />
          </NFormItem>
        </NForm>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <NButton @click="showEditModal = false">取消</NButton>
          <NButton type="primary" @click="saveEdit">保存</NButton>
        </div>
      </div>
    </NModal>

    <!-- Add Herb Modal -->
    <NModal v-model:show="showAddModal" title="添加药材到库存">
      <div style="padding: 20px;">
        <NForm label-placement="left" label-width="80">
          <NFormItem label="选择药材" required>
            <NSelect v-model:value="addHerbId" :options="availableHerbs" filterable remote clearable
              placeholder="输入药材名称搜索" @search="searchAvailableHerbs" :loading="herbSearchLoading" />
          </NFormItem>
        </NForm>
        <p style="font-size: 12px; color: #999; margin-top: -8px;">
          选择药材后点击添加，初始库存为 0，可在库存列表中编辑具体数值。
        </p>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <NButton @click="showAddModal = false">取消</NButton>
          <NButton type="primary" :loading="addLoading" @click="addHerb">添加</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>
