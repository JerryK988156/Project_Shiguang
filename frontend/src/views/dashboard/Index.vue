<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

import { getStatOverviewApi, getStatTrend7Api, getStatTrend30Api, getGoalProgressApi, getCheckinCalendarApi } from '@/api/stat'
import { askPermission, sendNotification, shouldNotifyToday } from '@/utils/notification'
import { Tickets, Calendar, Timer, TrendCharts } from '@element-plus/icons-vue'

const overview = ref({
  totalGoals: 0,
  totalCheckins: 0,
  totalMinutes: 0,
  streakDays: 0
})

const animatedNumbers = ref({
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

const animateValue = (key, target) => {
  const duration = 800
  const start = animatedNumbers.value[key]
  const diff = target - start
  const startTime = performance.now()
  
  const step = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedNumbers.value[key] = Math.round(start + diff * eased)
    if (progress < 1) {
      requestAnimationFrame(step)
    }
  }
  requestAnimationFrame(step)
}

watch(overview, (val) => {
  if (val.totalGoals > 0 || val.totalCheckins > 0) {
    animateValue('totalGoals', val.totalGoals)
    animateValue('totalCheckins', val.totalCheckins)
    animateValue('totalMinutes', val.totalMinutes)
    animateValue('streakDays', val.streakDays)
  }
}, { deep: true })

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
  <div class="dashboard-page">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-title">学习概览</div>
      </template>
      <p>这里展示当前账号的目标执行情况、累计打卡和近 7 天趋势。</p>
    </el-card>

    <div class="notify-row">
      <el-switch v-model="notifyEnabled" active-text="打卡提醒" />
    </div>

    <!-- 骨架屏 -->
    <template v-if="loading">
      <div class="info-grid">
        <div v-for="i in 4" :key="i" class="info-card skeleton-pulse">
          <el-skeleton animated>
            <template #template>
              <div style="display:flex;align-items:center;gap:16px;padding:8px 0">
                <el-skeleton-item variant="circle" style="width:52px;height:52px" />
                <div style="flex:1">
                  <el-skeleton-item variant="text" style="width:60px;margin-bottom:8px" />
                  <el-skeleton-item variant="text" style="width:80px;height:32px" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
      <el-card><div class="chart-container"><el-skeleton animated :rows="6" /></div></el-card>
      <el-card><div class="chart-container chart-calendar"><el-skeleton animated :rows="4" /></div></el-card>
      <el-card><div class="chart-container"><el-skeleton animated :rows="6" /></div></el-card>
    </template>

    <!-- 实际内容 -->
    <template v-else>
      <div class="info-grid">
        <div class="info-card">
          <div class="info-icon" style="background: #eef2ff; color: #6366f1;">
            <el-icon :size="24"><Tickets /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">目标总数</div>
            <div class="info-value">{{ animatedNumbers.totalGoals }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon" style="background: #d1fae5; color: #10b981;">
            <el-icon :size="24"><Calendar /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">打卡总数</div>
            <div class="info-value">{{ animatedNumbers.totalCheckins }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon" style="background: #fef3c7; color: #f59e0b;">
            <el-icon :size="24"><Timer /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">累计时长</div>
            <div class="info-value">{{ animatedNumbers.totalMinutes }} <span class="info-unit">分钟</span></div>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon" style="background: #fee2e2; color: #ef4444;">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">连续打卡</div>
            <div class="info-value">{{ animatedNumbers.streakDays }} <span class="info-unit">天</span></div>
          </div>
        </div>
      </div>

      <!-- 目标卡片列表 -->
      <el-card v-if="goalList.length > 0">
        <template #header><div class="card-title">我的目标</div></template>
        <div class="goal-cards">
          <div v-for="goal in goalList" :key="goal.id" class="goal-card-item">
            <div class="goal-card-left">
              <h4 class="goal-card-title">{{ goal.title }}</h4>
              <div class="goal-card-meta">
                <el-tag :type="goal.status === '已完成' ? 'success' : goal.status === '已放弃' ? 'info' : 'warning'" size="small">
                  {{ goal.status || '进行中' }}
                </el-tag>
                <span class="goal-card-days">{{ goal.completedDays || 0 }} / {{ goal.targetDays || 0 }} 天</span>
              </div>
            </div>
            <el-button
              v-if="goal.status !== '已完成' && goal.status !== '已放弃'"
              size="small"
              type="success"
              plain
              @click="router.push({ path: '/checkin/add', query: { goalId: goal.id } })"
            >打卡</el-button>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header><div class="card-title">目标状态分布</div></template>
        <div ref="pieChartRef" class="chart-container"></div>
      </el-card>
      <el-card>
        <template #header><div class="card-title">打卡日历</div></template>
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
    </template>
  </div>
</template>

<style scoped lang="scss">
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.info-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.info-value {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
}

.info-unit {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
}

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

.skeleton-pulse {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}
@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.goal-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.goal-card-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: box-shadow 0.2s;
}

.goal-card-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.goal-card-left {
  flex: 1;
  min-width: 0;
}

.goal-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goal-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goal-card-days {
  font-size: 13px;
  color: #64748b;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr !important;
  }

  .goal-cards {
    grid-template-columns: 1fr !important;
  }

  .chart-container {
    height: 260px;
  }

  .chart-calendar {
    height: 240px;
  }

  .info-value {
    font-size: 22px;
  }
}
</style>
