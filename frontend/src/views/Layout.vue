<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { logoutApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => {
  if (userStore.isAdmin()) {
    return [
      { index: '/admin/dashboard', title: '首页看板', icon: 'House' },
      { index: '/admin/stat', title: '管理概览', icon: 'Histogram' },
      { index: '/user/profile', title: '个人中心', icon: 'User' }
    ]
  }

  return [
    { index: '/dashboard', title: '首页看板', icon: 'House' },
    { index: '/goal/list', title: '目标列表', icon: 'Tickets' },
    { index: '/goal/edit', title: '新建目标', icon: 'EditPen' },
    { index: '/checkin/add', title: '今日打卡', icon: 'Calendar' },
    { index: '/checkin/list', title: '打卡记录', icon: 'List' },
    { index: '/stat/personal', title: '个人统计', icon: 'DataAnalysis' },
    { index: '/user/profile', title: '个人中心', icon: 'User' }
  ]
})

const currentTitle = computed(() => {
  const current = menus.value.find((item) => item.index === router.currentRoute.value.path)
  return current?.title || '拾光计划'
})

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    // 会话失效时仍允许前端退出
  }
  userStore.resetUser()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside-panel">
      <div class="logo">拾光计划</div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header-panel">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-user">
          <el-avatar :size="32" :src="userStore.userInfo.avatar" class="header-avatar">
            {{ (userStore.userInfo.nickname || userStore.userInfo.username || '?').charAt(0) }}
          </el-avatar>
          <el-tag size="small" :type="userStore.isAdmin() ? 'danger' : 'primary'">
            {{ userStore.isAdmin() ? '管理员' : '普通用户' }}
          </el-tag>
          <span>{{ userStore.userInfo.nickname || userStore.userInfo.username || '未登录用户' }}</span>
          <el-button link type="primary" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout-container {
  min-height: 100vh;
}

.aside-panel {
  color: #fff;
  background: #111827;
}

.logo {
  padding: 28px 24px;
  font-size: 24px;
  font-weight: 700;
}

.menu {
  border-right: 0;
}

.header-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-panel {
  background: #f5f7fb;
}
</style>
