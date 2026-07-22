import request from '@/utils/request'

export const loadHistory = (sessionId) => request({
  url: '/agent/history',
  method: 'get',
  params: { sessionId }
})

export const chatApi = (messages) => request({
  url: '/agent/chat',
  method: 'post',
  data: { messages }
})

export const chatStreamApi = (messages, onToken, onDone, onError) => {
  const token = localStorage.getItem('shiguang-token')

  fetch('/api/agent/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({ messages })
  }).then(async (response) => {
    if (!response.ok) throw new Error('Stream failed')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) { onDone(); break }
      
      buffer += decoder.decode(value, { stream: true })
      
      // 解析 SSE 数据行
      const lines = buffer.split('\n')
      // 保留最后一个不完整的行
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          // 跳过空值和结束标记
          if (!data || data === 'null' || data === '[DONE]') continue
          onToken(data)
        }
      }
    }
  }).catch(onError)
}
