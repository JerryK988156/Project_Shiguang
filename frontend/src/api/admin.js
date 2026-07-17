import request from '@/utils/request'

export const getAdminOverviewApi = () => request({
  url: '/admin/stat/overview',
  method: 'get'
})

export const getAdminUserListApi = () => request({
  url: '/admin/user/list',
  method: 'get'
})

export const updateUserStatusApi = (id, status) => request({
  url: `/admin/user/status/${id}`,
  method: 'post',
  params: { status }
})
