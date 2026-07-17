import request from '@/utils/request'

export const addCheckinApi = (data) => request({
  url: '/checkin/add',
  method: 'post',
  data
})

export const getCheckinListApi = (params) => request({
  url: '/checkin/list',
  method: 'get',
  params
})

export const getTodayCheckinApi = (params) => request({
  url: '/checkin/today',
  method: 'get',
  params
})

export const deleteCheckinApi = (id) => request({
  url: `/checkin/delete/${id}`,
  method: 'post'
})
