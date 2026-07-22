<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import confetti from 'canvas-confetti'

import { addCheckinApi, getTodayCheckinApi } from '@/api/checkin'
import { getGoalListApi } from '@/api/goal'

const route = useRoute()

const loading = ref(false)
const checkinSuccess = ref(false)
const goalList = ref([])
const todayStatus = ref(null)
const achievementVisible = ref(false)
const achievementInfo = ref({ goalTitle: '', milestoneDays: 0, badgeName: '' })

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
    const result = await addCheckinApi({ ...form, checkinDate: new Date().toISOString().slice(0, 10) })
    ElMessage.success('打卡成功')
    await loadTodayStatus()
    form.content = ''
    form.remark = ''

    // 打卡成功动画
    checkinSuccess.value = true
    setTimeout(() => { checkinSuccess.value = false }, 1500)

    const ach = result?.achievement
    if (ach && ach.earned) {
      achievementInfo.value = {
        goalTitle: ach.goalTitle || '',
        milestoneDays: ach.milestoneDays || 0,
        badgeName: ach.badgeName || ''
      }
      achievementVisible.value = true
      // 成就彩带效果
      nextTick(() => {
        confetti({
          particleCount: 120,
          spread: 80,
          origin: { y: 0.6 },
          colors: ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#ec4899']
        })
      })
    }
  } finally {
    loading.value = false
  }
}

watch(() => form.goalId, loadTodayStatus)

onMounted(async () => {
  await loadGoals()
  if (route.query.goalId && goalList.value.some(g => String(g.id) === String(route.query.goalId))) {
    form.goalId = route.query.goalId
  } else if (goalList.value.length > 0) {
    form.goalId = goalList.value[0].id
  }
})
</script>

<template>
  <div class="page-container">
    <el-card class="checkin-card" v-loading="loading">
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
          <el-button
            type="primary"
            :class="{ 'checkin-success-btn': checkinSuccess }"
            @click="handleSubmit"
          >{{ checkinSuccess ? '✓ 打卡成功' : '提交打卡' }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>

  <el-dialog v-model="achievementVisible" title="🎉 成就达成" width="420px" center>
    <div style="text-align: center; padding: 20px">
      <div style="font-size: 48px; margin-bottom: 8px">🏆</div>
      <div style="font-size: 24px; font-weight: 700; margin: 16px 0 8px">
        {{ achievementInfo.goalTitle }}
      </div>
      <div style="font-size: 18px; color: #6b7280">
        累计打卡 {{ achievementInfo.milestoneDays }} 天，获得 {{ achievementInfo.badgeName }}
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.page-container {
  padding: 24px;
  max-width: 1400px;
}

.checkin-card {
  border-radius: 16px;
}

.checkin-success-btn {
  background-color: #67c23a !important;
  border-color: #67c23a !important;
  animation: checkin-pulse 1.5s ease-out;
}

@keyframes checkin-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.6);
  }
  50% {
    box-shadow: 0 0 0 12px rgba(103, 194, 58, 0.1);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}
</style>
