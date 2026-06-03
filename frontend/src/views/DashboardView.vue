<script setup lang="ts">
import { NCard, NGrid, NGridItem, NSpace, NTag, NButton, NStatistic } from 'naive-ui'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ref, onMounted } from 'vue'
import { patientApi } from '@/api/patients'
import { appointmentApi } from '@/api/appointments'
import { inventoryApi } from '@/api/inventory'

const router = useRouter()
const auth = useAuthStore()
const patientCount = ref(0)
const pendingAppointments = ref(0)
const lowStockCount = ref(0)
const recentAppointments = ref<any[]>([])

onMounted(async () => {
  try {
    const [pRes, aRes, iRes] = await Promise.all([
      patientApi.list({ page: 1, size: 1 }),
      appointmentApi.list({ status: 'PENDING', page: 1, size: 5 }),
      inventoryApi.list({ lowStock: true, page: 1, size: 1 })
    ])
    patientCount.value = pRes.data.data?.total || 0
    recentAppointments.value = aRes.data.data?.records || []
    pendingAppointments.value = aRes.data.data?.total || 0
    lowStockCount.value = iRes.data.data?.total || 0
  } catch {}
})

const shortcuts = [
  { label: '新建病人', icon: '👤', desc: '创建病人档案', path: '/patients/new' },
  { label: '处方管理', icon: '💊', desc: '开具中药处方', path: '/prescriptions/new' },
  { label: '库存管理', icon: '📦', desc: '管理药材库存', path: '/inventory' },
  { label: '预约二维码', icon: '📱', desc: '生成微信预约码', path: '/appointments/qr' },
]

function getStatusType(s: string) { return { PENDING: 'warning', CONFIRMED: 'success', CANCELLED: 'default', COMPLETED: 'info' }[s] || 'default' as any }
function getStatusLabel(s: string) { return { PENDING: '待确认', CONFIRMED: '已确认', CANCELLED: '已取消', COMPLETED: '已完成' }[s] || s }
</script>

<template>
  <div class="dashboard">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div>
        <h2>欢迎回来，{{ auth.user?.name }}</h2>
        <p>{{ auth.user?.clinicName || '悬壶通中医诊所' }} · 悬壶济世，妙手回春</p>
      </div>
    </div>

    <!-- Stats Row -->
    <NGrid :cols="3" :x-gap="20" responsive="screen" style="margin-bottom: 24px;">
      <NGridItem><NCard class="stat-card"><div class="stat-row"><div class="stat-icon" style="background:rgba(16,185,129,0.12);color:#10b981;">👤</div><div class="stat-body"><NStatistic label="病人总数" :value="patientCount" /></div></div></NCard></NGridItem>
      <NGridItem><NCard class="stat-card"><div class="stat-row"><div class="stat-icon" style="background:rgba(245,158,11,0.12);color:#f59e0b;">📅</div><div class="stat-body"><NStatistic label="待确认预约" :value="pendingAppointments" /></div></div></NCard></NGridItem>
      <NGridItem><NCard class="stat-card" :style="lowStockCount > 0 ? 'border-left:3px solid #ef4444;' : ''"><div class="stat-row"><div class="stat-icon" style="background:rgba(239,68,68,0.12);color:#ef4444;">⚠️</div><div class="stat-body"><NStatistic label="低库存预警" :value="lowStockCount" /></div></div></NCard></NGridItem>
    </NGrid>

    <!-- Shortcuts -->
    <h3 class="section-title">快捷操作</h3>
    <NGrid :cols="4" :x-gap="16" responsive="screen" style="margin-bottom: 24px;">
      <NGridItem v-for="s in shortcuts" :key="s.path">
        <NCard class="shortcut-card" hoverable @click="router.push(s.path)">
          <div class="shortcut-row">
            <span class="shortcut-icon">{{ s.icon }}</span>
            <div class="shortcut-text"><strong>{{ s.label }}</strong><span>{{ s.desc }}</span></div>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>

    <!-- Recent Appointments -->
    <h3 class="section-title">近期预约</h3>
    <NCard>
      <div v-if="recentAppointments.length === 0" class="empty-hint"><p>暂无待处理的预约，去 <a @click="router.push('/appointments')" style="color:#166534;cursor:pointer;">预约管理</a> 查看</p></div>
      <div v-else class="apt-list">
        <div v-for="apt in recentAppointments" :key="apt.id" class="apt-row">
          <div class="apt-info"><strong>{{ apt.patientName }}</strong><span>{{ apt.patientPhone }}</span></div>
          <div class="apt-time">{{ new Date(apt.appointmentTime).toLocaleString() }}</div>
          <NTag :type="getStatusType(apt.status)" size="small">{{ getStatusLabel(apt.status) }}</NTag>
        </div>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.dashboard { max-width: 1100px; margin: 0 auto; }
.welcome-banner {
  background: linear-gradient(135deg, #14532d 0%, #166534 50%, #15803d 100%);
  color: #fff; padding: 28px 32px; border-radius: 16px; margin-bottom: 24px;
}
.welcome-banner h2 { margin: 0 0 4px; font-size: 22px; font-weight: 600; }
.welcome-banner p { margin: 0; opacity: 0.75; font-size: 14px; }
.section-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 12px; }
.stat-card { border-radius: 12px; }
.stat-row { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.stat-body { flex: 1; }
.shortcut-card { border-radius: 12px; cursor: pointer; transition: all 0.2s; }
.shortcut-card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.08); }
.shortcut-row { display: flex; align-items: center; gap: 14px; }
.shortcut-icon { font-size: 28px; }
.shortcut-text { display: flex; flex-direction: column; }
.shortcut-text strong { font-size: 14px; }
.shortcut-text span { font-size: 12px; color: #999; margin-top: 2px; }
.empty-hint { text-align: center; padding: 32px; color: #999; }
.apt-list { display: flex; flex-direction: column; }
.apt-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.apt-row:last-child { border-bottom: none; }
.apt-info { display: flex; flex-direction: column; }
.apt-info strong { font-size: 14px; } .apt-info span { font-size: 12px; color: #999; }
.apt-time { font-size: 13px; color: #666; }
</style>
