<script setup lang="ts">
import { NLayout, NLayoutHeader, NLayoutSider, NLayoutContent, NMenu, NButton, NAvatar, NDropdown, NSpace, NTag } from 'naive-ui'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed, h, ref } from 'vue'
import type { MenuOption } from 'naive-ui'
import { renderIcon } from '@/utils/icons'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const collapsed = ref(false)

function renderLabel(icon: any, label: string) {
  return h('span', { style: { marginLeft: '6px' } }, label)
}

const menuOptions: MenuOption[] = [
  { label: () => renderLabel('📊', '工作台'), key: '/' },
  { label: () => renderLabel('👤', '病人管理'), key: '/patients' },
  { label: () => renderLabel('💊', '处方管理'), key: '/prescriptions' },
  { label: () => renderLabel('📦', '药材库存'), key: '/inventory' },
  { label: () => renderLabel('📅', '预约管理'), key: '/appointments' },
  { label: () => renderLabel('📜', '经典药方'), key: '/classics' },
  { label: () => renderLabel('📚', '知识库'), key: '/rag' },
];

const activeKey = computed(() => {
  const path = route.path
  if (path.startsWith('/patients')) return '/patients'
  if (path.startsWith('/prescriptions')) return '/prescriptions'
  if (path.startsWith('/inventory')) return '/inventory'
  if (path.startsWith('/appointments')) return '/appointments'
  if (path.startsWith('/classics')) return '/classics'
  if (path.startsWith('/rag')) return '/rag'
  return '/'
})

function handleMenuUpdate(key: string) {
  router.push(key)
}

const dropdownOptions = [
  { label: '退出登录', key: 'logout' }
]

function handleDropdownSelect(key: string) {
  if (key === 'logout') {
    auth.logout()
  }
}
</script>

<template>
  <NLayout has-sider position="absolute">
    <NLayoutSider bordered collapse-mode="width" :collapsed-width="64" :width="200" :collapsed="collapsed" show-trigger @collapse="collapsed = true" @expand="collapsed = false">
      <div style="height: 64px; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: bold; color: #18a058; border-bottom: 1px solid #eee;">
        <span v-if="!collapsed">悬壶通</span>
        <span v-else style="font-size: 16px;">悬</span>
      </div>
      <NMenu :collapsed-width="64" :collapsed-icon-size="22" :options="menuOptions" :value="activeKey" @update:value="handleMenuUpdate" />
    </NLayoutSider>
    <NLayout>
      <NLayoutHeader bordered style="height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px;">
        <div style="font-size: 16px; color: #666;">
          悬壶通 - 中医门店管理系统
        </div>
        <NDropdown trigger="click" :options="dropdownOptions" @select="handleDropdownSelect">
          <NSpace align="center" style="cursor: pointer;">
            <span>{{ auth.user?.name }}</span>
            <NTag size="small" type="success">{{ auth.user?.clinicName || '诊所' }}</NTag>
          </NSpace>
        </NDropdown>
      </NLayoutHeader>
      <NLayoutContent style="padding: 24px; background: #f5f7fa; min-height: calc(100vh - 64px);">
        <router-view />
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>
