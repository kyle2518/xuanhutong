<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NCard, NForm, NFormItem, NInput, NSelect, NInputNumber, useMessage } from 'naive-ui'
import { patientApi } from '@/api/patients'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const isEdit = ref(!!route.params.id && route.name === 'PatientEdit')
const loading = ref(false)

const form = ref({
  name: '', phone: '', gender: undefined as number | undefined, age: undefined as number | undefined,
  birthDate: null as string | null, address: '', idCard: '', chiefComplaint: ''
})

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await patientApi.get(Number(route.params.id))
      Object.assign(form.value, {
        name: res.data.data.name, phone: res.data.data.phone, gender: res.data.data.gender,
        age: res.data.data.age, birthDate: res.data.data.birthDate, address: res.data.data.address,
        idCard: res.data.data.idCard, chiefComplaint: res.data.data.chiefComplaint
      })
    } catch { message.error('加载病人信息失败') }
  }
})

async function submit() {
  if (!form.value.name) {
    message.warning('请填写姓名')
    return
  }
  if (!/^1\d{10}$/.test(form.value.phone)) {
    message.warning('请输入正确的手机号（11位数字）')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await patientApi.update(Number(route.params.id), form.value)
    } else {
      await patientApi.create(form.value)
    }
    message.success(isEdit.value ? '更新成功' : '创建成功')
    router.push('/patients')
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
  } finally { loading.value = false }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px;">{{ isEdit ? '编辑病人' : '新建病人' }}</h2>
    <NCard>
      <NForm label-placement="left" label-width="100">
        <NFormItem label="姓名" required>
          <NInput v-model:value="form.name" placeholder="请输入姓名" />
        </NFormItem>
        <NFormItem label="手机号" required>
          <NInput v-model:value="form.phone" placeholder="请输入手机号" />
        </NFormItem>
        <NFormItem label="性别">
          <NSelect v-model:value="form.gender" :options="[{ label: '男', value: 0 }, { label: '女', value: 1 }]" placeholder="请选择性别" clearable />
        </NFormItem>
        <NFormItem label="年龄">
          <NInputNumber v-model:value="form.age" :min="0" :max="150" placeholder="请输入年龄" />
        </NFormItem>
        <NFormItem label="住址">
          <NInput v-model:value="form.address" placeholder="请输入住址" />
        </NFormItem>
        <NFormItem label="身份证号">
          <NInput v-model:value="form.idCard" placeholder="请输入身份证号" maxlength="18" />
        </NFormItem>
        <NFormItem label="主诉">
          <NInput v-model:value="form.chiefComplaint" type="textarea" placeholder="请输入主诉" rows="3" />
        </NFormItem>
      </NForm>
      <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
        <NButton @click="router.back()">取消</NButton>
        <NButton type="primary" :loading="loading" @click="submit">{{ isEdit ? '更新' : '创建' }}</NButton>
      </div>
    </NCard>
  </div>
</template>
