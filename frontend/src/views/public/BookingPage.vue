<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NDatePicker, NTimePicker, NButton, NResult, useMessage } from 'naive-ui'
import { appointmentApi } from '@/api/appointments'

const route = useRoute()
const message = useMessage()
const doctorId = ref(Number(route.query.doctorId || 0))
const clinic = ref(route.query.clinic || '中医诊所')
const submitted = ref(false)

const form = ref({
  name: '',
  phone: '',
  date: null as number | null,
  time: null as number | null,
  notes: ''
})

async function submit() {
  if (!form.value.name || !form.value.phone || !form.value.date) {
    message.warning('请填写完整信息')
    return
  }

  // Convert date + time to datetime
  let appointmentTime = new Date(form.value.date)
  if (form.value.time) {
    appointmentTime = new Date(form.value.time)
  }

  try {
    await appointmentApi.book({
      patientName: form.value.name,
      patientPhone: form.value.phone,
      appointmentTime: appointmentTime.toISOString(),
      userId: doctorId.value,
      notes: form.value.notes
    })
    submitted.value = true
  } catch (e: any) {
    message.error(e.response?.data?.message || '预约失败，请稍后重试')
  }
}
</script>

<template>
  <div class="booking-wrapper">
    <NCard class="booking-card" :bordered="true">
      <div class="header">
        <h1>{{ clinic }}</h1>
        <p>在线预约</p>
      </div>

      <NResult
        v-if="submitted"
        status="success"
        title="预约提交成功"
        description="您的预约信息已提交，医生会尽快确认，请保持电话畅通。"
      >
        <template #footer>
          <NButton @click="submitted = false">继续预约</NButton>
        </template>
      </NResult>

      <NForm v-else label-placement="left" label-width="80">
        <NFormItem label="姓名" required>
          <NInput v-model:value="form.name" placeholder="请输入您的姓名" />
        </NFormItem>
        <NFormItem label="手机号" required>
          <NInput v-model:value="form.phone" placeholder="请输入手机号" maxlength="11" />
        </NFormItem>
        <NFormItem label="预约日期" required>
          <NDatePicker v-model:value="form.date" type="date" placeholder="请选择日期" />
        </NFormItem>
        <NFormItem label="预约时间">
          <NTimePicker v-model:value="form.time" format="HH:mm" placeholder="请选择时间（可选）" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="form.notes" type="textarea" placeholder="如有特殊需求请备注" rows="2" />
        </NFormItem>
      </NForm>
      <NButton v-if="!submitted" type="primary" block @click="submit" style="margin-top: 12px;">提交预约</NButton>
    </NCard>
  </div>
</template>

<style scoped>
.booking-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
}
.booking-card {
  width: 420px;
  padding: 20px 0;
}
.header {
  text-align: center;
  margin-bottom: 24px;
}
.header h1 {
  font-size: 24px;
  color: #18a058;
  margin: 0;
}
.header p {
  color: #999;
  margin: 4px 0 0;
}
</style>
