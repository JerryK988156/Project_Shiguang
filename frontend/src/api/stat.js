import request from '@/utils/request'

export const getStatOverviewApi = () => request({
  url: '/stat/overview',
  method: 'get'
})

export const getStatTrend7Api = () => request({
  url: '/stat/trend7',
  method: 'get'
})

export const getStatTrend30Api = () => request({
  url: '/stat/trend30',
  method: 'get'
})

export const getGoalProgressApi = () => request({
  url: '/stat/goalProgress',
  method: 'get'
})

export const getGoalTimeDistributionApi = () => request({
  url: '/stat/goalTimeDistribution',
  method: 'get'
})

export const getCheckinCalendarApi = () => request({
  url: '/stat/checkinCalendar',
  method: 'get'
})

export const getTagStatsApi = () => request({
  url: '/stat/tagStats',
  method: 'get'
})

export const getWeeklyReportApi = () => request({
  url: '/stat/weeklyReport',
  method: 'get'
})
