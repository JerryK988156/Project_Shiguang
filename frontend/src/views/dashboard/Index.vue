<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'

const route = useRoute()

import { getStatOverviewApi, getStatTrend7Api, getStatTrend30Api, getGoalProgressApi, getCheckinCalendarApi } from '@/api/stat'
import { askPermission, sendNotification, shouldNotifyToday } from '@/utils/notification'

const overview = ref({
  totalGoals: 0,
  totalCheckins: 0,
  totalMinutes: 0,
  streakDays: 0
})

const trendList = ref([])
const trend30List = ref([])
const goalList = ref([])
const calendarData = ref({ data: [], maxCount: 1, maxPossible: 1 })
const loading = ref(false)
const statsRange = ref('7')
const notifyEnabled = ref(localStorage.getItem('shiguang-notify-enabled') === 'true')

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const calendarRef = ref(null)
let trendChartInstance = null
let pieChartInstance = null
let calendarInstance = null

const loadDashboard = async () => {
  loading.value = true
  try {
    const [overviewData, trendData, trend30Data, goalData, calData] = await Promise.all([
      getStatOverviewApi(),
      getStatTrend7Api(),
      getStatTrend30Api(),
      getGoalProgressApi(),
      getCheckinCalendarApi()
    ])
    overview.value = {
      totalGoals: overviewData?.goalCount || 0,
      totalCheckins: overviewData?.totalCheckinCount || 0,
      totalMinutes: overviewData?.totalMinutes || 0,
      streakDays: overviewData?.currentStreak || 0
    }
    trendList.value = Array.isArray(trendData) ? trendData : []
    trend30List.value = Array.isArray(trend30Data) ? trend30Data : []
    goalList.value = Array.isArray(goalData) ? goalData : []
    calendarData.value = calData || { data: [], maxCount: 1 }
  } finally {
    loading.value = false
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }
  if (statsRange.value === '30') {
    const dates = trend30List.value.map((item) => item.date)
    const counts = trend30List.value.map((item) => item.count)
    const minutes = trend30List.value.map((item) => item.minutes)
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['打卡次数', '学习时长(分钟)'], bottom: 20 },
      grid: { left: 50, right: 50, bottom: 70, top: 40 },
      xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 15 ? 45 : 0, interval: dates.length > 15 ? 2 : 0 } },
      yAxis: [{ type: 'value', name: '打卡次数', minInterval: 1 }, { type: 'value', name: '学习时长(分钟)' }],
      series: [
        { name: '打卡次数', type: 'bar', data: counts, itemStyle: { color: '#409EFF' } },
        { name: '学习时长(分钟)', type: 'line', yAxisIndex: 1, data: minutes, smooth: true, itemStyle: { color: '#67C23A' } }
      ]
    })
  } else {
    const dates = trendList.value.map((item) => item.date)
    const counts = trendList.value.map((item) => item.count)
    const minutes = trendList.value.map((item) => item.minutes)
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['打卡次数', '学习时长(分钟)'], bottom: 20 },
      grid: { left: 50, right: 50, bottom: 50, top: 40 },
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
}

const renderCalendar = () => {
  if (!calendarRef.value) return
  if (!calendarInstance) calendarInstance = echarts.init(calendarRef.value)
  const data = Array.isArray(calendarData.value.data) ? calendarData.value.data : []
  const max = Math.max(1, calendarData.value.maxPossible || 1)
  const year = new Date().getFullYear()
  calendarInstance.setOption({
    tooltip: { formatter: (params) => `${params.data[0]}<br/>打卡 ${params.data[1]} 次` },
    visualMap: { min: 0, max: max, orient: 'horizontal', left: 'center', bottom: 10, inRange: { color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127'] }, calculable: true },
    calendar: { top: 60, left: 60, right: 30, bottom: 50, range: year, cellSize: ['auto', 16], splitLine: { show: true, lineStyle: { color: '#d0d7de', width: 1 } }, dayLabel: { firstDay: 1, nameMap: 'ZH' }, monthLabel: { nameMap: 'ZH' }, yearLabel: { show: true, fontSize: 14 } },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: data, itemStyle: { borderColor: '#d0d7de', borderWidth: 1, borderRadius: 2 } }]
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
    renderCalendar()
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
  calendarInstance?.resize()
}

watch(trendList, () => {
  nextTick(renderTrendChart)
})

watch(trend30List, () => {
  nextTick(renderTrendChart)
})

watch(statsRange, () => {
  nextTick(renderTrendChart)
})

watch(goalList, () => {
  nextTick(renderPieChart)
})

watch(calendarData, () => {
  nextTick(renderCalendar)
})

watch(notifyEnabled, (val) => {
  localStorage.setItem('shiguang-notify-enabled', val ? 'true' : 'false')
  if (val) {
    askPermission()
  }
})

onMounted(async () => {
  await loadDashboard()
  nextTick(() => {
    renderTrendChart()
    renderPieChart()
    renderCalendar()
  })
  window.addEventListener('resize', handleResize)

  const granted = await askPermission()
  if (granted && shouldNotifyToday()) {
    setTimeout(() => {
      sendNotification('拾光计划', '今天的打卡完成了吗？点击进入打卡页面')
    }, 2000)
  }
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
  calendarInstance?.dispose()
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

    <div class="notify-row">
      <el-switch v-model="notifyEnabled" active-text="打卡提醒" />
    </div>

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
        <div class="card-title">打卡日历</div>
      </template>
      <div ref="calendarRef" class="chart-container chart-calendar"></div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header-row">
          <span class="card-title">打卡趋势</span>
          <el-radio-group v-model="statsRange" size="small">
            <el-radio-button value="7">7 天</el-radio-button>
            <el-radio-button value="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
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

.notify-row {
  display: flex;
  align-items: center;
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
  height: 380px;
}

.chart-calendar {
  height: 320px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
