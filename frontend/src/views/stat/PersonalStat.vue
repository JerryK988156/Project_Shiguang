<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

import { getGoalProgressApi, getStatOverviewApi, getStatTrend7Api, getStatTrend30Api, getGoalTimeDistributionApi, getCheckinCalendarApi } from '@/api/stat'

const loading = ref(false)
const overview = ref({})
const trend7List = ref([])
const trend30List = ref([])
const goalProgressList = ref([])
const timeDistList = ref([])
const calendarData = ref({ data: [], maxCount: 1 })

const trend7ChartRef = ref(null)
const trend30ChartRef = ref(null)
const barChartRef = ref(null)
const timePieRef = ref(null)
const calendarRef = ref(null)
let trend7ChartInstance = null
let trend30ChartInstance = null
let barChartInstance = null
let timePieInstance = null
let calendarInstance = null

const loadStatData = async () => {
  loading.value = true
  try {
    const [overviewData, trend7Data, trend30Data, progressData, timeDistData, calData] = await Promise.all([
      getStatOverviewApi(),
      getStatTrend7Api(),
      getStatTrend30Api(),
      getGoalProgressApi(),
      getGoalTimeDistributionApi(),
      getCheckinCalendarApi()
    ])
    overview.value = overviewData || {}
    trend7List.value = Array.isArray(trend7Data) ? trend7Data : []
    trend30List.value = Array.isArray(trend30Data) ? trend30Data : []
    goalProgressList.value = Array.isArray(progressData) ? progressData : []
    timeDistList.value = Array.isArray(timeDistData) ? timeDistData : []
    calendarData.value = calData || { data: [], maxCount: 1 }
  } finally {
    loading.value = false
  }
}

const renderCalendar = () => {
  if (!calendarRef.value) return
  if (!calendarInstance) calendarInstance = echarts.init(calendarRef.value)
  const data = Array.isArray(calendarData.value.data) ? calendarData.value.data : []
  const max = Math.max(1, calendarData.value.maxCount || 1)
  const year = new Date().getFullYear()
  calendarInstance.setOption({
    tooltip: { formatter: (params) => `${params.data[0]}<br/>打卡 ${params.data[1]} 次` },
    visualMap: { min: 0, max: max, orient: 'horizontal', left: 'center', bottom: 10, inRange: { color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127'] }, calculable: true },
    calendar: { top: 60, left: 60, right: 30, bottom: 50, range: year, cellSize: ['auto', 16], splitLine: { show: true, lineStyle: { color: '#d0d7de', width: 1 } }, dayLabel: { firstDay: 1, nameMap: 'ZH' }, monthLabel: { nameMap: 'ZH' }, yearLabel: { show: true, fontSize: 14 } },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: data, itemStyle: { borderColor: '#d0d7de', borderWidth: 1, borderRadius: 2 } }]
  })
}

const renderTrend7Chart = () => {
  if (!trend7ChartRef.value) return
  if (!trend7ChartInstance) trend7ChartInstance = echarts.init(trend7ChartRef.value)
  const dates = trend7List.value.map((item) => item.date)
  const counts = trend7List.value.map((item) => item.count)
  const minutes = trend7List.value.map((item) => item.minutes)
  trend7ChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['打卡次数', '学习时长(分钟)'] },
    xAxis: { type: 'category', data: dates },
    yAxis: [{ type: 'value', name: '打卡次数', minInterval: 1 }, { type: 'value', name: '学习时长(分钟)' }],
    series: [
      { name: '打卡次数', type: 'line', data: counts, smooth: true, itemStyle: { color: '#409EFF' } },
      { name: '学习时长(分钟)', type: 'line', yAxisIndex: 1, data: minutes, smooth: true, itemStyle: { color: '#67C23A' } }
    ]
  })
}

const renderTrend30Chart = () => {
  if (!trend30ChartRef.value) return
  if (!trend30ChartInstance) trend30ChartInstance = echarts.init(trend30ChartRef.value)
  const dates = trend30List.value.map((item) => item.date)
  const counts = trend30List.value.map((item) => item.count)
  const minutes = trend30List.value.map((item) => item.minutes)
  trend30ChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['打卡次数', '学习时长(分钟)'] },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 15 ? 45 : 0, interval: dates.length > 15 ? 2 : 0 } },
    yAxis: [{ type: 'value', name: '打卡次数', minInterval: 1 }, { type: 'value', name: '学习时长(分钟)' }],
    series: [
      { name: '打卡次数', type: 'bar', data: counts, itemStyle: { color: '#409EFF' } },
      { name: '学习时长(分钟)', type: 'line', yAxisIndex: 1, data: minutes, smooth: true, itemStyle: { color: '#67C23A' } }
    ]
  })
}

