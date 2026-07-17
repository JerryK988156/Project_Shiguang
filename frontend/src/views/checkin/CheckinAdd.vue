<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { addCheckinApi, getTodayCheckinApi } from '@/api/checkin'
import { getGoalListApi } from '@/api/goal'

const loading = ref(false)
const goalList = ref([])
const todayStatus = ref(null)

const form = reactive({
  goalId: '',
  checkinDate: new Date().toISOString().slice(0, 10),
  studyDuration: 30,
  content: '',
  remark: ''
})

const loadGoals = async () => {
  const data = await getGoalListApi()
  goalList.value = Array.isArray(data) ? data.filter((item) => item.status !== '已放弃') : []
}

const loadTodayStatus = async () => {
  if (!form.goalId) { todayStatus.value = null; return }
  todayStatus.value = await getTodayCheckinApi({ goalId: form.goalId })
}

const handleSubmit = async () => {
  if (!form.goalId) { ElMessage.warning('请选择目标'); return }
  loading.value = true
  try {
    await addCheckinApi({ ...form, checkinDate: new Date().toISOString().slice(0, 10) })
    ElMessage.success('打卡成功')
    await loadTodayStatus()
    form.content = ''
    form.remark = ''
  } finally {
    loading.value = false
  }
}

watch(() => form.goalId, loadTodayStatus)

onMounted(async () => {
  await loadGoals()
  if (goalList.value.length > 0) form.goalId = goalList.value[0].id
})
</script>

<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header><span>今日打卡</span></template>

      <el-alert
        v-if="todayStatus"
        :title="todayStatus.hasChecked ? '当前目标今天已打卡' : '当前目标今天还未打卡'"
        :type="todayStatus.hasChecked ? 'success' : 'info'"
        show-icon :closable="false" class="page-alert"
      />

      <el-form label-width="100px">
        <el-form-item label="目标">
          <el-select v-model="form.goalId" placeholder="请选择目标" style="width: 320px">
            <el-option v-for="item in goalList" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="打卡日期">
          <el-input :model-value="form.checkinDate" disabled style="width: 200px" />
        </el-form-item>
        <el-form-item label="学习时长">
          <el-input-number v-model="form.studyDuration" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="学习内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入今天的学习内容" />
        </el-form-item>
        <el-form-item label="心得备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入心得或备注" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">提交打卡</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
