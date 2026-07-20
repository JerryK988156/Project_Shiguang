import request from '@/utils/request'

export const chatApi = (messages) => request({
  url: '/agent/chat',
  method: 'post',
  data: { messages }
})
