import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

request.interceptors.request.use((config) => config)

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
  (error) => {
    const message = error?.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
