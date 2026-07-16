<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { addGoalApi, getGoalDetailApi, updateGoalApi } from '@/api/goal'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const goalId = route.params.id

const form = reactive({
  id: '',
  title: '',
  description: '',
  startDate: '',
  endDate: '',
  targetDays: 0,
  status: '进行中'
})

const loadDetail = async () => {
  if (!goalId) {
    return
  }
  loading.value = true
  try {
    const data = await getGoalDetailApi(goalId)
    Object.assign(form, data)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.title || !form.startDate) {
    ElMessage.warning('请填写目标标题和开始日期')
    return
  }

  loading.value = true
  try {
    if (goalId) {
      await updateGoalApi(form)
      ElMessage.success('目标更新成功')
    } else {
      await addGoalApi(form)
      ElMessage.success('目标创建成功')
    }
    router.push('/goal/list')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <span>{{ goalId ? '编辑目标' : '新增目标' }}</span>
      </template>

      <el-form label-width="100px">
        <el-form-item label="目标标题">
          <el-input v-model="form.title" placeholder="请输入目标标题" />
        </el-form-item>
        <el-form-item label="目标说明">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入目标说明" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" />
        </el-form-item>
        <el-form-item label="计划天数">
          <el-input-number v-model="form.targetDays" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 180px">
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已放弃" value="已放弃" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" @click="handleSubmit">保存</el-button>
            <el-button @click="router.push('/goal/list')">返回</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