const renderBarChart = () => {
  if (!barChartRef.value) return
  if (!barChartInstance) barChartInstance = echarts.init(barChartRef.value)
  const names = goalProgressList.value.map((item) => item.title)
  const pcts = goalProgressList.value.map((item) => Number(item.progressPct || 0))
  barChartInstance.setOption({
    tooltip: { trigger: 'axis', formatter: (params) => { const p = Array.isArray(params) ? params[0] : params; return `${p.name}<br/>完成率: ${p.value}%` } },
    xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 5 ? 30 : 0 } },
    yAxis: { type: 'value', name: '完成率 (%)', max: 100 },
    series: [{
      name: '完成率', type: 'bar', data: pcts,
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#409EFF' }, { offset: 1, color: '#79bbff' }]) },
      label: { show: true, position: 'top', formatter: '{c}%' }
    }]
  })
}

const renderTimePie = () => {
  if (!timePieRef.value) return
  if (!timePieInstance) timePieInstance = echarts.init(timePieRef.value)
  const pieData = timeDistList.value.filter((item) => item.totalMinutes > 0).map((item) => ({ name: item.title, value: item.totalMinutes }))
  timePieInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 分钟 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ name: '学习时长', type: 'pie', radius: ['40%', '70%'], center: ['55%', '50%'], data: pieData, label: { formatter: '{b}\n{c} 分钟' } }]
  })
}

const handleResize = () => {
  trend7ChartInstance?.resize()
  trend30ChartInstance?.resize()
  barChartInstance?.resize()
  timePieInstance?.resize()
  calendarInstance?.resize()
}

watch(trend7List, () => nextTick(renderTrend7Chart))
watch(trend30List, () => nextTick(renderTrend30Chart))
watch(goalProgressList, () => nextTick(renderBarChart))
watch(timeDistList, () => nextTick(renderTimePie))
watch(calendarData, () => nextTick(renderCalendar))

onMounted(async () => {
  await loadStatData()
  nextTick(() => { renderTrend7Chart(); renderTrend30Chart(); renderBarChart(); renderTimePie(); renderCalendar() })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trend7ChartInstance?.dispose()
  trend30ChartInstance?.dispose()
  barChartInstance?.dispose()
  timePieInstance?.dispose()
  calendarInstance?.dispose()
})
</script>

<template>
  <div class="page-container stat-page" v-loading="loading">
    <div class="info-grid">
      <el-card><div class="info-label">累计学习时长</div><div class="info-value">{{ overview.totalMinutes || 0 }} 分钟</div></el-card>
      <el-card><div class="info-label">累计打卡次数</div><div class="info-value">{{ overview.totalCheckinCount || 0 }}</div></el-card>
      <el-card><div class="info-label">连续打卡天数</div><div class="info-value">{{ overview.currentStreak || 0 }} 天</div></el-card>
      <el-card><div class="info-label">目标总数</div><div class="info-value">{{ overview.goalCount || 0 }}</div></el-card>
    </div>

    <el-card>
      <template #header><span>打卡日历</span></template>
      <div ref="calendarRef" class="chart-container chart-calendar"></div>
    </el-card>

    <el-card>
      <template #header><span>近 7 天趋势</span></template>
      <div ref="trend7ChartRef" class="chart-container"></div>
    </el-card>

    <el-card>
      <template #header><span>近 30 天趋势</span></template>
      <div ref="trend30ChartRef" class="chart-container"></div>
    </el-card>

    <el-card>
      <template #header><span>目标完成进度</span></template>
      <div ref="barChartRef" class="chart-container"></div>
    </el-card>

    <el-card>
      <template #header><span>各目标学习时长占比</span></template>
      <div ref="timePieRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.stat-page { display: grid; gap: 20px; }
.info-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px; }
.info-label { margin-bottom: 12px; color: #6b7280; }
.info-value { font-size: 24px; font-weight: 700; color: #111827; }
.chart-container { width: 100%; height: 360px; }
.chart-calendar { height: 320px; }
</style>
