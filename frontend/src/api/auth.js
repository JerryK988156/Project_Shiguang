import request from '@/utils/request'

export const loginApi = (data) => request({
  url: '/auth/login',
  method: 'post',
  data
})

export const logoutApi = () => request({
  url: '/auth/logout',
  method: 'post'
})

export const getCurrentUserApi = () => request({
  url: '/auth/userInfo',
  method: 'get'
})

export const updateProfileApi = (data) => request({
  url: '/user/profile',
  method: 'put',
  data
})

export const updatePasswordApi = (data) => request({
  url: '/user/password',
  method: 'put',
  data
})
