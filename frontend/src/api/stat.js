import request from '@/utils/request'

export const getStatOverviewApi = () => request({
  url: '/stat/overview',
  method: 'get'
})

export const getStatTrend7Api = () => request({
  url: '/stat/trend7',
  method: 'get'
})

export const getGoalProgressApi = () => request({
  url: '/stat/goalProgress',
  method: 'get'
})
