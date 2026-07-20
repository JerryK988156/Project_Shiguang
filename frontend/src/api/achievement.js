import request from '@/utils/request'

export const getAchievementListApi = () => request({
  url: '/achievement/list',
  method: 'get'
})
