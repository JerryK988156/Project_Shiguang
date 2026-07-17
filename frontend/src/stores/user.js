import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('shiguang-token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('shiguang-user') || 'null') || {
    id: '',
    username: '',
    nickname: '',
    role: 'user',
    avatar: '',
    profile: ''
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
      id: value?.id || userInfo.value.id,
      username: value?.username || userInfo.value.username,
      nickname: value?.nickname || userInfo.value.nickname,
      role: value?.role || userInfo.value.role || 'user',
      avatar: value?.avatar !== undefined ? value.avatar : userInfo.value.avatar,
      profile: value?.profile !== undefined ? value.profile : userInfo.value.profile
    }
    localStorage.setItem('shiguang-user', JSON.stringify(userInfo.value))
  }

  const updateAvatar = (url) => {
    userInfo.value.avatar = url
    localStorage.setItem('shiguang-user', JSON.stringify(userInfo.value))
  }

  const isAdmin = () => userInfo.value.role === 'admin'

  const isSuperAdmin = () => userInfo.value.role === 'admin' && Number(userInfo.value.id) === 1

  const isLoggedIn = () => Boolean(token.value)

  const hasUserInfo = () => Boolean(userInfo.value.username)

  const hydrateSession = () => {
    token.value = localStorage.getItem('shiguang-token') || ''
    userInfo.value = JSON.parse(localStorage.getItem('shiguang-user') || 'null') || {
      id: '',
      username: '',
      nickname: '',
      role: 'user',
      avatar: '',
      profile: ''
    }
  }

  const resetUser = () => {
    clearToken()
    userInfo.value = {
      id: '',
      username: '',
      nickname: '',
      role: 'user',
      avatar: '',
      profile: ''
    }
    localStorage.removeItem('shiguang-user')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    updateAvatar,
    isAdmin,
    isSuperAdmin,
    isLoggedIn,
    hasUserInfo,
    hydrateSession,
    resetUser
  }
})
