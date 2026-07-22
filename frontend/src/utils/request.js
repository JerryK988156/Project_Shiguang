import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

let isRefreshing = false
let pendingRequests = []

const resolvePendingRequests = (token) => {
  pendingRequests.forEach((cb) => cb(token))
  pendingRequests = []
}

const rejectPendingRequests = () => {
  pendingRequests.forEach((cb) => cb(null))
  pendingRequests = []
}

const refreshToken = async () => {
  const refreshTokenValue = localStorage.getItem('shiguang-refresh-token')
  if (!refreshTokenValue) return null
  try {
    const res = await axios.post('/api/auth/refresh', { refreshToken: refreshTokenValue })
    const data = res.data
    if (data.code === 200 && data.data) {
      const { accessToken, refreshToken: newRefreshToken } = data.data
      if (accessToken) localStorage.setItem('shiguang-token', accessToken)
      if (newRefreshToken) localStorage.setItem('shiguang-refresh-token', newRefreshToken)
      return accessToken
    }
    return null
  } catch {
    return null
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('shiguang-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (!payload || typeof payload !== 'object' || !Object.prototype.hasOwnProperty.call(payload, 'code')) {
      return payload
    }

    if (payload.code === 200) {
      return payload.data
    }

    if (payload.code === 401) {
      localStorage.removeItem('shiguang-token')
      localStorage.removeItem('shiguang-user')
    }

    ElMessage.error(payload.message || '请求失败')
    return Promise.reject(new Error(payload.message || '请求失败'))
  },
  async (error) => {
    const originalRequest = error.config
    const status = error?.response?.status

    // Handle 401 with token refresh (max 2 retries)
    const retryCount = originalRequest._retryCount || 0
    if (status === 401 && retryCount < 2) {
      if (isRefreshing) {
        // Wait for the ongoing refresh to complete
        return new Promise((resolve, reject) => {
          pendingRequests.push((newToken) => {
            if (newToken) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(request(originalRequest))
            } else {
              reject(error)
            }
          })
        })
      }

      originalRequest._retryCount = retryCount + 1
      isRefreshing = true

      try {
        const newToken = await refreshToken()
        if (newToken) {
          resolvePendingRequests(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        } else {
          rejectPendingRequests()
          localStorage.removeItem('shiguang-token')
          localStorage.removeItem('shiguang-refresh-token')
          localStorage.removeItem('shiguang-user')
          window.location.href = '/login'
          return Promise.reject(error)
        }
      } catch {
        rejectPendingRequests()
        localStorage.removeItem('shiguang-token')
        localStorage.removeItem('shiguang-refresh-token')
        localStorage.removeItem('shiguang-user')
        window.location.href = '/login'
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    const message = error?.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
