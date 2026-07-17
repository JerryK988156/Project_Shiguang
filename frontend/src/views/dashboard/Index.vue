<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'

const route = useRoute()

import { getStatOverviewApi, getStatTrend7Api, getGoalProgressApi } from '@/api/stat'

const overview = ref({
  totalGoals: 0,
  totalCheckins: 0,
  totalMinutes: 0,
  streakDays: 0
})

const trendList = ref([])
const goalList = ref([])
const loading = ref(false)

const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChartInstance = null
let pieChartInstance = null

const loadDashboard = async () => {
  loading.value = true
  try {
    const [overviewData, trendData, goalData] = await Promise.all([
      getStatOverviewApi(),
      getStatTrend7Api(),
      getGoalProgressApi()
    ])
    overview.value = {
      totalGoals: overviewData?.goalCount || 0,
      totalCheckins: overviewData?.totalCheckinCount || 0,
      totalMinutes: overviewData?.totalMinutes || 0,
      streakDays: overviewData?.currentStreak || 0
    }
    trendList.value = Array.isArray(trendData) ? trendData : []
    goalList.value = Array.isArray(goalData) ? goalData : []
  } finally {
    loading.value = false
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }
  const dates = trendList.value.map((item) => item.date)
  const counts = trendList.value.map((item) => item.count)
  const minutes = trendList.value.map((item) => item.minutes)
  trendChartInstance.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['打卡次数', '学习时长(分钟)']
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: [
      {
        type: 'value',
        name: '打卡次数',
        minInterval: 1
      },
      {
        type: 'value',
        name: '学习时长(分钟)'
      }
    ],
    series: [
      {
        name: '打卡次数',
        type: 'line',
        data: counts,
        smooth: true,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '学习时长(分钟)',
        type: 'line',
        yAxisIndex: 1,
        data: minutes,
        smooth: true,
        itemStyle: { color: '#67C23A' }
      }
    ]
  })
}

const getStatusLabel = (status) => {
  if (status === '已完成') return '已完成'
  if (status === '已放弃') return '已放弃'
  return '进行中'
}

const reload = async () => {
  await loadDashboard()
  nextTick(() => {
    renderTrendChart()
    renderPieChart()
  })
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChartInstance) {
    pieChartInstance = echarts.init(pieChartRef.value)
  }
  const statusMap = {}
  goalList.value.forEach((item) => {
    const label = getStatusLabel(item.status)
    statusMap[label] = (statusMap[label] || 0) + 1
  })
  const pieData = Object.entries(statusMap).map(([name, value]) => ({ name, value }))
  pieChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '目标状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0,0,0,0.5)'
          }
        },
        label: {
          formatter: '{b}: {c}'
        }
      }
    ]
  })
}

const handleResize = () => {
  trendChartInstance?.resize()
  pieChartInstance?.resize()
}

watch(trendList, () => {
  nextTick(renderTrendChart)
})

watch(goalList, () => {
  nextTick(renderPieChart)
})

onMounted(async () => {
  await loadDashboard()
  nextTick(() => {
    renderTrendChart()
    renderPieChart()
  })
  window.addEventListener('resize', handleResize)
})

// 每次导航到本页面时重新加载数据
watch(() => route.path, async (to) => {
  if (to === '/dashboard') {
    await reload()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
  pieChartInstance?.dispose()
})
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
        <div class="card-title">目标状态分布</div>
      </template>
      <div ref="pieChartRef" class="chart-container"></div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-title">近 7 天打卡趋势</div>
      </template>
      <div ref="trendChartRef" class="chart-container"></div>
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

.chart-container {
  width: 100%;
  height: 360px;
}
</style>
