<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { logoutApi } from '@/api/auth'
import FloatingChat from '@/components/FloatingChat.vue'

const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value < 768)
const mobileMenuOpen = ref(false)

function onResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

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
    <el-aside :class="['aside-panel', { 'mobile-closed': isMobile && !mobileMenuOpen, 'mobile-open': isMobile && mobileMenuOpen }]" :style="isMobile ? {} : { width: '220px' }">
      <div class="logo">拾光计划</div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="right-container">
      <el-header class="header-panel">
        <div class="header-left">
          <el-button v-if="isMobile" :icon="mobileMenuOpen ? 'Close' : 'Menu'" text @click="mobileMenuOpen = !mobileMenuOpen" />
          <div class="header-title">{{ currentTitle }}</div>
        </div>
        <div class="header-user">
          <el-avatar :size="32" :src="userStore.userInfo.avatar" class="header-avatar">
            {{ (userStore.userInfo.nickname || userStore.userInfo.username || '?').charAt(0) }}
          </el-avatar>
          <el-tag size="small" :type="userStore.isAdmin() ? 'danger' : 'primary'">
            {{ userStore.isAdmin() ? '管理员' : '普通用户' }}
          </el-tag>
          <span>{{ userStore.userInfo.nickname || userStore.userInfo.username || '未登录用户' }}</span>
          <el-button type="danger" size="small" plain @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
    <div v-if="isMobile && mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    <FloatingChat />
  </el-container>
</template>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.aside-panel {
  color: var(--text-inverse);
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  overflow-y: auto;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.logo {
  padding: 28px 24px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #818cf8, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.menu {
  border-right: 0 !important;
  background: transparent !important;
  
  .el-menu-item {
    margin: 4px 12px;
    border-radius: 10px;
    color: #94a3b8 !important;
    font-weight: 500;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      background: linear-gradient(180deg, #818cf8, #6366f1);
      border-radius: 0 3px 3px 0;
      transition: height 0.3s ease;
    }
    
    &:hover {
      color: #e2e8f0 !important;
      background: rgba(255, 255, 255, 0.06) !important;
      
      &::before {
        height: 50%;
      }
    }
    
    &.is-active {
      color: #fff !important;
      background: rgba(99, 102, 241, 0.2) !important;
      font-weight: 600;
      
      &::before {
        height: 60%;
      }
      
      .el-icon {
        color: #818cf8;
      }
    }
    
    .el-icon {
      transition: color 0.3s ease;
    }
  }
}

.header-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  height: var(--header-height);
  padding: 0 var(--spacing-lg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .header-avatar {
    border: 2px solid var(--border-color);
    transition: border-color 0.3s;
    
    &:hover {
      border-color: var(--color-primary-light);
    }
  }
}

.main-panel {
  background: var(--bg-page);
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.right-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

// Mobile responsive
@media (max-width: 768px) {
  .aside-panel {
    position: fixed !important;
    z-index: 1000;
    height: 100vh;
    width: 220px !important;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &.mobile-open {
      transform: translateX(0);
    }
  }

  .mobile-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 999;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .main-panel {
    padding: 12px !important;
  }
}
</style>
