import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/wechat/book',
    name: 'WechatBook',
    component: () => import('@/views/public/BookingPage.vue')
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'patients', name: 'PatientList', component: () => import('@/views/patients/PatientListView.vue') },
      { path: 'patients/new', name: 'PatientCreate', component: () => import('@/views/patients/PatientFormView.vue') },
      { path: 'patients/:id', name: 'PatientDetail', component: () => import('@/views/patients/PatientDetailView.vue') },
      { path: 'patients/:id/edit', name: 'PatientEdit', component: () => import('@/views/patients/PatientFormView.vue') },
      { path: 'prescriptions', name: 'PrescriptionList', component: () => import('@/views/prescriptions/PrescriptionListView.vue') },
      { path: 'prescriptions/new/:patientId?', name: 'PrescriptionCreate', component: () => import('@/views/prescriptions/PrescriptionCreateView.vue') },
      { path: 'prescriptions/:id', name: 'PrescriptionDetail', component: () => import('@/views/prescriptions/PrescriptionDetailView.vue') },
      { path: 'prescriptions/:id/preview', name: 'PrescriptionPreview', component: () => import('@/views/prescriptions/PrescriptionPreviewView.vue') },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/inventory/InventoryView.vue') },
      { path: 'appointments', name: 'Appointments', component: () => import('@/views/appointments/AppointmentListView.vue') },
      { path: 'appointments/qr', name: 'AppointmentQR', component: () => import('@/views/appointments/AppointmentQRView.vue') },
      { path: 'classics', name: 'ClassicPrescriptions', component: () => import('@/views/classics/ClassicPrescriptionListView.vue') },
      { path: 'ai', name: 'AiDiagnosisList', component: () => import('@/views/ai/AiDiagnosisListView.vue') },
      { path: 'ai/new/:patientId?', name: 'AiDiagnosisCreate', component: () => import('@/views/ai/AiDiagnosisCreateView.vue') },
      { path: 'ai/:id', name: 'AiDiagnosisDetail', component: () => import('@/views/ai/AiDiagnosisDetailView.vue') },
      { path: 'rag', name: 'RagKnowledgeBases', component: () => import('@/views/rag/RagKnowledgeBasesView.vue') },
      { path: 'rag/knowledge-bases/:id', name: 'RagKnowledgeBaseDetail', component: () => import('@/views/rag/RagKnowledgeBaseDetailView.vue') },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const routesRaw: any = routes
const router = createRouter({
  history: createWebHistory(),
  routes: routesRaw
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
