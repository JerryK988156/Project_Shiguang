import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('shiguang-token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('shiguang-user') || 'null') || {
    id: '',
    username: '',
    nickname: '',
    role: 'user'
  })

  const setToken = (value) => {
    token.value = value
    if (value) {
      localStorage.setItem('shiguang-token', value)
    } else {
      localStorage.removeItem('shiguang-token')
    }
  }

  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('shiguang-token')
  }

  const setUserInfo = (value) => {
    userInfo.value = {
      id: value?.id || '',
      username: value?.username || '',
      nickname: value?.nickname || '',
      role: value?.role || 'user'
    }
    localStorage.setItem('shiguang-user', JSON.stringify(userInfo.value))
  }

  const isAdmin = () => userInfo.value.role === 'admin'

  const isLoggedIn = () => Boolean(token.value)

  const hasUserInfo = () => Boolean(userInfo.value.username)

  const hydrateSession = () => {
    token.value = localStorage.getItem('shiguang-token') || ''
    userInfo.value = JSON.parse(localStorage.getItem('shiguang-user') || 'null') || {
      id: '',
      username: '',
      nickname: '',
      role: 'user'
    }
  }

  const resetUser = () => {
    clearToken()
    userInfo.value = {
      id: '',
      username: '',
      nickname: '',
      role: 'user'
    }
    localStorage.removeItem('shiguang-user')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    isAdmin,
    isLoggedIn,
    hasUserInfo,
    hydrateSession,
    resetUser
  }
})
