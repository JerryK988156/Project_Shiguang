import request from '@/utils/request'

export const getGoalListApi = (params) => request({
  url: '/goal/list',
  method: 'get',
  params
})

export const getGoalDetailApi = (id) => request({
  url: `/goal/detail/${id}`,
  method: 'get'
})

export const addGoalApi = (data) => request({
  url: '/goal/add',
  method: 'post',
  data
})

export const updateGoalApi = (data) => request({
  url: '/goal/update',
  method: 'put',
  data
})

export const deleteGoalApi = (id) => request({
  url: `/goal/delete/${id}`,
  method: 'delete'
})

export const updateGoalStatusApi = (id, status) => request({
  url: `/goal/status/${id}`,
  method: 'put',
  params: { status }
})
