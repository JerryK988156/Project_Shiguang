<script setup>
import { onMounted, ref } from 'vue'

import { getStatOverviewApi, getStatTrend7Api } from '@/api/stat'

const overview = ref({
  totalGoals: 0,
  totalCheckins: 0,
  totalMinutes: 0,
  streakDays: 0
})

const trendList = ref([])
const loading = ref(false)

const loadDashboard = async () => {
  loading.value = true
  try {
    const [overviewData, trendData] = await Promise.all([
      getStatOverviewApi(),
      getStatTrend7Api()
    ])
    overview.value = {
      totalGoals: overviewData?.goalCount || 0,
      totalCheckins: overviewData?.totalCheckinCount || 0,
      totalMinutes: overviewData?.totalMinutes || 0,
      streakDays: overviewData?.currentStreak || 0
    }
    trendList.value = Array.isArray(trendData) ? trendData : []
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-title">学习概览</div>
      </template>

      <p>这里展示当前账号的目标执行情况、累计打卡和近 7 天趋势。</p>
    </el-card>

    <div class="info-grid">
      <el-card>
        <div class="info-label">目标总数</div>
        <div class="info-value">{{ overview.totalGoals }}</div>
      </el-card>
      <el-card>
        <div class="info-label">打卡总数</div>
        <div class="info-value">{{ overview.totalCheckins }}</div>
      </el-card>
      <el-card>
        <div class="info-label">累计时长</div>
        <div class="info-value">{{ overview.totalMinutes }} 分钟</div>
      </el-card>
      <el-card>
        <div class="info-label">连续打卡</div>
        <div class="info-value">{{ overview.streakDays }} 天</div>
      </el-card>
    </div>

    <el-card>
      <template #header>
        <div class="card-title">近 7 天打卡趋势</div>
      </template>

      <el-table :data="trendList" stripe>
        <el-table-column prop="date" label="日期" min-width="140" />
        <el-table-column prop="count" label="打卡次数" min-width="120" />
        <el-table-column prop="minutes" label="学习时长(分钟)" min-width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  display: grid;
  gap: 20px;
}

.welcome-card {
  border-radius: 16px;
}

.card-title {
  font-weight: 700;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.info-label {
  margin-bottom: 12px;
  color: #6b7280;
}

.info-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}
</style>
