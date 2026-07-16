<script setup>
import { onMounted, ref } from 'vue'

import { getGoalProgressApi, getStatOverviewApi, getStatTrend7Api } from '@/api/stat'

const loading = ref(false)
const overview = ref({})
const trendList = ref([])
const goalProgressList = ref([])

const loadStatData = async () => {
  loading.value = true
  try {
    const [overviewData, trendData, progressData] = await Promise.all([
      getStatOverviewApi(),
      getStatTrend7Api(),
      getGoalProgressApi()
    ])
    overview.value = overviewData || {}
    trendList.value = Array.isArray(trendData) ? trendData : []
    goalProgressList.value = Array.isArray(progressData) ? progressData : []
  } finally {
    loading.value = false
  }
}

onMounted(loadStatData)
</script>

<template>
  <div class="page-container stat-page" v-loading="loading">
    <div class="info-grid">
      <el-card>
        <div class="info-label">累计学习时长</div>
        <div class="info-value">{{ overview.totalMinutes || 0 }} 分钟</div>
      </el-card>
      <el-card>
        <div class="info-label">累计打卡次数</div>
        <div class="info-value">{{ overview.totalCheckinCount || 0 }}</div>
      </el-card>
      <el-card>
        <div class="info-label">连续打卡天数</div>
        <div class="info-value">{{ overview.currentStreak || 0 }} 天</div>
      </el-card>
      <el-card>
        <div class="info-label">目标总数</div>
        <div class="info-value">{{ overview.goalCount || 0 }}</div>
      </el-card>
    </div>

    <el-card>
      <template #header>
        <span>近 7 天趋势</span>
      </template>
      <el-table :data="trendList" stripe>
        <el-table-column prop="date" label="日期" min-width="140" />
        <el-table-column prop="count" label="打卡次数" min-width="120" />
        <el-table-column prop="minutes" label="学习时长(分钟)" min-width="140" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <span>目标完成进度</span>
      </template>
      <el-table :data="goalProgressList" stripe>
        <el-table-column prop="title" label="目标" min-width="180" />
        <el-table-column prop="checkedDays" label="已打卡天数" min-width="120" />
        <el-table-column prop="targetDays" label="计划天数" min-width="120" />
        <el-table-column prop="progressPct" label="完成率" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progressPct || 0)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.stat-page {
  display: grid;
  gap: 20px;
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
